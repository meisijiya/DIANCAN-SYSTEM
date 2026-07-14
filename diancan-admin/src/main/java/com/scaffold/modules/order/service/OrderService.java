package com.scaffold.modules.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.order.dto.AddItemDTO;
import com.scaffold.modules.order.dto.AdminOrderCreateDTO;
import com.scaffold.modules.order.dto.AdminOrderEstimateDTO;
import com.scaffold.modules.order.dto.OrderCreateDTO;
import com.scaffold.modules.order.dto.OrderDiscountDTO;
import com.scaffold.modules.order.dto.OrderQueryDTO;
import com.scaffold.modules.order.dto.ReplaceItemDTO;
import com.scaffold.modules.order.dto.ReturnItemDTO;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.vo.AdminOrderEstimateVO;
import com.scaffold.modules.order.vo.OrderDetailVO;
import com.scaffold.modules.order.vo.OrderVO;
import com.scaffold.common.result.PageResult;

import java.util.List;

/**
 * 订单服务接口
 *
 * @author Henfon
 */
public interface OrderService extends IService<Order> {

    /**
     * 小程序端下单（从购物车生成订单）
     *
     * @param openid 用户openid
     * @param dto    下单参数
     * @return 订单信息
     */
    OrderVO createOrder(String openid, OrderCreateDTO dto);

    /**
     * 管理端下单（支持预订单模式，直接传入菜品列表）
     *
     * @param dto 下单参数
     * @return 订单信息
     */
    OrderVO createAdminOrder(AdminOrderCreateDTO dto);

    /**
     * 管理端订单试算
     *
     * @param dto 试算参数
     * @return 试算结果
     * @author Henfon
     * @date 2026-07-03
     * @description 预估管理端点单场景下的小计、优惠券抵扣、积分抵扣和应付金额
     */
    AdminOrderEstimateVO estimateAdminOrder(AdminOrderEstimateDTO dto);

    /**
     * 加菜：追加菜品至现有订单
     *
     * @param orderId 订单ID
     * @param dto     加菜参数
     * @return 更新后的订单信息
     */
    OrderVO addItem(Long orderId, AddItemDTO dto);

    /**
     * 催单：对指定订单项发起催单
     *
     * @param orderId 订单ID
     * @param itemId  订单项ID
     */
    void rushItem(Long orderId, Long itemId);

    /**
     * 整单打折
     *
     * @param orderId 订单ID
     * @param dto     打折参数
     * @return 更新后的订单信息
     */
    OrderVO discountOrder(Long orderId, OrderDiscountDTO dto);

    /**
     * 赠送订单项
     *
     * @param itemId 订单项ID
     * @return 更新后的订单信息
     */
    OrderVO giftItem(Long itemId);

    /**
     * 退菜
     *
     * @param itemId 订单项ID
     * @param dto    退菜参数（含授权密码和原因）
     * @return 更新后的订单信息
     */
    OrderVO returnItem(Long itemId, ReturnItemDTO dto);

    /**
     * 换菜
     *
     * @param itemId 订单项ID
     * @param dto    换菜参数（含新菜品、授权密码和原因）
     * @return 更新后的订单信息
     */
    OrderVO replaceItem(Long itemId, ReplaceItemDTO dto);

    /**
     * 获取订单详情（含订单项列表）
     *
     * @param orderId 订单ID
     * @return 订单信息
     */
    OrderVO getOrderDetail(Long orderId);

    /**
     * 获取桌台当前活跃订单列表（状态为待支付）
     *
     * @param tableId 桌台ID
     * @return 订单列表
     */
    List<OrderVO> getTableOrders(Long tableId);

    /**
     * 管理端订单分页列表（支持筛选）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param queryDTO 筛选条件
     * @return 分页结果
     */
    PageResult<OrderVO> listOrdersForAdmin(int pageNum, int pageSize, OrderQueryDTO queryDTO);

    /**
     * 管理端订单详情（含订单项、操作日志）
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    OrderDetailVO getAdminOrderDetail(Long orderId);

    /**
     * 小程序餐前付订单支付成功后推送后厨新单
     *
     * @param orderId 订单ID
     * @author Henfon
     * @date 2026-07-14
     * @description 仅对支付成功后才允许出餐的订单推送后厨与前厅播报，避免未支付订单提前制作。
     */
    void notifyKitchenOrderPaid(Long orderId);
}
