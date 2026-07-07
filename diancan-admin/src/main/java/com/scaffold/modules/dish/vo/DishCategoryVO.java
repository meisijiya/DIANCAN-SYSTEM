package com.scaffold.modules.dish.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜品分类 VO
 *
 * @author Henfon
 */
@Data
public class DishCategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long id;

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
     * 默认规格组ID列表
     */
    private List<Long> specGroupIds = new ArrayList<>();

    /**
     * 默认规格组名称列表
     */
    private List<String> specGroupNames = new ArrayList<>();

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
