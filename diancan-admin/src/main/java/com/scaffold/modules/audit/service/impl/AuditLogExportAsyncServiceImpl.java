package com.scaffold.modules.audit.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.audit.constant.AuditLogExportConstants;
import com.scaffold.modules.audit.dto.AuditLogQueryDTO;
import com.scaffold.modules.audit.entity.AuditLogExportTask;
import com.scaffold.modules.audit.mapper.AuditLogExportTaskMapper;
import com.scaffold.modules.audit.service.AuditLogExportAsyncService;
import com.scaffold.modules.audit.vo.AuditLogVO;
import com.scaffold.modules.order.entity.OrderOperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 审计日志异步导出服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogExportAsyncServiceImpl implements AuditLogExportAsyncService {

    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AuditLogExportTaskMapper auditLogExportTaskMapper;
    private final AuditLogQuerySupport auditLogQuerySupport;

    /**
     * 异步执行审计日志导出
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 读取任务筛选条件后生成 Excel 文件，并将结果路径保存到任务表。
     * @param taskId 任务ID
     */
    @Override
    @Async
    public void exportAuditLogsAsync(Long taskId) {
        AuditLogExportTask task = auditLogExportTaskMapper.selectById(taskId);
        if (task == null) {
            return;
        }

        try {
            markTaskProcessing(taskId);

            AuditLogQueryDTO queryDTO = buildQueryDTO(task);
            List<OrderOperationLog> logs = auditLogQuerySupport.listAuditLogEntities(queryDTO);
            List<AuditLogVO> voList = logs.stream().map(auditLogQuerySupport::toVO).toList();
            Path filePath = buildExportFile(taskId);

            // 重试或重复执行时先清理旧文件，避免返回脏结果。
            Files.deleteIfExists(filePath);
            EasyExcel.write(filePath.toFile(), AuditLogVO.class)
                    .sheet("审计日志")
                    .doWrite(voList);

            markTaskSuccess(taskId, logs.size(), buildFileName(taskId), filePath);
        } catch (Exception ex) {
            log.error("审计日志异步导出失败: taskId={}", taskId, ex);
            markTaskFailed(taskId, ex.getMessage());
        }
    }

    /**
     * 标记任务处理中
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 进入异步线程后立即回写处理中状态，前端可据此轮询展示。
     * @param taskId 任务ID
     */
    private void markTaskProcessing(Long taskId) {
        LambdaUpdateWrapper<AuditLogExportTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuditLogExportTask::getId, taskId)
                .set(AuditLogExportTask::getTaskStatus, AuditLogExportConstants.TASK_PROCESSING)
                .set(AuditLogExportTask::getStartedTime, LocalDateTime.now())
                .set(AuditLogExportTask::getLastError, null);
        auditLogExportTaskMapper.update(null, wrapper);
    }

    /**
     * 标记任务成功
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 导出完成后回写文件信息和统计结果，供任务列表和下载接口使用。
     * @param taskId 任务ID
     * @param totalCount 总记录数
     * @param fileName 文件名
     * @param filePath 文件路径
     */
    private void markTaskSuccess(Long taskId, int totalCount, String fileName, Path filePath) {
        LambdaUpdateWrapper<AuditLogExportTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuditLogExportTask::getId, taskId)
                .set(AuditLogExportTask::getTaskStatus, AuditLogExportConstants.TASK_SUCCESS)
                .set(AuditLogExportTask::getTotalCount, totalCount)
                .set(AuditLogExportTask::getExportedCount, totalCount)
                .set(AuditLogExportTask::getFileName, fileName)
                .set(AuditLogExportTask::getFilePath, filePath.toString())
                .set(AuditLogExportTask::getFinishedTime, LocalDateTime.now());
        auditLogExportTaskMapper.update(null, wrapper);
    }

    /**
     * 标记任务失败
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 统一回写失败原因，避免任务长时间停留在处理中。
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    private void markTaskFailed(Long taskId, String errorMessage) {
        LambdaUpdateWrapper<AuditLogExportTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AuditLogExportTask::getId, taskId)
                .set(AuditLogExportTask::getTaskStatus, AuditLogExportConstants.TASK_FAILED)
                .set(AuditLogExportTask::getLastError, normalizeErrorMessage(errorMessage))
                .set(AuditLogExportTask::getFinishedTime, LocalDateTime.now());
        auditLogExportTaskMapper.update(null, wrapper);
    }

    /**
     * 构建导出筛选条件
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 从任务快照还原查询条件，保证异步执行时与提交时口径一致。
     * @param task 任务实体
     * @return 查询条件
     */
    private AuditLogQueryDTO buildQueryDTO(AuditLogExportTask task) {
        AuditLogQueryDTO queryDTO = new AuditLogQueryDTO();
        queryDTO.setStartDate(task.getStartDate());
        queryDTO.setEndDate(task.getEndDate());
        queryDTO.setOperatorName(task.getOperatorName());
        queryDTO.setOperationType(task.getOperationType());
        return queryDTO;
    }

    /**
     * 构建导出文件路径
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 统一将审计日志导出文件落到系统临时目录下的专用子目录。
     * @param taskId 任务ID
     * @return 文件路径
     */
    private Path buildExportFile(Long taskId) throws IOException {
        Path tempDir = Files.createDirectories(Path.of(System.getProperty("java.io.tmpdir"), "diancan-audit-export"));
        return tempDir.resolve(buildFileName(taskId));
    }

    /**
     * 构建导出文件名
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 通过任务ID和时间戳生成稳定且可读的文件名。
     * @param taskId 任务ID
     * @return 文件名
     */
    private String buildFileName(Long taskId) {
        return "审计日志-" + taskId + "-" + FILE_TIME_FORMATTER.format(LocalDateTime.now()) + ".xlsx";
    }

    /**
     * 标准化错误信息
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 控制错误文本长度，避免数据库字段过长写入失败。
     * @param errorMessage 原始错误
     * @return 标准化后的错误信息
     */
    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return ResultCode.FAIL.getMessage();
        }
        return errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage;
    }
}
