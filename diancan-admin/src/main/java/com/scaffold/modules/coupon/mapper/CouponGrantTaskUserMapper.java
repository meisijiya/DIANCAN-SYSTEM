package com.scaffold.modules.coupon.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.modules.coupon.entity.CouponGrantTaskUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 发券任务用户 Mapper
 *
 * @author Henfon
 */
@Mapper
public interface CouponGrantTaskUserMapper extends BaseMapper<CouponGrantTaskUser> {

    /**
     * 批量插入任务用户快照
     *
     * @param list 快照列表
     * @return 插入条数
     * @author Henfon
     * @date 2026-06-26
     * @description 将任务用户快照批量写入数据库，供后续拆批发券使用
     */
    int batchInsert(@Param("list") List<CouponGrantTaskUser> list);
}
