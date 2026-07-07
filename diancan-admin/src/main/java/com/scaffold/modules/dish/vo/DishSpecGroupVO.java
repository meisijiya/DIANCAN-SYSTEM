package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品规格组 VO
 *
 * @author Henfon
 */
@Data
public class DishSpecGroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
    private List<DishSpecOptionVO> options = new ArrayList<>();
}
