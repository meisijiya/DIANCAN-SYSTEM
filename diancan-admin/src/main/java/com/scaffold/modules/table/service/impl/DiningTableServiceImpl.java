package com.scaffold.modules.table.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.enums.WsEventType;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.ResultCode;
import com.scaffold.framework.redis.RedisUtils;
import com.scaffold.framework.websocket.WsService;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.mapper.OrderMapper;
import com.scaffold.modules.system.service.MinioStorageService;
import com.scaffold.modules.system.vo.FileUploadVO;
import com.scaffold.modules.table.dto.TableCreateDTO;
import com.scaffold.modules.table.dto.TableUpdateDTO;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.entity.TableArea;
import com.scaffold.modules.table.mapper.DiningTableMapper;
import com.scaffold.modules.table.mapper.TableAreaMapper;
import com.scaffold.modules.table.service.DiningTableService;
import com.scaffold.modules.table.service.TableQrCodeTaskService;
import com.scaffold.modules.table.vo.DiningTableVO;
import com.scaffold.modules.table.vo.QrCodeTaskVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 桌台服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiningTableServiceImpl extends ServiceImpl<DiningTableMapper, DiningTable> implements DiningTableService {

    private final RedisUtils redisUtils;
    private final WsService wsService;
    private final MinioStorageService minioStorageService;
    private final TableAreaMapper tableAreaMapper;
    private final OrderMapper orderMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectProvider<TableQrCodeTaskService> tableQrCodeTaskServiceProvider;

    @Value("${wechat.miniapp.enabled:false}")
    private boolean wechatMiniAppEnabled;

    @Value("${wechat.miniapp.app-id:}")
    private String wechatMiniAppAppId;

    @Value("${wechat.miniapp.app-secret:}")
    private String wechatMiniAppAppSecret;

    @Value("${wechat.miniapp.page:pages/index/index}")
    private String wechatMiniAppPage;

    @Value("${wechat.miniapp.env-version:release}")
    private String wechatMiniAppEnvVersion;

    @Value("${wechat.miniapp.width:430}")
    private int wechatMiniAppWidth;

    /** 桌台状态 Redis key 前缀 */
    private static final String TABLE_STATUS_KEY_PREFIX = "table:status:";
    private static final String TABLE_USER_BINDING_KEY_PREFIX = "table:user-binding:";
    private static final String TABLE_SESSION_MEMBERS_KEY_PREFIX = "table:session-members:";
    private static final String WECHAT_ACCESS_TOKEN_KEY = "wechat:miniapp:access-token";
    private static final String QR_TASK_KEY_PREFIX = "table:qrcode:task:";
    private static final long QR_TASK_EXPIRE_SECONDS = 24 * 60 * 60L;

    /** 桌台状态常量 */
    private static final int STATUS_FREE = 0;
    private static final int STATUS_OCCUPIED = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_TO_CLEAN = 3;
    private static final int ORDER_STATUS_PENDING = 0;
    private static final int ORDER_STATUS_PAID = 1;

    @Override
    public List<DiningTableVO> listAll() {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(DiningTable::getCode);
        List<DiningTable> tables = list(wrapper);
        return tables.stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public DiningTableVO getByCode(String code) {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiningTable::getCode, code);
        DiningTable table = getOne(wrapper);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return toVO(table);
    }

    /**
     * 预创建当前桌次编码
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 为桌台即将开始的一轮点单生成稳定桌次编码，避免同桌不同批客人串单。
     * @param id 桌台ID
     * @return 当前桌次编码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String prepareCurrentSessionCode(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ensureCurrentSessionCode(table, true);
    }

    /**
     * 获取当前活跃桌次编码
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 仅在桌台处于占用、已结账、待清洁时返回当前桌次，缺失则即时补齐。
     * @param id 桌台ID
     * @return 当前活跃桌次编码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String getActiveSessionCode(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ensureCurrentSessionCode(table, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openTable(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 状态机：空闲(0) → 占用(1)
        if (table.getStatus() != STATUS_FREE) {
            throw new BusinessException(ResultCode.TABLE_NOT_AVAILABLE);
        }
        // 开台前先准备桌次，保证这一轮订单、购物车都能挂到同一批次。
        ensureCurrentSessionCode(table, true);
        doUpdateStatus(table, STATUS_OCCUPIED);
        log.info("开台成功: id={}, code={}", id, table.getCode());
    }

    /**
     * 管理端结台
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 以当前桌次为边界检查所有订单，全部结清后才将桌台推进到待清洁状态。
     * @param id 桌台ID
     * @return true 表示结台完成，false 表示当前桌次仍有待支付订单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkoutTableIfSettled(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Integer status = table.getStatus();
        if (status == null || status == STATUS_FREE) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR, "当前桌台未开台，无法结台");
        }
        if (status == STATUS_TO_CLEAN) {
            return true;
        }

        String sessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        if (StrUtil.isBlank(sessionCode)) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR, "当前桌台桌次异常，请刷新后重试");
        }

        // 结台检查覆盖同一桌次下所有顾客创建的订单，不能只判断本次收款的单笔订单。
        Long pendingOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getTableId, id)
                .eq(Order::getTableSessionCode, sessionCode)
                .eq(Order::getStatus, ORDER_STATUS_PENDING)
                .eq(Order::getDeleted, 0));
        if (pendingOrderCount != null && pendingOrderCount > 0) {
            return false;
        }

        if (status == STATUS_OCCUPIED) {
            doUpdateStatus(table, STATUS_PAID);
            table.setStatus(STATUS_PAID);
        }
        if (table.getStatus() == STATUS_PAID) {
            doUpdateStatus(table, STATUS_TO_CLEAN);
            log.info("管理端结台成功: id={}, code={}, sessionCode={}", id, table.getCode(), sessionCode);
            return true;
        }

        throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
    }

    /**
     * 绑定当前顾客到指定桌台
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 维护顾客与当前桌次的一对一关系；顾客切到新桌时，仅变更绑定关系，不迁移旧订单和购物车。
     * @param id 桌台ID
     * @param openid 当前顾客openid
     * @return 绑定后的桌台信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DiningTableVO bindCurrentUser(Long id, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录后再绑定桌台");
        }

        DiningTable targetTable = getById(id);
        if (targetTable == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Integer targetStatus = targetTable.getStatus();
        if (targetStatus == null) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
        }

        if (targetStatus == STATUS_FREE) {
            // 空闲桌首次绑定时直接开新桌次，保证后续点单、购物车都落到同一批次。
            ensureCurrentSessionCode(targetTable, true);
            doUpdateStatus(targetTable, STATUS_OCCUPIED);
        } else if (targetStatus != STATUS_OCCUPIED) {
            throw new BusinessException(ResultCode.TABLE_NOT_AVAILABLE, "当前桌台暂不可点单，请联系服务员处理");
        }

        String targetSessionCode = ensureCurrentSessionCode(targetTable, false);
        if (StrUtil.isBlank(targetSessionCode)) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR, "目标桌当前桌次异常，请刷新后重试");
        }

        TableBindingRef currentBinding = readUserBinding(openid);
        if (currentBinding != null && currentBinding.matches(id, targetSessionCode)) {
            // 绑定已存在时补齐成员集合，兼容 Redis 过期或补偿场景。
            saveUserBinding(openid, id, targetSessionCode);
            addUserToSession(openid, id, targetSessionCode);
            DiningTable latestTable = getById(id);
            return latestTable == null ? toVO(targetTable) : toVO(latestTable);
        }

        if (currentBinding != null) {
            removeUserFromSession(openid, currentBinding);
        }

        saveUserBinding(openid, id, targetSessionCode);
        addUserToSession(openid, id, targetSessionCode);
        log.info("顾客绑定桌台成功: openid={}, tableCode={}, sessionCode={}", openid, targetTable.getCode(), targetSessionCode);

        DiningTable latestTable = getById(id);
        return latestTable == null ? toVO(targetTable) : toVO(latestTable);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTable(Long fromId, Long toId) {
        DiningTable fromTable = getById(fromId);
        DiningTable toTable = getById(toId);
        if (fromTable == null || toTable == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 原桌必须是占用状态
        if (fromTable.getStatus() != STATUS_OCCUPIED) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
        }
        // 目标桌必须是空闲状态
        if (toTable.getStatus() != STATUS_FREE) {
            throw new BusinessException(ResultCode.TABLE_CHANGE_FAILED);
        }

        String activeSessionCode = ensureCurrentSessionCode(fromTable, false);
        if (!StringUtils.hasText(activeSessionCode)) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR, "原桌当前桌次异常，请刷新后重试");
        }

        // 先迁移活动订单，再切换桌态，确保后续查单立即命中新桌。
        int movedOrderCount = migrateActiveSessionOrders(fromTable, toTable, activeSessionCode);

        // 目标桌沿用原桌桌次，保证同一批客人的订单与购物车继续归属于同一桌次。
        toTable.setCurrentSessionCode(activeSessionCode);
        int movedBindingCount = migrateSessionBindings(fromId, toId, activeSessionCode);
        doUpdateStatus(fromTable, STATUS_FREE);
        doUpdateStatus(toTable, STATUS_OCCUPIED);

        int movedCartCount = migrateActiveSessionCarts(fromId, toId, activeSessionCode);
        log.info("换桌成功: from={} → to={}, sessionCode={}, movedOrders={}, movedCarts={}, movedBindings={}",
                fromTable.getCode(), toTable.getCode(), activeSessionCode, movedOrderCount, movedCartCount, movedBindingCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markClean(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        // 状态机：待清洁(3) → 空闲(0)
        if (table.getStatus() != STATUS_TO_CLEAN) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
        }
        doUpdateStatus(table, STATUS_FREE);
        log.info("标记清洁成功: id={}, code={}", id, table.getCode());
    }

    /**
     * 释放桌台
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 管理端可释放未产生订单的占用桌，以及已结账或待清洁桌台；已有订单的占用桌仍禁止释放。
     * @param id 桌台ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseTable(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Integer currentStatus = table.getStatus();
        if (currentStatus == null || currentStatus == STATUS_FREE) {
            log.info("桌台已为空闲状态，无需释放: id={}, code={}", id, table.getCode());
            return;
        }

        if (currentStatus == STATUS_OCCUPIED) {
            long currentSessionOrderCount = countCurrentSessionOrders(table);
            if (currentSessionOrderCount > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "当前桌次已有订单，请先完成结账后再释放桌台");
            }

            // 仅扫码绑定但没有提交订单的空桌次可以直接收口，同时清理桌次和顾客绑定。
            String releasedSessionCode = table.getCurrentSessionCode();
            doUpdateStatus(table, STATUS_FREE);
            log.info("释放占用空桌成功: id={}, code={}, sessionCode={}",
                    id, table.getCode(), releasedSessionCode);
            return;
        }

        // 已结账桌台先补齐待清洁流转，再统一回到空闲，避免状态跳变过于突兀。
        if (currentStatus == STATUS_PAID) {
            doUpdateStatus(table, STATUS_TO_CLEAN);
            table.setStatus(STATUS_TO_CLEAN);
        }

        if (table.getStatus() == STATUS_TO_CLEAN) {
            doUpdateStatus(table, STATUS_FREE);
            log.info("释放桌台成功: id={}, code={}", id, table.getCode());
            return;
        }

        throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
    }

    /**
     * 统计当前桌次订单数量
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 优先按桌次编码统计全部未删除订单；桌次缺失时仅检查该桌台仍处于活动状态的订单。
     * @param table 桌台实体
     * @return 当前桌次订单数量
     */
    private long countCurrentSessionOrders(DiningTable table) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getTableId, table.getId())
                .eq(Order::getDeleted, 0);

        String sessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        if (StrUtil.isNotBlank(sessionCode)) {
            wrapper.eq(Order::getTableSessionCode, sessionCode);
        } else {
            // 兼容历史异常数据：没有桌次编码时，至少不能释放仍有待支付或已支付订单的桌台。
            wrapper.in(Order::getStatus, ORDER_STATUS_PENDING, ORDER_STATUS_PAID);
        }

        Long orderCount = orderMapper.selectCount(wrapper);
        return orderCount == null ? 0L : orderCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTableStatus(Long id, Integer status) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        validateStateTransition(table.getStatus(), status);
        doUpdateStatus(table, status);
        log.info("桌台状态更新: id={}, {} → {}", id, table.getStatus(), status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTable(TableCreateDTO dto) {
        // 校验编号唯一
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiningTable::getCode, dto.getCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "桌台编号已存在");
        }

        DiningTable table = new DiningTable();
        BeanUtil.copyProperties(dto, table);
        applyAreaBinding(table, dto.getAreaId(), dto.getAreaName());
        table.setStatus(STATUS_FREE);
        table.setCurrentSessionCode(null);
        save(table);

        // 缓存桌台状态到 Redis
        redisUtils.set(TABLE_STATUS_KEY_PREFIX + table.getId(), STATUS_FREE);
        log.info("桌台创建成功: code={}", dto.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTable(TableUpdateDTO dto) {
        DiningTable existTable = getById(dto.getId());
        if (existTable == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 如果修改了编号，校验唯一性
        if (dto.getCode() != null && !dto.getCode().equals(existTable.getCode())) {
            LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DiningTable::getCode, dto.getCode());
            if (count(wrapper) > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "桌台编号已存在");
            }
        }

        DiningTable table = new DiningTable();
        BeanUtil.copyProperties(dto, table);
        applyAreaBinding(table, dto.getAreaId(), dto.getAreaName());
        updateById(table);
        log.info("桌台更新成功: id={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long id) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        String sessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        if (StrUtil.isNotBlank(sessionCode)) {
            clearSessionBindings(id, sessionCode);
        }
        removeById(id);
        // 清除 Redis 缓存
        redisUtils.delete(TABLE_STATUS_KEY_PREFIX + id);
        log.info("桌台删除成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateAllQrCodes() {
        List<DiningTable> tables = list();
        if (tables.isEmpty()) {
            return 0;
        }

        int generated = 0;
        for (DiningTable table : tables) {
            if (!StringUtils.hasText(table.getCode())) {
                continue;
            }

            try {
                byte[] pngBytes = generateTableQrCodeImage(table.getCode());
                FileUploadVO upload = minioStorageService.uploadImageBytes(pngBytes, "table/qrcode", table.getCode(), "image/png");
                DiningTable update = new DiningTable();
                update.setId(table.getId());
                update.setQrCodeUrl(upload.getObjectName());
                updateById(update);
                generated++;
            } catch (BusinessException e) {
                log.warn("生成桌台二维码失败: tableId={}, code={}, msg={}", table.getId(), table.getCode(), e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("生成桌台二维码失败: tableId={}, code={}", table.getId(), table.getCode(), e);
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "桌台[" + table.getCode() + "]二维码生成失败: " + e.getMessage());
            }
        }

        log.info("批量生成桌台二维码完成: total={}, generated={}", tables.size(), generated);
        return generated;
    }

    @Override
    public QrCodeTaskVO submitGenerateAllQrCodesTask() {
        QrCodeTaskVO task = createTask("GENERATE_ALL", "已提交批量生成任务");
        tableQrCodeTaskServiceProvider.getObject().generateAllQrCodesAsync(task.getTaskId());
        return task;
    }

    @Override
    public void downloadQrCode(Long id, HttpServletResponse response) {
        DiningTable table = getById(id);
        if (table == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (!StringUtils.hasText(table.getQrCodeUrl())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前桌台未配置二维码地址");
        }

        byte[] imageBytes = resolveQrCodeBytes(table.getQrCodeUrl());

        String rawFileName = String.format("%s-%s-qrcode.png", table.getCode(), table.getName());
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        try {
            response.setContentType("image/png");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载桌台二维码失败: tableId={}, code={}", id, table.getCode(), e);
            throw new RuntimeException("下载桌台二维码失败", e);
        }
    }

    @Override
    public QrCodeTaskVO submitDownloadAllQrCodesTask() {
        QrCodeTaskVO task = createTask("DOWNLOAD_ALL", "已提交二维码打包任务");
        tableQrCodeTaskServiceProvider.getObject().packageAllQrCodesAsync(task.getTaskId());
        return task;
    }

    @Override
    public QrCodeTaskVO getQrCodeTask(String taskId) {
        QrCodeTaskVO task = readTask(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "二维码任务不存在或已过期");
        }
        return task;
    }

    @Override
    public void downloadQrCodeTaskFile(String taskId, HttpServletResponse response) {
        QrCodeTaskVO task = getQrCodeTask(taskId);
        if (!"SUCCESS".equals(task.getStatus()) || !Boolean.TRUE.equals(task.getDownloadable())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前任务尚未生成可下载文件");
        }

        if (!StringUtils.hasText(task.getFilePath())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务文件不存在");
        }

        Path filePath = Path.of(task.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务文件不存在或已被清理");
        }

        String rawFileName = StringUtils.hasText(task.getFileName()) ? task.getFileName() : taskId + ".zip";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            response.setContentType("application/zip");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载二维码打包文件失败: taskId={}", taskId, e);
            throw new RuntimeException("下载二维码打包文件失败", e);
        }
    }

    /**
     * 执行状态更新：更新数据库 + Redis 缓存 + WebSocket 广播
     */
    private void doUpdateStatus(DiningTable table, int newStatus) {
        Integer oldStatus = table.getStatus();
        String previousSessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        LambdaUpdateWrapper<DiningTable> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DiningTable::getId, table.getId())
                .set(DiningTable::getStatus, newStatus);

        if (newStatus == STATUS_FREE) {
            // 桌台回到空闲，说明这一轮点单已经彻底结束，清空当前桌次。
            wrapper.set(DiningTable::getCurrentSessionCode, null);
            table.setCurrentSessionCode(null);
        } else if (StrUtil.isNotBlank(table.getCurrentSessionCode())) {
            // 非空闲状态统一带上当前桌次，确保后续查询能锁定到同一轮。
            wrapper.set(DiningTable::getCurrentSessionCode, table.getCurrentSessionCode());
        }
        update(wrapper);

        if (newStatus == STATUS_FREE && StrUtil.isNotBlank(previousSessionCode)) {
            clearSessionBindings(table.getId(), previousSessionCode);
        }

        // 缓存桌台状态到 Redis
        redisUtils.set(TABLE_STATUS_KEY_PREFIX + table.getId(), newStatus);

        // 通过 WebSocket 广播 TABLE_STATUS 事件
        Map<String, Object> tableStatusData = new HashMap<>();
        tableStatusData.put("tableId", table.getId());
        tableStatusData.put("tableCode", table.getCode());
        tableStatusData.put("tableName", table.getName());
        tableStatusData.put("oldStatus", oldStatus);
        tableStatusData.put("newStatus", newStatus);
        wsService.broadcast(WsEventType.TABLE_STATUS, "/topic/table-status", tableStatusData);

        table.setStatus(newStatus);
    }

    /**
     * 验证状态转换是否合法
     * 合法转换：空闲(0)→占用(1), 占用(1)→已结账(2), 已结账(2)→待清洁(3), 待清洁(3)→空闲(0), 占用(1)→空闲(0)
     */
    private void validateStateTransition(int currentStatus, int targetStatus) {
        boolean valid = switch (currentStatus) {
            case STATUS_FREE -> targetStatus == STATUS_OCCUPIED;
            case STATUS_OCCUPIED -> targetStatus == STATUS_PAID || targetStatus == STATUS_FREE;
            case STATUS_PAID -> targetStatus == STATUS_TO_CLEAN;
            case STATUS_TO_CLEAN -> targetStatus == STATUS_FREE;
            default -> false;
        };
        if (!valid) {
            throw new BusinessException(ResultCode.TABLE_STATUS_ERROR);
        }
    }

    /**
     * 将 DiningTable 实体转换为 DiningTableVO
     */
    private DiningTableVO toVO(DiningTable table) {
        ensureCurrentSessionCode(table, false);
        DiningTableVO vo = BeanUtil.copyProperties(table, DiningTableVO.class);
        vo.setQrCodeUrl(minioStorageService.resolveAccessUrl(table.getQrCodeUrl()));
        return vo;
    }

    /**
     * 确保桌台具备当前桌次编码
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 仅在需要时补齐桌次编码；空闲桌默认不创建，除非显式准备新一轮点单。
     * @param table 桌台实体
     * @param createWhenFree 空闲桌是否允许创建桌次
     * @return 当前桌次编码
     */
    private String ensureCurrentSessionCode(DiningTable table, boolean createWhenFree) {
        if (table == null) {
            return null;
        }

        String sessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        if (StrUtil.isNotBlank(sessionCode)) {
            table.setCurrentSessionCode(sessionCode);
            return sessionCode;
        }

        Integer status = table.getStatus();
        if (!createWhenFree && (status == null || status == STATUS_FREE)) {
            return null;
        }

        String nextSessionCode = generateSessionCode();
        LambdaUpdateWrapper<DiningTable> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DiningTable::getId, table.getId())
                .and(w -> w.isNull(DiningTable::getCurrentSessionCode).or().eq(DiningTable::getCurrentSessionCode, ""))
                .set(DiningTable::getCurrentSessionCode, nextSessionCode);
        boolean updated = update(wrapper);
        if (updated) {
            table.setCurrentSessionCode(nextSessionCode);
            return nextSessionCode;
        }

        DiningTable latestTable = getById(table.getId());
        String latestSessionCode = latestTable == null ? null : StrUtil.trimToNull(latestTable.getCurrentSessionCode());
        table.setCurrentSessionCode(latestSessionCode);
        return latestSessionCode;
    }

    /**
     * 生成桌次编码
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 使用短 UUID 生成桌次编码，避免同桌多轮点单共享同一标识。
     * @return 桌次编码
     */
    private String generateSessionCode() {
        return "TS" + IdUtil.fastSimpleUUID();
    }

    /**
     * 迁移当前桌次的活动订单
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 将原桌未关闭的当前桌次订单改挂到目标桌，避免换桌后旧桌继续暴露上一批客人的订单。
     * @param fromTable 原桌实体
     * @param toTable 目标桌实体
     * @param sessionCode 当前桌次编码
     * @return 迁移订单数
     */
    private int migrateActiveSessionOrders(DiningTable fromTable, DiningTable toTable, String sessionCode) {
        LambdaUpdateWrapper<Order> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Order::getTableId, fromTable.getId())
                .eq(Order::getTableSessionCode, sessionCode)
                .in(Order::getStatus, ORDER_STATUS_PENDING, ORDER_STATUS_PAID)
                .eq(Order::getDeleted, 0)
                .set(Order::getTableId, toTable.getId())
                .set(Order::getTableCode, toTable.getCode());
        return orderMapper.update(null, wrapper);
    }

    /**
     * 迁移当前桌次的购物车
     *
     * @author Henfon
     * @date 2026-07-10
     * @description 批量搬运同一桌次下所有顾客 Redis 购物车，保证换桌后未提交菜品也跟随迁移。
     * @param fromTableId 原桌台ID
     * @param toTableId 目标桌台ID
     * @param sessionCode 当前桌次编码
     * @return 迁移购物车数量
     */
    private int migrateActiveSessionCarts(Long fromTableId, Long toTableId, String sessionCode) {
        String sourcePattern = "cart:*:" + fromTableId + ":" + sessionCode;
        Set<String> sourceKeys = redisTemplate.keys(sourcePattern);
        if (sourceKeys == null || sourceKeys.isEmpty()) {
            return 0;
        }

        int movedCount = 0;
        String sourceSuffix = ":" + fromTableId + ":" + sessionCode;
        String targetSuffix = ":" + toTableId + ":" + sessionCode;
        for (String sourceKey : sourceKeys) {
            if (!StringUtils.hasText(sourceKey) || !sourceKey.endsWith(sourceSuffix)) {
                continue;
            }

            String targetKey = sourceKey.substring(0, sourceKey.length() - sourceSuffix.length()) + targetSuffix;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(sourceKey);
            if (entries != null && !entries.isEmpty()) {
                // 先复制购物车内容，再删除旧 key，尽量避免中途异常导致数据丢失。
                redisTemplate.opsForHash().putAll(targetKey, entries);
            }

            Long ttlSeconds = redisTemplate.getExpire(sourceKey, TimeUnit.SECONDS);
            if (ttlSeconds != null && ttlSeconds > 0) {
                redisTemplate.expire(targetKey, ttlSeconds, TimeUnit.SECONDS);
            }
            redisTemplate.delete(sourceKey);
            movedCount++;
        }
        return movedCount;
    }

    /**
     * 迁移当前桌次的顾客绑定关系
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 显式换桌时同步迁移桌次成员集合和顾客当前绑定，避免后续解绑误伤原桌。
     * @param fromTableId 原桌台ID
     * @param toTableId 目标桌台ID
     * @param sessionCode 当前桌次编码
     * @return 迁移绑定数量
     */
    private int migrateSessionBindings(Long fromTableId, Long toTableId, String sessionCode) {
        String sourceKey = buildSessionMembersKey(fromTableId, sessionCode);
        Set<Object> members = redisUtils.sMembers(sourceKey);
        if (members == null || members.isEmpty()) {
            return 0;
        }

        int movedCount = 0;
        String targetKey = buildSessionMembersKey(toTableId, sessionCode);
        for (Object member : members) {
            String openid = StrUtil.trimToNull(member == null ? null : member.toString());
            if (StrUtil.isBlank(openid)) {
                continue;
            }

            redisUtils.sAdd(targetKey, openid);
            TableBindingRef bindingRef = readUserBinding(openid);
            if (bindingRef != null && bindingRef.matches(fromTableId, sessionCode)) {
                saveUserBinding(openid, toTableId, sessionCode);
            }
            movedCount++;
        }
        redisUtils.delete(sourceKey);
        return movedCount;
    }

    /**
     * 保存顾客当前桌次绑定
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 将 openid 当前所在桌次落到 Redis，便于后续重绑时从旧桌解绑。
     * @param openid 顾客openid
     * @param tableId 桌台ID
     * @param sessionCode 桌次编码
     */
    private void saveUserBinding(String openid, Long tableId, String sessionCode) {
        redisUtils.set(buildUserBindingKey(openid), encodeBindingValue(tableId, sessionCode));
    }

    /**
     * 读取顾客当前桌次绑定
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 从 Redis 反查顾客当前所在桌次，供换桌重绑时做旧桌解绑。
     * @param openid 顾客openid
     * @return 顾客桌次绑定
     */
    private TableBindingRef readUserBinding(String openid) {
        if (StrUtil.isBlank(openid)) {
            return null;
        }

        Object rawBinding = redisUtils.get(buildUserBindingKey(openid));
        if (!(rawBinding instanceof String bindingValue) || StrUtil.isBlank(bindingValue)) {
            return null;
        }
        return parseBindingValue(bindingValue);
    }

    /**
     * 将顾客加入桌次成员集合
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 同桌多人共享同一桌次时，通过集合记录当前仍在该桌的顾客。
     * @param openid 顾客openid
     * @param tableId 桌台ID
     * @param sessionCode 桌次编码
     */
    private void addUserToSession(String openid, Long tableId, String sessionCode) {
        redisUtils.sAdd(buildSessionMembersKey(tableId, sessionCode), openid);
    }

    /**
     * 从旧桌次移除顾客
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 顾客重绑到新桌时，从原桌次成员集合移除；若旧桌无人则自动结束旧桌当前桌次。
     * @param openid 顾客openid
     * @param bindingRef 旧桌次绑定
     */
    private void removeUserFromSession(String openid, TableBindingRef bindingRef) {
        if (bindingRef == null || StrUtil.isBlank(openid)) {
            return;
        }

        String sessionKey = buildSessionMembersKey(bindingRef.tableId, bindingRef.sessionCode);
        redisTemplate.opsForSet().remove(sessionKey, openid);
        redisUtils.delete(buildUserBindingKey(openid));

        Set<Object> remainingMembers = redisUtils.sMembers(sessionKey);
        boolean hasRemainingMembers = remainingMembers != null
                && remainingMembers.stream()
                .map(member -> StrUtil.trimToNull(member == null ? null : member.toString()))
                .anyMatch(StrUtil::isNotBlank);
        if (hasRemainingMembers) {
            return;
        }

        redisUtils.delete(sessionKey);
        closeSessionIfNoMember(bindingRef);
    }

    /**
     * 结束无成员桌次
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 仅当桌台仍挂着该桌次且处于占用中时，自动收掉当前桌次，避免下一批顾客继续看到旧单。
     * @param bindingRef 空成员桌次绑定
     */
    private void closeSessionIfNoMember(TableBindingRef bindingRef) {
        DiningTable table = getById(bindingRef.tableId);
        if (table == null) {
            return;
        }

        String currentSessionCode = StrUtil.trimToNull(table.getCurrentSessionCode());
        if (!StrUtil.equals(currentSessionCode, bindingRef.sessionCode)) {
            return;
        }

        if (!Integer.valueOf(STATUS_OCCUPIED).equals(table.getStatus())) {
            log.info("桌次成员已清空，但桌台已进入收尾流程: tableCode={}, status={}, sessionCode={}",
                    table.getCode(), table.getStatus(), bindingRef.sessionCode);
            return;
        }

        // 最后一位顾客离开后，直接结束原桌当前桌次，避免新客扫码看到上一批订单。
        doUpdateStatus(table, STATUS_FREE);
        log.info("桌台最后一位顾客离桌，自动结束当前桌次: tableCode={}, sessionCode={}",
                table.getCode(), bindingRef.sessionCode);
    }

    /**
     * 清理桌次下的全部顾客绑定
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 桌台恢复空闲时，同时回收该桌次对应的成员集合和顾客当前绑定。
     * @param tableId 桌台ID
     * @param sessionCode 桌次编码
     */
    private void clearSessionBindings(Long tableId, String sessionCode) {
        String sessionKey = buildSessionMembersKey(tableId, sessionCode);
        Set<Object> members = redisUtils.sMembers(sessionKey);
        if (members != null) {
            for (Object member : members) {
                String openid = StrUtil.trimToNull(member == null ? null : member.toString());
                if (StrUtil.isBlank(openid)) {
                    continue;
                }

                TableBindingRef bindingRef = readUserBinding(openid);
                if (bindingRef != null && bindingRef.matches(tableId, sessionCode)) {
                    redisUtils.delete(buildUserBindingKey(openid));
                }
            }
        }
        redisUtils.delete(sessionKey);
    }

    /**
     * 构建顾客绑定 Redis key
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 使用 openid 作为单顾客维度索引，记录其当前所在桌次。
     * @param openid 顾客openid
     * @return Redis key
     */
    private String buildUserBindingKey(String openid) {
        return TABLE_USER_BINDING_KEY_PREFIX + openid;
    }

    /**
     * 构建桌次成员集合 Redis key
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 通过桌台ID与桌次编码组合区分同一桌台的不同批顾客。
     * @param tableId 桌台ID
     * @param sessionCode 桌次编码
     * @return Redis key
     */
    private String buildSessionMembersKey(Long tableId, String sessionCode) {
        return TABLE_SESSION_MEMBERS_KEY_PREFIX + tableId + ":" + sessionCode;
    }

    /**
     * 编码顾客绑定值
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 采用轻量字符串格式存储桌台ID与桌次编码，减少额外序列化成本。
     * @param tableId 桌台ID
     * @param sessionCode 桌次编码
     * @return 编码后的绑定值
     */
    private String encodeBindingValue(Long tableId, String sessionCode) {
        return tableId + "#" + sessionCode;
    }

    /**
     * 解析顾客绑定值
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 将 Redis 中存储的桌台绑定字符串还原为结构化对象。
     * @param bindingValue 绑定值
     * @return 顾客桌次绑定
     */
    private TableBindingRef parseBindingValue(String bindingValue) {
        if (StrUtil.isBlank(bindingValue)) {
            return null;
        }

        String[] parts = bindingValue.split("#", 2);
        if (parts.length < 2 || StrUtil.isBlank(parts[0]) || StrUtil.isBlank(parts[1])) {
            return null;
        }

        try {
            return new TableBindingRef(Long.valueOf(parts[0]), parts[1]);
        } catch (NumberFormatException ex) {
            log.warn("解析桌次绑定失败: value={}", bindingValue);
            return null;
        }
    }

    /**
     * 应用区域绑定信息
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 优先根据区域主数据回填 areaId 与 areaName，兼容旧调用传入的 areaName。
     * @param table 桌台实体
     * @param areaId 区域ID
     * @param areaName 区域名称
     */
    private void applyAreaBinding(DiningTable table, Long areaId, String areaName) {
        if (areaId != null) {
            TableArea area = tableAreaMapper.selectById(areaId);
            if (area == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "区域不存在");
            }
            table.setAreaId(areaId);
            table.setAreaName(StrUtil.trim(area.getName()));
            return;
        }

        String normalizedAreaName = StrUtil.trim(areaName);
        if (StrUtil.isBlank(normalizedAreaName)) {
            table.setAreaId(null);
            table.setAreaName(null);
            return;
        }

        table.setAreaId(null);
        table.setAreaName(normalizedAreaName);
    }

    /**
     * 创建二维码异步任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 初始化二维码任务状态并写入 Redis。
     * @param taskType 任务类型
     * @param message 初始提示信息
     * @return 任务状态
     */
    private QrCodeTaskVO createTask(String taskType, String message) {
        QrCodeTaskVO task = new QrCodeTaskVO();
        task.setTaskId(IdUtil.fastSimpleUUID());
        task.setTaskType(taskType);
        task.setStatus("PENDING");
        task.setMessage(message);
        task.setTotal(0);
        task.setCompleted(0);
        task.setDownloadable(false);
        task.setCreateTime(LocalDateTime.now());
        saveTask(task);
        return task;
    }

    /**
     * 保存二维码任务状态
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 统一将任务状态序列化到 Redis，便于前端轮询查询。
     * @param task 任务状态
     */
    private void saveTask(QrCodeTaskVO task) {
        redisUtils.set(QR_TASK_KEY_PREFIX + task.getTaskId(), JSONUtil.toJsonStr(task), QR_TASK_EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 读取二维码任务状态
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 从 Redis 反序列化任务状态对象。
     * @param taskId 任务ID
     * @return 任务状态
     */
    public QrCodeTaskVO readTask(String taskId) {
        Object raw = redisUtils.get(QR_TASK_KEY_PREFIX + taskId);
        if (!(raw instanceof String json) || !StringUtils.hasText(json)) {
            return null;
        }
        return JSONUtil.toBean(json, QrCodeTaskVO.class);
    }

    /**
     * 更新二维码任务状态
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 按需合并并持久化二维码任务状态，供异步线程更新进度。
     * @param taskId 任务ID
     * @param status 任务状态
     * @param message 状态消息
     * @param total 总量
     * @param completed 已完成量
     * @param downloadable 是否可下载
     * @param fileName 下载文件名
     * @param filePath 下载文件路径
     */
    public void updateTask(String taskId, String status, String message, Integer total, Integer completed,
                           Boolean downloadable, String fileName, String filePath) {
        QrCodeTaskVO task = readTask(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(status);
        if (message != null) {
            task.setMessage(message);
        }
        if (total != null) {
            task.setTotal(total);
        }
        if (completed != null) {
            task.setCompleted(completed);
        }
        if (downloadable != null) {
            task.setDownloadable(downloadable);
        }
        if (fileName != null) {
            task.setFileName(fileName);
        }
        if (filePath != null) {
            task.setFilePath(filePath);
        }
        if ("SUCCESS".equals(status) || "FAILED".equals(status)) {
            task.setFinishTime(LocalDateTime.now());
        }

        saveTask(task);
    }

    /**
     * 获取全部桌台实体
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 返回按桌号排序的桌台实体，供异步任务统一处理。
     * @return 桌台列表
     */
    public List<DiningTable> listTablesForQrTask() {
        LambdaQueryWrapper<DiningTable> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(DiningTable::getCode)
                .ne(DiningTable::getCode, "");

        Map<Long, TableArea> areaMap = loadAreaMap();
        return list(wrapper).stream()
                .sorted(Comparator
                        .comparingInt((DiningTable table) -> resolveAreaSort(areaMap, table.getAreaId()))
                        .thenComparing(table -> StrUtil.blankToDefault(table.getAreaName(), "未分区"))
                        .thenComparing(table -> StrUtil.blankToDefault(table.getCode(), "")))
                .toList();
    }

    /**
     * 确保桌台二维码存在并返回图片字节
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 优先复用已存在二维码，缺失时即时生成并回写对象存储地址。
     * @param table 桌台实体
     * @return 二维码图片字节
     */
    public byte[] ensureQrCodeBytes(DiningTable table) {
        if (StringUtils.hasText(table.getQrCodeUrl())) {
            return resolveQrCodeBytes(table.getQrCodeUrl());
        }

        byte[] pngBytes = generateTableQrCodeImage(table.getCode());
        FileUploadVO upload = minioStorageService.uploadImageBytes(pngBytes, "table/qrcode", table.getCode(), "image/png");

        DiningTable update = new DiningTable();
        update.setId(table.getId());
        update.setQrCodeUrl(upload.getObjectName());
        updateById(update);
        table.setQrCodeUrl(upload.getObjectName());
        return pngBytes;
    }

    /**
     * 构建区域分组压缩包
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 将桌台二维码按区域归档到 zip 包中，方便统一下载。
     * @param tables 桌台列表
     * @param taskId 任务ID
     * @return zip 文件路径
     */
    public Path buildQrCodeZip(List<DiningTable> tables, String taskId) {
        try {
            Path tempDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "diancan-qrcode-task"));
            Path zipPath = tempDir.resolve("table-qrcodes-" + taskId + ".zip");
            Map<Long, TableArea> areaMap = loadAreaMap();

            // 先删除旧文件，避免同名任务重试时读到脏数据。
            Files.deleteIfExists(zipPath);

            try (var outputStream = Files.newOutputStream(zipPath);
                 var zipStream = new java.util.zip.ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
                int completed = 0;
                List<DiningTable> orderedTables = tables.stream()
                        .sorted(Comparator
                                .comparingInt((DiningTable table) -> resolveAreaSort(areaMap, table.getAreaId()))
                                .thenComparing(table -> StrUtil.blankToDefault(table.getAreaName(), "未分区"))
                                .thenComparing(table -> StrUtil.blankToDefault(table.getCode(), "")))
                        .toList();

                for (DiningTable table : orderedTables) {
                    byte[] qrBytes = ensureQrCodeBytes(table);
                    String areaName = sanitizePathSegment(StrUtil.blankToDefault(table.getAreaName(), "未分区"));
                    String fileName = sanitizePathSegment(table.getCode() + "-" + table.getName() + "-qrcode.png");
                    zipStream.putNextEntry(new java.util.zip.ZipEntry(areaName + "/" + fileName));
                    zipStream.write(qrBytes);
                    zipStream.closeEntry();

                    completed++;
                    // 打包阶段同步更新进度，前端可实时看到已归档数量。
                    updateTask(taskId, "RUNNING", "正在打包 " + table.getCode() + " 的二维码",
                            tables.size(), completed, false, null, null);
                }
            }

            return zipPath;
        } catch (IOException e) {
            throw new BusinessException(ResultCode.FAIL, "二维码压缩包生成失败: " + e.getMessage());
        }
    }

    /**
     * 加载区域主数据映射
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 为桌台排序、区域打包等场景提供区域排序和名称元数据。
     * @return 区域映射
     */
    private Map<Long, TableArea> loadAreaMap() {
        LambdaQueryWrapper<TableArea> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(TableArea::getSort)
                .orderByAsc(TableArea::getId);
        return tableAreaMapper.selectList(wrapper).stream()
                .collect(java.util.stream.Collectors.toMap(TableArea::getId, area -> area, (left, right) -> left));
    }

    /**
     * 解析区域排序值
     *
     * @author Henfon
     * @date 2026-07-02
     * @description 未命中区域主数据时，统一放到最后，避免影响已治理区域的稳定顺序。
     * @param areaMap 区域映射
     * @param areaId 区域ID
     * @return 排序值
     */
    private int resolveAreaSort(Map<Long, TableArea> areaMap, Long areaId) {
        if (areaId == null) {
            return Integer.MAX_VALUE;
        }
        TableArea area = areaMap.get(areaId);
        if (area == null || area.getSort() == null) {
            return Integer.MAX_VALUE;
        }
        return area.getSort();
    }

    /**
     * 顾客当前桌次绑定
     *
     * @author Henfon
     * @date 2026-07-11
     * @description 保存顾客当前绑定的桌台ID与桌次编码，便于重绑时精准解绑旧桌次。
     */
    private static final class TableBindingRef {

        private final Long tableId;
        private final String sessionCode;

        private TableBindingRef(Long tableId, String sessionCode) {
            this.tableId = tableId;
            this.sessionCode = sessionCode;
        }

        private boolean matches(Long targetTableId, String targetSessionCode) {
            return tableId != null
                    && tableId.equals(targetTableId)
                    && StrUtil.equals(sessionCode, StrUtil.trimToNull(targetSessionCode));
        }
    }

    /**
     * 生成二维码并回写地址
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 为单个桌台生成二维码图片并更新数据库中的二维码地址。
     * @param table 桌台实体
     */
    public void generateAndSaveTableQrCode(DiningTable table) {
        byte[] pngBytes = generateTableQrCodeImage(table.getCode());
        FileUploadVO upload = minioStorageService.uploadImageBytes(pngBytes, "table/qrcode", table.getCode(), "image/png");

        DiningTable update = new DiningTable();
        update.setId(table.getId());
        update.setQrCodeUrl(upload.getObjectName());
        updateById(update);
    }

    /**
     * 清洗 zip 路径片段
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 替换文件名和目录中的非法字符，避免压缩包结构异常。
     * @param value 原始值
     * @return 清洗后的值
     */
    private String sanitizePathSegment(String value) {
        String sanitized = value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (!StringUtils.hasText(sanitized)) {
            return "未命名";
        }
        return sanitized;
    }

    /**
     * 解析二维码图片字节
     * 支持 http/https URL 和 data:image/*;base64,xxx
     */
    private byte[] resolveQrCodeBytes(String qrCodeUrl) {
        String source = qrCodeUrl.trim();

        if (source.startsWith("data:image")) {
            int commaIndex = source.indexOf(',');
            if (commaIndex < 0 || commaIndex == source.length() - 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "二维码数据格式不正确");
            }
            String base64Data = source.substring(commaIndex + 1);
            return Base64.decode(base64Data);
        }

        if (source.startsWith("http://") || source.startsWith("https://")) {
            try {
                return HttpUtil.downloadBytes(source);
            } catch (HttpException e) {
                log.error("二维码地址下载失败: {}", source, e);
                throw new BusinessException(ResultCode.FAIL, "二维码图片下载失败");
            }
        }

        // MinIO 对象键：先换成可访问 URL 再下载
        String accessUrl = minioStorageService.resolveAccessUrl(source);
        if (StringUtils.hasText(accessUrl) && (accessUrl.startsWith("http://") || accessUrl.startsWith("https://"))) {
            try {
                return HttpUtil.downloadBytes(accessUrl);
            } catch (HttpException e) {
                log.error("二维码对象下载失败: key={}, url={}", source, accessUrl, e);
                throw new BusinessException(ResultCode.FAIL, "二维码图片下载失败");
            }
        }

        throw new BusinessException(ResultCode.PARAM_ERROR, "二维码地址格式不支持，请使用 http/https / data:image 或 MinIO对象键");
    }

    /**
     * 生成桌台二维码图片
     *
     * @param tableCode 桌号编码
     * @return 二维码图片字节数组
     * @author Henfon
     * @date 2026-06-26
     * @description 优先生成微信小程序桌贴码，未启用时降级为普通桌号二维码。
     */
    private byte[] generateTableQrCodeImage(String tableCode) {
        // 已配置小程序能力时，优先生成可直接拉起小程序点餐页的桌贴码。
        if (wechatMiniAppEnabled) {
            try {
                return generateWechatMiniProgramCode(tableCode);
            } catch (Exception e) {
                log.warn("生成微信小程序码失败，降级为普通二维码: code={}, msg={}", tableCode, e.getMessage());
            }
        }

        // 普通二维码内容固定为 code=桌号，小程序内扫码和后台手动识别都能继续兼容。
        String qrContent = "code=" + tableCode;
        return QrCodeUtil.generatePng(qrContent, 300, 300);
    }

    /**
     * 调用微信接口生成小程序码
     *
     * @author Henfon
     * @date 2026-07-01
     * @description 根据桌号生成可直接进入小程序点餐页的无限制小程序码。
     */
    private byte[] generateWechatMiniProgramCode(String tableCode) {
        if (!StringUtils.hasText(wechatMiniAppAppId) || !StringUtils.hasText(wechatMiniAppAppSecret)) {
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "微信小程序码配置不完整，请配置 app-id/app-secret");
        }

        String accessToken = fetchWechatAccessToken();
        String apiUrl = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        JSONObject body = new JSONObject();
        body.set("scene", "code=" + tableCode);
        body.set("page", wechatMiniAppPage);
        body.set("check_path", false);
        body.set("env_version", wechatMiniAppEnvVersion);
        body.set("width", Math.max(280, wechatMiniAppWidth));

        try (HttpResponse response = HttpRequest.post(apiUrl)
                .header("Content-Type", "application/json;charset=UTF-8")
                .body(body.toString())
                .execute()) {

            byte[] responseBytes = response.bodyBytes();
            String contentType = response.header("Content-Type");
            if (contentType == null) {
                contentType = "";
            }
            if (contentType.contains("application/json")) {
                JSONObject json = JSONUtil.parseObj(new String(responseBytes, StandardCharsets.UTF_8));
                Integer errCode = json.getInt("errcode", 0);
                String errMsg = json.getStr("errmsg", "unknown");
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "微信小程序码生成失败: " + errCode + " - " + errMsg);
            }
            return responseBytes;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信小程序码接口失败: code={}", tableCode, e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "微信小程序码生成失败: " + e.getMessage());
        }
    }

    /**
     * 获取微信 access_token（Redis 缓存）
     */
    private String fetchWechatAccessToken() {
        Object cached = redisUtils.get(WECHAT_ACCESS_TOKEN_KEY);
        if (cached instanceof String cachedToken && StringUtils.hasText(cachedToken)) {
            return cachedToken;
        }

        try (HttpResponse response = HttpRequest.get("https://api.weixin.qq.com/cgi-bin/token")
                .form("grant_type", "client_credential")
                .form("appid", wechatMiniAppAppId)
                .form("secret", wechatMiniAppAppSecret)
                .execute()) {

            JSONObject json = JSONUtil.parseObj(response.body());
            String token = json.getStr("access_token");
            Integer expiresIn = json.getInt("expires_in", 7200);
            if (!StringUtils.hasText(token)) {
                Integer errCode = json.getInt("errcode", -1);
                String errMsg = json.getStr("errmsg", "unknown");
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "获取微信access_token失败: " + errCode + " - " + errMsg);
            }

            long cacheSeconds = Math.max(300, expiresIn - 120L);
            redisUtils.set(WECHAT_ACCESS_TOKEN_KEY, token, cacheSeconds);
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信token接口失败", e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED, "获取微信access_token失败: " + e.getMessage());
        }
    }
}
