package com.scaffold.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 购物车项 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "购物车项请求参数")
public class CartItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "菜品ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜品ID不能为空")
    private Long dishId;

    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为1")
    private Integer quantity;

    @Schema(description = "备注")
    private String remark;
}
