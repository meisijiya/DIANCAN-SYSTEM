package com.scaffold.modules.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import com.scaffold.modules.coupon.entity.UserCoupon;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.member.constant.MemberBenefitConfigKeys;
import com.scaffold.modules.member.constant.MemberBizTypeConstants;
import com.scaffold.modules.member.constant.MemberPointsChangeTypeConstants;
import com.scaffold.modules.member.dto.MemberBenefitConfigSaveDTO;
import com.scaffold.modules.member.dto.MemberBirthdayBenefitRuleDTO;
import com.scaffold.modules.member.dto.MemberCouponExchangeSaveDTO;
import com.scaffold.modules.member.dto.MemberPointsDeductionRuleDTO;
import com.scaffold.modules.member.dto.MemberPointsExpireRuleDTO;
import com.scaffold.modules.member.entity.MemberBenefitGrantLog;
import com.scaffold.modules.member.entity.MemberCouponExchange;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberPointsRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberBenefitGrantLogMapper;
import com.scaffold.modules.member.mapper.MemberCouponExchangeMapper;
import com.scaffold.modules.member.mapper.MemberLevelMapper;
import com.scaffold.modules.member.mapper.MemberPointsRecordMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberBenefitService;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.vo.AppMemberBenefitOverviewVO;
import com.scaffold.modules.member.vo.AppMemberCouponExchangeVO;
import com.scaffold.modules.member.vo.AppMemberExclusiveBenefitVO;
import com.scaffold.modules.member.vo.AppMemberPointsDeductionRuleVO;
import com.scaffold.modules.member.vo.MemberBenefitConfigVO;
import com.scaffold.modules.member.vo.MemberCouponExchangeVO;
import com.scaffold.modules.member.vo.MemberPointsDeductionPreviewVO;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 会员权益服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBenefitServiceImpl implements MemberBenefitService {

    private static final int EXCHANGE_STATUS_ENABLED = 1;
    private static final int SOURCE_TYPE_POINTS_EXCHANGE = 3;
    private static final int SOURCE_TYPE_BIRTHDAY = 4;
    private static final int SOURCE_TYPE_LEVEL_UPGRADE = 5;
    private static final int SOURCE_TYPE_LEVEL_EXCLUSIVE = 6;

    private static final int BENEFIT_TYPE_BIRTHDAY = 1;
    private static final int BENEFIT_TYPE_UPGRADE = 2;
    private static final int BENEFIT_TYPE_EXCLUSIVE = 3;
    private static final int BENEFIT_TYPE_EXCHANGE = 4;

    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;
    private final MemberCouponExchangeMapper memberCouponExchangeMapper;
    private final MemberBenefitGrantLogMapper memberBenefitGrantLogMapper;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberPointsRecordMapper memberPointsRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final CouponService couponService;
    private final MemberPointsService memberPointsService;

    @Override
    public MemberBenefitConfigVO getBenefitConfig() {
        MemberBenefitConfigVO vo = new MemberBenefitConfigVO();
        vo.setPointsDeductionRule(readConfig(MemberBenefitConfigKeys.POINTS_DEDUCTION_RULE, defaultPointsDeductionRule(), MemberPointsDeductionRuleDTO.class));
        vo.setPointsExpireRule(readConfig(MemberBenefitConfigKeys.POINTS_EXPIRE_RULE, defaultPointsExpireRule(), MemberPointsExpireRuleDTO.class));
        vo.setBirthdayBenefitRule(readConfig(MemberBenefitConfigKeys.BIRTHDAY_BENEFIT_RULE, defaultBirthdayRule(), MemberBirthdayBenefitRuleDTO.class));
        return vo;
    }

    /**
     * 保存会员权益配置
     *
     * @param dto 配置参数
     * @author Henfon
     * @date 2026-07-02
     * @description 将积分抵现、积分过期和生日权益统一存入系统参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBenefitConfig(MemberBenefitConfigSaveDTO dto) {
        validateBirthdayRule(dto.getBirthdayBenefitRule());
        saveConfig(MemberBenefitConfigKeys.POINTS_DEDUCTION_RULE, dto.getPointsDeductionRule(), "会员积分抵现规则");
        saveConfig(MemberBenefitConfigKeys.POINTS_EXPIRE_RULE, dto.getPointsExpireRule(), "会员积分过期规则");
        saveConfig(MemberBenefitConfigKeys.BIRTHDAY_BENEFIT_RULE, dto.getBirthdayBenefitRule(), "会员生日权益规则");
    }

    @Override
    public List<MemberCouponExchangeVO> listExchangeConfigs() {
        return memberCouponExchangeMapper.selectList(new LambdaQueryWrapper<MemberCouponExchange>()
                        .orderByAsc(MemberCouponExchange::getSort)
                        .orderByDesc(MemberCouponExchange::getCreateTime))
                .stream()
                .map(item -> BeanUtil.copyProperties(item, MemberCouponExchangeVO.class))
                .toList();
    }

    /**
     * 保存积分兑换优惠券配置
     *
     * @param dto 配置参数
     * @author Henfon
     * @date 2026-07-02
     * @description 支持新增和更新积分兑换项，并同步冗余模板名称
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveExchangeConfig(MemberCouponExchangeSaveDTO dto) {
        CouponTemplate template = couponService.getTemplateEntity(dto.getTemplateId());
        MemberCouponExchange entity = dto.getId() == null ? new MemberCouponExchange() : memberCouponExchangeMapper.selectById(dto.getId());
        if (dto.getId() != null && entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "积分兑换配置不存在");
        }
        if (!Objects.equals(template.getStatus(), 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券模板未启用");
        }

        entity.setTemplateId(template.getId());
        entity.setTemplateName(template.getName());
        entity.setPointsCost(dto.getPointsCost());
        entity.setPerUserLimit(dto.getPerUserLimit());
        entity.setSort(dto.getSort());
        entity.setStatus(dto.getStatus());
        entity.setRemark(dto.getRemark());

        if (dto.getId() == null) {
            memberCouponExchangeMapper.insert(entity);
        } else {
            memberCouponExchangeMapper.updateById(entity);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExchangeConfig(Long id) {
        if (memberCouponExchangeMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "积分兑换配置不存在");
        }
        memberCouponExchangeMapper.deleteById(id);
    }

    @Override
    public MemberPointsDeductionPreviewVO previewPointsDeduction(Long userId, BigDecimal orderAmount, Integer requestedPoints) {
        MemberPointsDeductionRuleDTO rule = readConfig(MemberBenefitConfigKeys.POINTS_DEDUCTION_RULE, defaultPointsDeductionRule(), MemberPointsDeductionRuleDTO.class);
        MemberPointsDeductionPreviewVO vo = new MemberPointsDeductionPreviewVO();
        vo.setEnabled(Boolean.TRUE.equals(rule.getEnabled()));
        vo.setRequestedPoints(Math.max(requestedPoints == null ? 0 : requestedPoints, 0));

        MemberProfile profile = userId == null ? null : memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        int availablePoints = profile == null || profile.getPointsBalance() == null ? 0 : profile.getPointsBalance();
        vo.setAvailablePoints(availablePoints);

        if (!Boolean.TRUE.equals(rule.getEnabled()) || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            vo.setMaxUsablePoints(0);
            vo.setActualUsedPoints(0);
            vo.setDeductionAmount(BigDecimal.ZERO);
            return vo;
        }

        BigDecimal maxDeductionAmount = orderAmount.multiply(rule.getMaxDeductionRatio() == null ? BigDecimal.ZERO : rule.getMaxDeductionRatio())
                .setScale(2, RoundingMode.DOWN);
        int maxByAmount = calculatePointsByDeductionAmount(maxDeductionAmount, rule);
        int maxPoints = availablePoints;
        if (rule.getMaxPointsPerOrder() != null && rule.getMaxPointsPerOrder() > 0) {
            maxPoints = Math.min(maxPoints, rule.getMaxPointsPerOrder());
        }
        maxPoints = Math.min(maxPoints, maxByAmount);
        maxPoints = normalizePointsByStep(maxPoints, rule);
        vo.setMaxUsablePoints(Math.max(maxPoints, 0));

        int actualUsedPoints = normalizePointsByStep(Math.min(vo.getRequestedPoints(), vo.getMaxUsablePoints()), rule);
        vo.setActualUsedPoints(Math.max(actualUsedPoints, 0));
        vo.setDeductionAmount(calculateDeductionAmount(actualUsedPoints, rule));
        return vo;
    }

    @Override
    public AppMemberBenefitOverviewVO getAppBenefitOverview(Long userId) {
        AppMemberBenefitOverviewVO vo = new AppMemberBenefitOverviewVO();
        MemberPointsDeductionRuleDTO deductionRule = readConfig(MemberBenefitConfigKeys.POINTS_DEDUCTION_RULE, defaultPointsDeductionRule(), MemberPointsDeductionRuleDTO.class);
        AppMemberPointsDeductionRuleVO ruleVO = BeanUtil.copyProperties(deductionRule, AppMemberPointsDeductionRuleVO.class);
        vo.setPointsDeductionRule(ruleVO);

        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        List<MemberCouponExchange> exchangeList = memberCouponExchangeMapper.selectList(new LambdaQueryWrapper<MemberCouponExchange>()
                .eq(MemberCouponExchange::getStatus, EXCHANGE_STATUS_ENABLED)
                .orderByAsc(MemberCouponExchange::getSort)
                .orderByDesc(MemberCouponExchange::getCreateTime));
        vo.setExchangeCoupons(exchangeList.stream().map(item -> buildAppExchangeVO(item, userId)).toList());
        vo.setExclusiveBenefit(buildExclusiveBenefit(profile));
        return vo;
    }

    /**
     * 积分兑换优惠券
     *
     * @param userId 用户ID
     * @param exchangeConfigId 兑换项ID
     * @author Henfon
     * @date 2026-07-02
     * @description 校验积分余额与兑换次数后同步扣分并发券
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void exchangeCoupon(Long userId, Long exchangeConfigId) {
        MemberCouponExchange exchange = memberCouponExchangeMapper.selectById(exchangeConfigId);
        if (exchange == null || !Objects.equals(exchange.getStatus(), EXCHANGE_STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "兑换配置不存在或未启用");
        }

        MemberProfile profile = memberProfileMapper.selectByUserIdForUpdate(userId);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (exchange.getPerUserLimit() != null && exchange.getPerUserLimit() > 0) {
            Long exchangedCount = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                    .eq(MemberBenefitGrantLog::getUserId, userId)
                    .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_EXCHANGE)
                    .eq(MemberBenefitGrantLog::getTriggerKey, "EXCHANGE")
                    .eq(MemberBenefitGrantLog::getTriggerValue, String.valueOf(exchangeConfigId)));
            if (exchangedCount >= exchange.getPerUserLimit()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "已达到该兑换项可兑换上限");
            }
        }

        consumePoints(profile, exchange.getPointsCost(), MemberBizTypeConstants.COUPON_EXCHANGE, exchangeConfigId,
                "积分兑换优惠券：" + exchange.getTemplateName());
        UserCoupon coupon = couponService.grantCouponToUser(exchange.getTemplateId(), user, SOURCE_TYPE_POINTS_EXCHANGE, "积分兑换");
        createBenefitGrantLog(profile, profile.getLevelId(), exchange.getTemplateId(), coupon.getId(), BENEFIT_TYPE_EXCHANGE,
                "EXCHANGE", String.valueOf(exchangeConfigId), "积分兑换优惠券");
    }

    /**
     * 领取等级专属券
     *
     * @param userId 用户ID
     * @author Henfon
     * @date 2026-07-02
     * @description 当前等级配置了专属券时，允许会员按月领取一次
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimExclusiveCoupon(Long userId) {
        MemberProfile profile = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        if (level == null || level.getExclusiveCouponTemplateId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前等级未配置专属权益");
        }
        String monthKey = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        boolean claimed = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                .eq(MemberBenefitGrantLog::getUserId, userId)
                .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_EXCLUSIVE)
                .eq(MemberBenefitGrantLog::getTriggerKey, "LEVEL_MONTH")
                .eq(MemberBenefitGrantLog::getTriggerValue, level.getId() + ":" + monthKey)) > 0;
        if (claimed) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "本月该等级专属券已领取");
        }

        SysUser user = sysUserMapper.selectById(userId);
        UserCoupon coupon = couponService.grantCouponToUser(level.getExclusiveCouponTemplateId(), user, SOURCE_TYPE_LEVEL_EXCLUSIVE, "等级专属券");
        createBenefitGrantLog(profile, level.getId(), level.getExclusiveCouponTemplateId(), coupon.getId(), BENEFIT_TYPE_EXCLUSIVE,
                "LEVEL_MONTH", level.getId() + ":" + monthKey, "等级专属券领取");
    }

    /**
     * 生日权益发放
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 每日扫描当天生日的会员，避免同一年重复发券
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantBirthdayBenefits() {
        MemberBirthdayBenefitRuleDTO rule = readConfig(MemberBenefitConfigKeys.BIRTHDAY_BENEFIT_RULE, defaultBirthdayRule(), MemberBirthdayBenefitRuleDTO.class);
        if (!Boolean.TRUE.equals(rule.getEnabled()) || rule.getCouponTemplateId() == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        String birthdayKey = today.format(DateTimeFormatter.ofPattern("MM-dd"));
        String yearKey = String.valueOf(today.getYear());
        List<MemberProfile> birthdayMembers = memberProfileMapper.selectList(new LambdaQueryWrapper<MemberProfile>()
                .isNotNull(MemberProfile::getBirthday)
                .apply("DATE_FORMAT(birthday,'%m-%d') = {0}", birthdayKey));

        for (MemberProfile profile : birthdayMembers) {
            boolean granted = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                    .eq(MemberBenefitGrantLog::getMemberId, profile.getId())
                    .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_BIRTHDAY)
                    .eq(MemberBenefitGrantLog::getTriggerKey, "BIRTHDAY_YEAR")
                    .eq(MemberBenefitGrantLog::getTriggerValue, yearKey)) > 0;
            if (granted) {
                continue;
            }

            SysUser user = sysUserMapper.selectById(profile.getUserId());
            if (user == null) {
                continue;
            }
            UserCoupon coupon = couponService.grantCouponToUser(rule.getCouponTemplateId(), user, SOURCE_TYPE_BIRTHDAY, "生日权益");
            createBenefitGrantLog(profile, profile.getLevelId(), rule.getCouponTemplateId(), coupon.getId(), BENEFIT_TYPE_BIRTHDAY,
                    "BIRTHDAY_YEAR", yearKey, "生日权益发放");
        }
    }

    /**
     * 执行积分过期
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 基于积分流水先进先出计算已过期且尚未消耗的积分并写入过期流水
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireMemberPoints() {
        MemberPointsExpireRuleDTO rule = readConfig(MemberBenefitConfigKeys.POINTS_EXPIRE_RULE, defaultPointsExpireRule(), MemberPointsExpireRuleDTO.class);
        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            return;
        }

        List<MemberPointsRecord> expiredPositiveRecords = memberPointsRecordMapper.selectList(new LambdaQueryWrapper<MemberPointsRecord>()
                .gt(MemberPointsRecord::getChangeAmount, 0)
                .isNotNull(MemberPointsRecord::getExpireTime)
                .le(MemberPointsRecord::getExpireTime, LocalDateTime.now())
                .orderByAsc(MemberPointsRecord::getExpireTime)
                .orderByAsc(MemberPointsRecord::getCreateTime));
        Map<Long, List<MemberPointsRecord>> recordMap = expiredPositiveRecords.stream()
                .collect(Collectors.groupingBy(MemberPointsRecord::getMemberId));

        for (Map.Entry<Long, List<MemberPointsRecord>> entry : recordMap.entrySet()) {
            MemberProfile profile = memberProfileMapper.selectByIdForUpdate(entry.getKey());
            if (profile == null) {
                continue;
            }
            Map<Long, Integer> remainingMap = calculateRemainingLotMap(profile.getId());
            for (MemberPointsRecord sourceRecord : entry.getValue()) {
                Integer remaining = remainingMap.getOrDefault(sourceRecord.getId(), 0);
                if (remaining <= 0) {
                    continue;
                }
                boolean expired = memberPointsRecordMapper.selectCount(new LambdaQueryWrapper<MemberPointsRecord>()
                        .eq(MemberPointsRecord::getBizType, MemberBizTypeConstants.POINTS_EXPIRE)
                        .eq(MemberPointsRecord::getBizId, sourceRecord.getId())) > 0;
                if (expired) {
                    continue;
                }

                int currentBalance = profile.getPointsBalance() == null ? 0 : profile.getPointsBalance();
                int expireAmount = Math.min(currentBalance, remaining);
                if (expireAmount <= 0) {
                    continue;
                }

                profile.setPointsBalance(currentBalance - expireAmount);
                memberProfileMapper.updateById(profile);

                MemberPointsRecord record = new MemberPointsRecord();
                record.setMemberId(profile.getId());
                record.setUserId(profile.getUserId());
                record.setChangeType(MemberPointsChangeTypeConstants.EXPIRE);
                record.setBizType(MemberBizTypeConstants.POINTS_EXPIRE);
                record.setBizId(sourceRecord.getId());
                record.setChangeAmount(-expireAmount);
                record.setBalanceAfter(profile.getPointsBalance());
                record.setRemark("积分过期，来源流水ID=" + sourceRecord.getId());
                memberPointsRecordMapper.insert(record);
            }
        }
    }

    /**
     * 发放升级礼包
     *
     * @param memberId 会员ID
     * @param levelId 等级ID
     * @author Henfon
     * @date 2026-07-02
     * @description 当会员升级到配置了升级礼包的等级时自动发放一次
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantUpgradeGift(Long memberId, Long levelId) {
        if (memberId == null || levelId == null) {
            return;
        }
        MemberProfile profile = memberProfileMapper.selectById(memberId);
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (profile == null || level == null || level.getUpgradeCouponTemplateId() == null) {
            return;
        }
        boolean granted = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                .eq(MemberBenefitGrantLog::getMemberId, memberId)
                .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_UPGRADE)
                .eq(MemberBenefitGrantLog::getTriggerKey, "LEVEL_UPGRADE")
                .eq(MemberBenefitGrantLog::getTriggerValue, String.valueOf(levelId))) > 0;
        if (granted) {
            return;
        }

        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user == null) {
            return;
        }
        UserCoupon coupon = couponService.grantCouponToUser(level.getUpgradeCouponTemplateId(), user, SOURCE_TYPE_LEVEL_UPGRADE, "升级礼包");
        createBenefitGrantLog(profile, levelId, level.getUpgradeCouponTemplateId(), coupon.getId(), BENEFIT_TYPE_UPGRADE,
                "LEVEL_UPGRADE", String.valueOf(levelId), "会员升级礼包");
    }

    /**
     * 调整订单积分抵现
     *
     * @param order 订单
     * @param userId 用户ID
     * @author Henfon
     * @date 2026-07-02
     * @description 订单创建和重算时统一裁剪积分抵现，并在金额下降时释放超额积分
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustOrderPointsDeduction(Order order, Long userId) {
        if (order == null || order.getId() == null) {
            return;
        }
        int currentPoints = order.getPointsUsed() == null ? 0 : order.getPointsUsed();
        if (currentPoints <= 0 && (order.getPointsDiscountAmount() == null || order.getPointsDiscountAmount().compareTo(BigDecimal.ZERO) <= 0)) {
            order.setPointsUsed(0);
            order.setPointsDiscountAmount(BigDecimal.ZERO);
            return;
        }

        BigDecimal baseAmount = (order.getOriginalAmount() == null ? BigDecimal.ZERO : order.getOriginalAmount())
                .multiply(order.getDiscountRate() == null ? BigDecimal.ONE : order.getDiscountRate())
                .setScale(2, RoundingMode.HALF_UP);
        MemberPointsDeductionPreviewVO preview = previewPointsDeduction(userId, baseAmount, currentPoints);
        int actualUsed = preview.getActualUsedPoints() == null ? 0 : preview.getActualUsedPoints();

        if (actualUsed < currentPoints && userId != null) {
            MemberProfile profile = memberProfileMapper.selectByUserIdForUpdate(userId);
            if (profile != null) {
                memberPointsService.returnPointsForOrder(profile, order.getId(), currentPoints - actualUsed, "订单重算释放积分");
            }
        } else if (actualUsed > currentPoints && userId != null) {
            MemberProfile profile = memberProfileMapper.selectByUserIdForUpdate(userId);
            if (profile != null) {
                memberPointsService.deductPointsForOrder(profile, order.getId(), actualUsed - currentPoints, "订单重算补扣积分");
            }
        }

        order.setPointsUsed(actualUsed);
        order.setPointsDiscountAmount(preview.getDeductionAmount() == null ? BigDecimal.ZERO : preview.getDeductionAmount());
    }

    private <T> T readConfig(String key, T defaultValue, Class<T> clazz) {
        try {
            String configValue = sysConfigService.getConfigValue(key);
            if (StrUtil.isBlank(configValue)) {
                return defaultValue;
            }
            return objectMapper.readValue(configValue, clazz);
        } catch (Exception e) {
            log.warn("读取会员权益配置失败: key={}, error={}", key, e.getMessage());
            return defaultValue;
        }
    }

    private void saveConfig(String key, Object value, String name) {
        try {
            sysConfigService.saveConfigValue(key, objectMapper.writeValueAsString(value), name, "会员权益配置");
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "保存会员权益配置失败: " + e.getMessage());
        }
    }

    private MemberPointsDeductionRuleDTO defaultPointsDeductionRule() {
        MemberPointsDeductionRuleDTO dto = new MemberPointsDeductionRuleDTO();
        dto.setEnabled(false);
        dto.setPointsPerStep(100);
        dto.setAmountPerStep(new BigDecimal("1.00"));
        dto.setMaxDeductionRatio(new BigDecimal("0.20"));
        dto.setMaxPointsPerOrder(2000);
        return dto;
    }

    private MemberPointsExpireRuleDTO defaultPointsExpireRule() {
        MemberPointsExpireRuleDTO dto = new MemberPointsExpireRuleDTO();
        dto.setEnabled(false);
        dto.setExpireDays(365);
        return dto;
    }

    private MemberBirthdayBenefitRuleDTO defaultBirthdayRule() {
        MemberBirthdayBenefitRuleDTO dto = new MemberBirthdayBenefitRuleDTO();
        dto.setEnabled(false);
        dto.setCouponTemplateId(null);
        return dto;
    }

    private void validateBirthdayRule(MemberBirthdayBenefitRuleDTO rule) {
        if (rule != null && Boolean.TRUE.equals(rule.getEnabled()) && rule.getCouponTemplateId() != null) {
            couponService.getTemplateEntity(rule.getCouponTemplateId());
        }
    }

    private int normalizePointsByStep(int points, MemberPointsDeductionRuleDTO rule) {
        if (points <= 0 || rule.getPointsPerStep() == null || rule.getPointsPerStep() <= 0) {
            return 0;
        }
        return points / rule.getPointsPerStep() * rule.getPointsPerStep();
    }

    private int calculatePointsByDeductionAmount(BigDecimal amount, MemberPointsDeductionRuleDTO rule) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || rule.getAmountPerStep() == null || rule.getAmountPerStep().compareTo(BigDecimal.ZERO) <= 0
                || rule.getPointsPerStep() == null || rule.getPointsPerStep() <= 0) {
            return 0;
        }
        int stepCount = amount.divide(rule.getAmountPerStep(), 0, RoundingMode.DOWN).intValue();
        return stepCount * rule.getPointsPerStep();
    }

    private BigDecimal calculateDeductionAmount(int points, MemberPointsDeductionRuleDTO rule) {
        if (points <= 0 || rule.getPointsPerStep() == null || rule.getPointsPerStep() <= 0
                || rule.getAmountPerStep() == null || rule.getAmountPerStep().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return rule.getAmountPerStep()
                .multiply(BigDecimal.valueOf(points / rule.getPointsPerStep()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private AppMemberCouponExchangeVO buildAppExchangeVO(MemberCouponExchange exchange, Long userId) {
        AppMemberCouponExchangeVO vo = BeanUtil.copyProperties(exchange, AppMemberCouponExchangeVO.class);
        Long exchangedCount = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                .eq(MemberBenefitGrantLog::getUserId, userId)
                .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_EXCHANGE)
                .eq(MemberBenefitGrantLog::getTriggerKey, "EXCHANGE")
                .eq(MemberBenefitGrantLog::getTriggerValue, String.valueOf(exchange.getId())));
        vo.setExchangedCount(exchangedCount == null ? 0 : exchangedCount.intValue());
        CouponTemplate template = couponService.getTemplateEntity(exchange.getTemplateId());
        vo.setDescription(template.getDescription());
        return vo;
    }

    private AppMemberExclusiveBenefitVO buildExclusiveBenefit(MemberProfile profile) {
        AppMemberExclusiveBenefitVO vo = new AppMemberExclusiveBenefitVO();
        if (profile == null || profile.getLevelId() == null) {
            vo.setClaimable(false);
            vo.setClaimTip("当前暂无可领取的等级专属权益");
            return vo;
        }

        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        if (level == null || level.getExclusiveCouponTemplateId() == null) {
            vo.setLevelId(profile.getLevelId());
            vo.setClaimable(false);
            vo.setClaimTip("当前等级未配置专属权益");
            return vo;
        }

        String monthKey = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        boolean claimed = memberBenefitGrantLogMapper.selectCount(new LambdaQueryWrapper<MemberBenefitGrantLog>()
                .eq(MemberBenefitGrantLog::getMemberId, profile.getId())
                .eq(MemberBenefitGrantLog::getBenefitType, BENEFIT_TYPE_EXCLUSIVE)
                .eq(MemberBenefitGrantLog::getTriggerKey, "LEVEL_MONTH")
                .eq(MemberBenefitGrantLog::getTriggerValue, level.getId() + ":" + monthKey)) > 0;
        CouponTemplate template = couponService.getTemplateEntity(level.getExclusiveCouponTemplateId());
        vo.setLevelId(level.getId());
        vo.setLevelName(level.getLevelName());
        vo.setTemplateId(template.getId());
        vo.setTemplateName(template.getName());
        vo.setClaimable(!claimed);
        vo.setClaimTip(claimed ? "本月已领取" : "本月可领取一次");
        return vo;
    }

    private void consumePoints(MemberProfile profile, Integer points, String bizType, Long bizId, String remark) {
        if (profile == null || points == null || points <= 0) {
            return;
        }
        int currentBalance = profile.getPointsBalance() == null ? 0 : profile.getPointsBalance();
        if (currentBalance < points) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "会员积分不足");
        }
        int currentUsed = profile.getTotalPointsUsed() == null ? 0 : profile.getTotalPointsUsed();
        profile.setPointsBalance(currentBalance - points);
        profile.setTotalPointsUsed(currentUsed + points);
        memberProfileMapper.updateById(profile);

        MemberPointsRecord record = new MemberPointsRecord();
        record.setMemberId(profile.getId());
        record.setUserId(profile.getUserId());
        record.setChangeType(MemberPointsChangeTypeConstants.DECREASE);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setChangeAmount(-points);
        record.setBalanceAfter(profile.getPointsBalance());
        record.setRemark(remark);
        memberPointsRecordMapper.insert(record);
    }

    private void createBenefitGrantLog(MemberProfile profile, Long levelId, Long templateId, Long userCouponId,
                                       Integer benefitType, String triggerKey, String triggerValue, String remark) {
        MemberBenefitGrantLog logEntity = new MemberBenefitGrantLog();
        logEntity.setMemberId(profile.getId());
        logEntity.setUserId(profile.getUserId());
        logEntity.setLevelId(levelId);
        logEntity.setTemplateId(templateId);
        logEntity.setUserCouponId(userCouponId);
        logEntity.setBenefitType(benefitType);
        logEntity.setTriggerKey(triggerKey);
        logEntity.setTriggerValue(triggerValue);
        logEntity.setRemark(remark);
        memberBenefitGrantLogMapper.insert(logEntity);
    }

    private Map<Long, Integer> calculateRemainingLotMap(Long memberId) {
        List<MemberPointsRecord> records = memberPointsRecordMapper.selectList(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getMemberId, memberId)
                .orderByAsc(MemberPointsRecord::getCreateTime)
                .orderByAsc(MemberPointsRecord::getId));
        Deque<PointsLot> lots = new ArrayDeque<>();
        for (MemberPointsRecord record : records) {
            int amount = record.getChangeAmount() == null ? 0 : record.getChangeAmount();
            if (amount > 0) {
                lots.addLast(new PointsLot(record.getId(), amount, record.getExpireTime()));
                continue;
            }
            int consume = Math.abs(amount);
            while (consume > 0 && !lots.isEmpty()) {
                PointsLot lot = lots.peekFirst();
                int used = Math.min(consume, lot.remaining);
                lot.remaining -= used;
                consume -= used;
                if (lot.remaining <= 0) {
                    lots.pollFirst();
                }
            }
        }
        return lots.stream()
                .sorted(Comparator.comparing(PointsLot::getRecordId))
                .collect(Collectors.toMap(PointsLot::getRecordId, PointsLot::getRemaining));
    }

    /**
     * 积分批次余额
     */
    private static final class PointsLot {

        private final Long recordId;
        private int remaining;
        private final LocalDateTime expireTime;

        private PointsLot(Long recordId, int remaining, LocalDateTime expireTime) {
            this.recordId = recordId;
            this.remaining = remaining;
            this.expireTime = expireTime;
        }

        public Long getRecordId() {
            return recordId;
        }

        public int getRemaining() {
            return remaining;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }
    }
}
