package com.scaffold.modules.mq.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.mq.dto.MqMessageQueryDTO;
import com.scaffold.modules.mq.service.ReliableMessageService;
import com.scaffold.modules.mq.vo.MqMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 消息管理控制器
 *
 * @author Henfon
 */
@Tag(name = "消息管理（管理端）")
@RestController
@RequestMapping("/admin/mq/message")
@RequiredArgsConstructor
public class AdminMqMessageController {

    private final ReliableMessageService reliableMessageService;

    /**
     * 分页查询消息记录
     *
     * @param dto 查询条件
     * @return 消息分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端查看可靠消息投递状态
     */
    @Operation(summary = "分页查询消息记录")
    @SaCheckPermission("mq:message:list")
    @GetMapping("/page")
    public Result<PageResult<MqMessageVO>> page(MqMessageQueryDTO dto) {
        return Result.success(reliableMessageService.pageMessages(dto));
    }

    /**
     * 手动重试消息
     *
     * @param id 消息ID
     * @return 空结果
     * @author Henfon
     * @date 2026-06-26
     * @description 手动将失败消息恢复为待发送状态
     */
    @Operation(summary = "手动重试消息")
    @SaCheckPermission("mq:message:retry")
    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) {
        reliableMessageService.retryMessage(id);
        return Result.success();
    }
}
