package com.scaffold.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 发券 MQ 配置
 *
 * @author Henfon
 */
@Data
@Component
@ConfigurationProperties(prefix = "coupon.grant.mq")
public class CouponGrantMqProperties {

    /**
     * 是否启用 MQ 发券
     */
    private boolean enabled = true;

    /**
     * 发券主题
     */
    private String topic = "coupon-grant-topic";

    /**
     * 发券分发标签
     */
    private String dispatchTag = "TASK_DISPATCH";

    /**
     * 发券批处理标签
     */
    private String batchTag = "TASK_BATCH";

    /**
     * 批量发券单批人数
     */
    private int batchSize = 500;

    /**
     * 发送补偿单次扫描条数
     */
    private int dispatchLimit = 100;

    /**
     * 发送失败最大重试次数
     */
    private int maxRetryCount = 16;

    /**
     * 正在发送状态超时分钟数
     */
    private int sendingTimeoutMinutes = 5;

    /**
     * 消费处理中超时分钟数
     */
    private int consumeTimeoutMinutes = 10;

    /**
     * 分发消费者组
     */
    private String dispatchConsumerGroup = "coupon-grant-dispatch-group";

    /**
     * 批处理消费者组
     */
    private String batchConsumerGroup = "coupon-grant-batch-group";
}
