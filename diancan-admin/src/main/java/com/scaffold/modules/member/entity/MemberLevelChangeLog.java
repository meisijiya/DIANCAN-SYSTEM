package com.scaffold.modules.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 会员等级变更日志实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_level_change_log")
public class MemberLevelChangeLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long memberId;

    private Long userId;

    private Long oldLevelId;

    private Long newLevelId;

    private String changeReason;

    private String bizType;

    private Long bizId;

    private String remark;
}
