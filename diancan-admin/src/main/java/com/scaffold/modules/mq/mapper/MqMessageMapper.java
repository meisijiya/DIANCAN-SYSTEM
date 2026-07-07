package com.scaffold.modules.mq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.mq.entity.MqMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * MQ 消息 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface MqMessageMapper extends BaseMapper<MqMessage> {

    /**
     * 抢占待发送消息
     *
     * @param id 消息ID
     * @param pendingStatus 待发送状态
     * @param failedStatus 失败状态
     * @param sendingStatus 发送中状态
     * @param retryBefore 下次重试时间阈值
     * @param sendingExpireTime 发送中超时阈值
     * @return 更新条数
     * @author Henfon
     * @date 2026-06-26
     * @description 通过原子更新把消息占用到当前发送流程，避免并发重复发送
     */
    int claimForSend(@Param("id") Long id,
                     @Param("pendingStatus") Integer pendingStatus,
                     @Param("failedStatus") Integer failedStatus,
                     @Param("sendingStatus") Integer sendingStatus,
                     @Param("retryBefore") LocalDateTime retryBefore,
                     @Param("sendingExpireTime") LocalDateTime sendingExpireTime);
}
