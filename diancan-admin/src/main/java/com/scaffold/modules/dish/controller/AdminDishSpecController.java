package com.scaffold.modules.dish.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.dish.dto.DishSpecGroupCreateDTO;
import com.scaffold.modules.dish.dto.DishSpecGroupUpdateDTO;
import com.scaffold.modules.dish.service.DishSpecGroupService;
import com.scaffold.modules.dish.vo.DishSpecGroupVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品规格管理控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "菜品规格管理（管理端）")
@RestController
@RequestMapping("/admin/dish/spec")
@RequiredArgsConstructor
public class AdminDishSpecController {

    private final DishSpecGroupService dishSpecGroupService;

    @Operation(summary = "获取规格组列表")
    @GetMapping("/list")
    public Result<List<DishSpecGroupVO>> list() {
        return Result.success(dishSpecGroupService.listAll());
    }

    @Operation(summary = "创建规格组")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DishSpecGroupCreateDTO dto) {
        dishSpecGroupService.createGroup(dto);
        return Result.success();
    }

    @Operation(summary = "更新规格组")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "规格组ID") @PathVariable Long id,
                               @Valid @RequestBody DishSpecGroupUpdateDTO dto) {
        dto.setId(id);
        dishSpecGroupService.updateGroup(dto);
        return Result.success();
    }

    @Operation(summary = "删除规格组")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "规格组ID") @PathVariable Long id) {
        dishSpecGroupService.deleteGroup(id);
        return Result.success();
    }
}
