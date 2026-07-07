package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 小程序会员中心 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberCenterVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long memberId;

    private String memberNo;

    private Long levelId;

    private String levelName;

    private Integer growthValue;

    private Integer pointsBalance;

    private String nextLevelName;

    private Integer nextLevelThreshold;

    private Integer pointsToNextLevel;

    private BigDecimal totalAmountConsumed;

    private BigDecimal pointsRate;

    private BigDecimal discountRate;
}
