package com.scaffold.modules.audit.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.audit.constant.AuditLogExportConstants;
import com.scaffold.modules.audit.dto.AuditLogExportTaskQueryDTO;
import com.scaffold.modules.audit.dto.AuditLogQueryDTO;
import com.scaffold.modules.audit.entity.AuditLogExportTask;
import com.scaffold.modules.audit.mapper.AuditLogExportTaskMapper;
import com.scaffold.modules.audit.service.AuditLogExportAsyncService;
import com.scaffold.modules.audit.service.AuditLogService;
import com.scaffold.modules.audit.vo.AuditLogExportTaskVO;
import com.scaffold.modules.audit.vo.AuditLogVO;
import com.scaffold.modules.order.entity.OrderOperationLog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 审计日志服务实现
 * <p>
 * 基于 OrderOperationLog 表查询审计日志，
 * 该表记录了所有敏感操作（退菜、换菜、赠送、打折、催单等）。
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogExportTaskMapper auditLogExportTaskMapper;
    private final AuditLogExportAsyncService auditLogExportAsyncService;
    private final AuditLogQuerySupport auditLogQuerySupport;

    /**
     * 分页查询审计日志
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 查询订单敏感操作日志并转换为页面展示所需的审计日志数据。
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AuditLogVO> listAuditLogs(int pageNum, int pageSize, AuditLogQueryDTO queryDTO) {
        List<OrderOperationLog> allLogs = auditLogQuerySupport.listAuditLogEntities(queryDTO);
        int fromIndex = Math.max(0, (pageNum - 1) * pageSize);
        int toIndex = Math.min(allLogs.size(), fromIndex + pageSize);
        List<OrderOperationLog> pageRecords = fromIndex >= allLogs.size()
                ? List.of()
                : allLogs.subList(fromIndex, toIndex);

        List<AuditLogVO> voList = pageRecords.stream()
                .map(auditLogQuerySupport::toVO)
                .toList();

        return PageResult.of(voList, (long) pageNum, (long) pageSize, (long) allLogs.size());
    }

    /**
     * 提交审计日志导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 先持久化筛选快照，再启动异步线程生成导出文件。
     * @param queryDTO 查询条件
     * @return 导出任务
     */
    @Override
    public AuditLogExportTaskVO submitExportTask(AuditLogQueryDTO queryDTO) {
        AuditLogExportTask task = new AuditLogExportTask();
        if (queryDTO != null) {
            task.setStartDate(queryDTO.getStartDate());
            task.setEndDate(queryDTO.getEndDate());
            task.setOperatorName(queryDTO.getOperatorName());
            task.setOperationType(queryDTO.getOperationType());
        }
        task.setTaskStatus(AuditLogExportConstants.TASK_PENDING);
        task.setTotalCount(0);
        task.setExportedCount(0);
        auditLogExportTaskMapper.insert(task);

        // 任务落库后再异步执行，避免前端轮询到不存在的任务。
        auditLogExportAsyncService.exportAuditLogsAsync(task.getId());
        return BeanUtil.copyProperties(task, AuditLogExportTaskVO.class);
    }

    /**
     * 分页查询导出任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 仅返回当前登录后台用户自己创建的审计日志导出任务。
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<AuditLogExportTaskVO> pageExportTasks(AuditLogExportTaskQueryDTO queryDTO) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        Page<AuditLogExportTask> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<AuditLogExportTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLogExportTask::getCreateBy, currentUserId)
                .eq(queryDTO.getTaskStatus() != null, AuditLogExportTask::getTaskStatus, queryDTO.getTaskStatus())
                .orderByDesc(AuditLogExportTask::getCreateTime);
        Page<AuditLogExportTask> result = auditLogExportTaskMapper.selectPage(page, wrapper);
        List<AuditLogExportTaskVO> records = BeanUtil.copyToList(result.getRecords(), AuditLogExportTaskVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 下载导出任务文件
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 校验任务归属和导出结果后输出 Excel 文件流。
     * @param taskId 任务ID
     * @param response HTTP 响应
     */
    @Override
    public void downloadExportTaskFile(Long taskId, HttpServletResponse response) {
        AuditLogExportTask task = requireOwnedTask(taskId);
        if (!Objects.equals(task.getTaskStatus(), AuditLogExportConstants.TASK_SUCCESS)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前任务尚未生成可下载文件");
        }
        if (!StringUtils.hasText(task.getFilePath())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务文件不存在");
        }

        Path filePath = Path.of(task.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务文件不存在或已被清理");
        }

        String rawFileName = StringUtils.hasText(task.getFileName()) ? task.getFileName() : "审计日志.xlsx";
        String encodedFileName = URLEncoder.encode(rawFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName);
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("下载审计日志导出文件失败: taskId={}", taskId, e);
            throw new RuntimeException("下载审计日志导出文件失败", e);
        }
    }

    /**
     * 校验并获取当前用户的任务
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 只允许任务创建人查看和下载自己的导出文件。
     * @param taskId 任务ID
     * @return 任务实体
     */
    private AuditLogExportTask requireOwnedTask(Long taskId) {
        AuditLogExportTask task = auditLogExportTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "导出任务不存在");
        }
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(task.getCreateBy(), currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问该导出任务");
        }
        return task;
    }
}
