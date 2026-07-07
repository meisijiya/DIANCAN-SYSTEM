package com.scaffold.modules.report.service;

import com.scaffold.modules.report.vo.DishRankingVO;
import com.scaffold.modules.report.vo.DashboardOverviewVO;
import com.scaffold.modules.report.vo.RevenueVO;
import com.scaffold.modules.report.vo.TableTurnoverVO;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表服务接口
 *
 * @author Henfon
 */
public interface ReportService {

    /**
     * 营业额统计（日/周/月维度，汇总已支付订单 actualAmount）
     *
     * @param dimension 维度：day/week/month
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 营业额统计列表
     */
    List<RevenueVO> getRevenue(String dimension, LocalDate startDate, LocalDate endDate);

    /**
     * 菜品销售排行（按销量降序）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param limit     返回条数（Top N）
     * @return 菜品销售排行列表
     */
    List<DishRankingVO> getDishRanking(LocalDate startDate, LocalDate endDate, Integer limit);

    /**
     * 翻台率统计（订单数/桌台总数）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 翻台率统计列表
     */
    List<TableTurnoverVO> getTableTurnover(LocalDate startDate, LocalDate endDate);

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 获取管理端首页经营概览，聚合核心指标、趋势、桌态和经营提醒
     * @return 首页经营概览
     */
    DashboardOverviewVO getDashboardOverview();

    /**
     * 导出营业额报表为 Excel
     *
     * @param dimension 维度：day/week/month
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param response  HTTP 响应
     */
    void exportRevenue(String dimension, LocalDate startDate, LocalDate endDate, HttpServletResponse response);
}
