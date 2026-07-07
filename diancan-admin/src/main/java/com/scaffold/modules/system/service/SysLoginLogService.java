package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.LoginLogQueryDTO;
import com.scaffold.modules.system.entity.SysLoginLog;
import com.scaffold.modules.system.vo.LoginLogVO;

/**
 * 登录日志服务接口
 *
 * @author Henfon
 */
public interface SysLoginLogService extends IService<SysLoginLog> {

    /**
     * 记录登录日志
     *
     * @param username 用户名
     * @param status   状态（0-失败 1-成功）
     * @param message  消息
     */
    void recordLoginLog(String username, Integer status, String message);

    /**
     * 分页查询登录日志
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageResult<LoginLogVO> pageList(LoginLogQueryDTO dto);
}
