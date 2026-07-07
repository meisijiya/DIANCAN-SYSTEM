package com.scaffold.modules.mq.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * MQ 消费日志实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mq_consume_log")
public class MqConsumeLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消费者组
     */
    private String consumerGroup;

    /**
     * 主题
     */
    private String topic;

    /**
     * 标签
     */
    private String tag;

    /**
     * 消息唯一键
     */
    private String messageKey;

    /**
     * 业务主键
     */
    private String bizKey;

    /**
     * 消费状态
     */
    private Integer consumeStatus;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最后错误信息
     */
    private String lastError;

    /**
     * 完成时间
     */
    private LocalDateTime finishedTime;
}
