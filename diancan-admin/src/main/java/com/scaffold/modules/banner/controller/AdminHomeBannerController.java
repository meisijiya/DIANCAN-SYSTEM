package com.scaffold.modules.banner.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.banner.dto.HomeBannerCreateDTO;
import com.scaffold.modules.banner.dto.HomeBannerQueryDTO;
import com.scaffold.modules.banner.dto.HomeBannerUpdateDTO;
import com.scaffold.modules.banner.service.HomeBannerService;
import com.scaffold.modules.banner.vo.HomeBannerVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 首页轮播图控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "首页轮播图（管理端）")
@RestController
@RequestMapping("/admin/banner")
@RequiredArgsConstructor
public class AdminHomeBannerController {

    private final HomeBannerService homeBannerService;

    /**
     * 分页查询轮播图
     *
     * @param dto 查询条件
     * @return 轮播图分页结果
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端分页查询轮播图配置
     */
    @Operation(summary = "分页查询轮播图")
    @SaCheckPermission("banner:list")
    @GetMapping("/page")
    public Result<PageResult<HomeBannerVO>> pageList(HomeBannerQueryDTO dto) {
        return Result.success(homeBannerService.pageList(dto));
    }

    /**
     * 创建轮播图
     *
     * @param dto 创建参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 新增首页轮播图
     */
    @Operation(summary = "创建轮播图")
    @SaCheckPermission("banner:create")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody HomeBannerCreateDTO dto) {
        homeBannerService.create(dto);
        return Result.success();
    }

    /**
     * 更新轮播图
     *
     * @param id 轮播图ID
     * @param dto 更新参数
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 更新首页轮播图
     */
    @Operation(summary = "更新轮播图")
    @SaCheckPermission("banner:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody HomeBannerUpdateDTO dto) {
        dto.setId(id);
        homeBannerService.update(dto);
        return Result.success();
    }

    /**
     * 更新轮播图状态
     *
     * @param id 轮播图ID
     * @param status 状态
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 启用或停用首页轮播图
     */
    @Operation(summary = "更新轮播图状态")
    @SaCheckPermission("banner:update")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        homeBannerService.updateStatus(id, status);
        return Result.success();
    }
}
