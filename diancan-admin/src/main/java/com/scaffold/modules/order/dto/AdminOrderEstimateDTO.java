package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 管理端订单试算请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "管理端订单试算请求参数")
public class AdminOrderEstimateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID")
    private Long tableId;

    @Schema(description = "会员用户ID")
    private Long userId;

    @Schema(description = "用户优惠券ID")
    private Long couponId;

    @Schema(description = "本单尝试使用积分")
    private Integer usePoints;

    @Schema(description = "菜品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜品列表不能为空")
    @Valid
    private List<AdminOrderEstimateItemDTO> items;

    /**
     * 管理端订单试算项 DTO
     */
    @Data
    @Schema(description = "管理端订单试算项")
    public static class AdminOrderEstimateItemDTO implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Schema(description = "菜品ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "菜品ID不能为空")
        private Long dishId;

        @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为1")
        private Integer quantity;
    }
}
