package com.scaffold.modules.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分抵现规则 DTO
 *
 * @author Henfon
 */
@Data
public class MemberPointsDeductionRuleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @NotNull(message = "积分换算步长不能为空")
    @Min(value = 1, message = "积分换算步长必须大于0")
    private Integer pointsPerStep;

    @NotNull(message = "每步抵现金额不能为空")
    @DecimalMin(value = "0.01", message = "每步抵现金额必须大于0")
    private BigDecimal amountPerStep;

    @NotNull(message = "每单最高抵扣比例不能为空")
    @DecimalMin(value = "0.00", message = "最高抵扣比例不能小于0")
    @DecimalMax(value = "1.00", message = "最高抵扣比例不能大于1")
    private BigDecimal maxDeductionRatio;

    @NotNull(message = "每单最多使用积分不能为空")
    @Min(value = 0, message = "每单最多使用积分不能小于0")
    private Integer maxPointsPerOrder;
}
