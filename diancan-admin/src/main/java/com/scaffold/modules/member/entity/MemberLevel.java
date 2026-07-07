package com.scaffold.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 会员等级实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_level")
public class MemberLevel extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private String levelCode;

    private String levelName;

    private Integer sort;

    private Integer growthThreshold;

    private BigDecimal pointsRate;

    private BigDecimal discountRate;

    private String benefitConfig;

    /**
     * 升级礼包优惠券模板ID
     */
    private Long upgradeCouponTemplateId;

    /**
     * 等级专属优惠券模板ID
     */
    private Long exclusiveCouponTemplateId;

    private Integer status;

    private String remark;
}
