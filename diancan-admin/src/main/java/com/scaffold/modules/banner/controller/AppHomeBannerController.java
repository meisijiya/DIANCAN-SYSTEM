package com.scaffold.modules.banner.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.banner.service.HomeBannerService;
import com.scaffold.modules.banner.vo.HomeBannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序轮播图控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "首页轮播图（小程序端）")
@RestController
@RequestMapping("/app/banner")
@RequiredArgsConstructor
public class AppHomeBannerController {

    private final HomeBannerService homeBannerService;

    /**
     * 查询启用轮播图列表
     *
     * @return 轮播图列表
     * @author Henfon
     * @date 2026-06-26
     * @description 小程序按投放位置查询已启用轮播图，用于首页、点餐页和我的页的独立运营位。
     */
    @Operation(summary = "查询启用轮播图列表")
    @GetMapping("/list")
    public Result<List<HomeBannerVO>> listEnabled(@RequestParam(required = false) String scene) {
        return Result.success(homeBannerService.listEnabled(scene));
    }
}
