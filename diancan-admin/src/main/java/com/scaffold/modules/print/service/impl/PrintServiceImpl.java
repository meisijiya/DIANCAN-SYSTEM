package com.scaffold.modules.print.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.scaffold.common.enums.WsEventType;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.dish.entity.Dish;
import com.scaffold.modules.dish.service.DishService;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.entity.OrderItem;
import com.scaffold.modules.order.mapper.OrderItemMapper;
import com.scaffold.modules.order.mapper.OrderMapper;
import com.scaffold.modules.print.dto.CategoryMappingDTO;
import com.scaffold.modules.print.dto.CategoryMappingItemDTO;
import com.scaffold.modules.print.dto.PrinterCreateDTO;
import com.scaffold.modules.print.dto.PrinterUpdateDTO;
import com.scaffold.modules.print.entity.Printer;
import com.scaffold.modules.print.entity.PrinterCategoryMapping;
import com.scaffold.modules.print.mapper.PrinterCategoryMappingMapper;
import com.scaffold.modules.print.mapper.PrinterMapper;
import com.scaffold.modules.print.service.PrintService;
import com.scaffold.modules.print.vo.PrinterVO;
import com.scaffold.framework.websocket.WsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 打印服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrintServiceImpl implements PrintService {

    private final PrinterMapper printerMapper;
    private final PrinterCategoryMappingMapper mappingMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final DishService dishService;
    private final WsService wsService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TaskScheduler taskScheduler;

    private static final String PRINT_RETRY_KEY = "print:retry";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int[] RETRY_DELAYS_SECONDS = {10, 30, 60};

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public List<PrinterVO> listPrinters() {
        // 查询所有打印机
        List<Printer> printers = printerMapper.selectList(new LambdaQueryWrapper<>());

        // 查询所有映射关系
        List<PrinterCategoryMapping> allMappings = mappingMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<Long>> printerCategoryMap = allMappings.stream()
                .collect(Collectors.groupingBy(
                        PrinterCategoryMapping::getPrinterId,
                        Collectors.mapping(PrinterCategoryMapping::getCategoryId, Collectors.toList())
                ));

        return printers.stream().map(printer -> {
            PrinterVO vo = BeanUtil.copyProperties(printer, PrinterVO.class);
            vo.setCategoryIds(printerCategoryMap.getOrDefault(printer.getId(), List.of()));
            return vo;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPrinter(PrinterCreateDTO dto) {
        LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Printer::getSn, dto.getSn());
        Printer existed = printerMapper.selectOne(wrapper);
        if (existed != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "打印机序列号已存在");
        }

        Printer printer = new Printer();
        BeanUtil.copyProperties(dto, printer);
        printer.setStatus(0); // 默认离线
        printerMapper.insert(printer);
        log.info("打印机创建成功: {}", dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePrinter(PrinterUpdateDTO dto) {
        Printer existing = printerMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (dto.getSn() != null && !dto.getSn().equals(existing.getSn())) {
            LambdaQueryWrapper<Printer> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Printer::getSn, dto.getSn());
            Printer duplicated = printerMapper.selectOne(wrapper);
            if (duplicated != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "打印机序列号已存在");
            }
        }

        Printer printer = new Printer();
        BeanUtil.copyProperties(dto, printer);
        printerMapper.updateById(printer);
        log.info("打印机更新成功: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePrinter(Long id) {
        Printer existing = printerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        LambdaQueryWrapper<PrinterCategoryMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(PrinterCategoryMapping::getPrinterId, id);
        mappingMapper.delete(mappingWrapper);

        printerMapper.deleteById(id);
        log.info("打印机删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryMapping(CategoryMappingDTO dto) {
        Set<Long> printerIds = new HashSet<>();
        if (dto.getPrinterIds() != null) {
            printerIds.addAll(dto.getPrinterIds());
        }

        if (dto.getMappings() != null) {
            dto.getMappings().stream().map(CategoryMappingItemDTO::getPrinterId).forEach(printerIds::add);
        }

        for (Long printerId : printerIds) {
            LambdaQueryWrapper<PrinterCategoryMapping> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PrinterCategoryMapping::getPrinterId, printerId);
            mappingMapper.delete(deleteWrapper);
        }

        if (dto.getMappings() == null || dto.getMappings().isEmpty()) {
            log.info("打印机-分类映射清空成功，涉及打印机: {}", printerIds);
            return;
        }

        // 同一分类仅允许映射一台打印机，先清理这些分类在其他打印机上的旧映射
        Set<Long> categoryIds = dto.getMappings().stream().map(CategoryMappingItemDTO::getCategoryId).collect(Collectors.toSet());
        if (!categoryIds.isEmpty()) {
            LambdaQueryWrapper<PrinterCategoryMapping> deleteByCategoryWrapper = new LambdaQueryWrapper<>();
            deleteByCategoryWrapper.in(PrinterCategoryMapping::getCategoryId, categoryIds);
            mappingMapper.delete(deleteByCategoryWrapper);
        }

        // 插入新映射（去重）
        Set<String> dedupKeys = new HashSet<>();
        for (CategoryMappingItemDTO item : dto.getMappings()) {
            String key = item.getPrinterId() + ":" + item.getCategoryId();
            if (!dedupKeys.add(key)) {
                continue;
            }

            PrinterCategoryMapping mapping = new PrinterCategoryMapping();
            mapping.setPrinterId(item.getPrinterId());
            mapping.setCategoryId(item.getCategoryId());
            mappingMapper.insert(mapping);
        }
        log.info("打印机-分类映射更新成功，涉及打印机: {}", printerIds);
    }

    @Override
    public void printOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }

        // 查询订单项
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> orderItems = orderItemMapper.selectList(itemWrapper);
        if (orderItems.isEmpty()) {
            log.warn("订单 {} 无订单项，跳过打印", orderId);
            return;
        }

        // 查询所有映射关系（categoryId -> printerId）
        List<PrinterCategoryMapping> allMappings = mappingMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> categoryToPrinter = allMappings.stream()
                .collect(Collectors.toMap(
                        PrinterCategoryMapping::getCategoryId,
                        PrinterCategoryMapping::getPrinterId,
                        (a, b) -> a // 如果一个分类映射了多个打印机，取第一个
                ));

        // 按打印机分组订单项
        Map<Long, List<OrderItem>> printerItemsMap = new HashMap<>();
        for (OrderItem item : orderItems) {
            Dish dish = dishService.getById(item.getDishId());
            if (dish == null) {
                log.warn("菜品不存在: dishId={}", item.getDishId());
                continue;
            }
            Long printerId = categoryToPrinter.get(dish.getCategoryId());
            if (printerId == null) {
                log.warn("菜品分类 {} 未配置打印机映射，跳过打印: {}", dish.getCategoryId(), item.getDishName());
                continue;
            }
            printerItemsMap.computeIfAbsent(printerId, k -> new ArrayList<>()).add(item);
        }

        // 向每台打印机发送打印任务
        for (Map.Entry<Long, List<OrderItem>> entry : printerItemsMap.entrySet()) {
            Long printerId = entry.getKey();
            List<OrderItem> items = entry.getValue();
            sendPrintJob(orderId, printerId, items, 0);
        }
    }

    @Override
    public void reprintOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ResultCode.ORDER_NOT_FOUND);
        }
        printOrder(orderId);
        log.info("订单重新打印: orderId={}", orderId);
    }

    /**
     * 发送打印任务（stub：仅记录日志）。失败时加入重试队列。
     */
    private void sendPrintJob(Long orderId, Long printerId, List<OrderItem> items, int retryCount) {
        try {
            Printer printer = printerMapper.selectById(printerId);
            if (printer == null) {
                log.error("打印机不存在: printerId={}", printerId);
                return;
            }

            // ===== STUB: 实际打印逻辑 =====
            log.info("【打印任务】打印机: {}({}), 订单: {}, 菜品数: {}, 重试次数: {}",
                    printer.getName(), printer.getSn(), orderId, items.size(), retryCount);
            for (OrderItem item : items) {
                log.info("  - {} x{} {}", item.getDishName(), item.getQuantity(),
                        item.getRemark() != null ? "(" + item.getRemark() + ")" : "");
            }
            // ===== STUB END =====

            // 模拟打印成功（实际场景中这里会调用打印机SDK/API）
            log.info("打印任务发送成功: printerId={}, orderId={}", printerId, orderId);

        } catch (Exception e) {
            log.error("打印任务失败: printerId={}, orderId={}, retryCount={}", printerId, orderId, retryCount, e);
            handlePrintFailure(orderId, printerId, items, retryCount);
        }
    }

    /**
     * 处理打印失败：加入重试队列或发送告警
     */
    private void handlePrintFailure(Long orderId, Long printerId, List<OrderItem> items, int retryCount) {
        if (retryCount >= MAX_RETRY_COUNT) {
            // 超过最大重试次数，发送 WebSocket 告警
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("orderId", orderId);
            alertData.put("printerId", printerId);
            alertData.put("message", "打印任务最终失败，已重试" + MAX_RETRY_COUNT + "次");
            wsService.broadcast(WsEventType.NEW_ORDER, "/topic/service", alertData);
            log.error("打印任务最终失败: orderId={}, printerId={}, 已重试{}次", orderId, printerId, MAX_RETRY_COUNT);
            return;
        }

        // 构建重试任务数据并推入 Redis 队列
        try {
            List<Long> itemIds = items.stream().map(OrderItem::getId).toList();
            Map<String, Object> retryTask = new HashMap<>();
            retryTask.put("orderId", orderId);
            retryTask.put("printerId", printerId);
            retryTask.put("itemIds", itemIds);
            retryTask.put("retryCount", retryCount + 1);

            String taskJson = OBJECT_MAPPER.writeValueAsString(retryTask);
            redisTemplate.opsForList().rightPush(PRINT_RETRY_KEY, taskJson);

            // 调度延迟重试
            int delaySeconds = RETRY_DELAYS_SECONDS[retryCount];
            taskScheduler.schedule(() -> executeRetry(taskJson), Instant.now().plusSeconds(delaySeconds));

            log.info("打印任务加入重试队列: orderId={}, printerId={}, 第{}次重试, {}秒后执行",
                    orderId, printerId, retryCount + 1, delaySeconds);
        } catch (JsonProcessingException e) {
            log.error("序列化重试任务失败", e);
        }
    }

    /**
     * 执行重试打印任务
     */
    @SuppressWarnings("unchecked")
    private void executeRetry(String taskJson) {
        try {
            Map<String, Object> retryTask = OBJECT_MAPPER.readValue(taskJson, Map.class);
            Long orderId = ((Number) retryTask.get("orderId")).longValue();
            Long printerId = ((Number) retryTask.get("printerId")).longValue();
            int retryCount = (int) retryTask.get("retryCount");
            List<Integer> itemIdInts = (List<Integer>) retryTask.get("itemIds");
            List<Long> itemIds = itemIdInts.stream().map(Number::longValue).toList();

            // 从重试队列中移除
            redisTemplate.opsForList().remove(PRINT_RETRY_KEY, 1, taskJson);

            // 重新查询订单项
            List<OrderItem> items = itemIds.stream()
                    .map(orderItemMapper::selectById)
                    .filter(Objects::nonNull)
                    .toList();

            if (items.isEmpty()) {
                log.warn("重试打印任务时订单项已不存在: orderId={}", orderId);
                return;
            }

            sendPrintJob(orderId, printerId, items, retryCount);
        } catch (Exception e) {
            log.error("执行重试打印任务失败", e);
        }
    }
}
