package com.scaffold.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 会员积分流水实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_points_record")
public class MemberPointsRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long memberId;

    private Long userId;

    private Integer changeType;

    private String bizType;

    private Long bizId;

    private Integer changeAmount;

    private Integer balanceAfter;

    private LocalDateTime expireTime;

    private String remark;
}
