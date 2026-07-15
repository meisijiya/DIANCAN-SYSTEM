package com.scaffold.modules.kitchen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.common.enums.WsEventType;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.websocket.WsService;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.kitchen.service.KitchenService;
import com.scaffold.modules.kitchen.vo.KitchenTaskVO;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.entity.OrderItem;
import com.scaffold.modules.order.mapper.OrderItemMapper;
import com.scaffold.modules.order.mapper.OrderMapper;
import com.scaffold.modules.system.service.SysConfigService;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.mapper.DiningTableMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 后厨任务服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KitchenServiceImpl implements KitchenService {

    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final DiningTableMapper diningTableMapper;
    private final DishService dishService;
    private final WsService wsService;
    private final SysConfigService sysConfigService;

    private static final String AUTO_ACCEPT_CONFIG_KEY = "kitchen.autoAccept";
    private static final String AUTO_ACCEPT_CONFIG_NAME = "后厨自动接单";
    private static final String AUTO_ACCEPT_CONFIG_REMARK = "控制新堂食订单是否自动接单";

    @Override
    public List<KitchenTaskVO> getTaskList() {
        // 1. 查询待制作(0)和制作中(1)的订单项，按 addedAt 升序
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getStatus, 0, 1)
                .eq(OrderItem::getDeleted, 0)
                .orderByAsc(OrderItem::getAddedAt);
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量查询关联的订单（待支付+已支付的活跃订单）
        Set<Long> orderIds = items.stream()
                .map(OrderItem::getOrderId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getId, orderIds)
                .in(Order::getStatus, 0, 1)
                .eq(Order::getDeleted, 0);
        List<Order> orders = orderMapper.selectList(orderWrapper);
        Map<Long, Order> orderMap = orders.stream()
                .collect(Collectors.toMap(Order::getId, o -> o));

        // 批量读取桌台区域，供后厨播报区域和桌号。
        Set<Long> tableIds = orders.stream()
                .map(Order::getTableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DiningTable> tableMap = tableIds.isEmpty()
                ? Collections.emptyMap()
                : diningTableMapper.selectBatchIds(tableIds).stream()
                        .collect(Collectors.toMap(DiningTable::getId, table -> table));

        // 3. 批量查询关联的菜品（获取 preparationTime）
        Set<Long> dishIds = items.stream()
                .map(OrderItem::getDishId)
                .collect(Collectors.toSet());
        Map<Long, Dish> dishMap = new HashMap<>();
        for (Long dishId : dishIds) {
            Dish dish = dishService.getById(dishId);
            if (dish != null) {
                dishMap.put(dishId, dish);
            }
        }

        // 4. 组装 VO，仅包含活跃订单中的订单项
        LocalDateTime now = LocalDateTime.now();
        List<KitchenTaskVO> result = new ArrayList<>();
        for (OrderItem item : items) {
            Order order = orderMap.get(item.getOrderId());
            if (!isVisibleKitchenOrder(order)) {
                // 餐前付订单必须完成支付后才能进入后厨，餐后付订单允许待支付时制作。
                continue;
            }
            KitchenTaskVO vo = new KitchenTaskVO();
            vo.setId(item.getId());
            vo.setOrderId(item.getOrderId());
            vo.setOrderNo(order.getOrderNo());
            vo.setTableCode(order.getTableCode());
            DiningTable table = tableMap.get(order.getTableId());
            vo.setAreaName(table == null ? null : table.getAreaName());
            vo.setPaymentMode(order.getPaymentMode());
            vo.setDishId(item.getDishId());
            vo.setDishName(item.getDishName());
            vo.setDishImage(item.getDishImage());
            vo.setQuantity(item.getQuantity());
            vo.setRemark(item.getRemark());
            vo.setStatus(item.getStatus());
            vo.setAddedAt(item.getAddedAt());

            // 设置制作时限和超时标记
            Dish dish = dishMap.get(item.getDishId());
            if (dish != null && dish.getPreparationTime() != null) {
                vo.setPreparationTime(dish.getPreparationTime());
                vo.setOvertime(item.getAddedAt() != null
                        && now.isAfter(item.getAddedAt().plusMinutes(dish.getPreparationTime())));
            } else {
                vo.setPreparationTime(null);
                vo.setOvertime(false);
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 判断订单是否允许展示在后厨任务列表
     *
     * @author Henfon
     * @date 2026-07-15
     * @description 餐前付订单仅在支付完成后展示，餐后付和历史未标记支付模式的订单允许先制作后结账。
     * @param order 订单
     * @return true 表示允许进入后厨任务列表
     */
    private boolean isVisibleKitchenOrder(Order order) {
        if (order == null) {
            return false;
        }
        boolean prepayOrder = order.getPaymentMode() != null && order.getPaymentMode() == 0;
        return !prepayOrder || (order.getStatus() != null && order.getStatus() == 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTask(Long itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(ResultCode.ORDER_ITEM_NOT_FOUND);
        }
        if (item.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单项状态不是待制作，无法接单");
        }
        item.setStatus(1);
        orderItemMapper.updateById(item);
        log.info("后厨接单，订单项ID: {}", itemId);
    }

    /**
     * 判断自动接单是否开启
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 从系统配置读取后厨自动接单开关，未配置时默认关闭。
     * @return 是否开启自动接单
     */
    @Override
    public boolean isAutoAcceptEnabled() {
        String value = sysConfigService.getConfigValue(AUTO_ACCEPT_CONFIG_KEY);
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 更新自动接单开关
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 保存后厨自动接单开关，供新堂食订单自动流转到制作中。
     * @param enabled 是否开启
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAutoAcceptEnabled(boolean enabled) {
        sysConfigService.saveConfigValue(
                AUTO_ACCEPT_CONFIG_KEY,
                Boolean.toString(enabled),
                AUTO_ACCEPT_CONFIG_NAME,
                AUTO_ACCEPT_CONFIG_REMARK
        );
        log.info("后厨自动接单开关更新: enabled={}", enabled);
    }

    /**
     * 自动接单指定订单项
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 批量将待制作订单项推进到制作中，仅用于自动接单场景。
     * @param itemIds 订单项ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAcceptTasks(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        for (Long itemId : itemIds) {
            if (itemId == null) {
                continue;
            }

            OrderItem item = orderItemMapper.selectById(itemId);
            if (item == null || item.getDeleted() == 1) {
                continue;
            }
            if (item.getStatus() != 0) {
                continue;
            }

            // 自动接单仅把待制作推进到制作中，不干预其他状态。
            item.setStatus(1);
            orderItemMapper.updateById(item);
        }
        log.info("后厨自动接单完成: itemCount={}", itemIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long itemId) {
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || item.getDeleted() == 1) {
            throw new BusinessException(ResultCode.ORDER_ITEM_NOT_FOUND);
        }
        if (item.getStatus() != 1) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "订单项状态不是制作中，无法划单");
        }
        item.setStatus(2);
        orderItemMapper.updateById(item);
        log.info("后厨划单完成，订单项ID: {}", itemId);

        // 检查该订单的所有订单项是否均已完成
        checkAndNotifyAllCompleted(item.getOrderId());
    }

    /**
     * 检查订单所有订单项是否均已完成，若是则推送 ALL_COMPLETED 事件
     */
    private void checkAndNotifyAllCompleted(Long orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId)
                .eq(OrderItem::getDeleted, 0);
        List<OrderItem> allItems = orderItemMapper.selectList(wrapper);

        boolean allCompleted = !allItems.isEmpty()
                && allItems.stream().allMatch(i -> i.getStatus() == 2);

        if (allCompleted) {
            Order order = orderMapper.selectById(orderId);
            if (order != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("orderId", orderId);
                data.put("orderNo", order.getOrderNo());
                data.put("tableCode", order.getTableCode());
                wsService.broadcast(WsEventType.ALL_COMPLETED, "/topic/service", data);
                log.info("订单全部出餐，订单ID: {}, 订单号: {}", orderId, order.getOrderNo());
            }
        }
    }
}
