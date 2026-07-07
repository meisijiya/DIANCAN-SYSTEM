package com.scaffold.modules.mq.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.mq.dto.MqMessageQueryDTO;
import com.scaffold.modules.mq.vo.MqMessageVO;

/**
 * 可靠消息服务
 *
 * @author Henfon
 */
public interface ReliableMessageService {

    /**
     * 保存待发送消息
     *
     * @param topic 主题
     * @param tag 标签
     * @param bizType 业务类型
     * @param bizKey 业务主键
     * @param messageKey 消息键
     * @param payloadJson JSON 消息体
     * @author Henfon
     * @date 2026-06-26
     * @description 将业务消息先落库，再由调度器异步投递到 RocketMQ
     */
    void saveMessage(String topic, String tag, String bizType, String bizKey, String messageKey, String payloadJson);

    /**
     * 投递待发送消息
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 定时扫描本地消息表并补偿发送 RocketMQ 消息
     */
    void dispatchPendingMessages();

    /**
     * 开始消费消息
     *
     * @param consumerGroup 消费者组
     * @param topic 主题
     * @param tag 标签
     * @param messageKey 消息键
     * @param bizKey 业务主键
     * @return 是否获得消费执行权
     * @author Henfon
     * @date 2026-06-26
     * @description 基于消费日志表做幂等校验，避免重复消费
     */
    boolean beginConsume(String consumerGroup, String topic, String tag, String messageKey, String bizKey);

    /**
     * 标记消费成功
     *
     * @param consumerGroup 消费者组
     * @param messageKey 消息键
     * @author Henfon
     * @date 2026-06-26
     * @description 业务执行成功后回写幂等日志
     */
    void markConsumeSuccess(String consumerGroup, String messageKey);

    /**
     * 标记消费失败
     *
     * @param consumerGroup 消费者组
     * @param messageKey 消息键
     * @param errorMessage 错误信息
     * @author Henfon
     * @date 2026-06-26
     * @description 业务执行失败后记录异常，供 RocketMQ 重试和人工排查
     */
    void markConsumeFailed(String consumerGroup, String messageKey, String errorMessage);

    /**
     * 分页查询消息记录
     *
     * @param dto 查询条件
     * @return 消息分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 提供管理端查看消息发送状态与重试信息
     */
    PageResult<MqMessageVO> pageMessages(MqMessageQueryDTO dto);

    /**
     * 手动重试消息
     *
     * @param id 消息ID
     * @author Henfon
     * @date 2026-06-26
     * @description 将失败或死信消息恢复为待发送状态，供人工触发补偿
     */
    void retryMessage(Long id);
}
