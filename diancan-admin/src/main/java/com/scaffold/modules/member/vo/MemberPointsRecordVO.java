package com.scaffold.modules.member.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 积分流水 VO
 *
 * @author Henfon
 */
@Data
public class MemberPointsRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long memberId;

    private Long userId;

    private String memberNo;

    private String nickname;

    private Integer changeType;

    private String bizType;

    private Long bizId;

    private Integer changeAmount;

    private Integer balanceAfter;

    private String remark;

    private LocalDateTime createTime;
}
