package com.scaffold.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.DictTypeCreateDTO;
import com.scaffold.modules.system.dto.DictTypeQueryDTO;
import com.scaffold.modules.system.dto.DictTypeUpdateDTO;
import com.scaffold.modules.system.service.SysDictTypeService;
import com.scaffold.modules.system.vo.DictTypeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典类型管理控制器
 *
 * @author Henfon
 */
@Tag(name = "字典类型管理")
@RestController
@RequestMapping("/system/dict/type")
@RequiredArgsConstructor
public class SysDictTypeController {

    private final SysDictTypeService dictTypeService;

    @Operation(summary = "创建字典类型")
    @PostMapping
    @SaCheckPermission("system:dict:add")
    public Result<Void> create(@Valid @RequestBody DictTypeCreateDTO dto) {
        dictTypeService.createDictType(dto);
        return Result.success();
    }

    @Operation(summary = "更新字典类型")
    @PutMapping
    @SaCheckPermission("system:dict:edit")
    public Result<Void> update(@Valid @RequestBody DictTypeUpdateDTO dto) {
        dictTypeService.updateDictType(dto);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/{dictTypeId}")
    @SaCheckPermission("system:dict:delete")
    public Result<Void> delete(@Parameter(description = "字典类型ID") @PathVariable Long dictTypeId) {
        dictTypeService.deleteDictType(dictTypeId);
        return Result.success();
    }

    @Operation(summary = "分页查询字典类型")
    @GetMapping("/page")
    @SaCheckPermission("system:dict:list")
    public Result<PageResult<DictTypeVO>> page(DictTypeQueryDTO dto) {
        return Result.success(dictTypeService.pageList(dto));
    }

    @Operation(summary = "获取所有字典类型列表")
    @GetMapping("/list")
    @SaCheckPermission("system:dict:list")
    public Result<List<DictTypeVO>> list() {
        return Result.success(dictTypeService.listAll());
    }

    @Operation(summary = "获取字典类型详情")
    @GetMapping("/{dictTypeId}")
    @SaCheckPermission("system:dict:list")
    public Result<DictTypeVO> getInfo(@Parameter(description = "字典类型ID") @PathVariable Long dictTypeId) {
        return Result.success(cn.hutool.core.bean.BeanUtil.copyProperties(
                dictTypeService.getById(dictTypeId), DictTypeVO.class));
    }
}
