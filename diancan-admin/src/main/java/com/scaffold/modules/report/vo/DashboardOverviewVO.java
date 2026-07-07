package com.scaffold.modules.report.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 首页经营概览 VO
 *
 * @author Henfon
 */
@Data
public class DashboardOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 今日营业额
     */
    private BigDecimal todayRevenue;

    /**
     * 昨日营业额
     */
    private BigDecimal yesterdayRevenue;

    /**
     * 今日订单数
     */
    private Integer todayOrderCount;

    /**
     * 昨日订单数
     */
    private Integer yesterdayOrderCount;

    /**
     * 今日客单价
     */
    private BigDecimal averageTicket;

    /**
     * 桌台占用率
     */
    private BigDecimal occupancyRate;

    /**
     * 今日翻台率
     */
    private BigDecimal todayTableTurnover;

    /**
     * 昨日翻台率
     */
    private BigDecimal yesterdayTableTurnover;

    /**
     * 桌台状态汇总
     */
    private DashboardTableStatsVO tableStats;

    /**
     * 近7日营收趋势
     */
    private List<RevenueVO> revenueTrend;

    /**
     * 今日菜品排行
     */
    private List<DishRankingVO> dishRanking;

    /**
     * 首页提醒
     */
    private List<DashboardAlertVO> alerts;

    /**
     * 今日营业时段表现
     */
    private List<DashboardSessionMetricVO> sessionMetrics;
}
