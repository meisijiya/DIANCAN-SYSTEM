package com.scaffold.modules.table.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.table.dto.TableCreateDTO;
import com.scaffold.modules.table.dto.TableUpdateDTO;
import com.scaffold.modules.table.entity.DiningTable;
import com.scaffold.modules.table.vo.DiningTableVO;
import com.scaffold.modules.table.vo.QrCodeTaskVO;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

/**
 * 桌台服务接口
 *
 * @author Henfon
 */
public interface DiningTableService extends IService<DiningTable> {

    /**
     * 获取所有桌台列表（看板用）
     *
     * @return 桌台列表
     */
    List<DiningTableVO> listAll();

    /**
     * 通过桌台编号获取桌台信息（扫码用）
     *
     * @param code 桌台编号
     * @return 桌台信息
     */
    DiningTableVO getByCode(String code);

    /**
     * 开台（空闲→占用）
     *
     * @param id 桌台ID
     */
    void openTable(Long id);

    /**
     * 换桌（原桌→空闲，目标桌→占用）
     *
     * @param fromId 原桌台ID
     * @param toId   目标桌台ID
     */
    void changeTable(Long fromId, Long toId);

    /**
     * 标记已清洁（待清洁→空闲）
     *
     * @param id 桌台ID
     */
    void markClean(Long id);

    /**
     * 更新桌台状态（通用方法，供其他模块调用）
     *
     * @param id     桌台ID
     * @param status 目标状态
     */
    void updateTableStatus(Long id, Integer status);

    /**
     * 创建桌台
     *
     * @param dto 创建参数
     */
    void createTable(TableCreateDTO dto);

    /**
     * 更新桌台
     *
     * @param dto 更新参数
     */
    void updateTable(TableUpdateDTO dto);

    /**
     * 删除桌台
     *
     * @param id 桌台ID
     */
    void deleteTable(Long id);

    /**
     * 批量生成所有桌台二维码
     *
     * @return 本次成功生成数量
     */
    int generateAllQrCodes();

    /**
     * 提交批量生成二维码异步任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 创建二维码批量生成任务并立即返回任务状态。
     * @return 任务状态
     */
    QrCodeTaskVO submitGenerateAllQrCodesTask();

    /**
     * 下载桌台二维码图片
     *
     * @param id       桌台ID
     * @param response HTTP 响应
     */
    void downloadQrCode(Long id, HttpServletResponse response);

    /**
     * 提交批量下载二维码异步任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 创建按区域压缩打包的二维码下载任务。
     * @return 任务状态
     */
    QrCodeTaskVO submitDownloadAllQrCodesTask();

    /**
     * 获取二维码任务状态
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 根据任务ID查询二维码生成或打包任务的执行状态。
     * @param taskId 任务ID
     * @return 任务状态
     */
    QrCodeTaskVO getQrCodeTask(String taskId);

    /**
     * 下载任务结果文件
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 下载异步打包完成后的二维码 zip 文件。
     * @param taskId 任务ID
     * @param response HTTP 响应
     */
    void downloadQrCodeTaskFile(String taskId, HttpServletResponse response);
}
