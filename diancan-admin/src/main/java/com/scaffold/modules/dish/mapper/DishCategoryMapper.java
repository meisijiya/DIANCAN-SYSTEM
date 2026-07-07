package com.scaffold.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.dish.entity.DishCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品分类 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface DishCategoryMapper extends BaseMapper<DishCategory> {
}
