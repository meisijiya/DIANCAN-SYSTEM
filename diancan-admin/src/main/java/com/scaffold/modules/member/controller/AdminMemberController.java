package com.scaffold.modules.member.controller;

import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.member.dto.MemberGrowthRecordQueryDTO;
import com.scaffold.modules.member.dto.MemberBenefitConfigSaveDTO;
import com.scaffold.modules.member.dto.MemberCouponExchangeSaveDTO;
import com.scaffold.modules.member.dto.MemberLevelCreateDTO;
import com.scaffold.modules.member.dto.MemberLevelStatusDTO;
import com.scaffold.modules.member.dto.MemberLevelUpdateDTO;
import com.scaffold.modules.member.dto.MemberPointsAdjustDTO;
import com.scaffold.modules.member.dto.MemberPointsRecordQueryDTO;
import com.scaffold.modules.member.dto.MemberQueryDTO;
import com.scaffold.modules.member.service.MemberBenefitService;
import com.scaffold.modules.member.service.MemberGrowthService;
import com.scaffold.modules.member.service.MemberLevelService;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.service.MemberProfileService;
import com.scaffold.modules.member.vo.MemberBenefitConfigVO;
import com.scaffold.modules.member.vo.MemberCouponExchangeVO;
import com.scaffold.modules.member.vo.MemberDetailVO;
import com.scaffold.modules.member.vo.MemberGrowthRecordVO;
import com.scaffold.modules.member.vo.MemberLevelVO;
import com.scaffold.modules.member.vo.MemberOverviewVO;
import com.scaffold.modules.member.vo.MemberPointsRecordVO;
import com.scaffold.modules.member.vo.MemberProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会员控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "会员（管理端）")
@RestController
@RequestMapping("/admin/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberProfileService memberProfileService;
    private final MemberLevelService memberLevelService;
    private final MemberPointsService memberPointsService;
    private final MemberGrowthService memberGrowthService;
    private final MemberBenefitService memberBenefitService;

    /**
     * 分页查询会员
     *
     * @param dto 查询参数
     * @return 会员分页数据
     * @author Henfon
     * @date 2026-06-30
     * @description 管理端按等级、状态、昵称和手机号筛选会员列表
     */
    @Operation(summary = "分页查询会员")
    @GetMapping("/page")
    public Result<PageResult<MemberProfileVO>> page(MemberQueryDTO dto) {
        return Result.success(memberProfileService.pageList(dto));
    }

    /**
     * 查询会员详情
     *
     * @param id 会员ID
     * @return 会员详情
     * @author Henfon
     * @date 2026-06-30
     * @description 返回会员基础信息与最近积分、成长值流水
     */
    @Operation(summary = "查询会员详情")
    @GetMapping("/{id}")
    public Result<MemberDetailVO> detail(@PathVariable Long id) {
        return Result.success(memberProfileService.getDetail(id));
    }

    /**
     * 查询会员统计总览
     *
     * @return 会员统计数据
     * @author Henfon
     * @date 2026-07-01
     * @description 管理端查询会员规模、等级分布和近7天新增趋势
     */
    @Operation(summary = "查询会员统计总览")
    @GetMapping("/overview")
    public Result<MemberOverviewVO> overview() {
        return Result.success(memberProfileService.getOverview());
    }

    /**
     * 查询会员等级列表
     *
     * @return 等级列表
     * @author Henfon
     * @date 2026-06-30
     * @description 管理端查询全部等级配置
     */
    @Operation(summary = "查询会员等级列表")
    @GetMapping("/level/list")
    public Result<List<MemberLevelVO>> levelList() {
        return Result.success(memberLevelService.listAll());
    }

    /**
     * 新增会员等级
     *
     * @param dto 创建参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-30
     * @description 新增会员等级配置
     */
    @Operation(summary = "新增会员等级")
    @PostMapping("/level")
    public Result<Void> createLevel(@Valid @RequestBody MemberLevelCreateDTO dto) {
        memberLevelService.create(dto);
        return Result.success();
    }

    /**
     * 更新会员等级
     *
     * @param id 等级ID
     * @param dto 更新参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-30
     * @description 更新会员等级配置
     */
    @Operation(summary = "更新会员等级")
    @PutMapping("/level/{id}")
    public Result<Void> updateLevel(@PathVariable Long id, @Valid @RequestBody MemberLevelUpdateDTO dto) {
        dto.setId(id);
        memberLevelService.update(dto);
        return Result.success();
    }

    /**
     * 更新会员等级状态
     *
     * @param id 等级ID
     * @param dto 状态参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-30
     * @description 启用或停用会员等级
     */
    @Operation(summary = "更新会员等级状态")
    @PutMapping("/level/{id}/status")
    public Result<Void> updateLevelStatus(@PathVariable Long id, @Valid @RequestBody MemberLevelStatusDTO dto) {
        memberLevelService.updateStatus(id, dto.getStatus());
        return Result.success();
    }

    /**
     * 分页查询积分流水
     *
     * @param dto 查询参数
     * @return 积分流水分页数据
     * @author Henfon
     * @date 2026-06-30
     * @description 管理端查询会员积分流水
     */
    @Operation(summary = "分页查询积分流水")
    @GetMapping("/points-record/page")
    public Result<PageResult<MemberPointsRecordVO>> pointsRecordPage(MemberPointsRecordQueryDTO dto) {
        return Result.success(memberPointsService.pageAdmin(dto));
    }

    /**
     * 分页查询成长值流水
     *
     * @param dto 查询参数
     * @return 成长值流水分页数据
     * @author Henfon
     * @date 2026-06-30
     * @description 管理端查询会员成长值流水
     */
    @Operation(summary = "分页查询成长值流水")
    @GetMapping("/growth-record/page")
    public Result<PageResult<MemberGrowthRecordVO>> growthRecordPage(MemberGrowthRecordQueryDTO dto) {
        return Result.success(memberGrowthService.pageAdmin(dto));
    }

    /**
     * 调整会员积分
     *
     * @param id 会员ID
     * @param dto 调整参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-30
     * @description 支持管理端手工增加或扣减积分
     */
    @Operation(summary = "调整会员积分")
    @PostMapping("/{id}/points-adjust")
    public Result<Void> adjustPoints(@PathVariable Long id, @Valid @RequestBody MemberPointsAdjustDTO dto) {
        memberPointsService.adjustPoints(id, dto);
        return Result.success();
    }

    /**
     * 查询会员权益配置
     *
     * @return 会员权益配置
     * @author Henfon
     * @date 2026-07-02
     * @description 返回积分抵现、积分过期和生日权益配置
     */
    @Operation(summary = "查询会员权益配置")
    @GetMapping("/benefit-config")
    public Result<MemberBenefitConfigVO> benefitConfig() {
        return Result.success(memberBenefitService.getBenefitConfig());
    }

    /**
     * 保存会员权益配置
     *
     * @param dto 配置参数
     * @return 空结果
     * @author Henfon
     * @date 2026-07-02
     * @description 保存会员二期三期基础权益规则
     */
    @Operation(summary = "保存会员权益配置")
    @PutMapping("/benefit-config")
    public Result<Void> saveBenefitConfig(@Valid @RequestBody MemberBenefitConfigSaveDTO dto) {
        memberBenefitService.saveBenefitConfig(dto);
        return Result.success();
    }

    /**
     * 查询积分兑换优惠券配置列表
     *
     * @return 配置列表
     * @author Henfon
     * @date 2026-07-02
     * @description 管理端查看积分兑换项
     */
    @Operation(summary = "查询积分兑换优惠券配置列表")
    @GetMapping("/exchange/list")
    public Result<List<MemberCouponExchangeVO>> exchangeList() {
        return Result.success(memberBenefitService.listExchangeConfigs());
    }

    /**
     * 保存积分兑换优惠券配置
     *
     * @param dto 配置参数
     * @return 空结果
     * @author Henfon
     * @date 2026-07-02
     * @description 新增或更新积分兑换优惠券配置
     */
    @Operation(summary = "保存积分兑换优惠券配置")
    @PostMapping("/exchange")
    public Result<Void> saveExchange(@Valid @RequestBody MemberCouponExchangeSaveDTO dto) {
        memberBenefitService.saveExchangeConfig(dto);
        return Result.success();
    }

    /**
     * 删除积分兑换优惠券配置
     *
     * @param id 配置ID
     * @return 空结果
     * @author Henfon
     * @date 2026-07-02
     * @description 删除指定积分兑换项
     */
    @Operation(summary = "删除积分兑换优惠券配置")
    @DeleteMapping("/exchange/{id}")
    public Result<Void> deleteExchange(@PathVariable Long id) {
        memberBenefitService.deleteExchangeConfig(id);
        return Result.success();
    }
}
