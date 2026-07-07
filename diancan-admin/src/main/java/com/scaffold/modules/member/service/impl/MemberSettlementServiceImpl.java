package com.scaffold.modules.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.modules.member.config.MemberRewardProperties;
import com.scaffold.modules.member.constant.MemberBizTypeConstants;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.member.service.MemberGrowthService;
import com.scaffold.modules.member.service.MemberBenefitService;
import com.scaffold.modules.member.service.MemberLevelService;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.service.MemberProfileService;
import com.scaffold.modules.member.service.MemberSettlementService;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 会员结算服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberSettlementServiceImpl implements MemberSettlementService {

    private final OrderService orderService;
    private final SysUserMapper sysUserMapper;
    private final MemberProfileService memberProfileService;
    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelService memberLevelService;
    private final MemberPointsService memberPointsService;
    private final MemberGrowthService memberGrowthService;
    private final MemberBenefitService memberBenefitService;
    private final MemberRewardProperties memberRewardProperties;

    /**
     * 订单支付完成后执行会员结算
     *
     * @param orderId 订单ID
     * @author Henfon
     * @date 2026-06-30
     * @description 根据订单实付金额结算积分和成长值，并在需要时自动升级会员等级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleAfterOrderPaid(Long orderId) {
        if (orderId == null) {
            return;
        }

        Order order = orderService.getById(orderId);
        if (order == null || order.getStatus() == null || order.getStatus() != 1) {
            return;
        }
        if (order.getCustomerOpenid() == null || order.getCustomerOpenid().isBlank()) {
            log.info("订单无顾客openid，跳过会员结算: orderId={}", orderId);
            return;
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenid, order.getCustomerOpenid())
                .last("LIMIT 1"));
        if (user == null) {
            log.warn("未根据openid匹配到用户，跳过会员结算: orderId={}, openid={}", orderId, order.getCustomerOpenid());
            return;
        }

        MemberProfile profile = memberProfileService.getOrCreateByUserId(user.getId());
        MemberLevel level = memberLevelService.getByIdOrThrow(profile.getLevelId());
        boolean pointsSettled = memberPointsService.existsByBiz(MemberBizTypeConstants.ORDER_PAY, orderId);
        boolean growthSettled = memberGrowthService.existsByBiz(MemberBizTypeConstants.ORDER_PAY, orderId);
        boolean firstSettlement = !pointsSettled && !growthSettled;

        Integer points = calculateRewardValue(order.getActualAmount(), memberRewardProperties.getPointsPerYuan(), level.getPointsRate());
        Integer growthValue = calculateRewardValue(order.getActualAmount(), memberRewardProperties.getGrowthPerYuan(), BigDecimal.ONE);

        if (!pointsSettled) {
            memberPointsService.addPointsForOrder(profile, orderId, points, "订单支付奖励积分");
        }
        if (!growthSettled) {
            memberGrowthService.addGrowthForOrder(profile, orderId, growthValue, "订单支付奖励成长值");
        }

        MemberProfile refreshedProfile = memberProfileService.getByIdForUpdate(profile.getId());
        if (firstSettlement) {
            Long levelIdBeforeUpgrade = refreshedProfile.getLevelId();
            refreshedProfile.setTotalAmountConsumed((refreshedProfile.getTotalAmountConsumed() == null ? BigDecimal.ZERO : refreshedProfile.getTotalAmountConsumed())
                    .add(order.getActualAmount() == null ? BigDecimal.ZERO : order.getActualAmount()));
            refreshedProfile.setLastConsumeTime(LocalDateTime.now());
            memberProfileMapper.updateById(refreshedProfile);
            memberLevelService.upgradeIfNeeded(refreshedProfile, MemberBizTypeConstants.ORDER_PAY, orderId);
            if (!Objects.equals(levelIdBeforeUpgrade, refreshedProfile.getLevelId())) {
                memberBenefitService.grantUpgradeGift(refreshedProfile.getId(), refreshedProfile.getLevelId());
            }
        }

        log.info("会员结算完成: orderId={}, userId={}, memberId={}, points={}, growthValue={}",
                orderId, user.getId(), refreshedProfile.getId(), points, growthValue);
    }

    /**
     * 订单退款后执行会员逆向回退
     *
     * @param orderId 订单ID
     * @param refundAmount 退款金额
     * @author Henfon
     * @date 2026-07-01
     * @description 按退款比例回退积分、成长值与累计消费，一期不触发会员降级
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackAfterOrderRefund(Long orderId, BigDecimal refundAmount) {
        if (orderId == null || refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Order order = orderService.getById(orderId);
        if (order == null || order.getCustomerOpenid() == null || order.getCustomerOpenid().isBlank()) {
            return;
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getOpenid, order.getCustomerOpenid())
                .last("LIMIT 1"));
        if (user == null) {
            log.warn("退款回退未匹配到用户，跳过会员逆向结算: orderId={}, openid={}", orderId, order.getCustomerOpenid());
            return;
        }

        MemberProfile profile = memberProfileService.getOrCreateByUserId(user.getId());
        BigDecimal orderAmount = order.getActualAmount() == null ? BigDecimal.ZERO : order.getActualAmount();
        if (orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal validRefundAmount = refundAmount.min(orderAmount);
        if (order.getPointsUsed() != null && order.getPointsUsed() > 0) {
            int returnPoints = BigDecimal.valueOf(order.getPointsUsed())
                    .multiply(validRefundAmount)
                    .divide(orderAmount, 0, RoundingMode.DOWN)
                    .intValue();
            if (validRefundAmount.compareTo(orderAmount) >= 0) {
                returnPoints = order.getPointsUsed();
            }
            if (returnPoints > 0) {
                memberPointsService.returnPointsForOrder(profile, orderId, returnPoints, "订单退款回退抵扣积分");
            }
        }
        memberPointsService.rollbackPointsForRefund(profile, orderId, orderAmount, validRefundAmount, "订单退款回退积分");
        memberGrowthService.rollbackGrowthForRefund(profile, orderId, orderAmount, validRefundAmount, "订单退款回退成长值");

        MemberProfile lockedProfile = memberProfileService.getByIdForUpdate(profile.getId());
        BigDecimal currentConsumed = lockedProfile.getTotalAmountConsumed() == null ? BigDecimal.ZERO : lockedProfile.getTotalAmountConsumed();
        lockedProfile.setTotalAmountConsumed(currentConsumed.subtract(validRefundAmount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        memberProfileMapper.updateById(lockedProfile);

        log.info("会员退款逆向结算完成: orderId={}, userId={}, memberId={}, refundAmount={}",
                orderId, user.getId(), lockedProfile.getId(), validRefundAmount);
    }

    /**
     * 计算奖励值
     *
     * @param amount 金额
     * @param basePerYuan 每元基础值
     * @param rate 倍率
     * @return 奖励值
     * @author Henfon
     * @date 2026-06-30
     * @description 将订单金额转换为积分或成长值，统一向下取整
     */
    private Integer calculateRewardValue(BigDecimal amount, Integer basePerYuan, BigDecimal rate) {
        if (amount == null || basePerYuan == null || basePerYuan <= 0 || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return amount.multiply(BigDecimal.valueOf(basePerYuan))
                .multiply(rate)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }
}
