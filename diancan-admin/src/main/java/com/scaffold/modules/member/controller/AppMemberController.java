package com.scaffold.modules.member.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.member.service.MemberGrowthService;
import com.scaffold.modules.member.service.MemberBenefitService;
import com.scaffold.modules.member.service.MemberLevelService;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.service.MemberProfileService;
import com.scaffold.modules.member.vo.AppMemberBenefitOverviewVO;
import com.scaffold.modules.member.vo.AppMemberCenterVO;
import com.scaffold.modules.member.vo.AppMemberRewardSummaryVO;
import com.scaffold.modules.member.vo.MemberGrowthRecordVO;
import com.scaffold.modules.member.vo.MemberLevelVO;
import com.scaffold.modules.member.vo.MemberPointsRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "会员（小程序端）")
@RestController
@RequestMapping("/app/member")
@RequiredArgsConstructor
public class AppMemberController {

    private final MemberProfileService memberProfileService;
    private final MemberLevelService memberLevelService;
    private final MemberPointsService memberPointsService;
    private final MemberGrowthService memberGrowthService;
    private final MemberBenefitService memberBenefitService;

    /**
     * 查询当前会员中心信息
     *
     * @return 会员中心数据
     * @author Henfon
     * @date 2026-06-30
     * @description 返回当前用户的等级、成长值、积分和下一等级门槛
     */
    @Operation(summary = "查询当前会员中心信息")
    @GetMapping("/me")
    public Result<AppMemberCenterVO> me() {
        return Result.success(memberProfileService.getCurrentMemberCenter(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 查询会员等级列表
     *
     * @return 等级列表
     * @author Henfon
     * @date 2026-06-30
     * @description 小程序查询启用中的会员等级说明
     */
    @Operation(summary = "查询会员等级列表")
    @GetMapping("/level/list")
    public Result<List<MemberLevelVO>> levelList() {
        return Result.success(memberLevelService.listEnabled());
    }

    /**
     * 查询我的积分流水
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 积分流水分页数据
     * @author Henfon
     * @date 2026-06-30
     * @description 小程序查询当前会员自己的积分流水
     */
    @Operation(summary = "查询我的积分流水")
    @GetMapping("/points-record/page")
    public Result<PageResult<MemberPointsRecordVO>> pointsPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(memberPointsService.pageCurrentMember(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    /**
     * 查询我的成长值流水
     *
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 成长值流水分页数据
     * @author Henfon
     * @date 2026-06-30
     * @description 小程序查询当前会员自己的成长值流水
     */
    @Operation(summary = "查询我的成长值流水")
    @GetMapping("/growth-record/page")
    public Result<PageResult<MemberGrowthRecordVO>> growthPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(memberGrowthService.pageCurrentMember(StpUtil.getLoginIdAsLong(), pageNum, pageSize));
    }

    /**
     * 查询订单会员奖励摘要
     *
     * @param orderId 订单ID
     * @return 会员奖励摘要
     * @author Henfon
     * @date 2026-07-01
     * @description 支付成功页按订单查询本次到账的积分和成长值
     */
    @Operation(summary = "查询订单会员奖励摘要")
    @GetMapping("/reward-summary")
    public Result<AppMemberRewardSummaryVO> rewardSummary(@RequestParam Long orderId) {
        Long userId = StpUtil.getLoginIdAsLong();
        AppMemberRewardSummaryVO vo = new AppMemberRewardSummaryVO();
        vo.setOrderId(orderId);
        vo.setPointsReward(memberPointsService.getRewardPointsByOrder(userId, orderId));
        vo.setGrowthReward(memberGrowthService.getRewardGrowthByOrder(userId, orderId));
        vo.setSettled((vo.getPointsReward() != null && vo.getPointsReward() > 0)
                || (vo.getGrowthReward() != null && vo.getGrowthReward() > 0));
        return Result.success(vo);
    }

    /**
     * 查询会员权益概览
     *
     * @return 权益概览
     * @author Henfon
     * @date 2026-07-02
     * @description 返回积分抵现规则、可兑换优惠券和等级专属权益
     */
    @Operation(summary = "查询会员权益概览")
    @GetMapping("/benefit-overview")
    public Result<AppMemberBenefitOverviewVO> benefitOverview() {
        return Result.success(memberBenefitService.getAppBenefitOverview(StpUtil.getLoginIdAsLong()));
    }

    /**
     * 兑换优惠券
     *
     * @param exchangeId 兑换配置ID
     * @return 空结果
     * @author Henfon
     * @date 2026-07-02
     * @description 当前会员使用积分兑换指定优惠券
     */
    @Operation(summary = "兑换优惠券")
    @PostMapping("/exchange/{exchangeId}")
    public Result<Void> exchangeCoupon(@PathVariable Long exchangeId) {
        memberBenefitService.exchangeCoupon(StpUtil.getLoginIdAsLong(), exchangeId);
        return Result.success();
    }

    /**
     * 领取等级专属券
     *
     * @return 空结果
     * @author Henfon
     * @date 2026-07-02
     * @description 当前会员领取本月等级专属优惠券
     */
    @Operation(summary = "领取等级专属券")
    @PostMapping("/exclusive-coupon/claim")
    public Result<Void> claimExclusiveCoupon() {
        memberBenefitService.claimExclusiveCoupon(StpUtil.getLoginIdAsLong());
        return Result.success();
    }
}
