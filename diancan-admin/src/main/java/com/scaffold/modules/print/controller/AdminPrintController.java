package com.scaffold.modules.print.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.Result;
import com.scaffold.modules.print.dto.CategoryMappingDTO;
import com.scaffold.modules.print.dto.PrinterCreateDTO;
import com.scaffold.modules.print.dto.PrinterUpdateDTO;
import com.scaffold.modules.print.service.PrintService;
import com.scaffold.modules.print.vo.PrinterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 打印管理控制器（管理端/服务端）
 *
 * @author Henfon
 */
@Tag(name = "打印管理")
@RestController
@RequestMapping("/admin/print")
@RequiredArgsConstructor
public class AdminPrintController {

    private final PrintService printService;

    @Operation(summary = "获取打印机列表")
    @GetMapping("/printer/list")
    @SaCheckPermission("print:manage")
    public Result<List<PrinterVO>> listPrinters() {
        return Result.success(printService.listPrinters());
    }

    @Operation(summary = "创建打印机")
    @PostMapping("/printer")
    @SaCheckPermission("print:manage")
    public Result<Void> createPrinter(@Valid @RequestBody PrinterCreateDTO dto) {
        printService.createPrinter(dto);
        return Result.success();
    }

    @Operation(summary = "更新打印机")
    @PutMapping("/printer/{id}")
    @SaCheckPermission("print:manage")
    public Result<Void> updatePrinter(@Parameter(description = "打印机ID") @PathVariable Long id,
                                      @Valid @RequestBody PrinterUpdateDTO dto) {
        dto.setId(id);
        printService.updatePrinter(dto);
        return Result.success();
    }

    @Operation(summary = "删除打印机")
    @DeleteMapping("/printer/{id}")
    @SaCheckPermission("print:manage")
    public Result<Void> deletePrinter(@Parameter(description = "打印机ID") @PathVariable Long id) {
        printService.deletePrinter(id);
        return Result.success();
    }

    @Operation(summary = "更新打印机-分类映射")
    @PutMapping("/category-mapping")
    @SaCheckPermission("print:manage")
    public Result<Void> updateCategoryMapping(@Valid @RequestBody CategoryMappingDTO dto) {
        printService.updateCategoryMapping(dto);
        return Result.success();
    }

    @Operation(summary = "重新打印订单")
    @PostMapping("/reprint/{orderId}")
    @SaCheckPermission("print:reprint")
    public Result<Void> reprintOrder(@Parameter(description = "订单ID") @PathVariable Long orderId) {
        printService.reprintOrder(orderId);
        return Result.success();
    }
}
