package com.scaffold.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 菜品规格映射实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dish_spec_mapping")
public class DishSpecMapping extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 菜品ID
     */
    private Long dishId;

    /**
     * 规格组ID
     */
    private Long specGroupId;

    /**
     * 可选规格值ID，逗号分隔
     */
    private String optionIds;
}
