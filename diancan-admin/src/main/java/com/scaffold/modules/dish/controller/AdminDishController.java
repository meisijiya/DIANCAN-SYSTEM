package com.scaffold.modules.dish.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.dish.dto.DishCreateDTO;
import com.scaffold.modules.dish.dto.DishQueryDTO;
import com.scaffold.modules.dish.dto.DishUpdateDTO;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.dish.vo.DishVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 菜品管理控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "菜品管理（管理端）")
@RestController
@RequestMapping("/admin/dish")
@RequiredArgsConstructor
public class AdminDishController {

    private final DishService dishService;

    @Operation(summary = "创建菜品")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody DishCreateDTO dto) {
        dishService.createDish(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜品")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "菜品ID") @PathVariable Long id,
                               @Valid @RequestBody DishUpdateDTO dto) {
        dto.setId(id);
        dishService.updateDish(dto);
        return Result.success();
    }

    @Operation(summary = "更新菜品状态（上架/下架）")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@Parameter(description = "菜品ID") @PathVariable Long id,
                                     @Parameter(description = "状态（0下架 1上架）") @RequestParam Integer status) {
        dishService.updateDishStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取菜品列表（管理端，分页，支持筛选）")
    @GetMapping("/list")
    public Result<PageResult<DishVO>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            DishQueryDTO queryDTO) {
        IPage<DishVO> page = dishService.listDishesForAdmin(pageNum, pageSize, queryDTO);
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "获取菜品详情")
    @GetMapping("/{id}")
    public Result<DishVO> detail(@Parameter(description = "菜品ID") @PathVariable Long id) {
        return Result.success(dishService.getDishDetail(id));
    }
}
