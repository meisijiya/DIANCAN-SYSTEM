package com.scaffold.modules.coupon.mq.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.config.CouponGrantMqProperties;
import com.scaffold.modules.coupon.mq.payload.CouponGrantDispatchPayload;
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
 * 发券任务分发监听器
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "coupon.grant.mq", name = "enabled", havingValue = "true")
@RocketMQMessageListener(
        consumerGroup = "${coupon.grant.mq.dispatch-consumer-group}",
        topic = "${coupon.grant.mq.topic}",
        selectorExpression = "${coupon.grant.mq.dispatch-tag}",
        consumeMode = ConsumeMode.CONCURRENTLY
)
public class CouponGrantDispatchListener implements RocketMQListener<String> {

    private final CouponGrantAsyncService couponGrantAsyncService;
    private final ReliableMessageService reliableMessageService;
    private final ObjectMapper objectMapper;
    private final CouponGrantMqProperties mqProperties;

    /**
     * 处理分发消息
     *
     * @param message 消息体
     * @author Henfon
     * @date 2026-06-26
     * @description 将发券任务拆分成多个批处理消息
     */
    @Override
    public void onMessage(String message) {
        String messageKey = null;
        try {
            CouponGrantDispatchPayload payload = objectMapper.readValue(message, CouponGrantDispatchPayload.class);
            messageKey = extractMessageKey(payload.getTaskId());
            boolean allowed = reliableMessageService.beginConsume(
                    mqProperties.getDispatchConsumerGroup(),
                    mqProperties.getTopic(),
                    mqProperties.getDispatchTag(),
                    messageKey,
                    String.valueOf(payload.getTaskId())
            );
            if (!allowed) {
                return;
            }

            couponGrantAsyncService.dispatchTask(payload);
            reliableMessageService.markConsumeSuccess(mqProperties.getDispatchConsumerGroup(), messageKey);
        } catch (Exception ex) {
            if (messageKey != null) {
                reliableMessageService.markConsumeFailed(mqProperties.getDispatchConsumerGroup(), messageKey, ex.getMessage());
            }
            log.error("处理发券任务分发消息失败", ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * 提取消息键
     *
     * @param taskId 任务ID
     * @return 消息键
     * @author Henfon
     * @date 2026-06-26
     * @description 监听器仅拿到消息体时，按业务约定重新计算消息键
     */
    private String extractMessageKey(Long taskId) {
        return "coupon-grant-dispatch-" + taskId;
    }
}
