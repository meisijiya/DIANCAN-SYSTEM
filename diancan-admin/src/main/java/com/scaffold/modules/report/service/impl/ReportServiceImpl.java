package com.scaffold.modules.report.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.entity.OrderItem;
import com.scaffold.modules.order.mapper.OrderItemMapper;
import com.scaffold.modules.order.mapper.OrderMapper;
import com.scaffold.modules.report.service.ReportService;
import com.scaffold.modules.report.vo.DashboardAlertVO;
import com.scaffold.modules.report.vo.DashboardOverviewVO;
import com.scaffold.modules.report.vo.DashboardSessionMetricVO;
import com.scaffold.modules.report.vo.DashboardTableStatsVO;
import com.scaffold.modules.report.vo.DishRankingVO;
import com.scaffold.modules.report.vo.RevenueVO;
import com.scaffold.modules.report.vo.TableTurnoverVO;
import com.scaffold.modules.system.service.SysConfigService;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.mapper.DiningTableMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 报表服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private static final String DASHBOARD_LUNCH_START_KEY = "dashboard.lunch.start";
    private static final String DASHBOARD_LUNCH_END_KEY = "dashboard.lunch.end";
    private static final String DASHBOARD_DINNER_START_KEY = "dashboard.dinner.start";
    private static final String DASHBOARD_DINNER_END_KEY = "dashboard.dinner.end";
    private static final LocalTime DEFAULT_LUNCH_START = LocalTime.of(0, 0);
    private static final LocalTime DEFAULT_LUNCH_END = LocalTime.of(14, 59, 59);
    private static final LocalTime DEFAULT_DINNER_START = LocalTime.of(15, 0);
    private static final LocalTime DEFAULT_DINNER_END = LocalTime.of(23, 59, 59);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final DiningTableMapper diningTableMapper;
    private final SysConfigService sysConfigService;

    @Override
    public List<RevenueVO> getRevenue(String dimension, LocalDate startDate, LocalDate endDate) {
        // 查询时间范围内的已支付订单
        List<Order> paidOrders = queryPaidOrders(startDate, endDate);

        // 按维度分组统计
        Map<String, List<Order>> grouped = paidOrders.stream()
                .collect(Collectors.groupingBy(order -> extractDateKey(order.getCreateTime(), dimension),
                        LinkedHashMap::new, Collectors.toList()));

        List<RevenueVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            RevenueVO vo = new RevenueVO();
            vo.setDate(entry.getKey());
            vo.setOrderCount(entry.getValue().size());
            vo.setTotalRevenue(entry.getValue().stream()
                    .map(Order::getActualAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(vo);
        }

        // 按日期排序
        result.sort(Comparator.comparing(RevenueVO::getDate));
        return result;
    }

    @Override
    public List<DishRankingVO> getDishRanking(LocalDate startDate, LocalDate endDate, Integer limit) {
        // 查询时间范围内的已支付订单ID列表
        List<Order> paidOrders = queryPaidOrders(startDate, endDate);
        if (paidOrders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = paidOrders.stream().map(Order::getId).toList();

        // 查询这些订单的订单项
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getOrderId, orderIds);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);

        // 按菜品分组统计
        Map<Long, List<OrderItem>> groupedByDish = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getDishId));

        List<DishRankingVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : groupedByDish.entrySet()) {
            DishRankingVO vo = new DishRankingVO();
            vo.setDishId(entry.getKey());
            vo.setDishName(entry.getValue().get(0).getDishName());
            vo.setTotalQuantity(entry.getValue().stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum());
            vo.setTotalAmount(entry.getValue().stream()
                    .map(OrderItem::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(vo);
        }

        // 按销量降序排序
        result.sort(Comparator.comparingInt(DishRankingVO::getTotalQuantity).reversed());

        // 限制返回条数
        if (limit != null && limit > 0 && result.size() > limit) {
            return result.subList(0, limit);
        }
        return result;
    }

    @Override
    public List<TableTurnoverVO> getTableTurnover(LocalDate startDate, LocalDate endDate) {
        // 查询时间范围内的已支付订单
        List<Order> paidOrders = queryPaidOrders(startDate, endDate);

        // 查询桌台总数
        LambdaQueryWrapper<DiningTable> tableWrapper = new LambdaQueryWrapper<>();
        Long totalTables = diningTableMapper.selectCount(tableWrapper);
        int tableCount = totalTables.intValue();
        if (tableCount == 0) {
            tableCount = 1; // 避免除零
        }

        // 按日分组统计订单数
        Map<String, List<Order>> grouped = paidOrders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCreateTime().toLocalDate().toString(),
                        LinkedHashMap::new, Collectors.toList()));

        List<TableTurnoverVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : grouped.entrySet()) {
            TableTurnoverVO vo = new TableTurnoverVO();
            vo.setDate(entry.getKey());
            vo.setTotalOrders(entry.getValue().size());
            vo.setTotalTables(tableCount);
            vo.setTurnoverRate(BigDecimal.valueOf(entry.getValue().size())
                    .divide(BigDecimal.valueOf(tableCount), 2, RoundingMode.HALF_UP));
            result.add(vo);
        }

        // 按日期排序
        result.sort(Comparator.comparing(TableTurnoverVO::getDate));
        return result;
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 获取管理端首页经营概览数据，统一聚合指标、趋势、桌态与提醒
     * @return 首页经营概览
     */
    @Override
    public DashboardOverviewVO getDashboardOverview() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate trendStart = today.minusDays(6);

        // 复用现有报表能力，收敛为首页固定视角数据
        List<RevenueVO> revenueTrend = buildContinuousRevenueTrend(getRevenue("day", trendStart, today), trendStart, today);
        // 首页菜品结构更偏经营过程看板，需覆盖今日待支付与已支付的有效订单
        List<DishRankingVO> allDishRanking = getDashboardDishRanking(today);
        List<TableTurnoverVO> turnoverList = getTableTurnover(yesterday, today);
        DashboardTableStatsVO tableStats = buildDashboardTableStats();

        RevenueVO todayRevenue = findRevenueByDate(revenueTrend, today);
        RevenueVO yesterdayRevenue = findRevenueByDate(revenueTrend, yesterday);
        TableTurnoverVO todayTurnover = findTurnoverByDate(turnoverList, today);
        TableTurnoverVO yesterdayTurnover = findTurnoverByDate(turnoverList, yesterday);

        DashboardOverviewVO overview = new DashboardOverviewVO();
        overview.setTodayRevenue(getRevenueAmount(todayRevenue));
        overview.setYesterdayRevenue(getRevenueAmount(yesterdayRevenue));
        overview.setTodayOrderCount(getOrderCount(todayRevenue));
        overview.setYesterdayOrderCount(getOrderCount(yesterdayRevenue));
        overview.setAverageTicket(calculateAverageTicket(overview.getTodayRevenue(), overview.getTodayOrderCount()));
        overview.setOccupancyRate(calculateOccupancyRate(tableStats));
        overview.setTodayTableTurnover(getTurnoverRate(todayTurnover));
        overview.setYesterdayTableTurnover(getTurnoverRate(yesterdayTurnover));
        overview.setTableStats(tableStats);
        overview.setRevenueTrend(revenueTrend);
        overview.setDishRanking(allDishRanking.stream().limit(5).toList());
        overview.setAlerts(buildDashboardAlerts(overview, allDishRanking));
        overview.setSessionMetrics(buildSessionMetrics(today));
        return overview;
    }

    @Override
    public void exportRevenue(String dimension, LocalDate startDate, LocalDate endDate, HttpServletResponse response) {
        List<RevenueVO> revenueList = getRevenue(dimension, startDate, endDate);

        // 转换为 Excel 导出数据
        List<RevenueExcelData> excelData = revenueList.stream().map(vo -> {
            RevenueExcelData data = new RevenueExcelData();
            data.setDate(vo.getDate());
            data.setTotalRevenue(vo.getTotalRevenue());
            data.setOrderCount(vo.getOrderCount());
            return data;
        }).toList();

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("营业额报表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            EasyExcel.write(response.getOutputStream(), RevenueExcelData.class)
                    .sheet("营业额统计")
                    .doWrite(excelData);
        } catch (IOException e) {
            log.error("导出营业额报表失败", e);
            throw new RuntimeException("导出报表失败", e);
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 查询时间范围内的已支付订单
     */
    private List<Order> queryPaidOrders(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 1); // 已支付
        wrapper.ge(Order::getCreateTime, LocalDateTime.of(startDate, LocalTime.MIN));
        wrapper.le(Order::getCreateTime, LocalDateTime.of(endDate, LocalTime.MAX));
        return orderMapper.selectList(wrapper);
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 构建首页连续 7 日趋势，缺失日期自动补零，避免图表断点
     * @param revenueList 原始营收列表
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 连续营收趋势
     */
    private List<RevenueVO> buildContinuousRevenueTrend(List<RevenueVO> revenueList, LocalDate startDate, LocalDate endDate) {
        Map<String, RevenueVO> revenueMap = revenueList.stream()
                .collect(Collectors.toMap(RevenueVO::getDate, item -> item, (left, right) -> left, LinkedHashMap::new));

        List<RevenueVO> result = new ArrayList<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            String dateKey = cursor.toString();
            RevenueVO source = revenueMap.get(dateKey);

            RevenueVO target = new RevenueVO();
            target.setDate(dateKey);
            target.setTotalRevenue(source != null && source.getTotalRevenue() != null ? source.getTotalRevenue() : BigDecimal.ZERO);
            target.setOrderCount(source != null && source.getOrderCount() != null ? source.getOrderCount() : 0);
            result.add(target);
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 构建首页桌台汇总信息，供桌态和提醒区直接使用
     * @return 首页桌台汇总
     */
    private DashboardTableStatsVO buildDashboardTableStats() {
        List<DiningTable> tables = diningTableMapper.selectList(new LambdaQueryWrapper<>());

        DashboardTableStatsVO stats = new DashboardTableStatsVO();
        stats.setTotal(tables.size());
        // 按业务状态直接汇总，前端无需再遍历桌台数组
        stats.setFree((int) tables.stream().filter(item -> Objects.equals(item.getStatus(), 0)).count());
        stats.setOccupied((int) tables.stream().filter(item -> Objects.equals(item.getStatus(), 1)).count());
        stats.setSettled((int) tables.stream().filter(item -> Objects.equals(item.getStatus(), 2)).count());
        stats.setCleaning((int) tables.stream().filter(item -> Objects.equals(item.getStatus(), 3)).count());
        return stats;
    }

    /**
     * @author Henfon
     * @date 2026/07/04
     * @description 统计首页“今日菜品结构”，按今日有效订单聚合，避免餐后付场景在未结账前无数据
     * @param date 统计日期
     * @return 今日菜品结构
     */
    private List<DishRankingVO> getDashboardDishRanking(LocalDate date) {
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.in(Order::getStatus, 0, 1)
                .ge(Order::getCreateTime, LocalDateTime.of(date, LocalTime.MIN))
                .le(Order::getCreateTime, LocalDateTime.of(date, LocalTime.MAX));
        List<Order> activeOrders = orderMapper.selectList(orderWrapper);
        if (activeOrders.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = activeOrders.stream().map(Order::getId).toList();

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getOrderId, orderIds)
                // 退菜、赠送项不参与首页爆品结构判断，避免影响真实点单偏好
                .eq(OrderItem::getDeleted, 0)
                .eq(OrderItem::getIsGift, 0);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        if (orderItems.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<OrderItem>> groupedByDish = orderItems.stream()
                .filter(item -> item.getDishId() != null)
                .collect(Collectors.groupingBy(OrderItem::getDishId));

        List<DishRankingVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<OrderItem>> entry : groupedByDish.entrySet()) {
            DishRankingVO vo = new DishRankingVO();
            vo.setDishId(entry.getKey());
            vo.setDishName(entry.getValue().get(0).getDishName());
            vo.setTotalQuantity(entry.getValue().stream()
                    .map(OrderItem::getQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum());
            vo.setTotalAmount(entry.getValue().stream()
                    .map(OrderItem::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            result.add(vo);
        }

        // 首页默认展示最受关注的高销量菜品，按销量降序即可
        result.sort(Comparator.comparingInt(DishRankingVO::getTotalQuantity).reversed());
        return result;
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 构建今日午市/晚市表现，首页用于快速判断高峰时段质量
     * @param date 统计日期
     * @return 时段指标列表
     */
    private List<DashboardSessionMetricVO> buildSessionMetrics(LocalDate date) {
        LocalTime lunchStart = readConfigTime(DASHBOARD_LUNCH_START_KEY, DEFAULT_LUNCH_START);
        LocalTime lunchEnd = readConfigTime(DASHBOARD_LUNCH_END_KEY, DEFAULT_LUNCH_END);
        LocalTime dinnerStart = readConfigTime(DASHBOARD_DINNER_START_KEY, DEFAULT_DINNER_START);
        LocalTime dinnerEnd = readConfigTime(DASHBOARD_DINNER_END_KEY, DEFAULT_DINNER_END);

        return List.of(
                buildSessionMetric("午市", date, lunchStart, lunchEnd),
                buildSessionMetric("晚市", date, dinnerStart, dinnerEnd)
        );
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 构建单个营业时段指标，统一汇总营业额、订单数与客单价
     * @param label 时段名称
     * @param date 统计日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时段指标
     */
    private DashboardSessionMetricVO buildSessionMetric(String label, LocalDate date, LocalTime startTime, LocalTime endTime) {
        List<Order> orders = queryPaidOrdersByDateTime(LocalDateTime.of(date, startTime), LocalDateTime.of(date, endTime));

        BigDecimal revenue = orders.stream()
                .map(Order::getActualAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DashboardSessionMetricVO metric = new DashboardSessionMetricVO();
        metric.setLabel(label);
        metric.setStartTime(startTime.toString());
        metric.setEndTime(endTime.toString());
        metric.setRevenue(revenue);
        metric.setOrderCount(orders.size());
        metric.setAverageTicket(calculateAverageTicket(revenue, orders.size()));
        return metric;
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 生成首页经营提醒，避免前端重复维护提醒规则
     * @param overview 首页经营概览
     * @param allDishRanking 今日完整菜品排行
     * @return 经营提醒列表
     */
    private List<DashboardAlertVO> buildDashboardAlerts(DashboardOverviewVO overview, List<DishRankingVO> allDishRanking) {
        List<DashboardAlertVO> alerts = new ArrayList<>();

        if (overview.getTableStats().getCleaning() >= 3) {
            alerts.add(createAlert(
                    "待清洁桌台偏多",
                    String.format("当前有 %d 张桌台待清洁，可能影响下一轮接待效率。", overview.getTableStats().getCleaning()),
                    "danger",
                    "查看桌台看板",
                    "/service/table-board"
            ));
        }

        if (overview.getTableStats().getTotal() > 0
                && overview.getOccupancyRate().compareTo(BigDecimal.valueOf(35)) < 0) {
            alerts.add(createAlert(
                    "当前上座偏低",
                    String.format("桌台占用率仅 %s%%，可关注引流活动或高峰预估。", overview.getOccupancyRate().setScale(1, RoundingMode.HALF_UP).toPlainString()),
                    "warning",
                    "查看订单中心",
                    "/order/list"
            ));
        }

        if (overview.getTodayTableTurnover().compareTo(BigDecimal.ZERO) > 0
                && overview.getYesterdayTableTurnover().compareTo(BigDecimal.ZERO) > 0
                && overview.getTodayTableTurnover().compareTo(overview.getYesterdayTableTurnover().multiply(BigDecimal.valueOf(0.85))) < 0) {
            BigDecimal dropRate = calculateChangeRate(overview.getTodayTableTurnover(), overview.getYesterdayTableTurnover()).abs();
            alerts.add(createAlert(
                    "翻台效率低于昨日",
                    String.format("今日翻台率较昨日下降 %s%%，建议关注桌台流转。", dropRate.setScale(1, RoundingMode.HALF_UP).toPlainString()),
                    "warning",
                    "进入桌台看板",
                    "/service/table-board"
            ));
        }

        if (!allDishRanking.isEmpty()) {
            DishRankingVO topDish = allDishRanking.get(0);
            int totalQuantity = allDishRanking.stream()
                    .map(DishRankingVO::getTotalQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            if (totalQuantity > 0) {
                BigDecimal topShare = BigDecimal.valueOf(Optional.ofNullable(topDish.getTotalQuantity()).orElse(0))
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalQuantity), 1, RoundingMode.HALF_UP);
                if (topShare.compareTo(BigDecimal.valueOf(45)) >= 0) {
                    alerts.add(createAlert(
                            "菜品销量集中",
                            String.format("%s 占今日菜品销量 %s%%，注意备货与后厨节奏。", topDish.getDishName(), topShare.toPlainString()),
                            "neutral",
                            "查看菜品排行",
                            "/report/dish-ranking"
                    ));
                }
            }
        }

        if (alerts.isEmpty()) {
            alerts.add(createAlert(
                    "当前经营平稳",
                    "营收、桌态和翻台暂无明显异常，可以重点关注趋势变化。",
                    "neutral",
                    "查看营业额报表",
                    "/report/revenue"
            ));
        }

        return alerts.stream().limit(3).toList();
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 按日期查找营收记录，若无则返回空对象以便首页按 0 展示
     * @param revenueList 营收列表
     * @param date 目标日期
     * @return 营收记录
     */
    private RevenueVO findRevenueByDate(List<RevenueVO> revenueList, LocalDate date) {
        return revenueList.stream()
                .filter(item -> Objects.equals(item.getDate(), date.toString()))
                .findFirst()
                .orElseGet(() -> {
                    RevenueVO revenueVO = new RevenueVO();
                    revenueVO.setDate(date.toString());
                    revenueVO.setTotalRevenue(BigDecimal.ZERO);
                    revenueVO.setOrderCount(0);
                    return revenueVO;
                });
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 按日期查找翻台率记录，若无则返回空对象以便首页按 0 展示
     * @param turnoverList 翻台率列表
     * @param date 目标日期
     * @return 翻台率记录
     */
    private TableTurnoverVO findTurnoverByDate(List<TableTurnoverVO> turnoverList, LocalDate date) {
        return turnoverList.stream()
                .filter(item -> Objects.equals(item.getDate(), date.toString()))
                .findFirst()
                .orElseGet(() -> {
                    TableTurnoverVO turnoverVO = new TableTurnoverVO();
                    turnoverVO.setDate(date.toString());
                    turnoverVO.setTotalOrders(0);
                    turnoverVO.setTotalTables(0);
                    turnoverVO.setTurnoverRate(BigDecimal.ZERO);
                    return turnoverVO;
                });
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 计算今日客单价，统一保留两位小数
     * @param revenue 今日营业额
     * @param orderCount 今日订单数
     * @return 客单价
     */
    private BigDecimal calculateAverageTicket(BigDecimal revenue, Integer orderCount) {
        if (orderCount == null || orderCount <= 0) {
            return BigDecimal.ZERO;
        }
        return revenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 计算桌台占用率，统一输出百分比值
     * @param tableStats 桌台汇总
     * @return 占用率百分比
     */
    private BigDecimal calculateOccupancyRate(DashboardTableStatsVO tableStats) {
        if (tableStats.getTotal() == null || tableStats.getTotal() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Optional.ofNullable(tableStats.getOccupied()).orElse(0))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(tableStats.getTotal()), 2, RoundingMode.HALF_UP);
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 计算两个指标的变化百分比，供提醒文案复用
     * @param current 当前值
     * @param previous 对比值
     * @return 变化百分比
     */
    private BigDecimal calculateChangeRate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 从系统配置读取营业时段边界，未配置或格式异常时回退默认值
     * @param configKey 配置键
     * @param defaultTime 默认时间
     * @return 营业时段时间点
     */
    private LocalTime readConfigTime(String configKey, LocalTime defaultTime) {
        String value = sysConfigService.getConfigValue(configKey);
        if (value == null || value.isBlank()) {
            return defaultTime;
        }

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            log.warn("首页营业时段配置格式非法: key={}, value={}", configKey, value);
            return defaultTime;
        }
    }

    /**
     * @author Henfon
     * @date 2026/07/03
     * @description 创建首页提醒对象，保证提醒结构一致
     * @param title 标题
     * @param detail 内容
     * @param tone 级别
     * @param actionLabel 操作文案
     * @param actionTo 跳转路由
     * @return 首页提醒
     */
    private DashboardAlertVO createAlert(String title, String detail, String tone, String actionLabel, String actionTo) {
        DashboardAlertVO alert = new DashboardAlertVO();
        alert.setTitle(title);
        alert.setDetail(detail);
        alert.setTone(tone);
        alert.setActionLabel(actionLabel);
        alert.setActionTo(actionTo);
        return alert;
    }

    /**
     * 读取营收金额，空值按 0 处理
     */
    private BigDecimal getRevenueAmount(RevenueVO revenueVO) {
        return revenueVO != null && revenueVO.getTotalRevenue() != null ? revenueVO.getTotalRevenue() : BigDecimal.ZERO;
    }

    /**
     * 读取订单数，空值按 0 处理
     */
    private Integer getOrderCount(RevenueVO revenueVO) {
        return revenueVO != null && revenueVO.getOrderCount() != null ? revenueVO.getOrderCount() : 0;
    }

    /**
     * 读取翻台率，空值按 0 处理
     */
    private BigDecimal getTurnoverRate(TableTurnoverVO turnoverVO) {
        return turnoverVO != null && turnoverVO.getTurnoverRate() != null ? turnoverVO.getTurnoverRate() : BigDecimal.ZERO;
    }

    /**
     * 查询指定时间范围内的已支付订单
     */
    private List<Order> queryPaidOrdersByDateTime(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, 1);
        wrapper.ge(Order::getCreateTime, startTime);
        wrapper.le(Order::getCreateTime, endTime);
        return orderMapper.selectList(wrapper);
    }

    /**
     * 根据维度提取日期分组 key
     */
    private String extractDateKey(LocalDateTime dateTime, String dimension) {
        return switch (dimension) {
            case "week" -> {
                int year = dateTime.getYear();
                int week = dateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                yield year + "-W" + String.format("%02d", week);
            }
            case "month" -> dateTime.getYear() + "-" + String.format("%02d", dateTime.getMonthValue());
            default -> dateTime.toLocalDate().toString(); // day
        };
    }

    /**
     * 营业额 Excel 导出数据模型
     */
    @Data
    public static class RevenueExcelData {

        @ExcelProperty("日期")
        private String date;

        @ExcelProperty("总营业额")
        private BigDecimal totalRevenue;

        @ExcelProperty("订单数")
        private Integer orderCount;
    }
}
