package com.scaffold.modules.table.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.Result;
import com.scaffold.modules.table.dto.TableAreaCreateDTO;
import com.scaffold.modules.table.dto.TableAreaUpdateDTO;
import com.scaffold.modules.table.service.TableAreaService;
import com.scaffold.modules.table.vo.TableAreaVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 桌台区域管理控制器
 *
 * @author Henfon
 */
@Tag(name = "桌台区域管理（管理端）")
@RestController
@RequestMapping("/admin/table/area")
@RequiredArgsConstructor
public class AdminTableAreaController {

    private final TableAreaService tableAreaService;

    @Operation(summary = "获取全部桌台区域列表")
    @GetMapping("/list")
    @SaCheckPermission("table:area:list")
    public Result<List<TableAreaVO>> list() {
        return Result.success(tableAreaService.listAll());
    }

    @Operation(summary = "获取启用中的桌台区域列表")
    @GetMapping("/enabled-list")
    public Result<List<TableAreaVO>> enabledList() {
        return Result.success(tableAreaService.listEnabled());
    }

    @Operation(summary = "创建桌台区域")
    @PostMapping
    @SaCheckPermission("table:area:manage")
    public Result<Void> create(@Valid @RequestBody TableAreaCreateDTO dto) {
        tableAreaService.createArea(dto);
        return Result.success();
    }

    @Operation(summary = "更新桌台区域")
    @PutMapping("/{id}")
    @SaCheckPermission("table:area:manage")
    public Result<Void> update(@Parameter(description = "区域ID") @PathVariable Long id,
                               @Valid @RequestBody TableAreaUpdateDTO dto) {
        dto.setId(id);
        tableAreaService.updateArea(dto);
        return Result.success();
    }

    @Operation(summary = "删除桌台区域")
    @DeleteMapping("/{id}")
    @SaCheckPermission("table:area:manage")
    public Result<Void> delete(@Parameter(description = "区域ID") @PathVariable Long id) {
        tableAreaService.deleteArea(id);
        return Result.success();
    }
}
