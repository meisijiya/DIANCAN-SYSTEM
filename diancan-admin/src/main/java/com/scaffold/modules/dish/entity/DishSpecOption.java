package com.scaffold.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜品规格选项实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_spec_option")
public class DishSpecOption extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规格组ID
     */
    private Long groupId;

    /**
     * 选项名称
     */
    private String name;

    /**
     * 排序序号
     */
    private Integer sort;
}
