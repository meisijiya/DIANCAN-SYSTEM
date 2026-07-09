package com.scaffold.modules.table.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.modules.system.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 桌台实体
 *
 * @author Henfon
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dining_table")
public class DiningTable extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桌台编号（二维码关联）
     */
    private String code;

    /**
     * 桌台名称（如"A1桌"）
     */
    private String name;

    /**
     * 座位数
     */
    private Integer capacity;

    /**
     * 状态（0空闲 1占用 2已结账 3待清洁）
     */
    private Integer status;

    /**
     * 二维码图片URL
     */
    private String qrCodeUrl;

    /**
     * 区域ID
     */
    private Long areaId;

    /**
     * 区域名称（如"大厅"、"包间"）
     */
    private String areaName;

    /**
     * 当前桌次编码
     */
    private String currentSessionCode;
}
