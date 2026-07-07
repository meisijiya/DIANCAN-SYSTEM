package com.scaffold.modules.member.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 积分过期规则 DTO
 *
 * @author Henfon
 */
@Data
public class MemberPointsExpireRuleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "是否启用不能为空")
    private Boolean enabled;

    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于0")
    private Integer expireDays;
}
