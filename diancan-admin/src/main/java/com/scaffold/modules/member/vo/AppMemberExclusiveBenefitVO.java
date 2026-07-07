package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序等级专属权益 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberExclusiveBenefitVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long levelId;

    private String levelName;

    private Long templateId;

    private String templateName;

    private Boolean claimable;

    private String claimTip;
}
