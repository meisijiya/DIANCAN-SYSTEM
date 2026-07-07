package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 积分兑换优惠券配置 VO
 *
 * @author Henfon
 */
@Data
public class MemberCouponExchangeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long templateId;

    private String templateName;

    private Integer pointsCost;

    private Integer perUserLimit;

    private Integer sort;

    private Integer status;

    private String remark;
}
