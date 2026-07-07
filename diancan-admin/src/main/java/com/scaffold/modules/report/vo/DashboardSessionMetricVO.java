package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 首页营业时段指标 VO
 *
 * @author Henfon
 */
@Data
public class DashboardSessionMetricVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 时段名称
     */
    private String label;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 营业额
     */
    private BigDecimal revenue;

    /**
     * 订单数
     */
    private Integer orderCount;

    /**
     * 客单价
     */
    private BigDecimal averageTicket;
}
