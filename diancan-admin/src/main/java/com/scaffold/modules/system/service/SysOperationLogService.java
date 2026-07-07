package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.OperationLogQueryDTO;
import com.scaffold.modules.system.entity.SysOperationLog;
import com.scaffold.modules.system.vo.OperationLogVO;

/**
 * 操作日志服务接口
 *
 * @author Henfon
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 记录操作日志
     *
     * @param log 日志对象
     */
    void recordOperationLog(SysOperationLog log);

    /**
     * 分页查询操作日志
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageResult<OperationLogVO> pageList(OperationLogQueryDTO dto);
}
