package com.scaffold.modules.dish.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.dish.dto.DishCategoryCreateDTO;
import com.scaffold.modules.dish.dto.DishCategorySortDTO;
import com.scaffold.modules.dish.dto.DishCategoryUpdateDTO;
import com.scaffold.modules.dish.entity.DishCategory;
import com.scaffold.modules.dish.vo.DishCategoryVO;

import java.util.List;

/**
 * 菜品分类服务接口
 *
 * @author Henfon
 */
public interface DishCategoryService extends IService<DishCategory> {

    /**
     * 获取所有启用的分类列表（按排序序号升序）
     *
     * @return 启用的分类列表
     */
    List<DishCategoryVO> listEnabled();

    /**
     * 获取所有分类列表（含停用，按排序序号升序）
     *
     * @return 所有分类列表
     */
    List<DishCategoryVO> listAll();

    /**
     * 创建菜品分类
     *
     * @param dto 创建参数
     */
    void createCategory(DishCategoryCreateDTO dto);

    /**
     * 更新菜品分类
     *
     * @param dto 更新参数
     */
    void updateCategory(DishCategoryUpdateDTO dto);

    /**
     * 删除菜品分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 批量更新分类排序
     *
     * @param dto 排序参数
     */
    void updateSort(DishCategorySortDTO dto);
}
