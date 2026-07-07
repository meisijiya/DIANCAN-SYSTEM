package com.scaffold.modules.mq.task;

import com.scaffold.modules.mq.service.ReliableMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * MQ 消息补偿任务
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqMessageDispatchTask {

    private final ReliableMessageService reliableMessageService;

    /**
     * 定时投递待发送消息
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 按固定间隔扫描本地消息表，补偿发送 RocketMQ 消息
     */
    @Scheduled(fixedDelay = 5000)
    public void dispatchPendingMessages() {
        reliableMessageService.dispatchPendingMessages();
    }
}
