package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页桌台汇总 VO
 *
 * @author Henfon
 */
@Data
public class DashboardTableStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 桌台总数
     */
    private Integer total;

    /**
     * 空闲桌台数
     */
    private Integer free;

    /**
     * 占用桌台数
     */
    private Integer occupied;

    /**
     * 已结账桌台数
     */
    private Integer settled;

    /**
     * 待清洁桌台数
     */
    private Integer cleaning;
}
