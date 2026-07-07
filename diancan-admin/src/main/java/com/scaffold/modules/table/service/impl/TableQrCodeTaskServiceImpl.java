package com.scaffold.modules.table.service.impl;

import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.service.TableQrCodeTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * 桌台二维码异步任务服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableQrCodeTaskServiceImpl implements TableQrCodeTaskService {

    private final DiningTableServiceImpl diningTableService;

    /**
     * 异步生成全部桌台二维码
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 后台逐桌生成二维码并持续回写任务进度。
     * @param taskId 任务ID
     */
    @Override
    @Async
    public void generateAllQrCodesAsync(String taskId) {
        List<DiningTable> tables = diningTableService.listTablesForQrTask();
        diningTableService.updateTask(taskId, "RUNNING", "开始批量生成二维码", tables.size(), 0, false, null, null);

        try {
            int completed = 0;
            for (DiningTable table : tables) {
                if (table.getCode() == null || table.getCode().isBlank()) {
                    continue;
                }

                // 逐桌更新进度，前端轮询时可以看到实时推进。
                diningTableService.generateAndSaveTableQrCode(table);
                completed++;
                diningTableService.updateTask(taskId, "RUNNING",
                        "正在生成 " + table.getCode() + " 的二维码", tables.size(), completed, false, null, null);
            }

            diningTableService.updateTask(taskId, "SUCCESS", "二维码批量生成完成", tables.size(), completed, false, null, null);
        } catch (Exception e) {
            log.error("批量生成二维码任务失败: taskId={}", taskId, e);
            diningTableService.updateTask(taskId, "FAILED", "二维码生成失败: " + e.getMessage(), tables.size(), null, false, null, null);
        }
    }

    /**
     * 异步打包全部桌台二维码
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 后台按区域准备二维码并生成 zip 压缩包。
     * @param taskId 任务ID
     */
    @Override
    @Async
    public void packageAllQrCodesAsync(String taskId) {
        List<DiningTable> tables = diningTableService.listTablesForQrTask();
        diningTableService.updateTask(taskId, "RUNNING", "开始按区域打包二维码", tables.size(), 0, false, null, null);

        try {
            Path zipPath = diningTableService.buildQrCodeZip(tables, taskId);
            String fileName = "桌台二维码-按区域-" + taskId + ".zip";
            diningTableService.updateTask(taskId, "SUCCESS", "二维码压缩包已生成", tables.size(), tables.size(),
                    true, fileName, zipPath.toString());
        } catch (Exception e) {
            log.error("二维码打包任务失败: taskId={}", taskId, e);
            diningTableService.updateTask(taskId, "FAILED", "二维码打包失败: " + e.getMessage(), tables.size(), null, false, null, null);
        }
    }
}
