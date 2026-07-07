package com.scaffold.modules.coupon.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券 VO
 *
 * @author Henfon
 */
@Data
public class UserCouponVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long templateId;
    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private String couponName;
    private Integer couponType;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer sourceType;
    private Integer status;
    private LocalDateTime receivedTime;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime usedTime;
    private Long orderId;
    private String availableWeekdays;
}
