package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序端下单请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "下单请求参数")
public class OrderCreateDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "桌台ID不能为空")
    private Long tableId;

    @Schema(description = "支付模式（0餐前付 1餐后付）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "支付模式不能为空")
    private Integer paymentMode;

    @Schema(description = "订单类型（0堂食 1外卖）")
    private Integer orderType = 0;

    @Schema(description = "订单备注")
    private String remark;

    @Schema(description = "用户优惠券ID")
    private Long couponId;

    @Schema(description = "本单使用积分")
    private Integer usePoints;
}
