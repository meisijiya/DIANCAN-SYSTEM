package com.scaffold.modules.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.enums.WsEventType;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.cart.service.CartService;
import com.scaffold.modules.cart.vo.CartItemVO;
import com.scaffold.modules.cart.vo.CartVO;
import com.scaffold.modules.coupon.entity.UserCoupon;
import com.scaffold.modules.coupon.mapper.UserCouponMapper;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberLevelMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.vo.MemberPointsDeductionPreviewVO;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.order.dto.AddItemDTO;
import com.scaffold.modules.order.dto.AdminOrderCreateDTO;
import com.scaffold.modules.order.dto.AdminOrderEstimateDTO;
import com.scaffold.modules.order.dto.OrderCreateDTO;
import com.scaffold.modules.order.dto.OrderDiscountDTO;
import com.scaffold.modules.order.dto.OrderQueryDTO;
import com.scaffold.modules.order.dto.ReplaceItemDTO;
import com.scaffold.modules.order.dto.ReturnItemDTO;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.entity.OrderItem;
import com.scaffold.modules.order.entity.OrderOperationLog;
import com.scaffold.modules.order.mapper.OrderItemMapper;
import com.scaffold.modules.order.mapper.OrderMapper;
import com.scaffold.modules.order.mapper.OrderOperationLogMapper;
import com.scaffold.modules.kitchen.service.KitchenService;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.order.vo.AdminOrderEstimateVO;
import com.scaffold.modules.order.vo.OrderDetailVO;
import com.scaffold.modules.order.vo.OrderItemVO;
import com.scaffold.modules.order.vo.OrderOperationLogVO;
import com.scaffold.modules.order.vo.OrderVO;
import com.scaffold.modules.payment.entity.PaymentRecord;
import com.scaffold.modules.payment.mapper.PaymentRecordMapper;
import com.scaffold.modules.member.service.MemberBenefitService;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.service.MinioStorageService;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.service.DiningTableService;
import com.scaffold.modules.table.vo.DiningTableVO;
import com.scaffold.framework.websocket.WsService;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderItemMapper orderItemMapper;
    private final OrderOperationLogMapper orderOperationLogMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final UserCouponMapper userCouponMapper;
    private final DishService dishService;
    private final DiningTableService diningTableService;
    private final KitchenService kitchenService;
    private final PaymentRecordMapper paymentRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final MemberBenefitService memberBenefitService;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberPointsService memberPointsService;
    private final MinioStorageService minioStorageService;
    private final WsService wsService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ORDER_LOCK_PREFIX = "order:lock:";
    private static final long ORDER_LOCK_TTL_SECONDS = 10;
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class
    );
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String RUSH_KEY_PREFIX = "rush:";
    private static final long RUSH_TTL_MINUTES = 5;
    private static final String RETURN_AUTH_PASSWORD = "123456";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(String openid, OrderCreateDTO dto) {
        Long tableId = dto.getTableId();
        String lockKey = ORDER_LOCK_PREFIX + tableId;
        String lockToken = UUID.randomUUID().toString();
        Long userId = StpUtil.getLoginIdAsLong();

        // 获取分布式锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, ORDER_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "下单处理中，请稍后重试");
        }

        try {
            // 1. 获取购物车数据
            CartVO cart = cartService.getCart(openid, tableId);
            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new BusinessException(ResultCode.CART_EMPTY);
            }

            // 2. 获取桌台信息
            DiningTableVO table = getTableInfo(tableId);

            // 3. 校验菜品可用性并扣减库存
            List<Long> deductedDishIds = new ArrayList<>();
            List<Integer> deductedQuantities = new ArrayList<>();
            try {
                for (CartItemVO cartItem : cart.getItems()) {
                    validateAndDeductStock(cartItem.getDishId(), cartItem.getQuantity());
                    deductedDishIds.add(cartItem.getDishId());
                    deductedQuantities.add(cartItem.getQuantity());
                }
            } catch (Exception e) {
                // 回滚已扣减的库存
                rollbackStock(deductedDishIds, deductedQuantities);
                throw e;
            }

            // 4. 计算金额
            BigDecimal originalAmount = cart.getItems().stream()
                    .map(CartItemVO::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String tableSessionCode = resolveTableSessionCodeForOrder(tableId);

            // 5. 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setTableId(tableId);
            order.setTableCode(table.getCode());
            order.setTableSessionCode(tableSessionCode);
            order.setOriginalAmount(originalAmount);
            order.setDiscountRate(BigDecimal.ONE);
            order.setActualAmount(originalAmount);
            order.setPointsUsed(0);
            order.setPointsDiscountAmount(BigDecimal.ZERO);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setStatus(0); // 待支付
            order.setPaymentMode(dto.getPaymentMode());
            order.setOrderType(dto.getOrderType() != null ? dto.getOrderType() : 0);
            order.setRemark(dto.getRemark());
            order.setCustomerOpenid(openid);
            save(order);

            // 5.1 若携带积分抵现，则先锁定积分，再写入订单抵现快照
            if (dto.getUsePoints() != null && dto.getUsePoints() > 0) {
                order.setPointsUsed(dto.getUsePoints());
                memberBenefitService.adjustOrderPointsDeduction(order, userId);
                order.setActualAmount(calculateActualAmount(order, originalAmount));
                updateById(order);
            }

            // 5.1 若携带优惠券，则在下单事务中先锁券，再将券快照写入订单
            if (dto.getCouponId() != null) {
                UserCoupon lockedCoupon = couponService.lockCoupon(userId, dto.getCouponId(),
                        order.getActualAmount() == null ? originalAmount : order.getActualAmount(), order.getId());
                applyCouponSnapshot(order, lockedCoupon);
                order.setActualAmount(calculateActualAmount(order, originalAmount));
                updateById(order);
            }

            // 6. 创建订单项
            List<OrderItem> orderItems = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (CartItemVO cartItem : cart.getItems()) {
                OrderItem item = buildOrderItem(order.getId(), cartItem, now);
                orderItemMapper.insert(item);
                orderItems.add(item);
            }

            // 7. 清空购物车
            cartService.clearCart(openid, tableId);

            // 8. 构建返回 VO
            OrderVO orderVO = buildOrderVO(order, orderItems);

            // 9. 桌台状态：空闲 → 占用（自动开台）
            if (table.getStatus() != null && table.getStatus() == 0) {
                try {
                    diningTableService.openTable(tableId);
                } catch (Exception e) {
                    log.warn("自动开台失败: tableId={}, {}", tableId, e.getMessage());
                }
            }

            // 10. 餐后付订单下单后即可进后厨；餐前付订单需等支付成功后再推送。
            if (shouldNotifyKitchenBeforePayment(order)) {
                publishNewOrderNotifications(order, orderItems, false);
            }

            log.info("订单创建成功: orderNo={}, tableId={}, itemCount={}, amount={}",
                    order.getOrderNo(), tableId, orderItems.size(), originalAmount);

            return orderVO;
        } finally {
            // 释放分布式锁
            safeReleaseOrderLock(lockKey, lockToken);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createAdminOrder(AdminOrderCreateDTO dto) {
        Long tableId = dto.getTableId();
        String lockKey = ORDER_LOCK_PREFIX + tableId;
        String lockToken = UUID.randomUUID().toString();

        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ResultCode.CART_EMPTY, "菜品列表不能为空");
        }

        // 获取分布式锁
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, ORDER_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        if (locked == null || !locked) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "下单处理中，请稍后重试");
        }

        try {
            // 1. 获取桌台信息
            DiningTableVO table = getTableInfo(tableId);
            String tableCode = dto.getTableCode() != null ? dto.getTableCode() : table.getCode();
            boolean isPreOrder = dto.getPreOrder() != null && dto.getPreOrder();
            String tableSessionCode = resolveTableSessionCodeForOrder(tableId);

            // 非预订单只允许在空闲桌台新开单，避免待清洁/已结账桌台被误用
            if (!isPreOrder) {
                validateAdminCreateTableStatus(table, tableCode);
                validateAdminCreateOrderConflict(tableId, tableCode, tableSessionCode);
            }

            // 2. 校验菜品可用性并扣减库存
            List<Long> deductedDishIds = new ArrayList<>();
            List<Integer> deductedQuantities = new ArrayList<>();
            BigDecimal originalAmount = BigDecimal.ZERO;
            List<DishSnapshot> dishSnapshots = new ArrayList<>();

            try {
                for (AdminOrderCreateDTO.AdminOrderItemDTO itemDTO : dto.getItems()) {
                    Dish dish = validateAndDeductStock(itemDTO.getDishId(), itemDTO.getQuantity());
                    deductedDishIds.add(itemDTO.getDishId());
                    deductedQuantities.add(itemDTO.getQuantity());

                    BigDecimal itemAmount = dish.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
                    originalAmount = originalAmount.add(itemAmount);
                    dishSnapshots.add(new DishSnapshot(dish, itemDTO.getQuantity(), itemDTO.getRemark(), itemAmount));
                }
            } catch (Exception e) {
                rollbackStock(deductedDishIds, deductedQuantities);
                throw e;
            }

            // 3. 创建订单
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setTableId(tableId);
            order.setTableCode(tableCode);
            order.setTableSessionCode(tableSessionCode);
            order.setOriginalAmount(originalAmount);
            order.setDiscountRate(resolveMemberDiscountRate(dto.getUserId()));
            order.setActualAmount(originalAmount);
            order.setPointsUsed(0);
            order.setPointsDiscountAmount(BigDecimal.ZERO);
            order.setPaidAmount(BigDecimal.ZERO);
            order.setStatus(0); // 待支付
            order.setPaymentMode(dto.getPaymentMode());
            order.setOrderType(dto.getOrderType() != null ? dto.getOrderType() : 0);
            order.setRemark(dto.getRemark());
            order.setCustomerOpenid(loadUserOpenid(dto.getUserId()));
            order.setActualAmount(calculateActualAmount(order, originalAmount));
            save(order);

            if (dto.getUsePoints() != null && dto.getUsePoints() > 0) {
                if (dto.getUserId() == null) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "使用积分前请先选择会员");
                }
                order.setPointsUsed(dto.getUsePoints());
                memberBenefitService.adjustOrderPointsDeduction(order, dto.getUserId());
                order.setActualAmount(calculateActualAmount(order, originalAmount));
                updateById(order);
            }

            if (dto.getCouponId() != null) {
                if (dto.getUserId() == null) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "使用优惠券前请先选择会员");
                }
                UserCoupon lockedCoupon = couponService.lockCoupon(dto.getUserId(), dto.getCouponId(),
                        order.getActualAmount() == null ? originalAmount : order.getActualAmount(), order.getId());
                applyCouponSnapshot(order, lockedCoupon);
                order.setActualAmount(calculateActualAmount(order, originalAmount));
                updateById(order);
            }

            // 4. 创建订单项
            List<OrderItem> orderItems = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (DishSnapshot snapshot : dishSnapshots) {
                OrderItem item = new OrderItem();
                item.setOrderId(order.getId());
                item.setDishId(snapshot.dish.getId());
                item.setDishName(snapshot.dish.getName());
                item.setDishImage(snapshot.dish.getImage());
                item.setPrice(snapshot.dish.getPrice());
                item.setQuantity(snapshot.quantity);
                item.setAmount(snapshot.amount);
                item.setRemark(snapshot.remark);
                item.setStatus(0); // 待制作
                item.setIsGift(0);
                item.setAddedAt(now);
                orderItemMapper.insert(item);
                orderItems.add(item);
            }

            // 5. 构建返回 VO
            OrderVO orderVO = buildOrderVO(order, orderItems);

            // 6. 桌台状态：空闲 → 占用（自动开台）
            if (!isPreOrder && table.getStatus() != null && table.getStatus() == 0) {
                try {
                    diningTableService.openTable(tableId);
                } catch (Exception e) {
                    log.warn("自动开台失败: tableId={}, {}", tableId, e.getMessage());
                }
            }

            // 7. 非预订单模式：推送 WebSocket 通知
            if (!isPreOrder) {
                publishNewOrderNotifications(order, orderItems, true);
            }

            log.info("管理端订单创建成功: orderNo={}, tableId={}, preOrder={}, itemCount={}, amount={}",
                    order.getOrderNo(), tableId, isPreOrder, orderItems.size(), originalAmount);

            return orderVO;
        } finally {
            safeReleaseOrderLock(lockKey, lockToken);
        }
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：校验管理端新开单时桌台是否已存在进行中订单，避免同桌误开新单覆盖当前堂食流程
     *
     * @param tableId 桌台ID
     * @param tableCode 桌台编号
     * @param tableSessionCode 当前桌次编码
     */
    private void validateAdminCreateOrderConflict(Long tableId, String tableCode, String tableSessionCode) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getTableId, tableId)
                .eq(StrUtil.isNotBlank(tableSessionCode), Order::getTableSessionCode, tableSessionCode)
                .in(Order::getStatus, 0, 1)
                .eq(Order::getDeleted, 0)
                .orderByDesc(Order::getCreateTime)
                .last("LIMIT 1");

        Order activeOrder = getOne(wrapper, false);
        if (activeOrder == null) {
            return;
        }

        throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                String.format("桌台%s已有进行中订单，请前往桌台看板执行加菜，避免重复开单", tableCode));
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-04
     * 描述：校验管理端新开单时桌台状态，仅允许空闲桌台直接开台点单
     *
     * @param table 桌台信息
     * @param tableCode 桌台编号
     */
    private void validateAdminCreateTableStatus(DiningTableVO table, String tableCode) {
        if (table == null || table.getStatus() == null) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "桌台状态异常，请刷新后重试");
        }

        if (table.getStatus() == 0) {
            return;
        }

        if (table.getStatus() == 2) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    String.format("桌台%s已结账，请先流转到清洁完成后再开台点单", tableCode));
        }

        if (table.getStatus() == 3) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR,
                    String.format("桌台%s待清洁，请先完成清洁后再开台点单", tableCode));
        }
    }

    /**
     * 管理端订单试算
     *
     * @param dto 试算参数
     * @return 试算结果
     * @author Henfon
     * @date 2026-07-03
     * @description 预估管理端点单场景下的商品小计、积分抵扣、优惠券抵扣和应付金额
     */
    @Override
    public AdminOrderEstimateVO estimateAdminOrder(AdminOrderEstimateDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new BusinessException(ResultCode.CART_EMPTY, "菜品列表不能为空");
        }

        AdminOrderEstimateVO estimateVO = new AdminOrderEstimateVO();
        BigDecimal originalAmount = BigDecimal.ZERO;
        List<AdminOrderEstimateVO.AdminOrderEstimateItemVO> estimateItems = new ArrayList<>();

        for (AdminOrderEstimateDTO.AdminOrderEstimateItemDTO itemDTO : dto.getItems()) {
            Dish dish = validateAvailableDish(itemDTO.getDishId());
            BigDecimal itemAmount = dish.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            originalAmount = originalAmount.add(itemAmount);

            AdminOrderEstimateVO.AdminOrderEstimateItemVO itemVO = new AdminOrderEstimateVO.AdminOrderEstimateItemVO();
            itemVO.setDishId(dish.getId());
            itemVO.setDishName(dish.getName());
            itemVO.setUnitPrice(dish.getPrice());
            itemVO.setQuantity(itemDTO.getQuantity());
            itemVO.setAmount(itemAmount);
            estimateItems.add(itemVO);
        }

        originalAmount = originalAmount.setScale(2, RoundingMode.HALF_UP);
        estimateVO.setOriginalAmount(originalAmount);
        estimateVO.setItems(estimateItems);
        estimateVO.setRequestedPoints(Math.max(dto.getUsePoints() == null ? 0 : dto.getUsePoints(), 0));

        BigDecimal memberDiscountAmount = applyMemberDiscountPreview(estimateVO, dto.getUserId(), originalAmount);
        BigDecimal amountAfterMember = originalAmount.subtract(memberDiscountAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        MemberPointsDeductionPreviewVO pointsPreview = memberBenefitService.previewPointsDeduction(
                dto.getUserId(),
                amountAfterMember,
                dto.getUsePoints()
        );
        applyPointsPreview(estimateVO, pointsPreview);

        BigDecimal amountAfterPoints = amountAfterMember.subtract(estimateVO.getPointsDiscountAmount())
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        UserCoupon previewCoupon = loadPreviewCoupon(dto.getUserId(), dto.getCouponId(), amountAfterPoints);
        if (previewCoupon != null) {
            Order previewOrder = new Order();
            previewOrder.setCouponId(previewCoupon.getId());
            previewOrder.setCouponType(previewCoupon.getCouponType());
            previewOrder.setCouponName(previewCoupon.getCouponName());
            previewOrder.setCouponThresholdAmount(previewCoupon.getThresholdAmount());
            previewOrder.setCouponDiscountAmount(previewCoupon.getDiscountAmount());
            previewOrder.setCouponDiscountRate(previewCoupon.getDiscountRate());

            BigDecimal couponDeduction = calculateCouponDeduction(previewOrder, amountAfterPoints);
            estimateVO.setCouponId(previewCoupon.getId());
            estimateVO.setCouponName(previewCoupon.getCouponName());
            estimateVO.setCouponDiscountAmount(couponDeduction);
        }

        BigDecimal totalDiscount = estimateVO.getMemberDiscountAmount()
                .add(estimateVO.getPointsDiscountAmount())
                .add(estimateVO.getCouponDiscountAmount())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal payableAmount = originalAmount.subtract(totalDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        estimateVO.setDiscountAmount(totalDiscount);
        estimateVO.setPayableAmount(payableAmount);
        estimateVO.setTips(buildEstimateTips(estimateVO, dto));
        return estimateVO;
    }

    // ==================== 加菜与催单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO addItem(Long orderId, AddItemDTO dto) {
        // 1. 校验订单存在且状态为待支付
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可加菜");
        }

        // 2. 校验菜品可用性并扣减库存
        Dish dish = validateAndDeductStock(dto.getDishId(), dto.getQuantity());

        // 3. 创建新订单项
        LocalDateTime now = LocalDateTime.now();
        BigDecimal itemAmount = dish.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));

        OrderItem newItem = new OrderItem();
        newItem.setOrderId(orderId);
        newItem.setDishId(dish.getId());
        newItem.setDishName(dish.getName());
        newItem.setDishImage(dish.getImage());
        newItem.setPrice(dish.getPrice());
        newItem.setQuantity(dto.getQuantity());
        newItem.setAmount(itemAmount);
        newItem.setRemark(dto.getRemark());
        newItem.setStatus(0); // 待制作
        newItem.setIsGift(0);
        newItem.setAddedAt(now);
        orderItemMapper.insert(newItem);

        // 4. 重新计算订单金额（查询所有非赠送订单项）
        List<OrderItem> allItems = orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .eq(OrderItem::getDeleted, 0)
        );
        BigDecimal newOriginalAmount = allItems.stream()
                .map(OrderItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal newActualAmount = calculateActualAmount(order, newOriginalAmount);

        order.setOriginalAmount(newOriginalAmount);
        order.setActualAmount(newActualAmount);
        updateById(order);

        // 5. 构建返回 VO
        OrderVO orderVO = buildOrderVO(order, allItems);

        // 6. 餐后付未结账订单允许边点边做；餐前付订单的加菜需等待支付成功后统一通知后厨。
        if (shouldNotifyKitchenBeforePayment(order)) {
            publishNewOrderNotifications(order, List.of(newItem), false);
        }

        log.info("加菜成功: orderId={}, dishId={}, quantity={}, newAmount={}",
                orderId, dto.getDishId(), dto.getQuantity(), newActualAmount);

        return orderVO;
    }

    @Override
    public void rushItem(Long orderId, Long itemId) {
        // 1. 校验订单存在
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 2. 校验订单项存在且属于该订单
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND, "订单项不存在");
        }

        // 3. Redis 限流：5分钟内同一订单项仅允许催单一次
        String rushKey = RUSH_KEY_PREFIX + orderId + ":" + itemId;
        Boolean absent = redisTemplate.opsForValue().setIfAbsent(rushKey, "1", RUSH_TTL_MINUTES, TimeUnit.MINUTES);
        if (absent == null || !absent) {
            throw new BusinessException(ResultCode.RUSH_ORDER_LIMIT);
        }

        // 4. WebSocket 推送 RUSH_ORDER 事件
        var rushData = java.util.Map.of("orderId", orderId, "itemId", itemId,
                "dishName", item.getDishName(), "tableCode", order.getTableCode());
        wsService.broadcast(WsEventType.RUSH_ORDER, "/topic/kitchen", rushData);
        wsService.broadcast(WsEventType.RUSH_ORDER, "/topic/service", rushData);

        log.info("催单成功: orderId={}, itemId={}, dishName={}", orderId, itemId, item.getDishName());
    }

    // ==================== 订单操作（打折、赠送、退菜、换菜） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO discountOrder(Long orderId, OrderDiscountDTO dto) {
        // 1. 校验订单存在且状态为待支付
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可打折");
        }

        // 2. 设置折扣比例，重算 actualAmount
        BigDecimal discountRate = dto.getDiscountRate();
        order.setDiscountRate(discountRate);
        order.setActualAmount(calculateActualAmount(order, order.getOriginalAmount()));
        updateById(order);

        // 3. 写入操作日志
        logOperation(order.getId(), null, "DISCOUNT",
                dto.getReason(),
                String.format("{\"discountRate\":%s,\"originalAmount\":%s,\"actualAmount\":%s}",
                        discountRate, order.getOriginalAmount(), order.getActualAmount()));

        // 4. 查询所有订单项并返回
        List<OrderItem> allItems = queryOrderItems(orderId);
        OrderVO orderVO = buildOrderVO(order, allItems);

        log.info("整单打折成功: orderId={}, discountRate={}, actualAmount={}", orderId, discountRate, order.getActualAmount());
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO giftItem(Long itemId) {
        // 1. 校验订单项存在
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.ORDER_ITEM_NOT_FOUND);
        }

        // 2. 校验订单状态为待支付
        Order order = getById(item.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可赠送");
        }

        // 3. 记录原金额用于日志
        BigDecimal originalItemAmount = item.getAmount();

        // 4. 设置赠送标记，金额归零
        item.setIsGift(1);
        item.setAmount(BigDecimal.ZERO);
        orderItemMapper.updateById(item);

        // 5. 重算订单金额
        recalculateOrderAmount(order);

        // 6. 写入操作日志
        logOperation(order.getId(), itemId, "GIFT",
                null,
                String.format("{\"dishName\":\"%s\",\"originalAmount\":%s}", item.getDishName(), originalItemAmount));

        // 7. 返回更新后的订单
        List<OrderItem> allItems = queryOrderItems(order.getId());
        OrderVO orderVO = buildOrderVO(order, allItems);

        log.info("赠送成功: orderId={}, itemId={}, dishName={}", order.getId(), itemId, item.getDishName());
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO returnItem(Long itemId, ReturnItemDTO dto) {
        // 1. 验证授权密码
        if (!RETURN_AUTH_PASSWORD.equals(dto.getAuthPassword())) {
            throw new BusinessException(ResultCode.RETURN_DISH_AUTH_FAILED);
        }

        // 2. 校验订单项存在
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ResultCode.ORDER_ITEM_NOT_FOUND);
        }

        // 3. 校验订单状态为待支付
        Order order = getById(item.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可退菜");
        }

        // 4. 软删除订单项（MyBatis-Plus @TableLogic 会处理）
        orderItemMapper.deleteById(itemId);

        // 5. 回补库存
        if (item.getDishId() != null) {
            try {
                dishService.deductStock(item.getDishId(), -item.getQuantity());
            } catch (Exception e) {
                log.warn("退菜回补库存失败: dishId={}, quantity={}", item.getDishId(), item.getQuantity(), e);
            }
        }

        // 6. 重算订单金额
        recalculateOrderAmount(order);

        // 7. 写入操作日志
        logOperation(order.getId(), itemId, "RETURN",
                dto.getReason(),
                String.format("{\"dishName\":\"%s\",\"quantity\":%d,\"amount\":%s}",
                        item.getDishName(), item.getQuantity(), item.getAmount()));

        // 8. 返回更新后的订单
        List<OrderItem> allItems = queryOrderItems(order.getId());
        OrderVO orderVO = buildOrderVO(order, allItems);

        log.info("退菜成功: orderId={}, itemId={}, dishName={}", order.getId(), itemId, item.getDishName());
        return orderVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO replaceItem(Long itemId, ReplaceItemDTO dto) {
        // 1. 验证授权密码
        if (!RETURN_AUTH_PASSWORD.equals(dto.getAuthPassword())) {
            throw new BusinessException(ResultCode.RETURN_DISH_AUTH_FAILED);
        }

        // 2. 校验原订单项存在
        OrderItem oldItem = orderItemMapper.selectById(itemId);
        if (oldItem == null) {
            throw new BusinessException(ResultCode.ORDER_ITEM_NOT_FOUND);
        }

        // 3. 校验订单状态为待支付
        Order order = getById(oldItem.getOrderId());
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "仅待支付订单可换菜");
        }

        // 4. 软删除原订单项 + 回补库存
        orderItemMapper.deleteById(itemId);
        if (oldItem.getDishId() != null) {
            try {
                dishService.deductStock(oldItem.getDishId(), -oldItem.getQuantity());
            } catch (Exception e) {
                log.warn("换菜回补库存失败: dishId={}, quantity={}", oldItem.getDishId(), oldItem.getQuantity(), e);
            }
        }

        // 5. 校验新菜品可用性并扣减库存
        Dish newDish = validateAndDeductStock(dto.getNewDishId(), dto.getQuantity());

        // 6. 创建新订单项
        BigDecimal newItemAmount = newDish.getPrice().multiply(BigDecimal.valueOf(dto.getQuantity()));
        OrderItem newItem = new OrderItem();
        newItem.setOrderId(order.getId());
        newItem.setDishId(newDish.getId());
        newItem.setDishName(newDish.getName());
        newItem.setDishImage(pickDishImage(newDish));
        newItem.setPrice(newDish.getPrice());
        newItem.setQuantity(dto.getQuantity());
        newItem.setAmount(newItemAmount);
        newItem.setRemark(dto.getRemark());
        newItem.setStatus(0); // 待制作
        newItem.setIsGift(0);
        newItem.setAddedAt(LocalDateTime.now());
        orderItemMapper.insert(newItem);

        // 7. 重算订单金额
        recalculateOrderAmount(order);

        // 8. 写入操作日志
        logOperation(order.getId(), itemId, "REPLACE",
                dto.getReason(),
                String.format("{\"oldDish\":\"%s\",\"oldQuantity\":%d,\"newDish\":\"%s\",\"newQuantity\":%d,\"newItemId\":%d}",
                        oldItem.getDishName(), oldItem.getQuantity(),
                        newDish.getName(), dto.getQuantity(), newItem.getId()));

        // 9. 返回更新后的订单
        List<OrderItem> allItems = queryOrderItems(order.getId());
        OrderVO orderVO = buildOrderVO(order, allItems);

        log.info("换菜成功: orderId={}, oldItemId={}, oldDish={}, newDish={}",
                order.getId(), itemId, oldItem.getDishName(), newDish.getName());
        return orderVO;
    }

    // ==================== 订单查询 ====================

    @Override
    public OrderVO getOrderDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        List<OrderItem> items = queryOrderItems(orderId);
        return buildOrderVO(order, items);
    }

    @Override
    public List<OrderVO> getTableOrders(Long tableId) {
        String activeSessionCode = diningTableService.getActiveSessionCode(tableId);
        if (StrUtil.isBlank(activeSessionCode)) {
            return Collections.emptyList();
        }

        List<Order> orders = list(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getTableId, tableId)
                        .eq(Order::getTableSessionCode, activeSessionCode)
                        .in(Order::getStatus, 0, 1) // 待支付 + 已支付
                        .eq(Order::getDeleted, 0)
                        .orderByDesc(Order::getCreateTime)
        );
        return orders.stream().map(order -> {
            List<OrderItem> items = queryOrderItems(order.getId());
            return buildOrderVO(order, items);
        }).toList();
    }

    @Override
    public PageResult<OrderVO> listOrdersForAdmin(int pageNum, int pageSize, OrderQueryDTO queryDTO) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0);

        if (queryDTO != null) {
            if (StrUtil.isNotBlank(queryDTO.getAreaName())) {
                List<Long> tableIds = loadTableIdsByAreaName(queryDTO.getAreaName());
                if (tableIds.isEmpty()) {
                    return PageResult.of(Collections.emptyList(), (long) pageNum, (long) pageSize, 0L);
                }
                wrapper.in(Order::getTableId, tableIds);
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(Order::getStatus, queryDTO.getStatus());
            }
            if (queryDTO.getTableId() != null) {
                wrapper.eq(Order::getTableId, queryDTO.getTableId());
            }
            if (queryDTO.getOrderNo() != null && !queryDTO.getOrderNo().isBlank()) {
                wrapper.like(Order::getOrderNo, queryDTO.getOrderNo());
            }
            if (queryDTO.getStartDate() != null) {
                wrapper.ge(Order::getCreateTime, queryDTO.getStartDate().atStartOfDay());
            }
            if (queryDTO.getEndDate() != null) {
                wrapper.le(Order::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
            }
        }

        wrapper.orderByDesc(Order::getCreateTime);

        Page<Order> page = page(new Page<>(pageNum, pageSize), wrapper);

        List<OrderVO> voList = page.getRecords().stream().map(order -> {
            List<OrderItem> items = queryOrderItems(order.getId());
            return buildOrderVO(order, items);
        }).toList();

        return PageResult.of(voList, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 根据区域名称加载桌台ID列表
     *
     * @param areaName 区域名称
     * @return 桌台ID列表
     * @author Henfon
     * @date 2026-07-03
     * @description 订单列表按区域筛选时，先查出匹配区域的桌台，再用桌台ID反查订单，避免改动订单表结构。
     */
    private List<Long> loadTableIdsByAreaName(String areaName) {
        String normalizedAreaName = StrUtil.trim(areaName);
        if (StrUtil.isBlank(normalizedAreaName)) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<DiningTable> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(DiningTable::getDeleted, 0);
        if (StrUtil.equals(normalizedAreaName, "未分区")) {
            tableWrapper.and(wrapper -> wrapper.isNull(DiningTable::getAreaName).or().eq(DiningTable::getAreaName, ""));
        } else {
            tableWrapper.eq(DiningTable::getAreaName, normalizedAreaName);
        }

        // 先批量锁定区域下的桌台，再回查订单，避免在订单列表页做逐单桌台判断。
        return diningTableService.list(tableWrapper).stream()
                .map(DiningTable::getId)
                .toList();
    }

    @Override
    public OrderDetailVO getAdminOrderDetail(Long orderId) {
        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 查询订单项
        List<OrderItem> items = queryOrderItems(orderId);

        // 查询操作日志
        List<OrderOperationLog> logs = orderOperationLogMapper.selectList(
                new LambdaQueryWrapper<OrderOperationLog>()
                        .eq(OrderOperationLog::getOrderId, orderId)
                        .eq(OrderOperationLog::getDeleted, 0)
                        .orderByDesc(OrderOperationLog::getCreateTime)
        );

        // 构建 OrderDetailVO
        OrderDetailVO detailVO = new OrderDetailVO();
        BeanUtils.copyProperties(order, detailVO);
        detailVO.setPaymentMethod(queryLatestPaidPaymentMethod(orderId));

        Map<Long, String> dishImageCache = new HashMap<>();
        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            String image = item.getDishImage();
            if (!hasText(image) && item.getDishId() != null) {
                image = dishImageCache.computeIfAbsent(item.getDishId(), dishId -> {
                    Dish dish = dishService.getById(dishId);
                    return dish == null ? null : pickDishImage(dish);
                });
            }
            itemVO.setDishImage(minioStorageService.resolveAccessUrl(image));
            return itemVO;
        }).toList();
        detailVO.setItems(itemVOs);

        List<OrderOperationLogVO> logVOs = logs.stream().map(opLog -> {
            OrderOperationLogVO logVO = new OrderOperationLogVO();
            BeanUtils.copyProperties(opLog, logVO);
            return logVO;
        }).toList();
        detailVO.setOperationLogs(logVOs);

        return detailVO;
    }

    /**
     * 小程序餐前付订单支付成功后推送后厨新单
     *
     * @author Henfon
     * @date 2026-07-14
     * @description 仅在订单支付完成后再触发后厨和前厅播报，避免未支付订单提前进入制作流程。
     * @param orderId 订单ID
     */
    @Override
    public void notifyKitchenOrderPaid(Long orderId) {
        if (orderId == null) {
            return;
        }

        Order order = getById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != 1) {
            log.info("订单未完成支付，跳过后厨新单推送: orderId={}, status={}", orderId, order.getStatus());
            return;
        }
        if (order.getPaymentMode() == null || order.getPaymentMode() != 0) {
            log.info("订单不是餐前付场景，跳过支付后后厨推送: orderId={}, paymentMode={}", orderId, order.getPaymentMode());
            return;
        }

        List<OrderItem> orderItems = queryOrderItems(orderId);
        if (orderItems.isEmpty()) {
            log.info("订单无可推送订单项，跳过支付后后厨推送: orderId={}", orderId);
            return;
        }

        publishNewOrderNotifications(order, orderItems, false);
        log.info("餐前付订单支付成功，已推送后厨新单: orderId={}, orderNo={}, itemCount={}",
                orderId, order.getOrderNo(), orderItems.size());
    }

    // ==================== 私有方法 ====================

    /**
     * 获取桌台信息并校验
     */
    private DiningTableVO getTableInfo(Long tableId) {
        DiningTableVO table = null;
        try {
            // 通过 ID 获取桌台 - DiningTableService extends IService, use getById
            var tableEntity = diningTableService.getById(tableId);
            if (tableEntity != null) {
                table = new DiningTableVO();
                table.setId(tableEntity.getId());
                table.setCode(tableEntity.getCode());
                table.setName(tableEntity.getName());
                table.setStatus(tableEntity.getStatus());
                table.setCurrentSessionCode(tableEntity.getCurrentSessionCode());
            }
        } catch (Exception e) {
            log.error("获取桌台信息失败: tableId={}", tableId, e);
        }
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "桌台不存在");
        }
        return table;
    }

    /**
     * 校验菜品可售但不扣减库存
     *
     * @param dishId 菜品ID
     * @return 菜品实体
     * @author Henfon
     * @date 2026-07-03
     * @description 订单试算场景只校验菜品是否可售，不产生库存副作用
     */
    private Dish validateAvailableDish(Long dishId) {
        Dish dish = dishService.getById(dishId);
        if (dish == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "菜品不存在");
        }
        if (dish.getStatus() == null || dish.getStatus() != 1) {
            throw new BusinessException(ResultCode.DISH_OFF_SHELF);
        }
        if (dish.getSoldOut() != null && dish.getSoldOut() == 1) {
            throw new BusinessException(ResultCode.DISH_SOLD_OUT);
        }
        return dish;
    }

    /**
     * 校验菜品可用性并扣减库存
     *
     * @return 菜品实体（用于快照）
     */
    private Dish validateAndDeductStock(Long dishId, int quantity) {
        Dish dish = validateAvailableDish(dishId);
        // 库存扣减（stock=-1 表示不限库存）
        if (dish.getStock() != null && dish.getStock() != -1) {
            boolean deducted = dishService.deductStock(dishId, quantity);
            if (!deducted) {
                throw new BusinessException(ResultCode.DISH_STOCK_NOT_ENOUGH);
            }
        }
        return dish;
    }

    /**
     * 应用积分试算结果
     *
     * @param estimateVO 订单试算结果
     * @param pointsPreview 积分抵现预览
     * @author Henfon
     * @date 2026-07-03
     * @description 将会员积分抵现预览中的可用积分、实际使用积分和抵现金额回填到订单试算结果
     */
    private void applyPointsPreview(AdminOrderEstimateVO estimateVO, MemberPointsDeductionPreviewVO pointsPreview) {
        if (estimateVO == null || pointsPreview == null) {
            return;
        }

        estimateVO.setAvailablePoints(pointsPreview.getAvailablePoints() == null ? 0 : pointsPreview.getAvailablePoints());
        estimateVO.setMaxUsablePoints(pointsPreview.getMaxUsablePoints() == null ? 0 : pointsPreview.getMaxUsablePoints());
        estimateVO.setActualUsedPoints(pointsPreview.getActualUsedPoints() == null ? 0 : pointsPreview.getActualUsedPoints());
        estimateVO.setPointsDiscountAmount(pointsPreview.getDeductionAmount() == null ? BigDecimal.ZERO : pointsPreview.getDeductionAmount());
    }

    /**
     * 解析会员折扣率
     *
     * @param userId 会员用户ID
     * @return 折扣率
     * @author Henfon
     * @date 2026-07-03
     * @description 根据会员当前启用等级返回整单折扣率，未命中时返回 1.00
     */
    private BigDecimal resolveMemberDiscountRate(Long userId) {
        MemberProfile profile = loadActiveMemberProfile(userId);
        MemberLevel level = loadEnabledMemberLevel(profile == null ? null : profile.getLevelId());
        if (level == null || level.getDiscountRate() == null) {
            return BigDecimal.ONE;
        }

        BigDecimal discountRate = level.getDiscountRate();
        if (discountRate.compareTo(BigDecimal.ZERO) <= 0 || discountRate.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ONE;
        }
        return discountRate;
    }

    /**
     * 应用会员等级折扣试算结果
     *
     * @param estimateVO 订单试算结果
     * @param userId 会员用户ID
     * @param originalAmount 商品原价小计
     * @return 会员优惠金额
     * @author Henfon
     * @date 2026-07-03
     * @description 按会员当前等级折扣率预估整单优惠金额，并作为积分与优惠券试算前的基础金额
     */
    private BigDecimal applyMemberDiscountPreview(AdminOrderEstimateVO estimateVO, Long userId, BigDecimal originalAmount) {
        if (estimateVO == null || userId == null || originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountRate = resolveMemberDiscountRate(userId);
        if (discountRate.compareTo(BigDecimal.ONE) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountedAmount = originalAmount.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal memberDiscountAmount = originalAmount.subtract(discountedAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        estimateVO.setMemberDiscountAmount(memberDiscountAmount);
        return memberDiscountAmount;
    }

    /**
     * 查询有效会员档案
     *
     * @param userId 会员用户ID
     * @return 会员档案
     * @author Henfon
     * @date 2026-07-03
     * @description 仅加载状态正常的会员档案，供管理端下单和试算共用
     */
    private MemberProfile loadActiveMemberProfile(Long userId) {
        if (userId == null) {
            return null;
        }
        return memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .eq(MemberProfile::getStatus, 1)
                .last("LIMIT 1"));
    }

    /**
     * 查询启用状态的会员等级
     *
     * @param levelId 会员等级ID
     * @return 会员等级
     * @author Henfon
     * @date 2026-07-03
     * @description 统一处理会员等级状态校验，避免停用等级继续参与优惠计算
     */
    private MemberLevel loadEnabledMemberLevel(Long levelId) {
        if (levelId == null) {
            return null;
        }
        MemberLevel level = memberLevelMapper.selectById(levelId);
        return level != null && Integer.valueOf(1).equals(level.getStatus()) ? level : null;
    }

    /**
     * 加载订单试算使用的优惠券
     *
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @param orderAmount 订单金额
     * @return 可用于试算的优惠券
     * @author Henfon
     * @date 2026-07-03
     * @description 订单试算仅做只读校验，验证优惠券归属、状态、时间和门槛，不修改数据库锁定状态
     */
    private UserCoupon loadPreviewCoupon(Long userId, Long couponId, BigDecimal orderAmount) {
        if (userId == null || couponId == null) {
            return null;
        }

        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !userId.equals(coupon.getUserId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (!Integer.valueOf(0).equals(coupon.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券当前不可用");
        }
        if (coupon.getValidTo() != null && coupon.getValidTo().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券已过期");
        }
        if (!isCouponAvailableToday(coupon)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券今日不可用");
        }
        if (coupon.getThresholdAmount() != null && orderAmount != null && orderAmount.compareTo(coupon.getThresholdAmount()) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "订单金额未达到优惠券使用门槛");
        }
        return coupon;
    }

    /**
     * 判断优惠券是否当天可用
     *
     * @param coupon 用户优惠券
     * @return 是否可用
     * @author Henfon
     * @date 2026-07-03
     * @description 复用优惠券工作日配置，在订单试算阶段同步给出与正式下单一致的可用性判断
     */
    private boolean isCouponAvailableToday(UserCoupon coupon) {
        if (coupon == null || StrUtil.isBlank(coupon.getAvailableWeekdays())) {
            return true;
        }

        String today = String.valueOf(LocalDate.now().getDayOfWeek().getValue());
        return StrUtil.splitTrim(coupon.getAvailableWeekdays(), ',').contains(today);
    }

    /**
     * 生成订单试算提示
     *
     * @param estimateVO 试算结果
     * @param dto 试算请求
     * @return 提示文案列表
     * @author Henfon
     * @date 2026-07-03
     * @description 根据当前试算输入补充提示，明确当前是否已接入会员、券、积分以及结果的“预计”属性
     */
    private List<String> buildEstimateTips(AdminOrderEstimateVO estimateVO, AdminOrderEstimateDTO dto) {
        List<String> tips = new ArrayList<>();
        tips.add("当前为试算结果，最终以下单时结算结果为准");
        if (dto.getUserId() == null) {
            tips.add("未选择会员时，优惠券和积分抵扣默认不参与试算");
        }
        if (estimateVO.getMemberDiscountAmount() != null && estimateVO.getMemberDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            tips.add("会员折扣已按当前等级权益参与试算");
        }
        if (estimateVO.getCouponId() == null && dto.getCouponId() != null) {
            tips.add("当前未命中可用优惠券，请检查会员归属、有效期和使用门槛");
        }
        if (estimateVO.getRequestedPoints() != null && estimateVO.getRequestedPoints() > 0
                && estimateVO.getActualUsedPoints() != null
                && estimateVO.getActualUsedPoints() < estimateVO.getRequestedPoints()) {
            tips.add("积分抵扣已按余额、步长和单笔上限自动裁剪");
        }
        return tips;
    }

    /**
     * 回滚已扣减的库存（补偿操作）
     */
    private void rollbackStock(List<Long> dishIds, List<Integer> quantities) {
        for (int i = 0; i < dishIds.size(); i++) {
            try {
                // 负数量扣减 = 回补库存
                dishService.deductStock(dishIds.get(i), -quantities.get(i));
            } catch (Exception e) {
                log.error("库存回滚失败: dishId={}, quantity={}", dishIds.get(i), quantities.get(i), e);
            }
        }
    }

    /**
     * 生成订单编号：ORD + yyyyMMddHHmmss + 4位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "ORD" + timestamp + random;
    }

    /**
     * 从购物车项构建订单项
     */
    private OrderItem buildOrderItem(Long orderId, CartItemVO cartItem, LocalDateTime addedAt) {
        OrderItem item = new OrderItem();
        item.setOrderId(orderId);
        item.setDishId(cartItem.getDishId());
        item.setDishName(cartItem.getDishName());
        item.setDishImage(cartItem.getDishImage());
        item.setPrice(cartItem.getPrice());
        item.setQuantity(cartItem.getQuantity());
        item.setAmount(cartItem.getAmount());
        item.setRemark(cartItem.getRemark());
        item.setStatus(0); // 待制作
        item.setPaymentStatus(0);
        item.setIsGift(0);
        item.setAddedAt(addedAt);
        return item;
    }

    /**
     * 构建订单 VO（含订单项列表）
     */
    private OrderVO buildOrderVO(Order order, List<OrderItem> orderItems) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        fillOrderAreaName(order, vo);

        Map<Long, String> dishImageCache = new HashMap<>();
        List<OrderItemVO> itemVOs = orderItems.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            String image = item.getDishImage();
            if (!hasText(image) && item.getDishId() != null) {
                image = dishImageCache.computeIfAbsent(item.getDishId(), dishId -> {
                    Dish dish = dishService.getById(dishId);
                    return dish == null ? null : pickDishImage(dish);
                });
            }
            itemVO.setDishImage(minioStorageService.resolveAccessUrl(image));
            return itemVO;
        }).toList();

        vo.setItems(itemVOs);
        return vo;
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 根据订单关联桌台回填区域名称，供待结账等前厅卡片直接展示区域信息
     * @param order 订单实体
     * @param orderVO 订单返回对象
     */
    private void fillOrderAreaName(Order order, OrderVO orderVO) {
        if (order.getTableId() == null) {
            return;
        }

        var table = diningTableService.getById(order.getTableId());
        if (table != null) {
            orderVO.setAreaName(table.getAreaName());
        }
    }

    /**
     * 作者：Henfon
     * 日期：2026-07-10
     * 描述：下单前为当前桌台准备桌次编码，保证新一轮点单不会命中上一轮遗留订单
     *
     * @param tableId 桌台ID
     * @return 当前桌次编码
     */
    private String resolveTableSessionCodeForOrder(Long tableId) {
        String tableSessionCode = diningTableService.prepareCurrentSessionCode(tableId);
        if (StrUtil.isBlank(tableSessionCode)) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR, "当前桌台桌次异常，请重新进入桌台后再试");
        }
        return tableSessionCode;
    }

    /**
     * 菜品快照（用于管理端下单时暂存菜品信息）
     */
    private record DishSnapshot(Dish dish, int quantity, String remark, BigDecimal amount) {
    }

    /**
     * 查询订单的所有未删除订单项
     */
    private List<OrderItem> queryOrderItems(Long orderId) {
        return orderItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .eq(OrderItem::getDeleted, 0)
        );
    }

    /**
     * 重算订单金额：originalAmount = 非赠送订单项金额之和，actualAmount = 折扣后金额 - 优惠券抵扣
     */
    private void recalculateOrderAmount(Order order) {
        List<OrderItem> items = queryOrderItems(order.getId());
        BigDecimal originalAmount = items.stream()
                .filter(i -> i.getIsGift() == null || i.getIsGift() == 0)
                .map(OrderItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setOriginalAmount(originalAmount);
        Long userId = resolveOrderUserId(order);
        memberBenefitService.adjustOrderPointsDeduction(order, userId);
        BigDecimal actualAmount = calculateActualAmount(order, originalAmount);
        order.setActualAmount(actualAmount);

        // 订单项被全部退掉：自动取消并释放桌台
        if (items.isEmpty()) {
            order.setPaidAmount(BigDecimal.ZERO);
            order.setStatus(2); // 已取消
            updateById(order);
            couponService.releaseLockedCoupon(order.getId());
            if (userId != null && order.getPointsUsed() != null && order.getPointsUsed() > 0) {
                MemberProfile memberProfile = memberProfileMapper.selectByUserIdForUpdate(userId);
                if (memberProfile != null) {
                    memberPointsService.returnPointsForOrder(memberProfile, order.getId(), order.getPointsUsed(), "订单取消回退抵扣积分");
                    order.setPointsUsed(0);
                    order.setPointsDiscountAmount(BigDecimal.ZERO);
                    updateById(order);
                }
            }
            logOperation(order.getId(), null, "AUTO_CANCEL_EMPTY", "ALL_RETURN",
                    "{\"actualAmount\":0}");
            return;
        }

        // 订单项仍存在但应付金额为0（全赠送）：只结清当前订单，桌台继续保留当前用餐场次。
        if (order.getStatus() != null && order.getStatus() == 0 && actualAmount.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaidAmount(BigDecimal.ZERO);
            order.setStatus(1); // 已支付
            updateById(order);
            handleCouponAfterOrderClosed(order, actualAmount);
            logOperation(order.getId(), null, "AUTO_CLOSE_ZERO", "ALL_GIFT",
                    String.format("{\"originalAmount\":%s,\"actualAmount\":%s}", originalAmount, actualAmount));
            return;
        }

        updateById(order);
    }

    /**
     * 应用优惠券快照到订单
     *
     * @param order 订单
     * @param coupon 已锁定优惠券
     * @author Henfon
     * @date 2026-06-26
     * @description 将用户优惠券的关键信息冗余到订单，便于后续金额重算与支付核销
     */
    private void applyCouponSnapshot(Order order, UserCoupon coupon) {
        order.setCouponId(coupon.getId());
        order.setCouponName(coupon.getCouponName());
        order.setCouponType(coupon.getCouponType());
        order.setCouponThresholdAmount(coupon.getThresholdAmount());
        order.setCouponDiscountAmount(coupon.getDiscountAmount());
        order.setCouponDiscountRate(coupon.getDiscountRate());
    }

    /**
     * 计算订单实付金额
     *
     * @param order 订单
     * @param originalAmount 原始金额
     * @return 实付金额
     * @author Henfon
     * @date 2026-06-26
     * @description 先计算整单折扣，再叠加优惠券抵扣，并保证结果不为负数
     */
    private BigDecimal calculateActualAmount(Order order, BigDecimal originalAmount) {
        BigDecimal discountRate = order.getDiscountRate() == null ? BigDecimal.ONE : order.getDiscountRate();
        BigDecimal discountedAmount = originalAmount.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal pointsDeduction = order.getPointsDiscountAmount() == null ? BigDecimal.ZERO : order.getPointsDiscountAmount();
        BigDecimal amountAfterPoints = discountedAmount.subtract(pointsDeduction).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal couponDeduction = calculateCouponDeduction(order, amountAfterPoints);
        BigDecimal actualAmount = amountAfterPoints.subtract(couponDeduction);
        if (actualAmount.compareTo(BigDecimal.ZERO) < 0) {
            actualAmount = BigDecimal.ZERO;
        }
        return actualAmount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算优惠券抵扣金额
     *
     * @param order 订单
     * @param discountedAmount 折扣后金额
     * @return 优惠券抵扣金额
     * @author Henfon
     * @date 2026-06-26
     * @description 根据订单上的优惠券快照实时计算满减或折扣的抵扣金额
     */
    private BigDecimal calculateCouponDeduction(Order order, BigDecimal discountedAmount) {
        if (order.getCouponId() == null || order.getCouponType() == null) {
            return BigDecimal.ZERO;
        }
        if (order.getCouponType() == 1) {
            BigDecimal thresholdAmount = order.getCouponThresholdAmount() == null ? BigDecimal.ZERO : order.getCouponThresholdAmount();
            if (discountedAmount.compareTo(thresholdAmount) < 0) {
                return BigDecimal.ZERO;
            }
            return order.getCouponDiscountAmount() == null ? BigDecimal.ZERO : order.getCouponDiscountAmount();
        }
        if (order.getCouponType() == 2 && order.getCouponDiscountRate() != null) {
            BigDecimal deduction = discountedAmount.multiply(BigDecimal.ONE.subtract(order.getCouponDiscountRate()));
            return deduction.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    /**
     * 订单关闭后同步优惠券状态
     *
     * @param order 订单
     * @param actualAmount 当前实付金额
     * @author Henfon
     * @date 2026-06-26
     * @description 订单零元自动结单时，根据是否实际使用优惠券决定核销还是释放
     */
    private void handleCouponAfterOrderClosed(Order order, BigDecimal actualAmount) {
        if (order.getCouponId() == null) {
            return;
        }
        BigDecimal baseAmount = (order.getOriginalAmount() == null ? BigDecimal.ZERO : order.getOriginalAmount())
                .multiply(order.getDiscountRate() == null ? BigDecimal.ONE : order.getDiscountRate())
                .subtract(order.getPointsDiscountAmount() == null ? BigDecimal.ZERO : order.getPointsDiscountAmount())
                .max(BigDecimal.ZERO);
        BigDecimal couponDeduction = calculateCouponDeduction(order, baseAmount);
        if (couponDeduction.compareTo(BigDecimal.ZERO) > 0) {
            couponService.markCouponUsed(order.getId());
        } else {
            couponService.releaseLockedCoupon(order.getId());
        }
    }

    /**
     * 解析订单关联用户ID
     *
     * @param order 订单
     * @return 用户ID
     * @author Henfon
     * @date 2026-07-02
     * @description 通过订单上的顾客 openid 反查用户，用于积分抵现重算与回退
     */
    private Long resolveOrderUserId(Order order) {
        if (order == null || StrUtil.isBlank(order.getCustomerOpenid())) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenid, order.getCustomerOpenid())
                .last("LIMIT 1"));
        return user == null ? null : user.getId();
    }

    /**
     * 按用户ID读取顾客 openid
     *
     * @param userId 用户ID
     * @return openid
     * @author Henfon
     * @date 2026-07-03
     * @description 管理端代客下单时补齐 openid，便于后续积分回退与订单重算复用既有逻辑
     */
    private String loadUserOpenid(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUser user = sysUserMapper.selectById(userId);
        return user == null ? null : user.getOpenid();
    }

    /**
     * 判断订单是否应在支付前进入后厨
     *
     * @author Henfon
     * @date 2026-07-14
     * @description 餐后付订单允许先出餐后支付；餐前付订单则必须等待支付成功后再推送后厨。
     * @param order 订单
     * @return true 表示下单后即可推送后厨，false 表示需等待支付成功
     */
    private boolean shouldNotifyKitchenBeforePayment(Order order) {
        return order != null && order.getPaymentMode() != null && order.getPaymentMode() == 1;
    }

    /**
     * 推送后厨新单通知
     *
     * @author Henfon
     * @date 2026-07-14
     * @description 统一封装后厨自动接单与 WebSocket 新单广播，保证管理端与支付成功场景使用同一套推送逻辑。
     * @param order 订单
     * @param orderItems 本次需要进入后厨的订单项
     * @param adminOrder 是否管理端订单
     */
    private void publishNewOrderNotifications(Order order, List<OrderItem> orderItems, boolean adminOrder) {
        if (order == null || orderItems == null || orderItems.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 等事务真正提交后再通知后厨，避免前端收到事件时数据库里还查不到最新任务。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublishNewOrderNotifications(order, orderItems, adminOrder);
                }
            });
            return;
        }

        doPublishNewOrderNotifications(order, orderItems, adminOrder);
    }

    /**
     * 执行后厨新单通知
     *
     * @author Henfon
     * @date 2026-07-14
     * @description 在事务提交后执行自动接单和 WebSocket 推送，确保后厨刷新任务时能查到最新订单项。
     * @param order 订单
     * @param orderItems 本次需要进入后厨的订单项
     * @param adminOrder 是否管理端订单
     */
    private void doPublishNewOrderNotifications(Order order, List<OrderItem> orderItems, boolean adminOrder) {
        if (order == null || orderItems == null || orderItems.isEmpty()) {
            return;
        }

        OrderVO orderVO = buildOrderVO(order, orderItems);
        autoAcceptKitchenItemsIfNeeded(order, orderItems, adminOrder);
        wsService.broadcast(WsEventType.NEW_ORDER, "/topic/kitchen", orderVO);
        wsService.broadcast(WsEventType.NEW_ORDER, "/topic/service", orderVO);
    }

    private String pickDishImage(Dish dish) {
        if (dish == null) {
            return null;
        }
        if (hasText(dish.getImage())) {
            return dish.getImage();
        }
        return hasText(dish.getThumbnail()) ? dish.getThumbnail() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 按配置对堂食新订单执行自动接单
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 仅在后厨自动接单开关开启且订单类型为堂食时，将新订单项从待制作推进到制作中。
     * @param order 订单
     * @param orderItems 新生成的订单项
     * @param adminOrder 是否管理端下单
     */
    private void autoAcceptKitchenItemsIfNeeded(Order order, List<OrderItem> orderItems, boolean adminOrder) {
        if (order == null || orderItems == null || orderItems.isEmpty()) {
            return;
        }
        if (order.getOrderType() != null && order.getOrderType() != 0) {
            return;
        }
        if (!kitchenService.isAutoAcceptEnabled()) {
            return;
        }

        List<Long> itemIds = orderItems.stream()
                .map(OrderItem::getId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (itemIds.isEmpty()) {
            return;
        }

        // 自动接单只改变后厨任务状态，不影响订单支付主状态。
        kitchenService.autoAcceptTasks(itemIds);
        log.info("新{}堂食订单已自动接单: orderNo={}, itemCount={}",
                adminOrder ? "管理端" : "小程序", order.getOrderNo(), itemIds.size());

        for (OrderItem item : orderItems) {
            item.setStatus(1);
        }
    }

    /**
     * 写入订单操作日志
     */
    private void logOperation(Long orderId, Long orderItemId, String operationType, String reason, String detail) {
        OrderOperationLog opLog = new OrderOperationLog();
        opLog.setOrderId(orderId);
        opLog.setOrderItemId(orderItemId);
        opLog.setOperationType(operationType);
        try {
            opLog.setOperatorId(Long.valueOf(StpUtil.getLoginIdAsString()));
        } catch (Exception e) {
            log.warn("获取操作人ID失败", e);
        }
        if (opLog.getOperatorId() != null) {
            try {
                SysUser operator = sysUserMapper.selectById(opLog.getOperatorId());
                if (operator != null) {
                    if (operator.getNickname() != null && !operator.getNickname().isBlank()) {
                        opLog.setOperatorName(operator.getNickname());
                    } else {
                        opLog.setOperatorName(operator.getUsername());
                    }
                }
            } catch (Exception e) {
                log.warn("查询操作人信息失败", e);
            }
        }
        if (opLog.getOperatorName() == null || opLog.getOperatorName().isBlank()) {
            try {
                opLog.setOperatorName(StpUtil.getLoginIdAsString());
            } catch (Exception ignored) {
                // ignore
            }
        }
        opLog.setReason(reason);
        opLog.setDetail(detail);
        orderOperationLogMapper.insert(opLog);
    }

    /**
     * 查询订单最新支付方式（仅取已支付记录）
     */
    private Integer queryLatestPaidPaymentMethod(Long orderId) {
        PaymentRecord latestPaid = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderId)
                        .eq(PaymentRecord::getStatus, 1)
                        .eq(PaymentRecord::getDeleted, 0)
                        .orderByDesc(PaymentRecord::getCreateTime)
                        .last("LIMIT 1")
        );
        return latestPaid != null ? latestPaid.getPaymentMethod() : null;
    }

    /**
     * 安全释放分布式锁（仅持有者可释放）
     */
    private void safeReleaseOrderLock(String lockKey, String lockToken) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
        } catch (Exception e) {
            log.warn("释放订单锁失败: key={}, err={}", lockKey, e.getMessage());
        }
    }
}
