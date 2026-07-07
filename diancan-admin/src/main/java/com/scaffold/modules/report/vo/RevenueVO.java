package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 营业额统计 VO
 *
 * @author Henfon
 */
@Data
public class RevenueVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日期（日维度: 2024-01-01, 周维度: 2024-01, 月维度: 2024-01）
     */
    private String date;

    /**
     * 总营业额
     */
    private BigDecimal totalRevenue;

    /**
     * 订单数
     */
    private Integer orderCount;
}
