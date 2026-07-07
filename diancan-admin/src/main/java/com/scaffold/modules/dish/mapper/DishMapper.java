package com.scaffold.modules.dish.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.dish.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

/**
 * 菜品 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
