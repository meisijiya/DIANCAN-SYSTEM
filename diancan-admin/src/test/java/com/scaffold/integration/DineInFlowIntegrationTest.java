package com.scaffold.integration;

import com.scaffold.DiancanAdminApplication;
import com.scaffold.modules.cart.dto.CartItemDTO;
import com.scaffold.modules.cart.service.CartService;
import com.scaffold.modules.cart.vo.CartVO;
import com.scaffold.modules.dish.dto.DishCategoryCreateDTO;
import com.scaffold.modules.dish.dto.DishCreateDTO;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.entity.DishCategory;
import com.scaffold.modules.dish.service.DishCategoryService;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.kitchen.service.KitchenService;
import com.scaffold.modules.order.dto.OrderCreateDTO;
import com.scaffold.modules.order.dto.AdminOrderCreateDTO;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.order.vo.OrderItemVO;
import com.scaffold.modules.order.vo.OrderVO;
import com.scaffold.modules.payment.dto.CashPayDTO;
import com.scaffold.modules.payment.service.PaymentService;
import com.scaffold.modules.payment.vo.CashPayVO;
import com.scaffold.modules.table.dto.TableCreateDTO;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.service.DiningTableService;
import com.scaffold.modules.table.vo.DiningTableVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 19.1 堂食正餐完整流程集成测试：
 * 扫码开台 -> 浏览菜品 -> 加入购物车 -> 提交订单 ->
 * 后厨接单/划单 -> 支付 -> 桌台待清洁 -> 标记清洁恢复空闲
 */
@SpringBootTest(classes = DiancanAdminApplication.class)
@ActiveProfiles("test")
class DineInFlowIntegrationTest {

    @Autowired
    private DishCategoryService dishCategoryService;

    @Autowired
    private DishService dishService;

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private KitchenService kitchenService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void dineInFullFlow_shouldWorkEndToEnd() {
        final String suffix = String.valueOf(System.currentTimeMillis());
        final String openid = "it-openid-" + suffix;

        // 1) 初始化测试数据：分类、菜品、桌台
        DishCategoryCreateDTO categoryDTO = new DishCategoryCreateDTO();
        categoryDTO.setName("IT分类-" + suffix);
        categoryDTO.setSort(1);
        dishCategoryService.createCategory(categoryDTO);

        DishCategory category = dishCategoryService.list().stream()
                .filter(c -> ("IT分类-" + suffix).equals(c.getName()))
                .findFirst()
                .orElseThrow();

        DishCreateDTO dishDTO = new DishCreateDTO();
        dishDTO.setCategoryId(category.getId());
        dishDTO.setName("IT菜品-" + suffix);
        dishDTO.setPrice(new BigDecimal("28.00"));
        dishDTO.setImage("/test/image.jpg");
        dishDTO.setThumbnail("/test/thumb.jpg");
        dishDTO.setSpiceLevel(1);
        dishDTO.setIngredients("[\"土豆\",\"牛肉\"]");
        dishDTO.setDescription("测试菜品");
        dishDTO.setStock(-1);
        dishDTO.setPreparationTime(10);
        dishService.createDish(dishDTO);

        Dish dish = dishService.list().stream()
                .filter(d -> ("IT菜品-" + suffix).equals(d.getName()))
                .findFirst()
                .orElseThrow();

        TableCreateDTO tableDTO = new TableCreateDTO();
        tableDTO.setCode("IT" + suffix.substring(Math.max(0, suffix.length() - 6)));
        tableDTO.setName("IT桌-" + suffix);
        tableDTO.setCapacity(4);
        tableDTO.setAreaName("测试区");
        diningTableService.createTable(tableDTO);

        DiningTable tableEntity = diningTableService.list().stream()
                .filter(t -> ("IT桌-" + suffix).equals(t.getName()))
                .findFirst()
                .orElseThrow();
        Long tableId = tableEntity.getId();

        // 2) 扫码开台（按 code 获取桌台 + 开台）
        DiningTableVO scannedTable = diningTableService.getByCode(tableDTO.getCode());
        assertEquals(tableId, scannedTable.getId());
        assertEquals(0, scannedTable.getStatus());

        diningTableService.openTable(tableId);
        DiningTable openedTable = diningTableService.getById(tableId);
        assertNotNull(openedTable);
        assertEquals(1, openedTable.getStatus());

        // 3) 浏览菜品：分类列表、在售列表、搜索
        assertTrue(
                dishCategoryService.listEnabled().stream().anyMatch(c -> c.getId().equals(category.getId())),
                "分类应在启用列表中"
        );

        Map<Long, List<com.scaffold.modules.dish.vo.DishListVO>> grouped = dishService.listOnSaleDishes();
        assertTrue(grouped.containsKey(category.getId()), "在售菜品应包含测试分类");
        assertTrue(grouped.get(category.getId()).stream().anyMatch(d -> d.getId().equals(dish.getId())), "在售菜品应包含测试菜品");

        assertTrue(
                dishService.searchDishes("IT菜品-" + suffix).stream().anyMatch(d -> d.getId().equals(dish.getId())),
                "搜索应返回测试菜品"
        );

        // 4) 加入购物车
        CartItemDTO cartItemDTO = new CartItemDTO();
        cartItemDTO.setDishId(dish.getId());
        cartItemDTO.setQuantity(2);
        cartItemDTO.setRemark("少辣");
        CartVO cart = cartService.addItem(openid, tableId, cartItemDTO);
        assertEquals(2, cart.getTotalCount());
        assertEquals(0, new BigDecimal("56.00").compareTo(cart.getTotalPrice()));

        // 5) 提交订单
        OrderCreateDTO orderCreateDTO = new OrderCreateDTO();
        orderCreateDTO.setTableId(tableId);
        orderCreateDTO.setPaymentMode(1); // 餐后付
        orderCreateDTO.setOrderType(0);   // 堂食
        orderCreateDTO.setRemark("集成测试订单");
        OrderVO order = orderService.createOrder(openid, orderCreateDTO);

        assertNotNull(order.getId());
        assertEquals(0, order.getStatus());
        assertEquals(tableId, order.getTableId());
        assertNotNull(order.getItems());
        assertFalse(order.getItems().isEmpty());
        assertTrue(order.getItems().stream().allMatch(i -> i.getStatus() == 0), "下单后订单项应为待制作");

        // 6) 后厨接单与划单
        Long firstItemId = order.getItems().get(0).getId();
        kitchenService.acceptTask(firstItemId);
        kitchenService.completeTask(firstItemId);

        OrderVO afterKitchen = orderService.getOrderDetail(order.getId());
        assertTrue(afterKitchen.getItems().stream().anyMatch(i -> i.getId().equals(firstItemId) && i.getStatus() == 2));

        // 若有多条订单项，全部划单完成
        for (OrderItemVO item : afterKitchen.getItems()) {
            if (item.getStatus() == 0) {
                kitchenService.acceptTask(item.getId());
                kitchenService.completeTask(item.getId());
            } else if (item.getStatus() == 1) {
                kitchenService.completeTask(item.getId());
            }
        }
        OrderVO allCompleted = orderService.getOrderDetail(order.getId());
        assertTrue(allCompleted.getItems().stream().allMatch(i -> i.getStatus() == 2), "应全部出餐完成");

        // 7) 支付（现金）并校验桌台状态变更为待清洁
        CashPayDTO cashPayDTO = new CashPayDTO();
        cashPayDTO.setOrderId(order.getId());
        cashPayDTO.setReceivedAmount(allCompleted.getActualAmount().add(new BigDecimal("10.00")));
        CashPayVO payVO = paymentService.cashPay(cashPayDTO);
        assertNotNull(payVO.getId());
        assertEquals(1, payVO.getStatus());

        OrderVO paidOrder = orderService.getOrderDetail(order.getId());
        assertEquals(1, paidOrder.getStatus(), "支付后订单应为已支付");

        DiningTable toCleanTable = diningTableService.getById(tableId);
        assertNotNull(toCleanTable);
        assertEquals(3, toCleanTable.getStatus(), "支付后桌台应为待清洁");

        // 8) 标记清洁，桌台恢复空闲
        diningTableService.markClean(tableId);
        DiningTable cleanedTable = diningTableService.getById(tableId);
        assertNotNull(cleanedTable);
        assertEquals(0, cleanedTable.getStatus(), "清洁后桌台应恢复空闲");
    }

    @Test
    void adminCreateOrder_shouldBeIdempotentByClientOrderNo() {
        final String suffix = String.valueOf(System.currentTimeMillis());

        DishCategoryCreateDTO categoryDTO = new DishCategoryCreateDTO();
        categoryDTO.setName("幂等分类-" + suffix);
        dishCategoryService.createCategory(categoryDTO);
        DishCategory category = dishCategoryService.list().stream()
                .filter(c -> ("幂等分类-" + suffix).equals(c.getName()))
                .findFirst()
                .orElseThrow();

        DishCreateDTO dishDTO = new DishCreateDTO();
        dishDTO.setCategoryId(category.getId());
        dishDTO.setName("幂等菜品-" + suffix);
        dishDTO.setPrice(new BigDecimal("18.00"));
        dishDTO.setImage("/test/i.jpg");
        dishDTO.setThumbnail("/test/t.jpg");
        dishDTO.setStock(-1);
        dishDTO.setPreparationTime(8);
        dishService.createDish(dishDTO);
        Dish dish = dishService.list().stream()
                .filter(d -> ("幂等菜品-" + suffix).equals(d.getName()))
                .findFirst()
                .orElseThrow();

        TableCreateDTO tableDTO = new TableCreateDTO();
        tableDTO.setCode("IDM" + suffix.substring(Math.max(0, suffix.length() - 5)));
        tableDTO.setName("幂等桌-" + suffix);
        tableDTO.setCapacity(4);
        diningTableService.createTable(tableDTO);
        DiningTable table = diningTableService.list().stream()
                .filter(t -> ("幂等桌-" + suffix).equals(t.getName()))
                .findFirst()
                .orElseThrow();

        String clientOrderNo = "OFF" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        AdminOrderCreateDTO dto = new AdminOrderCreateDTO();
        dto.setTableId(table.getId());
        dto.setTableCode(table.getCode());
        dto.setClientOrderNo(clientOrderNo);
        dto.setPaymentMode(1);
        dto.setOrderType(0);
        dto.setPreOrder(true);

        AdminOrderCreateDTO.AdminOrderItemDTO item = new AdminOrderCreateDTO.AdminOrderItemDTO();
        item.setDishId(dish.getId());
        item.setQuantity(1);
        dto.setItems(List.of(item));

        OrderVO first = orderService.createAdminOrder(dto);
        OrderVO second = orderService.createAdminOrder(dto);

        assertNotNull(first.getId());
        assertEquals(first.getId(), second.getId(), "同 clientOrderNo 重复提交应返回同一订单");
        assertEquals(clientOrderNo, first.getOrderNo());
        assertEquals(clientOrderNo, second.getOrderNo());
    }
}
