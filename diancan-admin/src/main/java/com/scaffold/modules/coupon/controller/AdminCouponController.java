package com.scaffold.modules.coupon.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.coupon.dto.CouponGrantDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskDetailQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateCreateDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateUpdateDTO;
import com.scaffold.modules.coupon.dto.UserCouponQueryDTO;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.coupon.vo.CouponGrantTaskDetailVO;
import com.scaffold.modules.coupon.vo.CouponGrantTaskVO;
import com.scaffold.modules.coupon.vo.CouponTemplateVO;
import com.scaffold.modules.coupon.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券管理控制器
 *
 * @author Henfon
 */
@Tag(name = "优惠券管理（管理端）")
@RestController
@RequestMapping("/admin/coupon")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    /**
     * 分页查询优惠券模板
     *
     * @param dto 查询条件
     * @return 模板分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端分页查看优惠券模板
     */
    @Operation(summary = "分页查询优惠券模板")
    @SaCheckPermission("coupon:template:list")
    @GetMapping("/template/page")
    public Result<PageResult<CouponTemplateVO>> pageTemplates(CouponTemplateQueryDTO dto) {
        return Result.success(couponService.pageTemplates(dto));
    }

    /**
     * 创建优惠券模板
     *
     * @param dto 创建参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 新增优惠券模板
     */
    @Operation(summary = "创建优惠券模板")
    @SaCheckPermission("coupon:template:create")
    @PostMapping("/template")
    public Result<Void> createTemplate(@Valid @RequestBody CouponTemplateCreateDTO dto) {
        couponService.createTemplate(dto);
        return Result.success();
    }

    /**
     * 更新优惠券模板
     *
     * @param id 模板ID
     * @param dto 更新参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 更新优惠券模板配置
     */
    @Operation(summary = "更新优惠券模板")
    @SaCheckPermission("coupon:template:update")
    @PutMapping("/template/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody CouponTemplateUpdateDTO dto) {
        dto.setId(id);
        couponService.updateTemplate(dto);
        return Result.success();
    }

    /**
     * 更新优惠券模板状态
     *
     * @param id 模板ID
     * @param status 状态
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 启用或停用优惠券模板
     */
    @Operation(summary = "更新优惠券模板状态")
    @SaCheckPermission("coupon:template:update")
    @PutMapping("/template/{id}/status")
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestParam Integer status) {
        couponService.updateTemplateStatus(id, status);
        return Result.success();
    }

    /**
     * 执行发券
     *
     * @param dto 发券参数
     * @return 发券任务结果
     * @author Henfon
     * @date 2026-06-26
     * @description 支持指定用户和全部用户发券
     */
    @Operation(summary = "执行发券")
    @SaCheckPermission("coupon:grant")
    @PostMapping("/grant")
    public Result<CouponGrantTaskVO> grantCoupons(@Valid @RequestBody CouponGrantDTO dto) {
        return Result.success(couponService.grantCoupons(dto));
    }

    /**
     * 分页查询发券任务
     *
     * @param dto 查询条件
     * @return 发券任务分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 查看异步发券任务进度与执行结果
     */
    @Operation(summary = "分页查询发券任务")
    @SaCheckPermission("coupon:task:list")
    @GetMapping("/task/page")
    public Result<PageResult<CouponGrantTaskVO>> pageGrantTasks(CouponGrantTaskQueryDTO dto) {
        return Result.success(couponService.pageGrantTasks(dto));
    }

    /**
     * 分页查询发券任务明细
     *
     * @param dto 查询条件
     * @return 发券任务明细分页数据
     * @author Henfon
     * @date 2026-07-01
     * @description 按任务查看用户级别的执行结果，便于运营追踪失败明细
     */
    @Operation(summary = "分页查询发券任务明细")
    @SaCheckPermission("coupon:task:list")
    @GetMapping("/task/detail/page")
    public Result<PageResult<CouponGrantTaskDetailVO>> pageGrantTaskDetails(CouponGrantTaskDetailQueryDTO dto) {
        return Result.success(couponService.pageGrantTaskDetails(dto));
    }

    /**
     * 分页查询用户优惠券
     *
     * @param dto 查询条件
     * @return 用户优惠券分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端查看用户持有的优惠券
     */
    @Operation(summary = "分页查询用户优惠券")
    @SaCheckPermission("coupon:user:list")
    @GetMapping("/user/page")
    public Result<PageResult<UserCouponVO>> pageUserCoupons(UserCouponQueryDTO dto) {
        return Result.success(couponService.pageUserCoupons(dto));
    }
}
