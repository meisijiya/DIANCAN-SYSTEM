package com.scaffold.modules.coupon.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板 VO
 *
 * @author Henfon
 */
@Data
public class CouponTemplateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer type;
    private BigDecimal thresholdAmount;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private Integer perUserLimit;
    private Integer validityType;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer validDays;
    private Integer status;
    private String description;
    private String availableWeekdays;
    private LocalDateTime createTime;
}
