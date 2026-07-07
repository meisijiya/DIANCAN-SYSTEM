package com.scaffold.modules.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.coupon.entity.CouponGrantTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 发券任务 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface CouponGrantTaskMapper extends BaseMapper<CouponGrantTask> {

    /**
     * 按主键加锁查询任务
     *
     * @param id 任务ID
     * @return 任务实体
     * @author Henfon
     * @date 2026-06-26
     * @description 批处理时锁定任务行，保证进度累计一致
     */
    CouponGrantTask selectByIdForUpdate(@Param("id") Long id);
}
