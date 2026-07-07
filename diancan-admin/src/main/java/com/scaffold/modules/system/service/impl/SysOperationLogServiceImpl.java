package com.scaffold.modules.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.OperationLogQueryDTO;
import com.scaffold.modules.system.entity.SysOperationLog;
import com.scaffold.modules.system.mapper.SysOperationLogMapper;
import com.scaffold.modules.system.service.SysOperationLogService;
import com.scaffold.modules.system.vo.OperationLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    @Override
    @Async
    public void recordOperationLog(SysOperationLog operationLog) {
        try {
            save(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败", e);
        }
    }

    @Override
    public PageResult<OperationLogVO> pageList(OperationLogQueryDTO dto) {
        Page<SysOperationLog> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getModule()), SysOperationLog::getModule, dto.getModule())
                .like(StrUtil.isNotBlank(dto.getUsername()), SysOperationLog::getUsername, dto.getUsername())
                .eq(dto.getStatus() != null, SysOperationLog::getStatus, dto.getStatus())
                .ge(dto.getStartTime() != null, SysOperationLog::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, SysOperationLog::getCreateTime, dto.getEndTime())
                .orderByDesc(SysOperationLog::getCreateTime);

        Page<SysOperationLog> result = page(page, wrapper);
        List<OperationLogVO> voList = BeanUtil.copyToList(result.getRecords(), OperationLogVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }
}
