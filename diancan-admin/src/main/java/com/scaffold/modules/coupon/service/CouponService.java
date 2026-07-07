package com.scaffold.modules.coupon.service;

import com.scaffold.common.result.PageResult;
import com.scaffold.modules.coupon.dto.AppCouponQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskDetailQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateCreateDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateUpdateDTO;
import com.scaffold.modules.coupon.dto.UserCouponQueryDTO;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import com.scaffold.modules.coupon.entity.UserCoupon;
import com.scaffold.modules.coupon.vo.CouponGrantTaskDetailVO;
import com.scaffold.modules.coupon.vo.CouponGrantTaskVO;
import com.scaffold.modules.coupon.vo.CouponTemplateVO;
import com.scaffold.modules.coupon.vo.UserCouponVO;
import com.scaffold.modules.system.entity.SysUser;

import java.math.BigDecimal;

/**
 * 优惠券服务
 *
 * @author Henfon
 */
public interface CouponService {

    /**
     * 分页查询优惠券模板
     *
     * @param dto 查询条件
     * @return 模板分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端分页查询优惠券模板
     */
    PageResult<CouponTemplateVO> pageTemplates(CouponTemplateQueryDTO dto);

    /**
     * 创建优惠券模板
     *
     * @param dto 创建参数
     * @author Henfon
     * @date 2026-06-26
     * @description 新建优惠券模板并初始化发放统计
     */
    void createTemplate(CouponTemplateCreateDTO dto);

    /**
     * 更新优惠券模板
     *
     * @param dto 更新参数
     * @author Henfon
     * @date 2026-06-26
     * @description 更新未删除的优惠券模板配置
     */
    void updateTemplate(CouponTemplateUpdateDTO dto);

    /**
     * 更新优惠券模板状态
     *
     * @param id 模板ID
     * @param status 状态
     * @author Henfon
     * @date 2026-06-26
     * @description 启用或停用优惠券模板
     */
    void updateTemplateStatus(Long id, Integer status);

    /**
     * 执行发券
     *
     * @param dto 发券参数
     * @return 发券任务结果
     * @author Henfon
     * @date 2026-06-26
     * @description 支持指定用户和全员发券
     */
    CouponGrantTaskVO grantCoupons(CouponGrantDTO dto);

    /**
     * 分页查询发券任务
     *
     * @param dto 查询条件
     * @return 发券任务分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端查看异步发券任务进度、结果与失败情况
     */
    PageResult<CouponGrantTaskVO> pageGrantTasks(CouponGrantTaskQueryDTO dto);

    /**
     * 分页查询发券任务明细
     *
     * @param dto 查询条件
     * @return 发券任务明细分页数据
     * @author Henfon
     * @date 2026-07-01
     * @description 管理端按任务查看每个用户的发券处理结果与失败原因
     */
    PageResult<CouponGrantTaskDetailVO> pageGrantTaskDetails(CouponGrantTaskDetailQueryDTO dto);

    /**
     * 分页查询用户优惠券
     *
     * @param dto 查询条件
     * @return 用户优惠券分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 管理端查看已发放的用户优惠券
     */
    PageResult<UserCouponVO> pageUserCoupons(UserCouponQueryDTO dto);

    /**
     * 分页查询当前用户优惠券
     *
     * @param userId 用户ID
     * @param dto 查询条件
     * @return 我的优惠券分页数据
     * @author Henfon
     * @date 2026-06-26
     * @description 小程序端查看当前用户持有的优惠券
     */
    PageResult<UserCouponVO> pageMyCoupons(Long userId, AppCouponQueryDTO dto);

    /**
     * 锁定用户优惠券
     *
     * @param userId 用户ID
     * @param couponId 优惠券ID
     * @param orderAmount 订单金额
     * @param orderId 订单ID
     * @return 锁定后的用户优惠券
     * @author Henfon
     * @date 2026-06-26
     * @description 下单时锁定优惠券，防止并发重复使用
     */
    UserCoupon lockCoupon(Long userId, Long couponId, BigDecimal orderAmount, Long orderId);

    /**
     * 按订单核销优惠券
     *
     * @param orderId 订单ID
     * @author Henfon
     * @date 2026-06-26
     * @description 支付成功后将订单关联的锁定优惠券核销为已使用
     */
    void markCouponUsed(Long orderId);

    /**
     * 释放订单锁定的优惠券
     *
     * @param orderId 订单ID
     * @author Henfon
     * @date 2026-06-26
     * @description 订单取消或无需支付时释放已锁定的优惠券
     */
    void releaseLockedCoupon(Long orderId);

    /**
     * 查询优惠券模板实体
     *
     * @param templateId 模板ID
     * @return 优惠券模板
     * @author Henfon
     * @date 2026-07-02
     * @description 会员权益、积分兑换等场景复用模板配置时使用
     */
    CouponTemplate getTemplateEntity(Long templateId);

    /**
     * 直接向单个用户发放优惠券
     *
     * @param templateId 模板ID
     * @param user 用户
     * @param sourceType 来源类型
     * @param remark 备注
     * @return 用户优惠券
     * @author Henfon
     * @date 2026-07-02
     * @description 提供给会员权益、生日券、升级礼包等同步发券场景
     */
    UserCoupon grantCouponToUser(Long templateId, SysUser user, Integer sourceType, String remark);
}
