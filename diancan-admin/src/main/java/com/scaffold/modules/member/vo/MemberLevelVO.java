package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会员等级 VO
 *
 * @author Henfon
 */
@Data
public class MemberLevelVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String levelCode;

    private String levelName;

    private Integer sort;

    private Integer growthThreshold;

    private BigDecimal pointsRate;

    private BigDecimal discountRate;

    private String benefitConfig;

    private Long upgradeCouponTemplateId;

    private Long exclusiveCouponTemplateId;

    private Integer status;

    private String remark;
}
