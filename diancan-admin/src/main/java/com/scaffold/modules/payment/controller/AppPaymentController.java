package com.scaffold.modules.payment.controller;

import com.scaffold.framework.satoken.SessionUtils;
import com.scaffold.common.result.Result;
import com.scaffold.modules.payment.dto.AAPayDTO;
import com.scaffold.modules.payment.dto.AlipayDTO;
import com.scaffold.modules.payment.dto.WechatPayDTO;
import com.scaffold.modules.payment.service.PaymentService;
import com.scaffold.modules.payment.vo.PaymentStatusVO;
import com.scaffold.modules.payment.vo.PaymentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制器（小程序端）
 *
 * @author Henfon
 */
@Tag(name = "支付（小程序端）")
@RestController
@RequestMapping("/app/payment")
@RequiredArgsConstructor
public class AppPaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "微信支付")
    @PostMapping("/wechat")
    public Result<PaymentVO> wechatPay(@Valid @RequestBody WechatPayDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(paymentService.wechatPay(openid, dto));
    }

    @Operation(summary = "支付宝支付")
    @PostMapping("/alipay")
    public Result<PaymentVO> alipay(@Valid @RequestBody AlipayDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(paymentService.alipay(openid, dto));
    }

    @Operation(summary = "AA分摊支付")
    @PostMapping("/aa")
    public Result<PaymentVO> aaPay(@Valid @RequestBody AAPayDTO dto) {
        String openid = SessionUtils.getCurrentOpenid();
        return Result.success(paymentService.aaPay(openid, dto));
    }

    @Operation(summary = "查询支付状态")
    @GetMapping("/{id}/status")
    public Result<PaymentStatusVO> getPaymentStatus(@PathVariable Long id) {
        return Result.success(paymentService.getPaymentStatus(id));
    }
}
