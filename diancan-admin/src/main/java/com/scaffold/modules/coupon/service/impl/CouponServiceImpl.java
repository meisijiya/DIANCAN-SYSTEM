package com.scaffold.modules.coupon.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.coupon.dto.AppCouponQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskDetailQueryDTO;
import com.scaffold.modules.coupon.dto.CouponGrantTaskQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateCreateDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateQueryDTO;
import com.scaffold.modules.coupon.dto.CouponTemplateUpdateDTO;
import com.scaffold.modules.coupon.dto.UserCouponQueryDTO;
import com.scaffold.modules.coupon.entity.CouponGrantTask;
import com.scaffold.modules.coupon.entity.CouponTemplate;
import com.scaffold.modules.coupon.entity.UserCoupon;
import com.scaffold.modules.coupon.mapper.CouponGrantTaskMapper;
import com.scaffold.modules.coupon.mapper.CouponTemplateMapper;
import com.scaffold.modules.coupon.mapper.UserCouponMapper;
import com.scaffold.modules.coupon.service.CouponService;
import com.scaffold.modules.coupon.service.CouponGrantAsyncService;
import com.scaffold.modules.coupon.vo.CouponGrantTaskDetailVO;
import com.scaffold.modules.coupon.vo.CouponGrantTaskVO;
import com.scaffold.modules.coupon.vo.CouponTemplateVO;
import com.scaffold.modules.coupon.vo.UserCouponVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 优惠券服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private static final int TEMPLATE_STATUS_DISABLED = 0;
    private static final int TEMPLATE_STATUS_ENABLED = 1;
    private static final int COUPON_TYPE_FULL_REDUCTION = 1;
    private static final int COUPON_TYPE_DISCOUNT = 2;
    private static final int VALIDITY_TYPE_FIXED = 1;
    private static final int VALIDITY_TYPE_RELATIVE = 2;
    private static final int GRANT_MODE_ASSIGN = 1;
    private static final int GRANT_MODE_ALL = 2;
    private static final int TASK_STATUS_PENDING = 0;
    private static final int TASK_STATUS_FINISHED = 1;
    private static final int TASK_STATUS_PARTIAL = 2;
    private static final int USER_COUPON_STATUS_UNUSED = 0;
    private static final int USER_COUPON_STATUS_USED = 1;
    private static final int USER_COUPON_STATUS_EXPIRED = 2;
    private static final int USER_COUPON_STATUS_LOCKED = 3;

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponGrantTaskMapper couponGrantTaskMapper;
    private final SysUserMapper sysUserMapper;
    private final CouponGrantAsyncService couponGrantAsyncService;

    @Override
    public PageResult<CouponTemplateVO> pageTemplates(CouponTemplateQueryDTO dto) {
        Page<CouponTemplate> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<CouponTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getName()), CouponTemplate::getName, dto.getName())
                .eq(dto.getStatus() != null, CouponTemplate::getStatus, dto.getStatus())
                .eq(dto.getType() != null, CouponTemplate::getType, dto.getType())
                .orderByDesc(CouponTemplate::getCreateTime);

        Page<CouponTemplate> result = couponTemplateMapper.selectPage(page, wrapper);
        List<CouponTemplateVO> records = BeanUtil.copyToList(result.getRecords(), CouponTemplateVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTemplate(CouponTemplateCreateDTO dto) {
        // 创建前先校验金额、有效期等规则，避免后续发券异常
        validateTemplateDTO(dto);

        CouponTemplate template = new CouponTemplate();
        BeanUtil.copyProperties(dto, template);
        template.setIssuedQuantity(0);
        template.setAvailableWeekdays(normalizeAvailableWeekdays(dto.getAvailableWeekdays()));
        couponTemplateMapper.insert(template);
        log.info("优惠券模板创建成功: templateId={}, name={}", template.getId(), template.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(CouponTemplateUpdateDTO dto) {
        CouponTemplate template = getTemplateOrThrow(dto.getId());
        validateTemplateDTO(dto);

        // 保留已发放统计，仅更新可编辑配置
        template.setName(dto.getName());
        template.setType(dto.getType());
        template.setThresholdAmount(normalizeAmount(dto.getThresholdAmount()));
        template.setDiscountAmount(normalizeAmount(dto.getDiscountAmount()));
        template.setDiscountRate(dto.getDiscountRate());
        template.setTotalQuantity(dto.getTotalQuantity());
        template.setPerUserLimit(dto.getPerUserLimit());
        template.setValidityType(dto.getValidityType());
        template.setValidFrom(dto.getValidFrom());
        template.setValidTo(dto.getValidTo());
        template.setValidDays(dto.getValidDays());
        template.setStatus(dto.getStatus());
        template.setDescription(dto.getDescription());
        template.setAvailableWeekdays(normalizeAvailableWeekdays(dto.getAvailableWeekdays()));
        couponTemplateMapper.updateById(template);
        log.info("优惠券模板更新成功: templateId={}", dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplateStatus(Long id, Integer status) {
        getTemplateOrThrow(id);
        LambdaUpdateWrapper<CouponTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplate::getId, id)
                .set(CouponTemplate::getStatus, status);
        couponTemplateMapper.update(null, wrapper);
        log.info("优惠券模板状态更新成功: templateId={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CouponGrantTaskVO grantCoupons(CouponGrantDTO dto) {
        CouponTemplate template = getTemplateOrThrow(dto.getTemplateId());
        if (!Objects.equals(template.getStatus(), TEMPLATE_STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券模板未启用，不能发放");
        }
        CouponGrantTaskVO taskVO = couponGrantAsyncService.createGrantTask(template, dto);
        log.info("优惠券异步发券任务创建成功: taskId={}, templateId={}", taskVO.getId(), template.getId());
        return taskVO;
    }

    @Override
    public PageResult<CouponGrantTaskVO> pageGrantTasks(CouponGrantTaskQueryDTO dto) {
        return couponGrantAsyncService.pageGrantTasks(dto);
    }

    @Override
    public PageResult<CouponGrantTaskDetailVO> pageGrantTaskDetails(CouponGrantTaskDetailQueryDTO dto) {
        return couponGrantAsyncService.pageGrantTaskDetails(dto);
    }

    @Override
    public PageResult<UserCouponVO> pageUserCoupons(UserCouponQueryDTO dto) {
        Page<UserCoupon> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dto.getTemplateId() != null, UserCoupon::getTemplateId, dto.getTemplateId())
                .eq(dto.getUserId() != null, UserCoupon::getUserId, dto.getUserId())
                .eq(dto.getStatus() != null, UserCoupon::getStatus, dto.getStatus())
                .and(StrUtil.isNotBlank(dto.getKeyword()), keyword -> keyword
                        .like(UserCoupon::getCouponName, dto.getKeyword())
                        .or()
                        .like(UserCoupon::getUsername, dto.getKeyword())
                        .or()
                        .like(UserCoupon::getPhone, dto.getKeyword()))
                .orderByDesc(UserCoupon::getReceivedTime)
                .orderByDesc(UserCoupon::getCreateTime);

        Page<UserCoupon> result = userCouponMapper.selectPage(page, wrapper);
        List<UserCouponVO> records = BeanUtil.copyToList(result.getRecords(), UserCouponVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public PageResult<UserCouponVO> pageMyCoupons(Long userId, AppCouponQueryDTO dto) {
        refreshExpiredCoupons(userId);

        Page<UserCoupon> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<UserCoupon> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(dto.getStatus() != null, UserCoupon::getStatus, dto.getStatus())
                .orderByAsc(UserCoupon::getStatus)
                .orderByAsc(UserCoupon::getValidTo)
                .orderByDesc(UserCoupon::getReceivedTime);

        Page<UserCoupon> result = userCouponMapper.selectPage(page, wrapper);
        List<UserCouponVO> records = BeanUtil.copyToList(result.getRecords(), UserCouponVO.class);
        return PageResult.of(records, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon lockCoupon(Long userId, Long couponId, BigDecimal orderAmount, Long orderId) {
        refreshExpiredCoupons(userId);
        UserCoupon coupon = userCouponMapper.selectById(couponId);
        if (coupon == null || !Objects.equals(coupon.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券不存在");
        }
        if (!Objects.equals(coupon.getStatus(), USER_COUPON_STATUS_UNUSED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该优惠券当前不可用");
        }
        if (coupon.getValidTo() != null && coupon.getValidTo().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券已过期");
        }
        if (!isCouponAvailableToday(coupon)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该优惠券当前星期不可用");
        }
        if (coupon.getThresholdAmount() != null && orderAmount.compareTo(coupon.getThresholdAmount()) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前订单金额未达到优惠券使用门槛");
        }

        // 使用 CAS 锁券，避免同一张券被并发下单重复占用
        LambdaUpdateWrapper<UserCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserCoupon::getId, couponId)
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
                .set(UserCoupon::getStatus, USER_COUPON_STATUS_LOCKED)
                .set(UserCoupon::getOrderId, orderId);
        boolean updated = userCouponMapper.update(null, wrapper) > 0;
        if (!updated) {
            throw new BusinessException(ResultCode.ORDER_STATUS_ERROR, "优惠券锁定失败，请重试");
        }
        return userCouponMapper.selectById(couponId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCouponUsed(Long orderId) {
        LambdaUpdateWrapper<UserCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_LOCKED)
                .set(UserCoupon::getStatus, USER_COUPON_STATUS_USED)
                .set(UserCoupon::getUsedTime, LocalDateTime.now());
        userCouponMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseLockedCoupon(Long orderId) {
        LambdaUpdateWrapper<UserCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserCoupon::getOrderId, orderId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_LOCKED)
                .set(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
                .set(UserCoupon::getOrderId, null);
        userCouponMapper.update(null, wrapper);
    }

    /**
     * 查询优惠券模板实体
     *
     * @param templateId 模板ID
     * @return 优惠券模板
     * @author Henfon
     * @date 2026-07-02
     * @description 提供给会员权益同步发券等场景复用模板原始配置
     */
    @Override
    public CouponTemplate getTemplateEntity(Long templateId) {
        return getTemplateOrThrow(templateId);
    }

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
     * @description 复用模板规则直接生成单用户优惠券，并同步更新模板已发放数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserCoupon grantCouponToUser(Long templateId, SysUser user, Integer sourceType, String remark) {
        if (user == null || user.getId() == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户不存在");
        }

        CouponTemplate template = getTemplateOrThrow(templateId);
        if (!Objects.equals(template.getStatus(), TEMPLATE_STATUS_ENABLED)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券模板未启用");
        }
        if (template.getTotalQuantity() != null && template.getTotalQuantity() > 0
                && template.getIssuedQuantity() != null
                && template.getIssuedQuantity() >= template.getTotalQuantity()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "优惠券库存不足");
        }
        if (template.getPerUserLimit() != null && template.getPerUserLimit() > 0) {
            Long ownedCount = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getTemplateId, templateId)
                    .eq(UserCoupon::getUserId, user.getId()));
            if (ownedCount >= template.getPerUserLimit()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "已达到该优惠券领取上限");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        UserCoupon coupon = buildDirectUserCoupon(template, user, sourceType, remark, now);
        userCouponMapper.insert(coupon);

        LambdaUpdateWrapper<CouponTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplate::getId, template.getId())
                .set(CouponTemplate::getIssuedQuantity, (template.getIssuedQuantity() == null ? 0 : template.getIssuedQuantity()) + 1);
        couponTemplateMapper.update(null, wrapper);
        return coupon;
    }

    /**
     * 校验优惠券模板参数
     *
     * @param dto 模板参数
     * @author Henfon
     * @date 2026-06-26
     * @description 校验金额规则、有效期规则与库存规则
     */
    private void validateTemplateDTO(CouponTemplateCreateDTO dto) {
        BigDecimal thresholdAmount = normalizeAmount(dto.getThresholdAmount());
        BigDecimal discountAmount = normalizeAmount(dto.getDiscountAmount());

        if (Objects.equals(dto.getType(), COUPON_TYPE_FULL_REDUCTION)) {
            if (discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "满减券的优惠金额必须大于0");
            }
            if (dto.getDiscountRate() != null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "满减券不需要填写折扣比例");
            }
        } else if (Objects.equals(dto.getType(), COUPON_TYPE_DISCOUNT)) {
            if (dto.getDiscountRate() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "折扣券必须填写折扣比例");
            }
            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "折扣券不需要填写优惠金额");
            }
        }

        if (Objects.equals(dto.getValidityType(), VALIDITY_TYPE_FIXED)) {
            if (dto.getValidFrom() == null || dto.getValidTo() == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "固定有效期必须填写生效和失效时间");
            }
            if (!dto.getValidTo().isAfter(dto.getValidFrom())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "失效时间必须晚于生效时间");
            }
        } else if (Objects.equals(dto.getValidityType(), VALIDITY_TYPE_RELATIVE)) {
            if (dto.getValidDays() == null || dto.getValidDays() <= 0) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "领券后有效天数必须大于0");
            }
        }

        if (dto.getTotalQuantity() != null && dto.getTotalQuantity() > 0 && dto.getPerUserLimit() != null
                && dto.getPerUserLimit() > dto.getTotalQuantity()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "每人限领不能大于发放总量");
        }
        normalizeAvailableWeekdays(dto.getAvailableWeekdays());
        if (thresholdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "使用门槛不能小于0");
        }
    }

    /**
     * 获取模板并校验存在性
     *
     * @param templateId 模板ID
     * @return 优惠券模板
     * @author Henfon
     * @date 2026-06-26
     * @description 查询未删除模板，不存在时抛出业务异常
     */
    private CouponTemplate getTemplateOrThrow(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "优惠券模板不存在");
        }
        return template;
    }

    /**
     * 刷新用户已过期优惠券状态
     *
     * @param userId 用户ID
     * @author Henfon
     * @date 2026-06-26
     * @description 小程序查询前顺手将已过期未使用的券标记为过期
     */
    private void refreshExpiredCoupons(Long userId) {
        LambdaUpdateWrapper<UserCoupon> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getStatus, USER_COUPON_STATUS_UNUSED)
                .lt(UserCoupon::getValidTo, LocalDateTime.now())
                .set(UserCoupon::getStatus, USER_COUPON_STATUS_EXPIRED);
        userCouponMapper.update(null, wrapper);
    }

    /**
     * 构建单用户直发优惠券
     *
     * @param template 模板
     * @param user 用户
     * @param sourceType 来源类型
     * @param remark 备注
     * @param now 当前时间
     * @return 用户优惠券
     * @author Henfon
     * @date 2026-07-02
     * @description 同步发券场景下基于模板生成用户优惠券快照
     */
    private UserCoupon buildDirectUserCoupon(CouponTemplate template, SysUser user, Integer sourceType, String remark, LocalDateTime now) {
        LocalDateTime validFrom = Objects.equals(template.getValidityType(), VALIDITY_TYPE_FIXED) ? template.getValidFrom() : now;
        LocalDateTime validTo = Objects.equals(template.getValidityType(), VALIDITY_TYPE_FIXED)
                ? template.getValidTo()
                : now.plusDays(template.getValidDays() == null ? 1 : template.getValidDays());

        UserCoupon coupon = new UserCoupon();
        coupon.setTemplateId(template.getId());
        coupon.setUserId(user.getId());
        coupon.setUsername(user.getUsername());
        coupon.setNickname(user.getNickname());
        coupon.setPhone(user.getPhone());
        coupon.setCouponName(template.getName());
        coupon.setCouponType(template.getType());
        coupon.setThresholdAmount(normalizeAmount(template.getThresholdAmount()));
        coupon.setDiscountAmount(normalizeAmount(template.getDiscountAmount()));
        coupon.setDiscountRate(template.getDiscountRate());
        coupon.setSourceType(sourceType);
        coupon.setStatus(USER_COUPON_STATUS_UNUSED);
        coupon.setReceivedTime(now);
        coupon.setValidFrom(validFrom);
        coupon.setValidTo(validTo);
        coupon.setAvailableWeekdays(template.getAvailableWeekdays());
        coupon.setOrderId(null);
        coupon.setGrantTaskId(null);
        return coupon;
    }

    /**
     * 标准化金额空值
     *
     * @param amount 金额
     * @return 非空金额
     * @author Henfon
     * @date 2026-06-26
     * @description 将空金额统一转换为0，避免后续比较和展示报错
     */
    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    /**
     * 标准化可用星期配置
     *
     * @param availableWeekdays 原始配置
     * @return 标准化后的星期配置，空字符串返回 null
     * @author Henfon
     * @date 2026-06-27
     * @description 校验星期范围与格式，并统一为升序逗号分隔字符串
     */
    private String normalizeAvailableWeekdays(String availableWeekdays) {
        if (StrUtil.isBlank(availableWeekdays)) {
            return null;
        }

        Set<Integer> weekdaySet = StrUtil.splitTrim(availableWeekdays, ',').stream()
                .filter(StrUtil::isNotBlank)
                .map(value -> {
                    if (!StrUtil.isNumeric(value)) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "可用星期格式不正确");
                    }
                    int weekday = Integer.parseInt(value);
                    if (weekday < 1 || weekday > 7) {
                        throw new BusinessException(ResultCode.PARAM_ERROR, "可用星期必须在1到7之间");
                    }
                    return weekday;
                })
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        if (weekdaySet.isEmpty() || weekdaySet.size() == 7) {
            return null;
        }

        return weekdaySet.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 判断优惠券当天是否可用
     *
     * @param coupon 用户优惠券
     * @return 是否可用
     * @author Henfon
     * @date 2026-06-27
     * @description 根据用户券快照中的可用星期限制判断当前日期是否允许使用
     */
    private boolean isCouponAvailableToday(UserCoupon coupon) {
        if (coupon == null || StrUtil.isBlank(coupon.getAvailableWeekdays())) {
            return true;
        }

        int currentWeekday = DayOfWeek.from(LocalDateTime.now()).getValue();
        List<String> availableWeekdayList = StrUtil.splitTrim(coupon.getAvailableWeekdays(), ',');
        return availableWeekdayList.contains(String.valueOf(currentWeekday));
    }

    /**
     * 获取整数默认值
     *
     * @param value 原值
     * @return 非空整数
     * @author Henfon
     * @date 2026-06-26
     * @description 将可空整数统一转为0以便计算
     */
    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }
}
