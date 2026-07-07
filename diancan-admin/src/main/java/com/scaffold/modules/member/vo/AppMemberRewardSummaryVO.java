package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序会员奖励摘要 VO
 *
 * @author Henfon
 */
@Data
public class AppMemberRewardSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 本次奖励积分
     */
    private Integer pointsReward;

    /**
     * 本次奖励成长值
     */
    private Integer growthReward;

    /**
     * 是否已结算会员奖励
     */
    private Boolean settled;
}
