package com.scaffold.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 会员积分兑换优惠券配置实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_coupon_exchange")
public class MemberCouponExchange extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 优惠券模板ID
     */
    private Long templateId;

    /**
     * 优惠券模板名称快照
     */
    private String templateName;

    /**
     * 所需积分
     */
    private Integer pointsCost;

    /**
     * 每人可兑换次数，0 不限
     */
    private Integer perUserLimit;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0停用 1启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
