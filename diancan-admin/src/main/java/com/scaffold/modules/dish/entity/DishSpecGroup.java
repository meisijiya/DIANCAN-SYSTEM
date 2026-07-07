package com.scaffold.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜品规格组实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_spec_group")
public class DishSpecGroup extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 规格组名称
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
}
