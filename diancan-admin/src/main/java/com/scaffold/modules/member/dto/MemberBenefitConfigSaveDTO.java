package com.scaffold.modules.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 会员权益配置保存 DTO
 *
 * @author Henfon
 */
@Data
public class MemberBenefitConfigSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull(message = "积分抵现规则不能为空")
    private MemberPointsDeductionRuleDTO pointsDeductionRule;

    @Valid
    @NotNull(message = "积分过期规则不能为空")
    private MemberPointsExpireRuleDTO pointsExpireRule;

    @Valid
    @NotNull(message = "生日权益规则不能为空")
    private MemberBirthdayBenefitRuleDTO birthdayBenefitRule;
}
