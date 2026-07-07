package com.scaffold.modules.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.modules.audit.dto.AuditLogQueryDTO;
import com.scaffold.modules.audit.vo.AuditLogVO;
import com.scaffold.modules.order.entity.OrderOperationLog;
import com.scaffold.modules.order.mapper.OrderOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 审计日志查询支撑组件
 *
 * @author Henfon
 */
@Component
@RequiredArgsConstructor
public class AuditLogQuerySupport {

    private final OrderOperationLogMapper orderOperationLogMapper;

    /**
     * 查询审计日志实体列表
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 供分页查询和异步导出任务共用，保证筛选条件一致。
     * @param queryDTO 查询条件
     * @return 审计日志实体列表
     */
    public List<OrderOperationLog> listAuditLogEntities(AuditLogQueryDTO queryDTO) {
        LambdaQueryWrapper<OrderOperationLog> wrapper = buildQueryWrapper(queryDTO);
        wrapper.orderByDesc(OrderOperationLog::getCreateTime);
        return orderOperationLogMapper.selectList(wrapper);
    }

    /**
     * 转换审计日志 VO
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 统一将订单操作日志实体映射为审计日志展示对象。
     * @param entity 审计日志实体
     * @return 审计日志 VO
     */
    public AuditLogVO toVO(OrderOperationLog entity) {
        AuditLogVO vo = new AuditLogVO();
        vo.setId(entity.getId());
        vo.setOrderId(entity.getOrderId());
        vo.setOrderItemId(entity.getOrderItemId());
        vo.setOperationType(entity.getOperationType());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        vo.setReason(entity.getReason());
        vo.setDetail(entity.getDetail());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    /**
     * 构建审计日志查询条件
     *
     * @author Henfon
     * @date 2026-07-04
     * @description 将页面筛选条件统一转成 MyBatis-Plus 查询包装器。
     * @param queryDTO 查询条件
     * @return 查询包装器
     */
    private LambdaQueryWrapper<OrderOperationLog> buildQueryWrapper(AuditLogQueryDTO queryDTO) {
        LambdaQueryWrapper<OrderOperationLog> wrapper = new LambdaQueryWrapper<>();

        if (queryDTO != null) {
            if (queryDTO.getStartDate() != null) {
                wrapper.ge(OrderOperationLog::getCreateTime,
                        LocalDateTime.of(queryDTO.getStartDate(), LocalTime.MIN));
            }
            if (queryDTO.getEndDate() != null) {
                wrapper.le(OrderOperationLog::getCreateTime,
                        LocalDateTime.of(queryDTO.getEndDate(), LocalTime.MAX));
            }
            if (StringUtils.hasText(queryDTO.getOperatorName())) {
                wrapper.like(OrderOperationLog::getOperatorName, queryDTO.getOperatorName());
            }
            if (StringUtils.hasText(queryDTO.getOperationType())) {
                wrapper.eq(OrderOperationLog::getOperationType, queryDTO.getOperationType());
            }
        }

        return wrapper;
    }
}
