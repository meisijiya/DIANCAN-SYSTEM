package com.scaffold.modules.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.Result;
import com.scaffold.modules.payment.dto.AdminPaymentQueryDTO;
import com.scaffold.modules.payment.dto.CashPayDTO;
import com.scaffold.modules.payment.dto.OrderRefundDTO;
import com.scaffold.modules.payment.dto.SplitBillDTO;
import com.scaffold.modules.payment.service.PaymentService;
import com.scaffold.modules.payment.vo.AdminPaymentRecordVO;
import com.scaffold.modules.payment.vo.CashPayVO;
import com.scaffold.modules.payment.vo.PaymentStatusVO;
import com.scaffold.modules.payment.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器（管理端/服务端）
 *
 * @author Henfon
 */
@Tag(name = "支付（管理端）")
@RestController
@RequestMapping("/admin/payment")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "现金支付")
    @PostMapping("/cash")
    public Result<CashPayVO> cashPay(@Valid @RequestBody CashPayDTO dto) {
        return Result.success(paymentService.cashPay(dto));
    }

    @Operation(summary = "分页查询支付记录")
    @SaCheckPermission("payment:list")
    @GetMapping("/list")
    public Result<PageResult<AdminPaymentRecordVO>> listPayments(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int pageSize,
            AdminPaymentQueryDTO queryDTO) {
        return Result.success(paymentService.listPaymentsForAdmin(pageNum, pageSize, queryDTO));
    }

    @Operation(summary = "生成收银二维码")
    @PostMapping("/qrcode")
    public Result<PaymentVO> generateQrCode(@RequestParam Long orderId) {
        return Result.success(paymentService.generateQrCode(orderId));
    }

    @Operation(summary = "分单结账")
    @PostMapping("/split-bill")
    public Result<Void> splitBill(@Valid @RequestBody SplitBillDTO dto) {
        paymentService.splitBill(dto);
        return Result.success();
    }

    @Operation(summary = "整单退款")
    @PostMapping("/order/{orderId}/refund")
    public Result<Void> refundOrder(@PathVariable Long orderId, @Valid @RequestBody OrderRefundDTO dto) {
        paymentService.refundOrder(orderId, dto);
        return Result.success();
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{id}/status")
    public Result<PaymentStatusVO> getPaymentStatus(@PathVariable Long id) {
        return Result.success(paymentService.getPaymentStatus(id));
    }
}
