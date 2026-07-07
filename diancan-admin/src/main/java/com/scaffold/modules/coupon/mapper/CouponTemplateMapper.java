package com.scaffold.modules.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 优惠券模板 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    /**
     * 按主键加锁查询模板
     *
     * @param id 模板ID
     * @return 模板实体
     * @author Henfon
     * @date 2026-06-26
     * @description 批量发券时锁定模板行，避免并发超发
     */
    CouponTemplate selectByIdForUpdate(@Param("id") Long id);
}
