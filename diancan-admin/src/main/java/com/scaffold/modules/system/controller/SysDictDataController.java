package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.DictDataCreateDTO;
import com.scaffold.modules.system.dto.DictDataUpdateDTO;
import com.scaffold.modules.system.service.SysDictDataService;
import com.scaffold.modules.system.vo.DictDataVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典数据管理控制器
 *
 * @author Henfon
 */
@Tag(name = "字典数据管理")
@RestController
@RequestMapping("/system/dict/data")
@RequiredArgsConstructor
public class SysDictDataController {

    private final SysDictDataService dictDataService;

    @Operation(summary = "创建字典数据")
    @PostMapping
    @SaCheckPermission("system:dict:add")
    public Result<Void> create(@Valid @RequestBody DictDataCreateDTO dto) {
        dictDataService.createDictData(dto);
        return Result.success();
    }

    @Operation(summary = "更新字典数据")
    @PutMapping
    @SaCheckPermission("system:dict:edit")
    public Result<Void> update(@Valid @RequestBody DictDataUpdateDTO dto) {
        dictDataService.updateDictData(dto);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/{dictDataId}")
    @SaCheckPermission("system:dict:delete")
    public Result<Void> delete(@Parameter(description = "字典数据ID") @PathVariable Long dictDataId) {
        dictDataService.deleteDictData(dictDataId);
        return Result.success();
    }

    @Operation(summary = "根据字典类型ID查询数据列表")
    @GetMapping("/type/{typeId}")
    public Result<List<DictDataVO>> getByTypeId(@Parameter(description = "字典类型ID") @PathVariable Long typeId) {
        return Result.success(dictDataService.getByTypeId(typeId));
    }

    @Operation(summary = "根据字典类型编码查询数据列表")
    @GetMapping("/code/{typeCode}")
    public Result<List<DictDataVO>> getByTypeCode(@Parameter(description = "字典类型编码") @PathVariable String typeCode) {
        return Result.success(dictDataService.getByTypeCode(typeCode));
    }

    @Operation(summary = "刷新字典缓存")
    @PostMapping("/refresh")
    @SaCheckPermission("system:dict:edit")
    public Result<Void> refreshCache() {
        dictDataService.refreshCache();
        return Result.success();
    }

    @Operation(summary = "获取字典数据详情")
    @GetMapping("/{dictDataId}")
    @SaCheckPermission("system:dict:list")
    public Result<DictDataVO> getInfo(@Parameter(description = "字典数据ID") @PathVariable Long dictDataId) {
        return Result.success(cn.hutool.core.bean.BeanUtil.copyProperties(
                dictDataService.getById(dictDataId), DictDataVO.class));
    }
}
