package com.scaffold.modules.dish.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.dish.service.DishCategoryService;
import com.scaffold.modules.dish.vo.DishCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜品分类控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "菜品分类（小程序端）")
@RestController
@RequestMapping("/app/dish/category")
@RequiredArgsConstructor
public class AppDishCategoryController {

    private final DishCategoryService dishCategoryService;

    @Operation(summary = "获取启用的分类列表")
    @GetMapping("/list")
    public Result<List<DishCategoryVO>> list() {
        return Result.success(dishCategoryService.listEnabled());
    }
}
