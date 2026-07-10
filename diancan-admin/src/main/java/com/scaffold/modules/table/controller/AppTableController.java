package com.scaffold.modules.table.controller;

import com.scaffold.common.result.Result;
import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.modules.table.dto.TableChangeDTO;
import com.scaffold.modules.table.service.DiningTableService;
import com.scaffold.modules.table.vo.DiningTableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 桌台控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "桌台（小程序端）")
@RestController
@RequestMapping("/app/table")
@RequiredArgsConstructor
public class AppTableController {

    private final DiningTableService diningTableService;

    @Operation(summary = "通过二维码编号获取桌台信息")
    @GetMapping("/{code}")
    public Result<DiningTableVO> getByCode(@Parameter(description = "桌台编号") @PathVariable String code) {
        return Result.success(diningTableService.getByCode(code));
    }

    @Operation(summary = "开台（空闲→占用）")
    @PutMapping("/{id}/open")
    public Result<Void> openTable(@Parameter(description = "桌台ID") @PathVariable Long id) {
        diningTableService.openTable(id);
        return Result.success();
    }

    @Operation(summary = "绑定当前登录顾客到桌台")
    @PutMapping("/{id}/bind")
    public Result<DiningTableVO> bindCurrentUser(@Parameter(description = "桌台ID") @PathVariable Long id) {
        return Result.success(diningTableService.bindCurrentUser(id, SessionUtils.getCurrentOpenid()));
    }

    @Operation(summary = "换桌")
    @PutMapping("/{id}/change")
    public Result<Void> changeTable(@Parameter(description = "原桌台ID") @PathVariable Long id,
                                    @Valid @RequestBody TableChangeDTO dto) {
        diningTableService.changeTable(id, dto.getTargetTableId());
        return Result.success();
    }
}
