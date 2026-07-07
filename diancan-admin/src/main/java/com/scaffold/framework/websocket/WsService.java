package com.scaffold.framework.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scaffold.common.enums.WsEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 消息发送服务
 * <p>
 * 通过 Redis Pub/Sub 发布消息，确保多实例部署时所有节点都能收到消息
 * 并通过 {@link RedisMessageSubscriber} 转发至 WebSocket 客户端。
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WsService {

    private final StringRedisTemplate stringRedisTemplate;

    /** Redis Pub/Sub 频道前缀 */
    private static final String CHANNEL_PREFIX = "ws:channel:";

    /** 默认广播频道 */
    public static final String DEFAULT_CHANNEL = "broadcast";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * 向指定 topic 广播消息
     *
     * @param eventType 事件类型
     * @param topic     WebSocket 目的地（如 /topic/kitchen）
     * @param data      业务数据
     */
    public void broadcast(WsEventType eventType, String topic, Object data) {
        broadcast(DEFAULT_CHANNEL, eventType, topic, data);
    }

    /**
     * 向指定 Redis 频道和 WebSocket topic 广播消息
     *
     * @param channel   Redis Pub/Sub 频道名
     * @param eventType 事件类型
     * @param topic     WebSocket 目的地
     * @param data      业务数据
     */
    public void broadcast(String channel, WsEventType eventType, String topic, Object data) {
        try {
            WsMessage message = WsMessage.of(eventType, topic, data);
            String json = OBJECT_MAPPER.writeValueAsString(message);
            stringRedisTemplate.convertAndSend(CHANNEL_PREFIX + channel, json);
            log.debug("Published WebSocket message to channel [{}], topic [{}], event: {}",
                    channel, topic, eventType);
        } catch (Exception e) {
            log.error("Failed to publish WebSocket message via Redis Pub/Sub", e);
        }
    }
}
