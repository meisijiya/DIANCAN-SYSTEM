package com.scaffold.modules.payment.controller;

import com.scaffold.modules.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器（公开访问，无需鉴权）
 *
 * @author Henfon
 */
@Slf4j
@Tag(name = "支付回调")
@RestController
@RequestMapping("/wx/pay")
@RequiredArgsConstructor
public class PaymentCallbackController {

    private final PaymentService paymentService;

    /**
     * 微信支付回调通知。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 接收微信支付平台异步通知，完成验签、解密和订单落账。
     */
    @Operation(summary = "微信支付回调")
    @PostMapping("/notify")
    public Map<String, String> wechatCallback(HttpServletRequest request, @RequestBody String callbackData) {
        try {
            log.info("收到微信支付回调");
            paymentService.handleWechatCallback(extractHeaders(request), callbackData);
            return buildWechatResponse("SUCCESS", "成功");
        } catch (Exception e) {
            log.error("微信支付回调处理失败: {}", e.getMessage(), e);
            return buildWechatResponse("FAIL", e.getMessage());
        }
    }

    /**
     * 提取请求头，供微信支付验签使用。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 将 HttpServletRequest 中的请求头转为普通 Map，便于服务层统一处理。
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }

    /**
     * 构造微信支付回调响应体。
     *
     * @author Henfon
     * @date 2026-06-25
     * @description 微信支付平台要求返回 JSON 格式的 code/message 结果。
     */
    private Map<String, String> buildWechatResponse(String code, String message) {
        Map<String, String> result = new HashMap<>(2);
        result.put("code", code);
        result.put("message", message);
        return result;
    }
}
