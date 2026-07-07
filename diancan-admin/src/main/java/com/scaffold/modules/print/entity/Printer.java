package com.scaffold.modules.print.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 打印机实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("printer")
public class Printer extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

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
}
