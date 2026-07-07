package com.scaffold.modules.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.audit.entity.AuditLogExportTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志导出任务 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface AuditLogExportTaskMapper extends BaseMapper<AuditLogExportTask> {
}
