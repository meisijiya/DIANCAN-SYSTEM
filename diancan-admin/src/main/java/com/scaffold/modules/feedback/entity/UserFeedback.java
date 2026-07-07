package com.scaffold.modules.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 用户反馈实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_feedback")
public class UserFeedback extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 反馈用户 openid
     */
    private String customerOpenid;

    /**
     * 用户手机号快照
     */
    private String customerPhone;

    /**
     * 联系手机号
     */
    private String contactPhone;

    /**
     * 反馈内容
     */
    private String content;

    /**
     * 回复内容
     */
    private String replyContent;

    /**
     * 回复时间
     */
    private LocalDateTime replyTime;

    /**
     * 状态：0-待回复，1-已回复
     */
    private Integer status;
}
