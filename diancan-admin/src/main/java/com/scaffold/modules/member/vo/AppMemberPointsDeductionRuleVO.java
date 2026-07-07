package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 小程序积分抵现规则 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberPointsDeductionRuleVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean enabled;

    private Integer pointsPerStep;

    private BigDecimal amountPerStep;

    private BigDecimal maxDeductionRatio;

    private Integer maxPointsPerOrder;
}
