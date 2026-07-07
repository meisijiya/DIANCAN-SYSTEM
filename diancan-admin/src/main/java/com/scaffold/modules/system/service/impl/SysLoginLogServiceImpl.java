package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.LoginLogQueryDTO;
import com.scaffold.modules.system.entity.SysLoginLog;
import com.scaffold.modules.system.mapper.SysLoginLogMapper;
import com.scaffold.modules.system.service.SysLoginLogService;
import com.scaffold.modules.system.vo.LoginLogVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements SysLoginLogService {

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：记录登录日志，先在主线程抓取请求快照，避免异步线程丢失上下文
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息
     */
    @Override
    public void recordLoginLog(String username, Integer status, String message) {
        RequestSnapshot snapshot = buildRequestSnapshot();
        saveLoginLogAsync(username, status, message, snapshot);
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：异步保存登录日志
     *
     * @param username 用户名
     * @param status 状态
     * @param message 消息
     * @param snapshot 请求快照
     */
    @Async
    public void saveLoginLogAsync(String username, Integer status, String message, RequestSnapshot snapshot) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUsername(username);
            loginLog.setStatus(status);
            loginLog.setMessage(message);
            loginLog.setLoginTime(LocalDateTime.now());

            if (snapshot != null) {
                loginLog.setIp(snapshot.ip());
                loginLog.setLocation(resolveLocation(snapshot.ip()));
                loginLog.setBrowser(snapshot.browser());
                loginLog.setOs(snapshot.os());
            }

            save(loginLog);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    @Override
    public PageResult<LoginLogVO> pageList(LoginLogQueryDTO dto) {
        Page<SysLoginLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getUsername()), SysLoginLog::getUsername, dto.getUsername())
                .eq(dto.getStatus() != null, SysLoginLog::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, SysLoginLog::getLoginTime, dto.getStartTime())
                .le(dto.getEndTime() != null, SysLoginLog::getLoginTime, dto.getEndTime())
                .orderByDesc(SysLoginLog::getLoginTime);

        Page<SysLoginLog> result = page(page, wrapper);
        List<LoginLogVO> voList = BeanUtil.copyToList(result.getRecords(), LoginLogVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：构建当前请求快照，供异步线程使用
     *
     * @return 请求快照
     */
    private RequestSnapshot buildRequestSnapshot() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = getIpAddress(request);
        String browser = null;
        String os = null;

        // 在请求线程里解析 User-Agent，避免异步线程拿不到 request。
        String userAgentStr = request.getHeader("User-Agent");
        if (StrUtil.isNotBlank(userAgentStr)) {
            UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
            browser = userAgent.getBrowser().getName();
            os = userAgent.getOs().getName();
        }

        return new RequestSnapshot(ip, browser, os);
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：根据 IP 生成登录地点兜底值
     *
     * @param ip IP 地址
     * @return 登录地点
     */
    private String resolveLocation(String ip) {
        if (StrUtil.isBlank(ip)) {
            return "未知";
        }

        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "本机地址";
        }

        if (NetUtil.isInnerIP(ip)) {
            return "局域网";
        }

        return "外网地址";
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：获取客户端 IP 地址
     *
     * @param request 请求对象
     * @return IP 地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 登录请求快照
     *
     * @param ip IP 地址
     * @param browser 浏览器
     * @param os 操作系统
     */
    private record RequestSnapshot(String ip, String browser, String os) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}
