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
    private static final String WECHAT_ACCESS_TOKEN_KEY = "wechat:miniapp:access-token";
    private static final String QR_TASK_KEY_PREFIX = "table:qrcode:task:";
    private static final long QR_TASK_EXPIRE_SECONDS = 24 * 60 * 60L;

    /** 桌台状态常量 */
    private static final int STATUS_FREE = 0;
    private static final int STATUS_OCCUPIED = 1;
    private static final int STATUS_PAID = 2;
    private static final int STATUS_TO_CLEAN = 3;

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
        doUpdateStatus(table, STATUS_OCCUPIED);
        log.info("开台成功: id={}, code={}", id, table.getCode());
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
        // 原桌 → 空闲，目标桌 → 占用
        doUpdateStatus(fromTable, STATUS_FREE);
        doUpdateStatus(toTable, STATUS_OCCUPIED);
        log.info("换桌成功: from={} → to={}", fromTable.getCode(), toTable.getCode());
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
        LambdaUpdateWrapper<DiningTable> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(DiningTable::getId, table.getId())
                .set(DiningTable::getStatus, newStatus);
        update(wrapper);

        // 缓存桌台状态到 Redis
        redisUtils.set(TABLE_STATUS_KEY_PREFIX + table.getId(), newStatus);

        // 通过 WebSocket 广播 TABLE_STATUS 事件
        Map<String, Object> tableStatusData = new HashMap<>();
        tableStatusData.put("tableId", table.getId());
        tableStatusData.put("tableCode", table.getCode());
        tableStatusData.put("tableName", table.getName());
        tableStatusData.put("oldStatus", table.getStatus());
        tableStatusData.put("newStatus", newStatus);
        wsService.broadcast(WsEventType.TABLE_STATUS, "/topic/table-status", tableStatusData);
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
        DiningTableVO vo = BeanUtil.copyProperties(table, DiningTableVO.class);
        vo.setQrCodeUrl(minioStorageService.resolveAccessUrl(table.getQrCodeUrl()));
        return vo;
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
