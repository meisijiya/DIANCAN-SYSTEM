package com.scaffold.modules.member.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 生日权益规则 DTO
 *
 * @author Henfon
 */
@Data
public class MemberBirthdayBenefitRuleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    /**
     * 生日券模板ID
     */
    private Long couponTemplateId;
}
