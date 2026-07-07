package com.scaffold.modules.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.coupon.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户优惠券 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    /**
     * 批量插入用户优惠券
     *
     * @param list 用户优惠券列表
     * @return 插入条数
     * @author Henfon
     * @date 2026-06-26
     * @description 用于批量发券时一次性写入多条用户优惠券记录
     */
    int batchInsert(@Param("list") List<UserCoupon> list);
}
