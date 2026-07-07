package com.scaffold.modules.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 整单打折请求 DTO
 *
 * @author Henfon
 */
@Data
@Schema(description = "整单打折请求参数")
public class OrderDiscountDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "折扣比例（0.01-1.00）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "折扣比例不能为空")
    @DecimalMin(value = "0.01", message = "折扣比例最小为0.01")
    @DecimalMax(value = "1.00", message = "折扣比例最大为1.00")
    private BigDecimal discountRate;

    @Schema(description = "打折原因")
    private String reason;
}
