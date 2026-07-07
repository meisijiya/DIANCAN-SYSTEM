package com.scaffold.modules.order.controller;

import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.order.dto.AddItemDTO;
import com.scaffold.modules.order.dto.AdminOrderCreateDTO;
import com.scaffold.modules.order.dto.AdminOrderEstimateDTO;
import com.scaffold.modules.order.dto.OrderDiscountDTO;
import com.scaffold.modules.order.dto.OrderQueryDTO;
import com.scaffold.modules.order.dto.ReplaceItemDTO;
import com.scaffold.modules.order.dto.ReturnItemDTO;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.order.vo.AdminOrderEstimateVO;
import com.scaffold.modules.order.vo.OrderDetailVO;
import com.scaffold.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器（管理端/服务端）
 *
 * @author Henfon
 */
@Tag(name = "订单（管理端）")
@RestController
@RequestMapping("/admin/order")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单（支持预订单模式）")
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody AdminOrderCreateDTO dto) {
        return Result.success(orderService.createAdminOrder(dto));
    }

    /**
     * 管理端订单试算
     *
     * @param dto 试算参数
     * @return 试算结果
     * @author Henfon
     * @date 2026-07-03
     * @description 返回服务员点单场景的订单预估金额，供购物车实时展示预计优惠与应付
     */
    @Operation(summary = "订单试算")
    @PostMapping("/estimate")
    public Result<AdminOrderEstimateVO> estimateOrder(@Valid @RequestBody AdminOrderEstimateDTO dto) {
        return Result.success(orderService.estimateAdminOrder(dto));
    }

    @Operation(summary = "加菜")
    @PostMapping("/{id}/add-item")
    public Result<OrderVO> addItem(@PathVariable Long id, @Valid @RequestBody AddItemDTO dto) {
        return Result.success(orderService.addItem(id, dto));
    }

    @Operation(summary = "催单")
    @PostMapping("/{id}/rush/{itemId}")
    public Result<Void> rushItem(@PathVariable Long id, @PathVariable Long itemId) {
        orderService.rushItem(id, itemId);
        return Result.success();
    }

    @Operation(summary = "整单打折")
    @PutMapping("/{id}/discount")
    public Result<OrderVO> discountOrder(@PathVariable Long id, @Valid @RequestBody OrderDiscountDTO dto) {
        return Result.success(orderService.discountOrder(id, dto));
    }

    @Operation(summary = "赠送订单项")
    @PutMapping("/item/{itemId}/gift")
    public Result<OrderVO> giftItem(@PathVariable Long itemId) {
        return Result.success(orderService.giftItem(itemId));
    }

    @Operation(summary = "退菜")
    @PutMapping("/item/{itemId}/return")
    public Result<OrderVO> returnItem(@PathVariable Long itemId, @Valid @RequestBody ReturnItemDTO dto) {
        return Result.success(orderService.returnItem(itemId, dto));
    }

    @Operation(summary = "换菜")
    @PutMapping("/item/{itemId}/replace")
    public Result<OrderVO> replaceItem(@PathVariable Long itemId, @Valid @RequestBody ReplaceItemDTO dto) {
        return Result.success(orderService.replaceItem(itemId, dto));
    }

    @Operation(summary = "订单列表（支持筛选）")
    @GetMapping("/list")
    public Result<PageResult<OrderVO>> listOrders(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            OrderQueryDTO queryDTO) {
        return Result.success(orderService.listOrdersForAdmin(pageNum, pageSize, queryDTO));
    }

    @Operation(summary = "订单详情（含操作日志）")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getAdminOrderDetail(id));
    }
}
