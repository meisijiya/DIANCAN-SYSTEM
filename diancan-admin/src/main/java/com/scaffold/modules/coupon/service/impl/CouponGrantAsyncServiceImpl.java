package com.scaffold.modules.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.config.CouponGrantMqProperties;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.coupon.constant.CouponGrantConstants;
import com.scaffold.modules.coupon.dto.CouponGrantDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskDetailQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskQueryDTO;
import com.scaffold.modules.coupon.entity.CouponGrantTask;
import com.scaffold.modules.coupon.entity.CouponGrantTaskUser;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import com.scaffold.modules.coupon.entity.UserCoupon;
import com.scaffold.modules.coupon.mapper.CouponGrantTaskMapper;
import com.scaffold.modules.coupon.mapper.CouponGrantTaskUserMapper;
import com.scaffold.modules.coupon.mapper.CouponTemplateMapper;
import com.scaffold.modules.coupon.mapper.UserCouponMapper;
import com.scaffold.modules.coupon.mq.payload.CouponGrantBatchPayload;
import com.scaffold.modules.coupon.mq.payload.CouponGrantDispatchPayload;
import com.scaffold.modules.coupon.service.CouponGrantAsyncService;
import com.scaffold.modules.coupon.vo.CouponGrantTaskDetailVO;
import com.scaffold.modules.coupon.vo.CouponGrantTaskVO;
import com.scaffold.modules.mq.constant.MqBizTypeConstants;
import com.scaffold.modules.mq.service.ReliableMessageService;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 异步发券服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponGrantAsyncServiceImpl implements CouponGrantAsyncService {

    private static final int TEMPLATE_STATUS_ENABLED = 1;
    private static final int VALIDITY_TYPE_FIXED = 1;
    private static final int GRANT_MODE_ASSIGN = 1;
    private static final int COUPON_STATUS_UNUSED = 0;
    private static final List<Integer> PROCESSING_TASK_STATUSES = Arrays.asList(
            CouponGrantConstants.TASK_PENDING,
            CouponGrantConstants.TASK_DISPATCHING,
            CouponGrantConstants.TASK_PROCESSING
    );

    private final CouponGrantTaskMapper couponGrantTaskMapper;
    private final CouponGrantTaskUserMapper couponGrantTaskUserMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final SysUserMapper sysUserMapper;
    private final ReliableMessageService reliableMessageService;
    private final CouponGrantMqProperties mqProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponGrantTaskVO createGrantTask(CouponTemplate template, CouponGrantDTO dto) {
        validateNoDuplicateProcessingTask(template.getId(), dto.getGrantMode());
        validateGrantParams(dto);

        CouponGrantTask task = new CouponGrantTask();
        task.setTemplateId(template.getId());
        task.setTemplateName(template.getName());
        task.setGrantMode(dto.getGrantMode());
        task.setTaskStatus(CouponGrantConstants.TASK_PENDING);
        task.setTargetCount(calculatePendingTargetCount(dto));
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setRemark(dto.getRemark());
        task.setTotalBatchCount(0);
        task.setFinishedBatchCount(0);
        couponGrantTaskMapper.insert(task);

        CouponGrantDispatchPayload payload = new CouponGrantDispatchPayload();
        payload.setTaskId(task.getId());
        payload.setGrantMode(dto.getGrantMode());
        payload.setUserIds(dto.getUserIds());
        reliableMessageService.saveMessage(
                mqProperties.getTopic(),
                mqProperties.getDispatchTag(),
                MqBizTypeConstants.COUPON_GRANT_DISPATCH,
                String.valueOf(task.getId()),
                buildDispatchMessageKey(task.getId()),
                toJson(payload)
        );

        return BeanUtil.copyProperties(task, CouponGrantTaskVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchTask(CouponGrantDispatchPayload payload) {
        Long taskId = payload.getTaskId();
        CouponGrantTask task = couponGrantTaskMapper.selectByIdForUpdate(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发券任务不存在");
        }
        if (task.getTotalBatchCount() != null && task.getTotalBatchCount() > 0) {
            return;
        }

        List<SysUser> targetUsers = listGrantUsers(payload);
        if (targetUsers.isEmpty()) {
            updateTaskAsFailed(taskId, "未找到可发放的用户");
            throw new BusinessException(ResultCode.PARAM_ERROR, "未找到可发放的用户");
        }

        List<CouponGrantTaskUser> snapshotUsers = buildTaskUsers(taskId, targetUsers);
        saveTaskUsersInBatch(snapshotUsers);

        LambdaQueryWrapper<CouponGrantTaskUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantTaskUser::getTaskId, taskId)
                .eq(CouponGrantTaskUser::getGrantStatus, CouponGrantConstants.TASK_USER_PENDING)
                .orderByAsc(CouponGrantTaskUser::getId);
        List<CouponGrantTaskUser> pendingTaskUsers = couponGrantTaskUserMapper.selectList(wrapper);
        if (pendingTaskUsers.isEmpty()) {
            updateTaskAsFailed(taskId, "任务用户快照为空");
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务用户快照为空");
        }

        int batchSize = Math.max(1, mqProperties.getBatchSize());
        int totalBatchCount = (pendingTaskUsers.size() + batchSize - 1) / batchSize;
        for (int i = 0; i < totalBatchCount; i++) {
            int fromIndex = i * batchSize;
            int toIndex = Math.min(fromIndex + batchSize, pendingTaskUsers.size());
            List<Long> taskUserIds = pendingTaskUsers.subList(fromIndex, toIndex).stream()
                    .map(CouponGrantTaskUser::getId)
                    .collect(Collectors.toList());

            CouponGrantBatchPayload batchPayload = new CouponGrantBatchPayload();
            batchPayload.setTaskId(taskId);
            batchPayload.setBatchNo(i + 1);
            batchPayload.setTaskUserIds(taskUserIds);

            reliableMessageService.saveMessage(
                    mqProperties.getTopic(),
                    mqProperties.getBatchTag(),
                    MqBizTypeConstants.COUPON_GRANT_BATCH,
                    taskId + ":" + (i + 1),
                    buildBatchMessageKey(taskId, i + 1),
                    toJson(batchPayload)
            );
        }

        LambdaUpdateWrapper<CouponGrantTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CouponGrantTask::getId, taskId)
                .set(CouponGrantTask::getTaskStatus, CouponGrantConstants.TASK_PROCESSING)
                .set(CouponGrantTask::getStartedTime, LocalDateTime.now())
                .set(CouponGrantTask::getTargetCount, pendingTaskUsers.size())
                .set(CouponGrantTask::getTotalBatchCount, totalBatchCount)
                .set(CouponGrantTask::getLastError, null);
        couponGrantTaskMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processBatch(CouponGrantBatchPayload payload) {
        CouponGrantTask task = couponGrantTaskMapper.selectByIdForUpdate(payload.getTaskId());
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发券任务不存在");
        }

        CouponTemplate template = couponTemplateMapper.selectByIdForUpdate(task.getTemplateId());
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券模板不存在");
        }
        if (!Objects.equals(template.getStatus(), TEMPLATE_STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券模板未启用");
        }

        List<CouponGrantTaskUser> taskUsers = couponGrantTaskUserMapper.selectBatchIds(payload.getTaskUserIds());
        if (taskUsers.isEmpty()) {
            completeBatch(task, 0, 0);
            return;
        }

        Map<Long, Long> receivedCountMap = loadUserReceivedCountMap(taskUsers, template.getId());
        int remainingQuantity = calculateRemainingQuantity(template);
        int successCount = 0;
        int failCount = 0;
        List<UserCoupon> coupons = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (CouponGrantTaskUser taskUser : taskUsers) {
            if (!Objects.equals(taskUser.getGrantStatus(), CouponGrantConstants.TASK_USER_PENDING)) {
                continue;
            }

            if (remainingQuantity == 0) {
                markTaskUserFailure(taskUser, "优惠券库存不足");
                failCount++;
                continue;
            }

            long currentReceivedCount = receivedCountMap.getOrDefault(taskUser.getUserId(), 0L);
            if (!canReceiveTemplate(currentReceivedCount, template)) {
                markTaskUserFailure(taskUser, "用户已达到限领次数");
                failCount++;
                continue;
            }

            UserCoupon coupon = buildUserCoupon(taskUser, template, task.getId(), task.getGrantMode(), now);
            coupons.add(coupon);
            receivedCountMap.put(taskUser.getUserId(), currentReceivedCount + 1);
            if (remainingQuantity > 0) {
                remainingQuantity--;
            }
            markTaskUserSuccess(taskUser, now);
            successCount++;
        }

        if (!coupons.isEmpty()) {
            userCouponMapper.batchInsert(coupons);
            LambdaUpdateWrapper<CouponTemplate> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(CouponTemplate::getId, template.getId())
                    .set(CouponTemplate::getIssuedQuantity, defaultInt(template.getIssuedQuantity()) + successCount);
            couponTemplateMapper.update(null, wrapper);
        }

        completeBatch(task, successCount, failCount);
    }

    @Override
    public PageResult<CouponGrantTaskVO> pageGrantTasks(CouponGrantTaskQueryDTO dto) {
        Page<CouponGrantTask> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<CouponGrantTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getTaskStatus() != null, CouponGrantTask::getTaskStatus, dto.getTaskStatus())
                .like(StrUtil.isNotBlank(dto.getTemplateName()), CouponGrantTask::getTemplateName, dto.getTemplateName())
                .orderByDesc(CouponGrantTask::getCreateTime);
        Page<CouponGrantTask> result = couponGrantTaskMapper.selectPage(page, wrapper);
        List<CouponGrantTaskVO> records = BeanUtil.copyToList(result.getRecords(), CouponGrantTaskVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public PageResult<CouponGrantTaskDetailVO> pageGrantTaskDetails(CouponGrantTaskDetailQueryDTO dto) {
        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "任务ID不能为空");
        }

        // 先校验任务存在，避免前端传入无效任务ID时返回空列表造成误判。
        CouponGrantTask task = couponGrantTaskMapper.selectById(dto.getTaskId());
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "发券任务不存在");
        }

        Page<CouponGrantTaskUser> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<CouponGrantTaskUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantTaskUser::getTaskId, dto.getTaskId())
                .eq(dto.getGrantStatus() != null, CouponGrantTaskUser::getGrantStatus, dto.getGrantStatus())
                .and(StrUtil.isNotBlank(dto.getKeyword()), keyword -> keyword
                        .like(CouponGrantTaskUser::getUsername, dto.getKeyword())
                        .or()
                        .like(CouponGrantTaskUser::getPhone, dto.getKeyword()))
                .orderByAsc(CouponGrantTaskUser::getGrantStatus)
                .orderByDesc(CouponGrantTaskUser::getFinishedTime)
                .orderByAsc(CouponGrantTaskUser::getId);
        Page<CouponGrantTaskUser> result = couponGrantTaskUserMapper.selectPage(page, wrapper);
        List<CouponGrantTaskDetailVO> records = BeanUtil.copyToList(result.getRecords(), CouponGrantTaskDetailVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 查询发券目标用户
     *
     * @param dto 发券参数
     * @return 用户列表
     * @author Henfon
     * @date 2026-06-26
     * @description 统一按用户启用状态筛选发券目标，避免给禁用账号发券
     */
    private List<SysUser> listGrantUsers(CouponGrantDispatchPayload payload) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, 1)
                .orderByAsc(SysUser::getId);
        if (Objects.equals(payload.getGrantMode(), GRANT_MODE_ASSIGN)) {
            if (payload.getUserIds() == null || payload.getUserIds().isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "请选择至少一个用户");
            }
            wrapper.in(SysUser::getId, payload.getUserIds());
        }
        return sysUserMapper.selectList(wrapper);
    }

    /**
     * 校验发券入参
     *
     * @param dto 发券参数
     * @author Henfon
     * @date 2026-07-01
     * @description 在创建任务前快速校验指定发券的用户列表，避免创建无效空任务
     */
    private void validateGrantParams(CouponGrantDTO dto) {
        if (Objects.equals(dto.getGrantMode(), GRANT_MODE_ASSIGN)
                && (dto.getUserIds() == null || dto.getUserIds().isEmpty())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择至少一个用户");
        }
    }

    /**
     * 计算任务待处理目标数
     *
     * @param dto 发券参数
     * @return 目标人数
     * @author Henfon
     * @date 2026-07-01
     * @description 指定用户发券先按去重后的用户数预估，全部用户发券交给分发阶段回填真实人数
     */
    private int calculatePendingTargetCount(CouponGrantDTO dto) {
        if (!Objects.equals(dto.getGrantMode(), GRANT_MODE_ASSIGN) || dto.getUserIds() == null) {
            return 0;
        }
        return (int) dto.getUserIds().stream().filter(Objects::nonNull).distinct().count();
    }

    /**
     * 校验是否存在重复进行中的发券任务
     *
     * @param templateId 模板ID
     * @param grantMode 发放方式
     * @author Henfon
     * @date 2026-06-27
     * @description 避免用户连续点击导致同一模板、同一发放方式重复创建进行中的任务
     */
    private void validateNoDuplicateProcessingTask(Long templateId, Integer grantMode) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<CouponGrantTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantTask::getTemplateId, templateId)
                .eq(CouponGrantTask::getGrantMode, grantMode)
                .eq(CouponGrantTask::getCreateBy, currentUserId)
                .in(CouponGrantTask::getTaskStatus, PROCESSING_TASK_STATUSES)
                .orderByDesc(CouponGrantTask::getCreateTime)
                .last("LIMIT 1");
        CouponGrantTask processingTask = couponGrantTaskMapper.selectOne(wrapper);
        if (processingTask != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "已有相同发券任务正在处理中，请勿重复提交");
        }
    }

    /**
     * 构建任务用户快照
     *
     * @param taskId 任务ID
     * @param users 用户列表
     * @return 任务用户快照列表
     * @author Henfon
     * @date 2026-06-26
     * @description 冻结本次任务的目标用户，保证后续异步处理范围稳定
     */
    private List<CouponGrantTaskUser> buildTaskUsers(Long taskId, List<SysUser> users) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponGrantTaskUser> taskUsers = new ArrayList<>(users.size());
        for (SysUser user : users) {
            CouponGrantTaskUser taskUser = new CouponGrantTaskUser();
            taskUser.setId(IdWorker.getId());
            taskUser.setTaskId(taskId);
            taskUser.setUserId(user.getId());
            taskUser.setUsername(user.getUsername());
            taskUser.setPhone(user.getPhone());
            taskUser.setGrantStatus(CouponGrantConstants.TASK_USER_PENDING);
            taskUser.setCreateBy(1L);
            taskUser.setUpdateBy(1L);
            taskUser.setCreateTime(now);
            taskUser.setUpdateTime(now);
            taskUser.setDeleted(0);
            taskUsers.add(taskUser);
        }
        return taskUsers;
    }

    /**
     * 批量保存任务用户快照
     *
     * @param taskUsers 任务用户列表
     * @author Henfon
     * @date 2026-06-26
     * @description 按固定分片批量落库，避免单 SQL 过大
     */
    private void saveTaskUsersInBatch(List<CouponGrantTaskUser> taskUsers) {
        int chunkSize = 1000;
        for (int i = 0; i < taskUsers.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, taskUsers.size());
            couponGrantTaskUserMapper.batchInsert(taskUsers.subList(i, end));
        }
    }

    /**
     * 加载用户已领数量映射
     *
     * @param taskUsers 任务用户列表
     * @param templateId 模板ID
     * @return 用户已领数量
     * @author Henfon
     * @date 2026-06-26
     * @description 批量查询本批用户的已领券数，避免逐条 selectCount
     */
    private Map<Long, Long> loadUserReceivedCountMap(List<CouponGrantTaskUser> taskUsers, Long templateId) {
        List<Long> userIds = taskUsers.stream().map(CouponGrantTaskUser::getUserId).collect(Collectors.toList());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(UserCoupon::getUserId)
                .eq(UserCoupon::getTemplateId, templateId)
                .in(UserCoupon::getUserId, userIds);
        List<UserCoupon> coupons = userCouponMapper.selectList(wrapper);
        return coupons.stream().collect(Collectors.groupingBy(UserCoupon::getUserId, Collectors.counting()));
    }

    /**
     * 计算剩余库存
     *
     * @param template 模板
     * @return 剩余库存，不限量返回 -1
     * @author Henfon
     * @date 2026-06-26
     * @description 根据模板当前库存决定本批还能继续发多少张
     */
    private int calculateRemainingQuantity(CouponTemplate template) {
        Integer totalQuantity = template.getTotalQuantity();
        if (totalQuantity == null || totalQuantity <= 0) {
            return -1;
        }
        return Math.max(0, totalQuantity - defaultInt(template.getIssuedQuantity()));
    }

    /**
     * 校验用户是否还能领券
     *
     * @param currentReceivedCount 当前已领数量
     * @param template 模板
     * @return 是否可领
     * @author Henfon
     * @date 2026-06-26
     * @description 使用预加载的已领数量判断每人限领
     */
    private boolean canReceiveTemplate(long currentReceivedCount, CouponTemplate template) {
        Integer perUserLimit = template.getPerUserLimit();
        return perUserLimit == null || perUserLimit <= 0 || currentReceivedCount < perUserLimit;
    }

    /**
     * 构建用户优惠券
     *
     * @param taskUser 任务用户
     * @param template 模板
     * @param taskId 任务ID
     * @param grantMode 发放方式
     * @param now 当前时间
     * @return 用户优惠券实体
     * @author Henfon
     * @date 2026-06-26
     * @description 根据任务用户快照和模板配置生成用户优惠券记录
     */
    private UserCoupon buildUserCoupon(CouponGrantTaskUser taskUser, CouponTemplate template, Long taskId, Integer grantMode, LocalDateTime now) {
        LocalDateTime validFrom = now;
        LocalDateTime validTo = now.plusDays(template.getValidDays() == null ? 0 : template.getValidDays());
        if (Objects.equals(template.getValidityType(), VALIDITY_TYPE_FIXED)) {
            validFrom = template.getValidFrom();
            validTo = template.getValidTo();
        }

        UserCoupon coupon = new UserCoupon();
        coupon.setId(IdWorker.getId());
        coupon.setTemplateId(template.getId());
        coupon.setUserId(taskUser.getUserId());
        coupon.setUsername(taskUser.getUsername());
        coupon.setPhone(taskUser.getPhone());
        coupon.setCouponName(template.getName());
        coupon.setCouponType(template.getType());
        coupon.setThresholdAmount(normalizeAmount(template.getThresholdAmount()));
        coupon.setDiscountAmount(normalizeAmount(template.getDiscountAmount()));
        coupon.setDiscountRate(template.getDiscountRate());
        coupon.setSourceType(grantMode);
        coupon.setStatus(COUPON_STATUS_UNUSED);
        coupon.setReceivedTime(now);
        coupon.setValidFrom(validFrom);
        coupon.setValidTo(validTo);
        coupon.setGrantTaskId(taskId);
        coupon.setAvailableWeekdays(template.getAvailableWeekdays());
        coupon.setCreateBy(1L);
        coupon.setUpdateBy(1L);
        coupon.setCreateTime(now);
        coupon.setUpdateTime(now);
        coupon.setDeleted(0);
        return coupon;
    }

    /**
     * 标记任务用户成功
     *
     * @param taskUser 任务用户
     * @param finishedTime 完成时间
     * @author Henfon
     * @date 2026-06-26
     * @description 回写任务用户行状态，便于后续查明单用户发券结果
     */
    private void markTaskUserSuccess(CouponGrantTaskUser taskUser, LocalDateTime finishedTime) {
        CouponGrantTaskUser update = new CouponGrantTaskUser();
        update.setId(taskUser.getId());
        update.setGrantStatus(CouponGrantConstants.TASK_USER_SUCCESS);
        update.setFailReason(null);
        update.setFinishedTime(finishedTime);
        couponGrantTaskUserMapper.updateById(update);
    }

    /**
     * 标记任务用户失败
     *
     * @param taskUser 任务用户
     * @param reason 原因
     * @author Henfon
     * @date 2026-06-26
     * @description 回写任务用户失败原因，便于运营排查
     */
    private void markTaskUserFailure(CouponGrantTaskUser taskUser, String reason) {
        CouponGrantTaskUser update = new CouponGrantTaskUser();
        update.setId(taskUser.getId());
        update.setGrantStatus(CouponGrantConstants.TASK_USER_FAILED);
        update.setFailReason(reason);
        update.setFinishedTime(LocalDateTime.now());
        couponGrantTaskUserMapper.updateById(update);
    }

    /**
     * 完成批次处理
     *
     * @param task 任务
     * @param successCount 成功数
     * @param failCount 失败数
     * @author Henfon
     * @date 2026-06-26
     * @description 累计任务进度，并在最后一个批次完成时计算最终状态
     */
    private void completeBatch(CouponGrantTask task, int successCount, int failCount) {
        int nextFinishedBatchCount = defaultInt(task.getFinishedBatchCount()) + 1;
        int nextSuccessCount = defaultInt(task.getSuccessCount()) + successCount;
        int nextFailCount = defaultInt(task.getFailCount()) + failCount;
        boolean finished = defaultInt(task.getTotalBatchCount()) > 0 && nextFinishedBatchCount >= defaultInt(task.getTotalBatchCount());

        CouponGrantTask update = new CouponGrantTask();
        update.setId(task.getId());
        update.setFinishedBatchCount(nextFinishedBatchCount);
        update.setSuccessCount(nextSuccessCount);
        update.setFailCount(nextFailCount);
        if (finished) {
            update.setFinishedTime(LocalDateTime.now());
            if (nextFailCount == 0) {
                update.setTaskStatus(CouponGrantConstants.TASK_SUCCESS);
            } else if (nextSuccessCount > 0) {
                update.setTaskStatus(CouponGrantConstants.TASK_PARTIAL_SUCCESS);
            } else {
                update.setTaskStatus(CouponGrantConstants.TASK_FAILED);
            }
        }
        couponGrantTaskMapper.updateById(update);
    }

    /**
     * 更新任务失败状态
     *
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     * @author Henfon
     * @date 2026-06-26
     * @description 在任务无法继续分发时直接置为失败，避免卡在待处理
     */
    private void updateTaskAsFailed(Long taskId, String errorMessage) {
        LambdaUpdateWrapper<CouponGrantTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponGrantTask::getId, taskId)
                .set(CouponGrantTask::getTaskStatus, CouponGrantConstants.TASK_FAILED)
                .set(CouponGrantTask::getLastError, errorMessage)
                .set(CouponGrantTask::getFinishedTime, LocalDateTime.now());
        couponGrantTaskMapper.update(null, wrapper);
    }

    /**
     * 构建分发消息键
     *
     * @param taskId 任务ID
     * @return 消息键
     * @author Henfon
     * @date 2026-06-26
     * @description 保证每个任务只有一条分发消息键
     */
    private String buildDispatchMessageKey(Long taskId) {
        return "coupon-grant-dispatch-" + taskId;
    }

    /**
     * 构建批处理消息键
     *
     * @param taskId 任务ID
     * @param batchNo 批次号
     * @return 消息键
     * @author Henfon
     * @date 2026-06-26
     * @description 保证每个任务批次只会生成一条唯一消息
     */
    private String buildBatchMessageKey(Long taskId, Integer batchNo) {
        return "coupon-grant-batch-" + taskId + "-" + batchNo;
    }

    /**
     * 对象转 JSON
     *
     * @param payload 消息体
     * @return JSON 字符串
     * @author Henfon
     * @date 2026-06-26
     * @description 持久化消息体到本地消息表时统一序列化
     */
    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ResultCode.FAIL, "序列化消息体失败");
        }
    }

    /**
     * 标准化金额空值
     *
     * @param amount 金额
     * @return 非空金额
     * @author Henfon
     * @date 2026-06-26
     * @description 避免模板金额字段为空时批量插入报错
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 获取整数默认值
     *
     * @param value 原值
     * @return 非空整数
     * @author Henfon
     * @date 2026-06-26
     * @description 将可空整数统一转为 0，便于做累计统计
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
