package com.scaffold.modules.mq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * MQ 消息出站实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mq_message")
public class MqMessage extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一键
     */
    private String messageKey;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务主键
     */
    private String bizKey;

    /**
     * 消息体
     */
    private String payload;

    /**
     * 投递状态
     */
    private Integer deliverStatus;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 发送成功时间
     */
    private LocalDateTime sentTime;
}
