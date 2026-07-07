package com.scaffold.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 会员权益发放日志实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_benefit_grant_log")
public class MemberBenefitGrantLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 等级ID
     */
    private Long levelId;

    /**
     * 优惠券模板ID
     */
    private Long templateId;

    /**
     * 用户优惠券ID
     */
    private Long userCouponId;

    /**
     * 权益类型：1生日权益 2升级礼包 3等级专属券
     */
    private Integer benefitType;

    /**
     * 触发键
     */
    private String triggerKey;

    /**
     * 触发值
     */
    private String triggerValue;

    /**
     * 备注
     */
    private String remark;
}
