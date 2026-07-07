package com.scaffold.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.system.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
