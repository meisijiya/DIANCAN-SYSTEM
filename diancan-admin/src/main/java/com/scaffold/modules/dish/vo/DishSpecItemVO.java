package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品规格项 VO
 *
 * @author Henfon
 */
@Data
public class DishSpecItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long specGroupId;
    private String specGroupName;
    private List<Long> optionIds = new ArrayList<>();
    private List<String> optionNames = new ArrayList<>();
}
