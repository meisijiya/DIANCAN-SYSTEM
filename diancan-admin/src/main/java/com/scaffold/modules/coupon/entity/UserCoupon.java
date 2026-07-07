package com.scaffold.modules.coupon.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名快照
     */
    private String username;

    /**
     * 用户昵称快照
     */
    private String nickname;

    /**
     * 手机号快照
     */
    private String phone;

    /**
     * 券名称快照
     */
    private String couponName;

    /**
     * 优惠券类型（1满减 2折扣）
     */
    private Integer couponType;

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
     * 来源类型（1后台发放 2全员发放）
     */
    private Integer sourceType;

    /**
     * 状态（0未使用 1已使用 2已过期 3已锁定）
     */
    private Integer status;

    /**
     * 领取时间
     */
    private LocalDateTime receivedTime;

    /**
     * 生效时间
     */
    private LocalDateTime validFrom;

    /**
     * 失效时间
     */
    private LocalDateTime validTo;

    /**
     * 使用时间
     */
    private LocalDateTime usedTime;

    /**
     * 使用订单ID
     */
    private Long orderId;

    /**
     * 发券任务ID
     */
    private Long grantTaskId;

    /**
     * 可用星期，使用 1-7 表示周一到周日，多个值使用逗号分隔，空表示每天可用
     */
    private String availableWeekdays;
}
