package com.scaffold.modules.print.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印机 VO
 *
 * @author Henfon
 */
@Data
public class PrinterVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 打印机ID
     */
    private Long id;

    /**
     * 打印机名称
     */
    private String name;

    /**
     * 打印机序列号
     */
    private String sn;

    /**
     * 类型（0前台 1后厨）
     */
    private Integer type;

    /**
     * 状态（0离线 1在线）
     */
    private Integer status;

    /**
     * 位置描述
     */
    private String location;

    /**
     * 关联的菜品分类ID列表
     */
    private List<Long> categoryIds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
