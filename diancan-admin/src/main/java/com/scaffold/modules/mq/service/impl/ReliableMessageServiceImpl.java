package com.scaffold.modules.mq.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.config.CouponGrantMqProperties;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.coupon.constant.CouponGrantConstants;
import com.scaffold.modules.coupon.entity.CouponGrantTask;
import com.scaffold.modules.coupon.mapper.CouponGrantTaskMapper;
import com.scaffold.modules.mq.constant.MqStatusConstants;
import com.scaffold.modules.mq.dto.MqMessageQueryDTO;
import com.scaffold.modules.mq.entity.MqConsumeLog;
import com.scaffold.modules.mq.entity.MqMessage;
import com.scaffold.modules.mq.mapper.MqConsumeLogMapper;
import com.scaffold.modules.mq.mapper.MqMessageMapper;
import com.scaffold.modules.mq.service.ReliableMessageService;
import com.scaffold.modules.mq.vo.MqMessageVO;
import com.scaffold.modules.mq.constant.MqBizTypeConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 可靠消息服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReliableMessageServiceImpl implements ReliableMessageService {

    private static final int SEND_TIMEOUT_MS = 10000;

    private final MqMessageMapper mqMessageMapper;
    private final MqConsumeLogMapper mqConsumeLogMapper;
    private final CouponGrantTaskMapper couponGrantTaskMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final CouponGrantMqProperties mqProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(String topic, String tag, String bizType, String bizKey, String messageKey, String payloadJson) {
        MqMessage message = new MqMessage();
        message.setMessageKey(messageKey);
        message.setTopic(topic);
        message.setTag(tag);
        message.setBizType(bizType);
        message.setBizKey(bizKey);
        message.setPayload(payloadJson);
        message.setDeliverStatus(MqStatusConstants.DELIVER_PENDING);
        message.setRetryCount(0);
        message.setNextRetryTime(LocalDateTime.now());
        try {
            mqMessageMapper.insert(message);
        } catch (DuplicateKeyException ex) {
            log.warn("可靠消息已存在，忽略重复创建: messageKey={}", messageKey);
        }
    }

    @Override
    public void dispatchPendingMessages() {
        if (!mqProperties.isEnabled()) {
            return;
        }

        LambdaQueryWrapper<MqMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(query -> query
                        .and(item -> item.in(MqMessage::getDeliverStatus, MqStatusConstants.DELIVER_PENDING, MqStatusConstants.DELIVER_FAILED)
                                .le(MqMessage::getNextRetryTime, LocalDateTime.now()))
                        .or(item -> item.eq(MqMessage::getDeliverStatus, MqStatusConstants.DELIVER_SENDING)
                                .le(MqMessage::getUpdateTime, LocalDateTime.now().minusMinutes(mqProperties.getSendingTimeoutMinutes()))))
                .orderByAsc(MqMessage::getNextRetryTime)
                .last("LIMIT " + mqProperties.getDispatchLimit());
        List<MqMessage> messages = mqMessageMapper.selectList(wrapper);
        for (MqMessage message : messages) {
            sendSingleMessage(message);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean beginConsume(String consumerGroup, String topic, String tag, String messageKey, String bizKey) {
        MqConsumeLog consumeLog = new MqConsumeLog();
        consumeLog.setConsumerGroup(consumerGroup);
        consumeLog.setTopic(topic);
        consumeLog.setTag(tag);
        consumeLog.setMessageKey(messageKey);
        consumeLog.setBizKey(bizKey);
        consumeLog.setConsumeStatus(MqStatusConstants.CONSUME_PROCESSING);
        consumeLog.setRetryCount(0);

        try {
            mqConsumeLogMapper.insert(consumeLog);
            return true;
        } catch (DuplicateKeyException ex) {
            LambdaQueryWrapper<MqConsumeLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MqConsumeLog::getConsumerGroup, consumerGroup)
                    .eq(MqConsumeLog::getMessageKey, messageKey)
                    .last("LIMIT 1");
            MqConsumeLog existing = mqConsumeLogMapper.selectOne(wrapper);
            if (existing == null) {
                return false;
            }
            if (MqStatusConstants.CONSUME_SUCCESS == existing.getConsumeStatus()) {
                return false;
            }
            if (MqStatusConstants.CONSUME_PROCESSING == existing.getConsumeStatus()
                    && existing.getUpdateTime() != null
                    && existing.getUpdateTime().isAfter(LocalDateTime.now().minusMinutes(mqProperties.getConsumeTimeoutMinutes()))) {
                return false;
            }

            LambdaUpdateWrapper<MqConsumeLog> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(MqConsumeLog::getId, existing.getId())
                    .set(MqConsumeLog::getConsumeStatus, MqStatusConstants.CONSUME_PROCESSING)
                    .set(MqConsumeLog::getRetryCount, existing.getRetryCount() + 1)
                    .set(MqConsumeLog::getLastError, null)
                    .set(MqConsumeLog::getFinishedTime, null);
            mqConsumeLogMapper.update(null, updateWrapper);
            return true;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConsumeSuccess(String consumerGroup, String messageKey) {
        LambdaUpdateWrapper<MqConsumeLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MqConsumeLog::getConsumerGroup, consumerGroup)
                .eq(MqConsumeLog::getMessageKey, messageKey)
                .set(MqConsumeLog::getConsumeStatus, MqStatusConstants.CONSUME_SUCCESS)
                .set(MqConsumeLog::getFinishedTime, LocalDateTime.now())
                .set(MqConsumeLog::getLastError, null);
        mqConsumeLogMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markConsumeFailed(String consumerGroup, String messageKey, String errorMessage) {
        LambdaUpdateWrapper<MqConsumeLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MqConsumeLog::getConsumerGroup, consumerGroup)
                .eq(MqConsumeLog::getMessageKey, messageKey)
                .set(MqConsumeLog::getConsumeStatus, MqStatusConstants.CONSUME_FAILED)
                .set(MqConsumeLog::getFinishedTime, LocalDateTime.now())
                .set(MqConsumeLog::getLastError, trimError(errorMessage));
        mqConsumeLogMapper.update(null, wrapper);
    }

    @Override
    public PageResult<MqMessageVO> pageMessages(MqMessageQueryDTO dto) {
        Page<MqMessage> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<MqMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getDeliverStatus() != null, MqMessage::getDeliverStatus, dto.getDeliverStatus())
                .eq(StrUtil.isNotBlank(dto.getBizType()), MqMessage::getBizType, dto.getBizType())
                .like(StrUtil.isNotBlank(dto.getMessageKey()), MqMessage::getMessageKey, dto.getMessageKey())
                .orderByDesc(MqMessage::getCreateTime);
        Page<MqMessage> result = mqMessageMapper.selectPage(page, wrapper);
        List<MqMessageVO> records = BeanUtil.copyToList(result.getRecords(), MqMessageVO.class);
        enrichBizResult(records);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 填充业务结果摘要
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 对发券类消息补充任务执行结果，避免消息投递成功被误解为业务全部成功。
     * @param records 消息记录
     */
    private void enrichBizResult(List<MqMessageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Map<Long, CouponGrantTask> couponTaskMap = loadCouponGrantTaskMap(records);
        for (MqMessageVO record : records) {
            if (!isCouponGrantBiz(record.getBizType())) {
                continue;
            }

            Long taskId = extractCouponTaskId(record.getBizKey());
            if (taskId == null) {
                record.setBizStatusText("业务任务未知");
                record.setBizStatusDetail("未能从业务主键解析发券任务ID");
                continue;
            }

            CouponGrantTask task = couponTaskMap.get(taskId);
            if (task == null) {
                record.setBizStatusText("业务任务不存在");
                record.setBizStatusDetail("未查询到对应发券任务");
                continue;
            }

            record.setBizStatus(task.getTaskStatus());
            record.setBizStatusText(mapCouponTaskStatusText(task.getTaskStatus()));
            record.setBizStatusDetail(buildCouponTaskDetail(task));
        }
    }

    /**
     * 批量加载发券任务映射
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 从消息业务主键中提取任务ID，批量查询发券任务状态用于消息页展示。
     * @param records 消息记录
     * @return 发券任务映射
     */
    private Map<Long, CouponGrantTask> loadCouponGrantTaskMap(List<MqMessageVO> records) {
        Set<Long> taskIds = records.stream()
                .filter(item -> isCouponGrantBiz(item.getBizType()))
                .map(item -> extractCouponTaskId(item.getBizKey()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (taskIds.isEmpty()) {
            return Map.of();
        }

        LambdaQueryWrapper<CouponGrantTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CouponGrantTask::getId, taskIds);
        List<CouponGrantTask> tasks = couponGrantTaskMapper.selectList(wrapper);
        Map<Long, CouponGrantTask> result = new HashMap<>(tasks.size());
        for (CouponGrantTask task : tasks) {
            result.put(task.getId(), task);
        }
        return result;
    }

    /**
     * 判断是否为发券业务消息
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 目前仅为发券分发和发券批处理消息补充业务结果摘要。
     * @param bizType 业务类型
     * @return 是否为发券业务
     */
    private boolean isCouponGrantBiz(String bizType) {
        return MqBizTypeConstants.COUPON_GRANT_DISPATCH.equals(bizType)
                || MqBizTypeConstants.COUPON_GRANT_BATCH.equals(bizType);
    }

    /**
     * 解析发券任务ID
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 兼容任务分发的 taskId 和批处理的 taskId:batchNo 两种业务主键格式。
     * @param bizKey 业务主键
     * @return 发券任务ID
     */
    private Long extractCouponTaskId(String bizKey) {
        if (StrUtil.isBlank(bizKey)) {
            return null;
        }
        String rawTaskId = bizKey.contains(":") ? StrUtil.subBefore(bizKey, ":", false) : bizKey;
        if (!StrUtil.isNumeric(rawTaskId)) {
            return null;
        }
        return Long.valueOf(rawTaskId);
    }

    /**
     * 映射发券任务状态文案
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 将发券任务状态码转换为运营可读文案。
     * @param taskStatus 任务状态
     * @return 任务状态文案
     */
    private String mapCouponTaskStatusText(Integer taskStatus) {
        if (taskStatus == null) {
            return "业务状态未知";
        }
        return switch (taskStatus) {
            case CouponGrantConstants.TASK_PENDING -> "待处理";
            case CouponGrantConstants.TASK_DISPATCHING -> "分发中";
            case CouponGrantConstants.TASK_PROCESSING -> "处理中";
            case CouponGrantConstants.TASK_SUCCESS -> "业务成功";
            case CouponGrantConstants.TASK_PARTIAL_SUCCESS -> "部分成功";
            case CouponGrantConstants.TASK_FAILED -> "业务失败";
            default -> "业务状态未知";
        };
    }

    /**
     * 构建发券任务摘要
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 输出成功数、失败数和最近错误，帮助运营直接在消息页判断业务结果。
     * @param task 发券任务
     * @return 摘要文案
     */
    private String buildCouponTaskDetail(CouponGrantTask task) {
        String summary = String.format("成功%d，失败%d，目标%d",
                defaultInt(task.getSuccessCount()),
                defaultInt(task.getFailCount()),
                defaultInt(task.getTargetCount()));
        if (StrUtil.isBlank(task.getLastError())) {
            return summary;
        }
        return summary + "；最近错误：" + StrUtil.maxLength(task.getLastError(), 80);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryMessage(Long id) {
        MqMessage message = mqMessageMapper.selectById(id);
        if (message == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "消息记录不存在");
        }

        LambdaUpdateWrapper<MqMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MqMessage::getId, id)
                .set(MqMessage::getDeliverStatus, MqStatusConstants.DELIVER_PENDING)
                .set(MqMessage::getNextRetryTime, LocalDateTime.now())
                .set(MqMessage::getLastError, null);
        mqMessageMapper.update(null, wrapper);
    }

    /**
     * 发送单条消息
     *
     * @param message 消息实体
     * @author Henfon
     * @date 2026-06-26
     * @description 先抢占消息，再同步发送到 RocketMQ，失败后进入补偿重试
     */
    private void sendSingleMessage(MqMessage message) {
        int claimed = mqMessageMapper.claimForSend(
                message.getId(),
                MqStatusConstants.DELIVER_PENDING,
                MqStatusConstants.DELIVER_FAILED,
                MqStatusConstants.DELIVER_SENDING,
                LocalDateTime.now(),
                LocalDateTime.now().minusMinutes(mqProperties.getSendingTimeoutMinutes())
        );
        if (claimed <= 0) {
            return;
        }

        String destination = buildDestination(message.getTopic(), message.getTag());
        try {
            SendResult sendResult = rocketMQTemplate.syncSend(
                    destination,
                    MessageBuilder.withPayload(message.getPayload())
                            .setHeader(RocketMQHeaders.KEYS, message.getMessageKey())
                            .build(),
                    SEND_TIMEOUT_MS
            );
            LambdaUpdateWrapper<MqMessage> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(MqMessage::getId, message.getId())
                    .set(MqMessage::getDeliverStatus, MqStatusConstants.DELIVER_SENT)
                    .set(MqMessage::getSentTime, LocalDateTime.now())
                    .set(MqMessage::getLastError, null);
            mqMessageMapper.update(null, wrapper);
            log.info("RocketMQ 消息发送成功: messageKey={}, msgId={}", message.getMessageKey(), sendResult.getMsgId());
        } catch (Exception ex) {
            handleSendFailure(message, ex);
        }
    }

    /**
     * 处理发送失败
     *
     * @param message 消息实体
     * @param ex 异常
     * @author Henfon
     * @date 2026-06-26
     * @description 根据重试次数决定继续补偿还是转为死信
     */
    private void handleSendFailure(MqMessage message, Exception ex) {
        int nextRetryCount = defaultInt(message.getRetryCount()) + 1;
        boolean dead = nextRetryCount >= mqProperties.getMaxRetryCount();
        LambdaUpdateWrapper<MqMessage> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MqMessage::getId, message.getId())
                .set(MqMessage::getDeliverStatus, dead ? MqStatusConstants.DELIVER_DEAD : MqStatusConstants.DELIVER_FAILED)
                .set(MqMessage::getRetryCount, nextRetryCount)
                .set(MqMessage::getLastError, trimError(ex.getMessage()))
                .set(MqMessage::getNextRetryTime, LocalDateTime.now().plusSeconds(calculateBackoffSeconds(nextRetryCount)));
        mqMessageMapper.update(null, wrapper);
        log.error("RocketMQ 消息发送失败: messageKey={}, retryCount={}", message.getMessageKey(), nextRetryCount, ex);
    }

    /**
     * 构建 RocketMQ 目的地
     *
     * @param topic 主题
     * @param tag 标签
     * @return 目的地字符串
     * @author Henfon
     * @date 2026-06-26
     * @description 按 RocketMQTemplate 要求拼装 topic:tag 形式目的地
     */
    private String buildDestination(String topic, String tag) {
        return StrUtil.isBlank(tag) ? topic : topic + ":" + tag;
    }

    /**
     * 计算退避秒数
     *
     * @param retryCount 重试次数
     * @return 退避秒数
     * @author Henfon
     * @date 2026-06-26
     * @description 使用指数退避控制重试节奏，避免故障期持续冲击 MQ
     */
    private long calculateBackoffSeconds(int retryCount) {
        return Math.min(1800L, (long) Math.pow(2, Math.min(retryCount, 8)) * 5L);
    }

    /**
     * 裁剪错误信息
     *
     * @param errorMessage 原始错误信息
     * @return 裁剪后的错误信息
     * @author Henfon
     * @date 2026-06-26
     * @description 控制错误字段长度，避免数据库字段溢出
     */
    private String trimError(String errorMessage) {
        if (StrUtil.isBlank(errorMessage)) {
            return null;
        }
        return StrUtil.maxLength(errorMessage, 500);
    }

    /**
     * 获取默认整数
     *
     * @param value 原值
     * @return 默认值
     * @author Henfon
     * @date 2026-06-26
     * @description 将可空整数统一为 0，避免重试计数空指针
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
