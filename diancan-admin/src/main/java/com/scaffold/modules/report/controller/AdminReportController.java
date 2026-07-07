package com.scaffold.modules.report.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.report.service.ReportService;
import com.scaffold.modules.report.vo.DishRankingVO;
import com.scaffold.modules.report.vo.DashboardOverviewVO;
import com.scaffold.modules.report.vo.RevenueVO;
import com.scaffold.modules.report.vo.TableTurnoverVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 报表控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "数据报表（管理端）")
@RestController
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 获取管理端首页经营概览，统一返回核心指标、趋势和提醒
     * @return 首页经营概览
     */
    @Operation(summary = "首页经营概览")
    @GetMapping("/dashboard-overview")
    public Result<DashboardOverviewVO> getDashboardOverview() {
        return Result.success(reportService.getDashboardOverview());
    }

    @Operation(summary = "营业额统计")
    @GetMapping("/revenue")
    public Result<List<RevenueVO>> getRevenue(
            @Parameter(description = "维度：day/week/month") @RequestParam(defaultValue = "day") String dimension,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportService.getRevenue(dimension, startDate, endDate));
    }

    @Operation(summary = "菜品销售排行")
    @GetMapping("/dish-ranking")
    public Result<List<DishRankingVO>> getDishRanking(
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "返回条数") @RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(reportService.getDishRanking(startDate, endDate, limit));
    }

    @Operation(summary = "翻台率统计")
    @GetMapping("/table-turnover")
    public Result<List<TableTurnoverVO>> getTableTurnover(
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(reportService.getTableTurnover(startDate, endDate));
    }

    @Operation(summary = "导出营业额报表")
    @GetMapping("/export")
    public void exportRevenue(
            @Parameter(description = "维度：day/week/month") @RequestParam(defaultValue = "day") String dimension,
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpServletResponse response) {
        reportService.exportRevenue(dimension, startDate, endDate, response);
    }
}
