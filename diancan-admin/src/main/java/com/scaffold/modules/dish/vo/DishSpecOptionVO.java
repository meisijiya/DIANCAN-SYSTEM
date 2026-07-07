package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 菜品规格选项 VO
 *
 * @author Henfon
 */
@Data
public class DishSpecOptionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer sort;
}
