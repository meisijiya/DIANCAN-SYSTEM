package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序积分兑换优惠券项 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberCouponExchangeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long templateId;

    private String templateName;

    private Integer pointsCost;

    private Integer perUserLimit;

    private Integer exchangedCount;

    private String description;
}
