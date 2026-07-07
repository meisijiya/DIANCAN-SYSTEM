package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 小程序会员权益概览 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberBenefitOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private AppMemberPointsDeductionRuleVO pointsDeductionRule;

    private List<AppMemberCouponExchangeVO> exchangeCoupons;

    private AppMemberExclusiveBenefitVO exclusiveBenefit;
}
