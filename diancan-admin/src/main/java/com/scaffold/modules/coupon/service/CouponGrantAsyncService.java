package com.scaffold.modules.coupon.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.coupon.dto.CouponGrantDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskDetailQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskQueryDTO;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import com.scaffold.modules.coupon.mq.payload.CouponGrantBatchPayload;
import com.scaffold.modules.coupon.mq.payload.CouponGrantDispatchPayload;
import com.scaffold.modules.coupon.vo.CouponGrantTaskDetailVO;
import com.scaffold.modules.coupon.vo.CouponGrantTaskVO;

/**
 * 异步发券服务
 *
 * @author Henfon
 */
public interface CouponGrantAsyncService {

    /**
     * 创建异步发券任务
     *
     * @param template 模板
     * @param dto 发券参数
     * @return 发券任务信息
     * @author Henfon
     * @date 2026-06-26
     * @description 创建任务、落用户快照并写入可靠消息表
     */
    CouponGrantTaskVO createGrantTask(CouponTemplate template, CouponGrantDTO dto);

    /**
     * 处理任务分发
     *
     * @param payload 分发消息体
     * @author Henfon
     * @date 2026-07-01
     * @description 异步构建任务用户快照，并按批次拆分生成批处理消息
     */
    void dispatchTask(CouponGrantDispatchPayload payload);

    /**
     * 处理批量发券
     *
     * @param payload 批处理消息体
     * @author Henfon
     * @date 2026-06-26
     * @description 消费批处理消息并实际落用户优惠券数据
     */
    void processBatch(CouponGrantBatchPayload payload);

    /**
     * 分页查询发券任务
     *
     * @param dto 查询条件
     * @return 发券任务分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 提供管理端查看异步发券进度与结果
     */
    PageResult<CouponGrantTaskVO> pageGrantTasks(CouponGrantTaskQueryDTO dto);

    /**
     * 分页查询发券任务明细
     *
     * @param dto 查询条件
     * @return 发券任务明细分页数据
     * @author Henfon
     * @date 2026-07-01
     * @description 管理端查看任务下每个用户的处理状态、失败原因和完成时间
     */
    PageResult<CouponGrantTaskDetailVO> pageGrantTaskDetails(CouponGrantTaskDetailQueryDTO dto);
}
