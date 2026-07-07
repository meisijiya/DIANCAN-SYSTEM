package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端下单请求 DTO（支持预订单模式，直接传入菜品列表）
 *
 * @author Henfon
 */
@Data
@Schema(description = "管理端下单请求参数")
public class AdminOrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "桌台ID不能为空")
    private Long tableId;

    @Schema(description = "桌台编号")
    private String tableCode;

    @Schema(description = "菜品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜品列表不能为空")
    @Valid
    private List<AdminOrderItemDTO> items;

    @Schema(description = "支付模式（0餐前付 1餐后付）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付模式不能为空")
    private Integer paymentMode;

    @Schema(description = "会员用户ID")
    private Long userId;

    @Schema(description = "用户优惠券ID")
    private Long couponId;

    @Schema(description = "本单尝试使用积分")
    private Integer usePoints;

    @Schema(description = "订单类型（0堂食 1外卖）")
    private Integer orderType = 0;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "是否预订单（true=仅保存不发送后厨）")
    private Boolean preOrder = false;

    /**
     * 管理端订单项 DTO
     */
    @Data
    @Schema(description = "管理端订单项")
    public static class AdminOrderItemDTO implements Serializable {

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
}
