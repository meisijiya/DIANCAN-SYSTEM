package com.scaffold.modules.dish.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.dish.dto.DishCategoryCreateDTO;
import com.scaffold.modules.dish.dto.DishCategorySortDTO;
import com.scaffold.modules.dish.dto.DishCategoryUpdateDTO;
import com.scaffold.modules.dish.service.DishCategoryService;
import com.scaffold.modules.dish.vo.DishCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品分类管理控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "菜品分类管理（管理端）")
@RestController
@RequestMapping("/admin/dish/category")
@RequiredArgsConstructor
public class AdminDishCategoryController {

    private final DishCategoryService dishCategoryService;

    @Operation(summary = "创建菜品分类")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DishCategoryCreateDTO dto) {
        dishCategoryService.createCategory(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜品分类")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "分类ID") @PathVariable Long id,
                               @Valid @RequestBody DishCategoryUpdateDTO dto) {
        dto.setId(id);
        dishCategoryService.updateCategory(dto);
        return Result.success();
    }

    @Operation(summary = "删除菜品分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        dishCategoryService.deleteCategory(id);
        return Result.success();
    }

    @Operation(summary = "批量更新分类排序")
    @PutMapping("/sort")
    public Result<Void> updateSort(@Valid @RequestBody DishCategorySortDTO dto) {
        dishCategoryService.updateSort(dto);
        return Result.success();
    }

    @Operation(summary = "获取所有分类列表（含停用）")
    @GetMapping("/list")
    public Result<List<DishCategoryVO>> list() {
        return Result.success(dishCategoryService.listAll());
    }
}
