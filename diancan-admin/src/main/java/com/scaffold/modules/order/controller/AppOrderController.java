package com.scaffold.modules.order.controller;

import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.common.result.Result;
import com.scaffold.modules.order.dto.AddItemDTO;
import com.scaffold.modules.order.dto.OrderCreateDTO;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "订单（小程序端）")
@RestController
@RequestMapping("/app/order")
@RequiredArgsConstructor
public class AppOrderController {

    private final OrderService orderService;

    @Operation(summary = "提交订单（从购物车生成）")
    @PostMapping
    public Result<OrderVO> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(orderService.createOrder(openid, dto));
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

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @Operation(summary = "获取桌台当前订单列表")
    @GetMapping("/table/{tableId}")
    public Result<List<OrderVO>> getTableOrders(@PathVariable Long tableId) {
        return Result.success(orderService.getTableOrders(tableId));
    }
}
