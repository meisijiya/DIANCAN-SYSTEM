package com.scaffold.modules.coupon.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.coupon.dto.AppCouponQueryDTO;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.coupon.vo.UserCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "优惠券（小程序端）")
@RestController
@RequestMapping("/app/coupon")
@RequiredArgsConstructor
public class AppCouponController {

    private final CouponService couponService;

    /**
     * 查询我的优惠券
     *
     * @param dto 查询条件
     * @return 我的优惠券分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 小程序端分页查询当前登录用户的优惠券
     */
    @Operation(summary = "查询我的优惠券")
    @GetMapping("/my")
    public Result<PageResult<UserCouponVO>> pageMyCoupons(AppCouponQueryDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(couponService.pageMyCoupons(userId, dto));
    }
}
