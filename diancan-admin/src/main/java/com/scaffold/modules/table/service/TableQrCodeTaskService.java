package com.scaffold.modules.table.service;

/**
 * 桌台二维码异步任务服务
 *
 * @author Henfon
 */
public interface TableQrCodeTaskService {

    /**
     * 启动批量生成二维码任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 异步生成全部桌台二维码并回写二维码地址。
     * @param taskId 任务ID
     */
    void generateAllQrCodesAsync(String taskId);

    /**
     * 启动批量下载二维码打包任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 异步按区域打包桌台二维码为 zip 文件。
     * @param taskId 任务ID
     */
    void packageAllQrCodesAsync(String taskId);
}
