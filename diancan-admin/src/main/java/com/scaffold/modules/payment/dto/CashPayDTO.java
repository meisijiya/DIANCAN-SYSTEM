package com.scaffold.modules.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 现金支付请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "现金支付请求参数")
public class CashPayDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    @Schema(description = "收款金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "收款金额不能为空")
    private BigDecimal receivedAmount;
}
