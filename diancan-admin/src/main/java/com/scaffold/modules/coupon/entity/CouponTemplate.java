package com.scaffold.modules.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("coupon_template")
public class CouponTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 优惠券类型（1满减 2折扣）
     */
    private Integer type;

    /**
     * 使用门槛金额
     */
    private BigDecimal thresholdAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 折扣比例
     */
    private BigDecimal discountRate;

    /**
     * 发放总量（0 表示不限量）
     */
    private Integer totalQuantity;

    /**
     * 已发放数量
     */
    private Integer issuedQuantity;

    /**
     * 每人限领张数（0 表示不限）
     */
    private Integer perUserLimit;

    /**
     * 有效期类型（1固定时间 2领券后N天）
     */
    private Integer validityType;

    /**
     * 固定生效时间
     */
    private LocalDateTime validFrom;

    /**
     * 固定失效时间
     */
    private LocalDateTime validTo;

    /**
     * 领券后有效天数
     */
    private Integer validDays;

    /**
     * 状态（0停用 1启用）
     */
    private Integer status;

    /**
     * 说明
     */
    private String description;

    /**
     * 可用星期，使用 1-7 表示周一到周日，多个值使用逗号分隔，空表示每天可用
     */
    private String availableWeekdays;
}
