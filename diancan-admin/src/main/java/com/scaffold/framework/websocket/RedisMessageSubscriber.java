package com.scaffold.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 订阅者
 * <p>
 * 接收 Redis 频道消息并转发至 WebSocket STOMP 目的地，
 * 实现多实例部署下的消息广播。
 *
 * @author Henfon
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            WsMessage wsMessage = OBJECT_MAPPER.readValue(body, WsMessage.class);

            if (wsMessage.getTopic() != null) {
                messagingTemplate.convertAndSend(wsMessage.getTopic(), wsMessage);
                log.debug("Forwarded WebSocket message to topic [{}], event: {}",
                        wsMessage.getTopic(), wsMessage.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process Redis Pub/Sub message", e);
        }
    }
}
