package com.scaffold.modules.cart.controller;

import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.common.result.Result;
import com.scaffold.modules.cart.dto.CartItemDTO;
import com.scaffold.modules.cart.service.CartService;
import com.scaffold.modules.cart.vo.CartVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 购物车控制器（小程序端）
 *
 * @author Henfon
 */
@Slf4j
@Tag(name = "购物车（小程序端）")
@RestController
@RequestMapping("/app/cart")
@RequiredArgsConstructor
public class AppCartController {

    private final CartService cartService;

    @Operation(summary = "获取购物车")
    @GetMapping
    public Result<CartVO> getCart(@Parameter(description = "桌台ID") @RequestParam Long tableId) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(cartService.getCart(openid, tableId));
    }

    @Operation(summary = "添加菜品到购物车")
    @PostMapping("/item")
    public Result<CartVO> addItem(@Parameter(description = "桌台ID") @RequestParam Long tableId,
                                  @Valid @RequestBody CartItemDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(cartService.addItem(openid, tableId, dto));
    }

    @Operation(summary = "修改购物车项数量或备注")
    @PutMapping("/item/{dishId}")
    public Result<CartVO> updateItem(@Parameter(description = "菜品ID") @PathVariable Long dishId,
                                     @Parameter(description = "桌台ID") @RequestParam Long tableId,
                                     @Parameter(description = "数量") @RequestParam(required = false) Integer quantity,
                                     @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        String openid = SessionUtils.getCurrentOpenid();
        CartVO result = null;
        if (quantity != null) {
            result = cartService.updateItemQuantity(openid, tableId, dishId, quantity);
        }
        if (remark != null) {
            result = cartService.updateItemRemark(openid, tableId, dishId, remark);
        }
        if (result == null) {
            result = cartService.getCart(openid, tableId);
        }
        return Result.success(result);
    }

    @Operation(summary = "移除购物车项")
    @DeleteMapping("/item/{dishId}")
    public Result<Void> removeItem(@Parameter(description = "菜品ID") @PathVariable Long dishId,
                                   @Parameter(description = "桌台ID") @RequestParam Long tableId) {
        String openid = SessionUtils.getCurrentOpenid();
        cartService.removeItem(openid, tableId, dishId);
        return Result.success();
    }

    @Operation(summary = "清空购物车")
    @DeleteMapping
    public Result<Void> clearCart(@Parameter(description = "桌台ID") @RequestParam Long tableId) {
        String openid = SessionUtils.getCurrentOpenid();
        cartService.clearCart(openid, tableId);
        return Result.success();
    }
}
