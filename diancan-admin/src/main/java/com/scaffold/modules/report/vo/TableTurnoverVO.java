package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 翻台率统计 VO
 *
 * @author Henfon
 */
@Data
public class TableTurnoverVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日期
     */
    private String date;

    /**
     * 当日订单数
     */
    private Integer totalOrders;

    /**
     * 桌台总数
     */
    private Integer totalTables;

    /**
     * 翻台率（订单数/桌台总数）
     */
    private BigDecimal turnoverRate;
}
