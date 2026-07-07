package com.scaffold.modules.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.member.constant.MemberLevelChangeReasonConstants;
import com.scaffold.modules.member.dto.MemberLevelCreateDTO;
import com.scaffold.modules.member.dto.MemberLevelUpdateDTO;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberLevelChangeLog;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberLevelChangeLogMapper;
import com.scaffold.modules.member.mapper.MemberLevelMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberLevelService;
import com.scaffold.modules.member.vo.MemberLevelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 会员等级服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberLevelServiceImpl implements MemberLevelService {

    private final MemberLevelMapper memberLevelMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelChangeLogMapper memberLevelChangeLogMapper;

    @Override
    public List<MemberLevelVO> listAll() {
        return memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                        .orderByAsc(MemberLevel::getSort)
                        .orderByAsc(MemberLevel::getGrowthThreshold))
                .stream()
                .map(item -> BeanUtil.copyProperties(item, MemberLevelVO.class))
                .toList();
    }

    @Override
    public List<MemberLevelVO> listEnabled() {
        return memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                        .eq(MemberLevel::getStatus, 1)
                        .orderByAsc(MemberLevel::getSort)
                        .orderByAsc(MemberLevel::getGrowthThreshold))
                .stream()
                .map(item -> BeanUtil.copyProperties(item, MemberLevelVO.class))
                .toList();
    }

    /**
     * 创建会员等级
     *
     * @param dto 创建参数
     * @author Henfon
     * @date 2026-06-30
     * @description 新增会员等级配置，并校验等级编码唯一性
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(MemberLevelCreateDTO dto) {
        validateRate(dto.getPointsRate(), "积分倍率");
        validateRate(dto.getDiscountRate(), "折扣倍率");

        long exists = memberLevelMapper.selectCount(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getLevelCode, dto.getLevelCode()));
        if (exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "等级编码已存在");
        }

        MemberLevel level = BeanUtil.copyProperties(dto, MemberLevel.class);
        level.setUpgradeCouponTemplateId(dto.getUpgradeCouponTemplateId());
        level.setExclusiveCouponTemplateId(dto.getExclusiveCouponTemplateId());
        level.setStatus(1);
        memberLevelMapper.insert(level);
    }

    /**
     * 更新会员等级
     *
     * @param dto 更新参数
     * @author Henfon
     * @date 2026-06-30
     * @description 更新等级的门槛、倍率和权益配置
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MemberLevelUpdateDTO dto) {
        MemberLevel level = getByIdOrThrow(dto.getId());
        validateRate(dto.getPointsRate(), "积分倍率");
        validateRate(dto.getDiscountRate(), "折扣倍率");

        level.setLevelName(dto.getLevelName());
        level.setSort(dto.getSort());
        level.setGrowthThreshold(dto.getGrowthThreshold());
        level.setPointsRate(dto.getPointsRate());
        level.setDiscountRate(dto.getDiscountRate());
        level.setBenefitConfig(dto.getBenefitConfig());
        level.setUpgradeCouponTemplateId(dto.getUpgradeCouponTemplateId());
        level.setExclusiveCouponTemplateId(dto.getExclusiveCouponTemplateId());
        level.setRemark(dto.getRemark());
        if (dto.getStatus() != null) {
            level.setStatus(dto.getStatus());
        }
        memberLevelMapper.updateById(level);
    }

    /**
     * 更新会员等级状态
     *
     * @param id 等级ID
     * @param status 状态
     * @author Henfon
     * @date 2026-06-30
     * @description 启用或禁用会员等级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        MemberLevel level = getByIdOrThrow(id);
        level.setStatus(status);
        memberLevelMapper.updateById(level);
    }

    @Override
    public MemberLevel getByIdOrThrow(Long id) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员等级不存在");
        }
        return level;
    }

    @Override
    public MemberLevel getDefaultLevel() {
        MemberLevel level = memberLevelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getGrowthThreshold)
                .orderByAsc(MemberLevel::getSort)
                .last("LIMIT 1"));
        if (level == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "未配置默认会员等级");
        }
        return level;
    }

    @Override
    public MemberLevel matchLevelByGrowthValue(Integer growthValue) {
        int currentGrowth = growthValue == null ? 0 : growthValue;
        MemberLevel level = memberLevelMapper.selectOne(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .le(MemberLevel::getGrowthThreshold, currentGrowth)
                .orderByDesc(MemberLevel::getGrowthThreshold)
                .orderByDesc(MemberLevel::getSort)
                .last("LIMIT 1"));
        return level == null ? getDefaultLevel() : level;
    }

    /**
     * 按成长值执行自动升级
     *
     * @param profile 会员档案
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @author Henfon
     * @date 2026-06-30
     * @description 当成长值达到更高门槛时，自动更新会员等级并记录变更日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upgradeIfNeeded(MemberProfile profile, String bizType, Long bizId) {
        if (profile == null) {
            return;
        }

        MemberLevel matchedLevel = matchLevelByGrowthValue(profile.getGrowthValue());
        if (matchedLevel.getId().equals(profile.getLevelId())) {
            return;
        }

        Long oldLevelId = profile.getLevelId();
        profile.setLevelId(matchedLevel.getId());
        memberProfileMapper.updateById(profile);

        MemberLevelChangeLog logEntity = new MemberLevelChangeLog();
        logEntity.setMemberId(profile.getId());
        logEntity.setUserId(profile.getUserId());
        logEntity.setOldLevelId(oldLevelId);
        logEntity.setNewLevelId(matchedLevel.getId());
        logEntity.setChangeReason(MemberLevelChangeReasonConstants.AUTO_UPGRADE);
        logEntity.setBizType(bizType);
        logEntity.setBizId(bizId);
        logEntity.setRemark("成长值达到升级门槛，自动升级");
        memberLevelChangeLogMapper.insert(logEntity);

        log.info("会员自动升级成功: memberId={}, userId={}, oldLevelId={}, newLevelId={}, bizType={}, bizId={}",
                profile.getId(), profile.getUserId(), oldLevelId, matchedLevel.getId(), bizType, bizId);
    }

    /**
     * 校验倍率参数
     *
     * @param rate 倍率
     * @param field 字段名
     * @author Henfon
     * @date 2026-06-30
     * @description 统一校验积分倍率和折扣倍率为正数
     */
    private void validateRate(java.math.BigDecimal rate, String field) {
        if (rate == null || rate.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, field + "必须大于0");
        }
    }
}
