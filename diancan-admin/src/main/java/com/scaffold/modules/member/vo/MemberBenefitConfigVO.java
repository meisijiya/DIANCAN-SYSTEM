package com.scaffold.modules.member.vo;

import com.scaffold.modules.member.dto.MemberBirthdayBenefitRuleDTO;
import com.scaffold.modules.member.dto.MemberPointsDeductionRuleDTO;
import com.scaffold.modules.member.dto.MemberPointsExpireRuleDTO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会员权益配置 VO
 *
 * @author Henfon
 */
@Data
public class MemberBenefitConfigVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private MemberPointsDeductionRuleDTO pointsDeductionRule;

    private MemberPointsExpireRuleDTO pointsExpireRule;

    private MemberBirthdayBenefitRuleDTO birthdayBenefitRule;
}
