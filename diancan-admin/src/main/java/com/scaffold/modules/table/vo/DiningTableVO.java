package com.scaffold.modules.table.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 桌台 VO
 *
 * @author Henfon
 */
@Data
public class DiningTableVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桌台ID
     */
    private Long id;

    /**
     * 桌台编号
     */
    private String code;

    /**
     * 桌台名称
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
     * 区域名称
     */
    private String areaName;

    /**
     * 当前桌次编码
     */
    private String currentSessionCode;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
