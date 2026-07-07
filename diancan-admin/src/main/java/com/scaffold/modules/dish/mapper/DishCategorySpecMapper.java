package com.scaffold.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.dish.entity.DishCategorySpec;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * 分类默认规格关联 Mapper
 *
 * @author Henfon
 */
public interface DishCategorySpecMapper extends BaseMapper<DishCategorySpec> {

    /**
     * 物理删除指定分类的默认规格关联
     *
     * @param categoryId 分类ID
     * @author Henfon
     * @date 2026-07-03
     * @description 分类默认规格重建前需彻底清空旧关联，避免逻辑删除记录占用唯一索引
     */
    @Delete("DELETE FROM dish_category_spec WHERE category_id = #{categoryId}")
    void deleteByCategoryIdPhysical(@Param("categoryId") Long categoryId);
}
