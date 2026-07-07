package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 积分抵现预览 VO
 *
 * @author Henfon
 */
@Data
public class MemberPointsDeductionPreviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Boolean enabled;

    private Integer requestedPoints;

    private Integer availablePoints;

    private Integer maxUsablePoints;

    private Integer actualUsedPoints;

    private BigDecimal deductionAmount;
}
