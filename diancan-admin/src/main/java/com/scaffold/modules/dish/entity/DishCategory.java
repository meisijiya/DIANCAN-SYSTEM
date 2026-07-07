package com.scaffold.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜品分类实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_category")
public class DishCategory extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序序号
     */
    private Integer sort;

    /**
     * 状态（0停用 1启用）
     */
    private Integer status;

    /**
     * 分类图片
     */
    private String image;

    /**
     * 规格模板（0无规格 1辣度 2饮品规格）
     */
    private Integer specTemplate;
}
