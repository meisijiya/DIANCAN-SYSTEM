package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.ConfigCreateDTO;
import com.scaffold.modules.system.dto.ConfigQueryDTO;
import com.scaffold.modules.system.dto.ConfigUpdateDTO;
import com.scaffold.modules.system.service.SysConfigService;
import com.scaffold.modules.system.vo.ConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置管理控制器
 *
 * @author Henfon
 */
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private static final String ADMIN_THEME_PRESET_KEY = "admin.theme.preset";
    private static final String ADMIN_THEME_PRESET_NAME = "管理端主题预设";
    private static final String ADMIN_THEME_PRESET_REMARK = "当前管理端全局主题预设ID";

    private final SysConfigService configService;

    @Operation(summary = "创建配置")
    @PostMapping
    @SaCheckPermission("system:config:add")
    public Result<Void> create(@Valid @RequestBody ConfigCreateDTO dto) {
        configService.createConfig(dto);
        return Result.success();
    }

    @Operation(summary = "更新配置")
    @PutMapping
    @SaCheckPermission("system:config:edit")
    public Result<Void> update(@Valid @RequestBody ConfigUpdateDTO dto) {
        configService.updateConfig(dto);
        return Result.success();
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{configId}")
    @SaCheckPermission("system:config:delete")
    public Result<Void> delete(@Parameter(description = "配置ID") @PathVariable Long configId) {
        configService.deleteConfig(configId);
        return Result.success();
    }

    @Operation(summary = "分页查询配置")
    @GetMapping("/page")
    @SaCheckPermission("system:config:list")
    public Result<PageResult<ConfigVO>> page(ConfigQueryDTO dto) {
        return Result.success(configService.pageList(dto));
    }

    @Operation(summary = "根据配置键获取配置值")
    @GetMapping("/key/{configKey}")
    public Result<String> getByKey(@Parameter(description = "配置键") @PathVariable String configKey) {
        return Result.success(configService.getConfigValue(configKey));
    }

    /**
     * 保存管理端主题预设
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 将当前管理端主题预设ID写入系统配置，供前端刷新后自动回显。
     * @param presetId 主题预设ID
     * @return 保存结果
     */
    @Operation(summary = "保存管理端主题预设")
    @PutMapping("/theme-preset/{presetId}")
    @SaCheckPermission("system:config:edit")
    public Result<Void> saveAdminThemePreset(@Parameter(description = "主题预设ID") @PathVariable String presetId) {
        configService.saveConfigValue(ADMIN_THEME_PRESET_KEY, presetId, ADMIN_THEME_PRESET_NAME, ADMIN_THEME_PRESET_REMARK);
        return Result.success();
    }

    /**
     * 获取管理端主题预设
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 读取当前管理端全局主题预设ID，供前端初始化主题状态。
     * @return 主题预设ID
     */
    @Operation(summary = "获取管理端主题预设")
    @GetMapping("/theme-preset")
    public Result<String> getAdminThemePreset() {
        return Result.success(configService.getConfigValue(ADMIN_THEME_PRESET_KEY));
    }

    @Operation(summary = "获取配置详情")
    @GetMapping("/{configId}")
    @SaCheckPermission("system:config:list")
    public Result<ConfigVO> getInfo(@Parameter(description = "配置ID") @PathVariable Long configId) {
        return Result.success(cn.hutool.core.bean.BeanUtil.copyProperties(
                configService.getById(configId), ConfigVO.class));
    }

    @Operation(summary = "刷新配置缓存")
    @PostMapping("/refresh")
    @SaCheckPermission("system:config:edit")
    public Result<Void> refreshCache() {
        configService.refreshCache();
        return Result.success();
    }
}
