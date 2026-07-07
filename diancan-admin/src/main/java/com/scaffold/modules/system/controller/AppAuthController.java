package com.scaffold.modules.system.controller;

import com.scaffold.common.result.Result;
import com.scaffold.modules.system.dto.PhoneLoginDTO;
import com.scaffold.modules.system.service.SysUserService;
import com.scaffold.modules.system.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序认证控制器
 *
 * @author Henfon
 * @date 2025/06/25
 */
@Tag(name = "小程序认证")
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class AppAuthController {

    private final SysUserService userService;

    /**
     * 手机号登录（微信手机号快速验证）
     *
     * @author Henfon
     * @date 2025/06/25
     * @description 小程序端通过微信手机号授权后调用此接口登录
     */
    @Operation(summary = "手机号登录")
    @PostMapping("/phone-login")
    public Result<LoginVO> phoneLogin(@Valid @RequestBody PhoneLoginDTO dto) {
        return Result.success(userService.loginByPhone(dto));
    }
}
