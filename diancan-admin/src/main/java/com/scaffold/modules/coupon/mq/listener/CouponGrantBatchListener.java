package com.scaffold.modules.coupon.mq.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.config.CouponGrantMqProperties;
import com.scaffold.modules.coupon.mq.payload.CouponGrantBatchPayload;
import com.scaffold.modules.coupon.service.CouponGrantAsyncService;
import com.scaffold.modules.mq.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 发券批处理监听器
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "coupon.grant.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        consumerGroup = "${coupon.grant.mq.batch-consumer-group}",
        topic = "${coupon.grant.mq.topic}",
        selectorExpression = "${coupon.grant.mq.batch-tag}",
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class CouponGrantBatchListener implements RocketMQListener<String> {

    private final CouponGrantAsyncService couponGrantAsyncService;
    private final ReliableMessageService reliableMessageService;
    private final ObjectMapper objectMapper;
    private final CouponGrantMqProperties mqProperties;

    /**
     * 处理批量发券消息
     *
     * @param message 消息体
     * @author Henfon
     * @date 2026-06-26
     * @description 消费批处理消息并执行真正的批量发券逻辑
     */
    @Override
    public void onMessage(String message) {
        CouponGrantBatchPayload payload = null;
        String messageKey = null;
        try {
            payload = objectMapper.readValue(message, CouponGrantBatchPayload.class);
            messageKey = "coupon-grant-batch-" + payload.getTaskId() + "-" + payload.getBatchNo();
            boolean allowed = reliableMessageService.beginConsume(
                    mqProperties.getBatchConsumerGroup(),
                    mqProperties.getTopic(),
                    mqProperties.getBatchTag(),
                    messageKey,
                    payload.getTaskId() + ":" + payload.getBatchNo()
            );
            if (!allowed) {
                return;
            }

            couponGrantAsyncService.processBatch(payload);
            reliableMessageService.markConsumeSuccess(mqProperties.getBatchConsumerGroup(), messageKey);
        } catch (Exception ex) {
            if (messageKey != null) {
                reliableMessageService.markConsumeFailed(mqProperties.getBatchConsumerGroup(), messageKey, ex.getMessage());
            }
            log.error("处理发券批量消息失败", ex);
            throw new RuntimeException(ex);
        }
    }
}
