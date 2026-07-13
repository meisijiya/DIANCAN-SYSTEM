package com.scaffold.modules.table.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.Result;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.table.dto.TableCreateDTO;
import com.scaffold.modules.table.dto.TableUpdateDTO;
import com.scaffold.modules.table.service.DiningTableService;
import com.scaffold.modules.table.vo.DiningTableVO;
import com.scaffold.modules.table.vo.QrCodeTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 桌台管理控制器（管理端）
 *
 * @author Henfon
 */
@Tag(name = "桌台管理（管理端）")
@RestController
@RequestMapping("/admin/table")
@RequiredArgsConstructor
public class AdminTableController {

    private final DiningTableService diningTableService;

    @Operation(summary = "创建桌台")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TableCreateDTO dto) {
        diningTableService.createTable(dto);
        return Result.success();
    }

    @Operation(summary = "更新桌台")
    @PutMapping("/{id}")
    public Result<Void> update(@Parameter(description = "桌台ID") @PathVariable Long id,
                               @Valid @RequestBody TableUpdateDTO dto) {
        dto.setId(id);
        diningTableService.updateTable(dto);
        return Result.success();
    }

    @Operation(summary = "删除桌台")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "桌台ID") @PathVariable Long id) {
        diningTableService.deleteTable(id);
        return Result.success();
    }

    @Operation(summary = "获取所有桌台列表（看板用）")
    @GetMapping("/list")
    public Result<List<DiningTableVO>> list() {
        return Result.success(diningTableService.listAll());
    }

    @Operation(summary = "获取桌台当前订单（占位，暂返回null）")
    @GetMapping("/{id}/order")
    public Result<Object> getTableOrder(@Parameter(description = "桌台ID") @PathVariable Long id) {
        // placeholder, returns null for now - will be implemented in order module
        return Result.success(null);
    }

    @Operation(summary = "标记桌台已清洁（待清洁→空闲）")
    @PutMapping("/{id}/clean")
    public Result<Void> markClean(@Parameter(description = "桌台ID") @PathVariable Long id) {
        diningTableService.markClean(id);
        return Result.success();
    }

    /**
     * 管理端确认结台
     *
     * @author Henfon
     * @date 2026-07-13
     * @description 校验当前桌次所有订单均已支付，再将桌台推进到待清洁状态。
     * @param id 桌台ID
     * @return 处理结果
     */
    @Operation(summary = "确认结台（占用→待清洁）")
    @PutMapping("/{id}/checkout")
    public Result<Void> checkout(@Parameter(description = "桌台ID") @PathVariable Long id) {
        if (!diningTableService.checkoutTableIfSettled(id)) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "当前桌次仍有待支付订单，请完成收款后再结台");
        }
        return Result.success();
    }

    @Operation(summary = "桌台推进到待清洁（已结账→待清洁）")
    @PutMapping("/{id}/to-clean")
    public Result<Void> toClean(@Parameter(description = "桌台ID") @PathVariable Long id) {
        diningTableService.updateTableStatus(id, 3);
        return Result.success();
    }

    /**
     * 一键释放桌台
     *
     * @author Henfon
     * @date 2026-07-09
     * @description 将空占用、已结账或待清洁桌台释放为空闲，已有订单的占用桌台会直接拒绝。
     * @param id 桌台ID
     * @return 处理结果
     */
    @Operation(summary = "一键释放空占用/已结账/待清洁桌台")
    @PutMapping("/{id}/release")
    public Result<Void> release(@Parameter(description = "桌台ID") @PathVariable Long id) {
        diningTableService.releaseTable(id);
        return Result.success();
    }

    @Operation(summary = "下载桌台二维码")
    @GetMapping("/{id}/qrcode/download")
    @SaCheckPermission("table:qrcode:download")
    public void downloadQrCode(@Parameter(description = "桌台ID") @PathVariable Long id, HttpServletResponse response) {
        diningTableService.downloadQrCode(id, response);
    }

    @Operation(summary = "批量生成所有桌台二维码")
    @PostMapping("/qrcode/generate-all")
    @SaCheckPermission("table:qrcode:generate")
    public Result<Integer> generateAllQrCodes() {
        return Result.success(diningTableService.generateAllQrCodes());
    }

    /**
     * 提交批量生成二维码任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 立即返回任务ID，由前端轮询任务状态。
     * @return 任务状态
     */
    @Operation(summary = "异步批量生成所有桌台二维码")
    @PostMapping("/qrcode/generate-all/task")
    @SaCheckPermission("table:qrcode:generate")
    public Result<QrCodeTaskVO> submitGenerateAllQrCodesTask() {
        return Result.success(diningTableService.submitGenerateAllQrCodesTask());
    }

    /**
     * 提交按区域打包二维码任务
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 生成 zip 文件前先异步准备二维码资源。
     * @return 任务状态
     */
    @Operation(summary = "异步按区域打包所有桌台二维码")
    @PostMapping("/qrcode/download-all/task")
    @SaCheckPermission("table:qrcode:download")
    public Result<QrCodeTaskVO> submitDownloadAllQrCodesTask() {
        return Result.success(diningTableService.submitDownloadAllQrCodesTask());
    }

    /**
     * 查询二维码任务状态
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 根据任务ID查询批量生成或打包下载的执行状态。
     * @param taskId 任务ID
     * @return 任务状态
     */
    @Operation(summary = "查询二维码异步任务状态")
    @GetMapping("/qrcode/task/{taskId}")
    public Result<QrCodeTaskVO> getQrCodeTask(@Parameter(description = "任务ID") @PathVariable String taskId) {
        return Result.success(diningTableService.getQrCodeTask(taskId));
    }

    /**
     * 下载二维码任务结果文件
     *
     * @author Henfon
     * @date 2026-06-26
     * @description 下载异步打包完成后的二维码压缩包。
     * @param taskId 任务ID
     * @param response HTTP 响应
     */
    @Operation(summary = "下载二维码异步任务结果文件")
    @GetMapping("/qrcode/task/{taskId}/download")
    @SaCheckPermission("table:qrcode:download")
    public void downloadQrCodeTaskFile(@Parameter(description = "任务ID") @PathVariable String taskId,
                                       HttpServletResponse response) {
        diningTableService.downloadQrCodeTaskFile(taskId, response);
    }
}
