package com.scaffold.modules.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.member.constant.MemberBenefitConfigKeys;
import com.scaffold.modules.member.constant.MemberBizTypeConstants;
import com.scaffold.modules.member.constant.MemberPointsChangeTypeConstants;
import com.scaffold.modules.member.dto.MemberPointsExpireRuleDTO;
import com.scaffold.modules.member.dto.MemberPointsAdjustDTO;
import com.scaffold.modules.member.dto.MemberPointsRecordQueryDTO;
import com.scaffold.modules.member.entity.MemberPointsRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberPointsRecordMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.vo.MemberPointsRecordVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员积分服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberPointsServiceImpl implements MemberPointsService {

    private final MemberPointsRecordMapper memberPointsRecordMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final SysUserMapper sysUserMapper;
    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<MemberPointsRecordVO> pageAdmin(MemberPointsRecordQueryDTO dto) {
        LambdaQueryWrapper<MemberPointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getMemberId() != null, MemberPointsRecord::getMemberId, dto.getMemberId())
                .eq(dto.getUserId() != null, MemberPointsRecord::getUserId, dto.getUserId())
                .eq(dto.getChangeType() != null, MemberPointsRecord::getChangeType, dto.getChangeType())
                .eq(dto.getBizType() != null && !dto.getBizType().isBlank(), MemberPointsRecord::getBizType, dto.getBizType())
                .orderByDesc(MemberPointsRecord::getCreateTime);
        if (dto.getStartDate() != null) {
            wrapper.ge(MemberPointsRecord::getCreateTime, dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            wrapper.le(MemberPointsRecord::getCreateTime, dto.getEndDate().plusDays(1).atStartOfDay());
        }

        Page<MemberPointsRecord> page = memberPointsRecordMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return buildPageResult(page);
    }

    @Override
    public PageResult<MemberPointsRecordVO> pageCurrentMember(Long userId, int pageNum, int pageSize) {
        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            return PageResult.of(Collections.emptyList(), Long.valueOf(pageNum), Long.valueOf(pageSize), 0L);
        }

        MemberPointsRecordQueryDTO dto = new MemberPointsRecordQueryDTO();
        dto.setMemberId(profile.getId());
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return pageAdmin(dto);
    }

    @Override
    public boolean existsByBiz(String bizType, Long bizId) {
        return memberPointsRecordMapper.selectCount(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getBizType, bizType)
                .eq(MemberPointsRecord::getBizId, bizId)) > 0;
    }

    /**
     * 写入订单支付积分
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param points 积分数量
     * @param remark 备注
     * @author Henfon
     * @date 2026-06-30
     * @description 订单支付成功后，增加会员积分并写入积分流水
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark) {
        if (profile == null || orderId == null || points == null || points <= 0 || existsByBiz(MemberBizTypeConstants.ORDER_PAY, orderId)) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentBalance = lockedProfile.getPointsBalance() == null ? 0 : lockedProfile.getPointsBalance();
        int currentEarned = lockedProfile.getTotalPointsEarned() == null ? 0 : lockedProfile.getTotalPointsEarned();
        lockedProfile.setPointsBalance(currentBalance + points);
        lockedProfile.setTotalPointsEarned(currentEarned + points);
        memberProfileMapper.updateById(lockedProfile);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(lockedProfile.getId());
        record.setUserId(lockedProfile.getUserId());
        record.setChangeType(MemberPointsChangeTypeConstants.INCREASE);
        record.setBizType(MemberBizTypeConstants.ORDER_PAY);
        record.setBizId(orderId);
        record.setChangeAmount(points);
        record.setBalanceAfter(lockedProfile.getPointsBalance());
        record.setExpireTime(resolveExpireTime());
        record.setRemark(remark);
        insertRecord(record);
    }

    /**
     * 管理端手工调整积分
     *
     * @param memberId 会员ID
     * @param dto 调整参数
     * @author Henfon
     * @date 2026-06-30
     * @description 支持管理端增减积分，并校验扣减后余额不能小于0
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustPoints(Long memberId, MemberPointsAdjustDTO dto) {
        MemberProfile profile = memberProfileMapper.selectByIdForUpdate(memberId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        if (dto.getChangeAmount() == null || dto.getChangeAmount() == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "调整积分不能为0");
        }

        int currentBalance = profile.getPointsBalance() == null ? 0 : profile.getPointsBalance();
        int nextBalance = currentBalance + dto.getChangeAmount();
        if (nextBalance < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "调整后积分不能小于0");
        }

        profile.setPointsBalance(nextBalance);
        if (dto.getChangeAmount() > 0) {
            int earned = profile.getTotalPointsEarned() == null ? 0 : profile.getTotalPointsEarned();
            profile.setTotalPointsEarned(earned + dto.getChangeAmount());
        } else {
            int used = profile.getTotalPointsUsed() == null ? 0 : profile.getTotalPointsUsed();
            profile.setTotalPointsUsed(used + Math.abs(dto.getChangeAmount()));
        }
        memberProfileMapper.updateById(profile);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(profile.getId());
        record.setUserId(profile.getUserId());
        record.setChangeType(dto.getChangeAmount() > 0 ? MemberPointsChangeTypeConstants.ADJUST : MemberPointsChangeTypeConstants.DECREASE);
        record.setBizType(MemberBizTypeConstants.ADMIN_ADJUST);
        record.setBizId(IdUtil.getSnowflakeNextId());
        record.setChangeAmount(dto.getChangeAmount());
        record.setBalanceAfter(nextBalance);
        record.setRemark(dto.getRemark());
        insertRecord(record);
    }

    @Override
    public void insertRecord(MemberPointsRecord record) {
        memberPointsRecordMapper.insert(record);
    }

    /**
     * 退款回退积分
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param orderAmount 原订单实付金额
     * @param refundAmount 退款金额
     * @param remark 备注
     * @author Henfon
     * @date 2026-07-01
     * @description 按退款比例回退订单支付奖励积分，避免积分余额扣成负数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackPointsForRefund(MemberProfile profile, Long orderId, BigDecimal orderAmount, BigDecimal refundAmount, String remark) {
        if (profile == null || orderId == null || orderAmount == null || refundAmount == null
                || orderAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || existsByBiz(MemberBizTypeConstants.ORDER_REFUND, orderId)) {
            return;
        }

        MemberPointsRecord payRecord = memberPointsRecordMapper.selectOne(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getMemberId, profile.getId())
                .eq(MemberPointsRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                .eq(MemberPointsRecord::getBizId, orderId)
                .last("LIMIT 1"));
        if (payRecord == null || payRecord.getChangeAmount() == null || payRecord.getChangeAmount() <= 0) {
            return;
        }

        int rollbackPoints = calculateRollbackAmount(payRecord.getChangeAmount(), orderAmount, refundAmount);
        if (rollbackPoints <= 0) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentBalance = lockedProfile.getPointsBalance() == null ? 0 : lockedProfile.getPointsBalance();
        int actualRollback = Math.min(currentBalance, rollbackPoints);
        if (actualRollback <= 0) {
            return;
        }

        int currentEarned = lockedProfile.getTotalPointsEarned() == null ? 0 : lockedProfile.getTotalPointsEarned();
        int currentUsed = lockedProfile.getTotalPointsUsed() == null ? 0 : lockedProfile.getTotalPointsUsed();
        lockedProfile.setPointsBalance(currentBalance - actualRollback);
        lockedProfile.setTotalPointsEarned(Math.max(0, currentEarned - actualRollback));
        lockedProfile.setTotalPointsUsed(currentUsed + actualRollback);
        memberProfileMapper.updateById(lockedProfile);

        MemberPointsRecord refundRecord = new MemberPointsRecord();
        refundRecord.setMemberId(lockedProfile.getId());
        refundRecord.setUserId(lockedProfile.getUserId());
        refundRecord.setChangeType(MemberPointsChangeTypeConstants.REFUND);
        refundRecord.setBizType(MemberBizTypeConstants.ORDER_REFUND);
        refundRecord.setBizId(orderId);
        refundRecord.setChangeAmount(-actualRollback);
        refundRecord.setBalanceAfter(lockedProfile.getPointsBalance());
        refundRecord.setRemark(remark);
        insertRecord(refundRecord);
    }

    /**
     * 订单抵现扣减积分
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param points 积分
     * @param remark 备注
     * @author Henfon
     * @date 2026-07-02
     * @description 在订单创建时先扣减本单计划使用的积分，避免同一余额被重复占用
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark) {
        if (profile == null || orderId == null || points == null || points <= 0
                || existsByBiz(MemberBizTypeConstants.ORDER_POINTS_DEDUCT, orderId)) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentBalance = lockedProfile.getPointsBalance() == null ? 0 : lockedProfile.getPointsBalance();
        if (currentBalance < points) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会员积分不足");
        }
        int currentUsed = lockedProfile.getTotalPointsUsed() == null ? 0 : lockedProfile.getTotalPointsUsed();
        lockedProfile.setPointsBalance(currentBalance - points);
        lockedProfile.setTotalPointsUsed(currentUsed + points);
        memberProfileMapper.updateById(lockedProfile);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(lockedProfile.getId());
        record.setUserId(lockedProfile.getUserId());
        record.setChangeType(MemberPointsChangeTypeConstants.DECREASE);
        record.setBizType(MemberBizTypeConstants.ORDER_POINTS_DEDUCT);
        record.setBizId(orderId);
        record.setChangeAmount(-points);
        record.setBalanceAfter(lockedProfile.getPointsBalance());
        record.setRemark(remark);
        insertRecord(record);
    }

    /**
     * 订单积分回退
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param points 积分
     * @param remark 备注
     * @author Henfon
     * @date 2026-07-02
     * @description 订单取消、退款或重算时回退本单已扣减积分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnPointsForOrder(MemberProfile profile, Long orderId, Integer points, String remark) {
        if (profile == null || orderId == null || points == null || points <= 0) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentBalance = lockedProfile.getPointsBalance() == null ? 0 : lockedProfile.getPointsBalance();
        int currentUsed = lockedProfile.getTotalPointsUsed() == null ? 0 : lockedProfile.getTotalPointsUsed();
        lockedProfile.setPointsBalance(currentBalance + points);
        lockedProfile.setTotalPointsUsed(Math.max(0, currentUsed - points));
        memberProfileMapper.updateById(lockedProfile);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(lockedProfile.getId());
        record.setUserId(lockedProfile.getUserId());
        record.setChangeType(MemberPointsChangeTypeConstants.REFUND);
        record.setBizType(MemberBizTypeConstants.ORDER_POINTS_RETURN);
        record.setBizId(IdUtil.getSnowflakeNextId());
        record.setChangeAmount(points);
        record.setBalanceAfter(lockedProfile.getPointsBalance());
        record.setRemark(StrUtil.blankToDefault(remark, "订单积分回退") + "，原订单ID=" + orderId);
        insertRecord(record);
    }

    /**
     * 查询订单支付奖励积分
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 奖励积分
     * @author Henfon
     * @date 2026-07-01
     * @description 仅返回当前用户该订单支付成功后实际发放的正向积分
     */
    @Override
    public Integer getRewardPointsByOrder(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return 0;
        }
        MemberPointsRecord record = memberPointsRecordMapper.selectOne(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getUserId, userId)
                .eq(MemberPointsRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                .eq(MemberPointsRecord::getBizId, orderId)
                .orderByDesc(MemberPointsRecord::getCreateTime)
                .last("LIMIT 1"));
        if (record == null || record.getChangeAmount() == null || record.getChangeAmount() <= 0) {
            return 0;
        }
        return record.getChangeAmount();
    }

    /**
     * 查询订单已抵扣积分
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 已抵扣积分
     * @author Henfon
     * @date 2026-07-02
     * @description 返回当前用户指定订单的正向扣减积分值
     */
    @Override
    public Integer getDeductedPointsByOrder(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return 0;
        }
        MemberPointsRecord record = memberPointsRecordMapper.selectOne(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getUserId, userId)
                .eq(MemberPointsRecord::getBizType, MemberBizTypeConstants.ORDER_POINTS_DEDUCT)
                .eq(MemberPointsRecord::getBizId, orderId)
                .orderByDesc(MemberPointsRecord::getCreateTime)
                .last("LIMIT 1"));
        if (record == null || record.getChangeAmount() == null) {
            return 0;
        }
        return Math.abs(record.getChangeAmount());
    }

    /**
     * 计算按比例回退的积分
     *
     * @param sourceAmount 原奖励积分
     * @param orderAmount 订单金额
     * @param refundAmount 退款金额
     * @return 回退积分
     * @author Henfon
     * @date 2026-07-01
     * @description 按退款金额占实付金额比例向下取整，整单退款时可全额回退
     */
    private int calculateRollbackAmount(Integer sourceAmount, BigDecimal orderAmount, BigDecimal refundAmount) {
        BigDecimal validRefundAmount = refundAmount.min(orderAmount);
        return BigDecimal.valueOf(sourceAmount)
                .multiply(validRefundAmount)
                .divide(orderAmount, 0, RoundingMode.DOWN)
                .intValue();
    }

    /**
     * 解析积分过期时间
     *
     * @return 过期时间
     * @author Henfon
     * @date 2026-07-02
     * @description 从系统配置读取积分有效期，启用时为正向积分流水写入到期时间
     */
    private LocalDateTime resolveExpireTime() {
        try {
            String configValue = sysConfigService.getConfigValue(MemberBenefitConfigKeys.POINTS_EXPIRE_RULE);
            if (StrUtil.isBlank(configValue)) {
                return null;
            }
            MemberPointsExpireRuleDTO rule = objectMapper.readValue(configValue, MemberPointsExpireRuleDTO.class);
            if (rule == null || !Boolean.TRUE.equals(rule.getEnabled()) || rule.getExpireDays() == null || rule.getExpireDays() <= 0) {
                return null;
            }
            return LocalDateTime.now().plusDays(rule.getExpireDays());
        } catch (Exception e) {
            log.warn("解析积分过期规则失败，忽略过期时间设置: {}", e.getMessage());
            return null;
        }
    }

    private PageResult<MemberPointsRecordVO> buildPageResult(Page<MemberPointsRecord> page) {
        Set<Long> memberIds = page.getRecords().stream().map(MemberPointsRecord::getMemberId).collect(Collectors.toSet());
        Set<Long> userIds = page.getRecords().stream().map(MemberPointsRecord::getUserId).collect(Collectors.toSet());
        Map<Long, MemberProfile> profileMap = memberIds.isEmpty() ? Collections.emptyMap() : memberProfileMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(MemberProfile::getId, Function.identity()));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return PageResult.of(page.getRecords().stream().map(record -> {
            MemberPointsRecordVO vo = BeanUtil.copyProperties(record, MemberPointsRecordVO.class);
            MemberProfile profile = profileMap.get(record.getMemberId());
            if (profile != null) {
                vo.setMemberNo(profile.getMemberNo());
            }
            SysUser user = userMap.get(record.getUserId());
            if (user != null) {
                vo.setNickname(user.getNickname());
            }
            return vo;
        }).toList(), page.getCurrent(), page.getSize(), page.getTotal());
    }
}
