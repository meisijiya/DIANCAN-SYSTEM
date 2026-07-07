package com.scaffold.modules.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.member.constant.MemberBizTypeConstants;
import com.scaffold.modules.member.dto.MemberGrowthRecordQueryDTO;
import com.scaffold.modules.member.entity.MemberGrowthRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberGrowthRecordMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberGrowthService;
import com.scaffold.modules.member.vo.MemberGrowthRecordVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员成长值服务实现
 *
 * @author Henfon
 */
@Service
@RequiredArgsConstructor
public class MemberGrowthServiceImpl implements MemberGrowthService {

    private final MemberGrowthRecordMapper memberGrowthRecordMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<MemberGrowthRecordVO> pageAdmin(MemberGrowthRecordQueryDTO dto) {
        LambdaQueryWrapper<MemberGrowthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getMemberId() != null, MemberGrowthRecord::getMemberId, dto.getMemberId())
                .eq(dto.getUserId() != null, MemberGrowthRecord::getUserId, dto.getUserId())
                .eq(dto.getBizType() != null && !dto.getBizType().isBlank(), MemberGrowthRecord::getBizType, dto.getBizType())
                .orderByDesc(MemberGrowthRecord::getCreateTime);
        if (dto.getStartDate() != null) {
            wrapper.ge(MemberGrowthRecord::getCreateTime, dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            wrapper.le(MemberGrowthRecord::getCreateTime, dto.getEndDate().plusDays(1).atStartOfDay());
        }

        Page<MemberGrowthRecord> page = memberGrowthRecordMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        return buildPageResult(page);
    }

    @Override
    public PageResult<MemberGrowthRecordVO> pageCurrentMember(Long userId, int pageNum, int pageSize) {
        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            return PageResult.of(Collections.emptyList(), Long.valueOf(pageNum), Long.valueOf(pageSize), 0L);
        }

        MemberGrowthRecordQueryDTO dto = new MemberGrowthRecordQueryDTO();
        dto.setMemberId(profile.getId());
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return pageAdmin(dto);
    }

    @Override
    public boolean existsByBiz(String bizType, Long bizId) {
        return memberGrowthRecordMapper.selectCount(new LambdaQueryWrapper<MemberGrowthRecord>()
                .eq(MemberGrowthRecord::getBizType, bizType)
                .eq(MemberGrowthRecord::getBizId, bizId)) > 0;
    }

    /**
     * 写入订单支付成长值
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param growthValue 成长值
     * @param remark 备注
     * @author Henfon
     * @date 2026-06-30
     * @description 订单支付成功后增加成长值，并记录成长值流水
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGrowthForOrder(MemberProfile profile, Long orderId, Integer growthValue, String remark) {
        if (profile == null || orderId == null || growthValue == null || growthValue <= 0 || existsByBiz(MemberBizTypeConstants.ORDER_PAY, orderId)) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentGrowth = lockedProfile.getGrowthValue() == null ? 0 : lockedProfile.getGrowthValue();
        lockedProfile.setGrowthValue(currentGrowth + growthValue);
        memberProfileMapper.updateById(lockedProfile);

        MemberGrowthRecord record = new MemberGrowthRecord();
        record.setMemberId(lockedProfile.getId());
        record.setUserId(lockedProfile.getUserId());
        record.setBizType(MemberBizTypeConstants.ORDER_PAY);
        record.setBizId(orderId);
        record.setChangeAmount(growthValue);
        record.setGrowthAfter(lockedProfile.getGrowthValue());
        record.setRemark(remark);
        insertRecord(record);
    }

    @Override
    public void insertRecord(MemberGrowthRecord record) {
        memberGrowthRecordMapper.insert(record);
    }

    /**
     * 退款回退成长值
     *
     * @param profile 会员档案
     * @param orderId 订单ID
     * @param orderAmount 原订单实付金额
     * @param refundAmount 退款金额
     * @param remark 备注
     * @author Henfon
     * @date 2026-07-01
     * @description 按退款比例回退成长值，一期只回退数值，不触发会员降级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackGrowthForRefund(MemberProfile profile, Long orderId, BigDecimal orderAmount, BigDecimal refundAmount, String remark) {
        if (profile == null || orderId == null || orderAmount == null || refundAmount == null
                || orderAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(BigDecimal.ZERO) <= 0
                || existsByBiz(MemberBizTypeConstants.ORDER_REFUND, orderId)) {
            return;
        }

        MemberGrowthRecord payRecord = memberGrowthRecordMapper.selectOne(new LambdaQueryWrapper<MemberGrowthRecord>()
                .eq(MemberGrowthRecord::getMemberId, profile.getId())
                .eq(MemberGrowthRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                .eq(MemberGrowthRecord::getBizId, orderId)
                .last("LIMIT 1"));
        if (payRecord == null || payRecord.getChangeAmount() == null || payRecord.getChangeAmount() <= 0) {
            return;
        }

        int rollbackGrowth = calculateRollbackAmount(payRecord.getChangeAmount(), orderAmount, refundAmount);
        if (rollbackGrowth <= 0) {
            return;
        }

        MemberProfile lockedProfile = memberProfileMapper.selectByIdForUpdate(profile.getId());
        int currentGrowth = lockedProfile.getGrowthValue() == null ? 0 : lockedProfile.getGrowthValue();
        int actualRollback = Math.min(currentGrowth, rollbackGrowth);
        if (actualRollback <= 0) {
            return;
        }

        lockedProfile.setGrowthValue(currentGrowth - actualRollback);
        memberProfileMapper.updateById(lockedProfile);

        MemberGrowthRecord refundRecord = new MemberGrowthRecord();
        refundRecord.setMemberId(lockedProfile.getId());
        refundRecord.setUserId(lockedProfile.getUserId());
        refundRecord.setBizType(MemberBizTypeConstants.ORDER_REFUND);
        refundRecord.setBizId(orderId);
        refundRecord.setChangeAmount(-actualRollback);
        refundRecord.setGrowthAfter(lockedProfile.getGrowthValue());
        refundRecord.setRemark(remark);
        insertRecord(refundRecord);
    }

    /**
     * 查询订单支付奖励成长值
     *
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 奖励成长值
     * @author Henfon
     * @date 2026-07-01
     * @description 仅返回当前用户该订单支付成功后实际发放的正向成长值
     */
    @Override
    public Integer getRewardGrowthByOrder(Long userId, Long orderId) {
        if (userId == null || orderId == null) {
            return 0;
        }
        MemberGrowthRecord record = memberGrowthRecordMapper.selectOne(new LambdaQueryWrapper<MemberGrowthRecord>()
                .eq(MemberGrowthRecord::getUserId, userId)
                .eq(MemberGrowthRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                .eq(MemberGrowthRecord::getBizId, orderId)
                .orderByDesc(MemberGrowthRecord::getCreateTime)
                .last("LIMIT 1"));
        if (record == null || record.getChangeAmount() == null || record.getChangeAmount() <= 0) {
            return 0;
        }
        return record.getChangeAmount();
    }

    /**
     * 计算按比例回退的成长值
     *
     * @param sourceAmount 原奖励成长值
     * @param orderAmount 订单金额
     * @param refundAmount 退款金额
     * @return 回退成长值
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

    private PageResult<MemberGrowthRecordVO> buildPageResult(Page<MemberGrowthRecord> page) {
        Set<Long> memberIds = page.getRecords().stream().map(MemberGrowthRecord::getMemberId).collect(Collectors.toSet());
        Set<Long> userIds = page.getRecords().stream().map(MemberGrowthRecord::getUserId).collect(Collectors.toSet());
        Map<Long, MemberProfile> profileMap = memberIds.isEmpty() ? Collections.emptyMap() : memberProfileMapper.selectBatchIds(memberIds).stream()
                .collect(Collectors.toMap(MemberProfile::getId, Function.identity()));
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() : sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        return PageResult.of(page.getRecords().stream().map(record -> {
            MemberGrowthRecordVO vo = BeanUtil.copyProperties(record, MemberGrowthRecordVO.class);
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
