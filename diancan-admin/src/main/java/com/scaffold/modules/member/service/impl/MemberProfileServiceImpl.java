package com.scaffold.modules.member.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.member.entity.MemberLevel;
import com.scaffold.modules.member.entity.MemberLevelChangeLog;
import com.scaffold.modules.member.entity.MemberGrowthRecord;
import com.scaffold.modules.member.entity.MemberPointsRecord;
import com.scaffold.modules.member.entity.MemberProfile;
import com.scaffold.modules.member.mapper.MemberLevelChangeLogMapper;
import com.scaffold.modules.member.mapper.MemberLevelMapper;
import com.scaffold.modules.member.mapper.MemberGrowthRecordMapper;
import com.scaffold.modules.member.mapper.MemberPointsRecordMapper;
import com.scaffold.modules.member.mapper.MemberProfileMapper;
import com.scaffold.modules.order.entity.Order;
import com.scaffold.modules.order.service.OrderService;
import com.scaffold.modules.member.service.MemberGrowthService;
import com.scaffold.modules.member.service.MemberLevelService;
import com.scaffold.modules.member.service.MemberPointsService;
import com.scaffold.modules.member.service.MemberProfileService;
import com.scaffold.modules.member.constant.MemberBizTypeConstants;
import com.scaffold.modules.member.vo.AppMemberCenterVO;
import com.scaffold.modules.member.vo.MemberDetailVO;
import com.scaffold.modules.member.vo.MemberGrowthRecordVO;
import com.scaffold.modules.member.vo.MemberLevelChangeLogVO;
import com.scaffold.modules.member.vo.MemberLevelDistributionVO;
import com.scaffold.modules.member.vo.MemberOverviewVO;
import com.scaffold.modules.member.vo.MemberOrderContributionVO;
import com.scaffold.modules.member.vo.MemberPointsRecordVO;
import com.scaffold.modules.member.vo.MemberProfileVO;
import com.scaffold.modules.member.vo.MemberTrendPointVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会员档案服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    private static final DateTimeFormatter MEMBER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MemberProfileMapper memberProfileMapper;
    private final MemberLevelMapper memberLevelMapper;
    private final MemberLevelChangeLogMapper memberLevelChangeLogMapper;
    private final MemberPointsRecordMapper memberPointsRecordMapper;
    private final MemberGrowthRecordMapper memberGrowthRecordMapper;
    private final MemberLevelService memberLevelService;
    private final MemberPointsService memberPointsService;
    private final MemberGrowthService memberGrowthService;
    private final OrderService orderService;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<MemberProfileVO> pageList(com.scaffold.modules.member.dto.MemberQueryDTO dto) {
        LambdaQueryWrapper<MemberProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getMemberNo()), MemberProfile::getMemberNo, dto.getMemberNo())
                .eq(dto.getLevelId() != null, MemberProfile::getLevelId, dto.getLevelId())
                .eq(dto.getStatus() != null, MemberProfile::getStatus, dto.getStatus())
                .orderByDesc(MemberProfile::getCreateTime);

        List<Long> matchedUserIds = queryUserIdsByCondition(dto.getNickname(), dto.getPhone());
        if (matchedUserIds != null) {
            if (matchedUserIds.isEmpty()) {
                return PageResult.of(Collections.emptyList(), Long.valueOf(dto.getPageNum()), Long.valueOf(dto.getPageSize()), 0L);
            }
            wrapper.in(MemberProfile::getUserId, matchedUserIds);
        }

        Page<MemberProfile> page = memberProfileMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        Map<Long, SysUser> userMap = loadUserMap(page.getRecords().stream().map(MemberProfile::getUserId).collect(Collectors.toSet()));
        Map<Long, MemberLevel> levelMap = loadLevelMap(page.getRecords().stream().map(MemberProfile::getLevelId).collect(Collectors.toSet()));

        List<MemberProfileVO> list = page.getRecords().stream().map(item -> toProfileVO(item, userMap, levelMap)).toList();
        return PageResult.of(list, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询会员详情
     *
     * @param id 会员ID
     * @return 会员详情
     * @author Henfon
     * @date 2026-06-30
     * @description 组装会员基础信息、当前等级信息以及最近的积分、成长值和等级变更流水
     */
    @Override
    public MemberDetailVO getDetail(Long id) {
        MemberProfile profile = memberProfileMapper.selectById(id);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }

        MemberDetailVO detailVO = new MemberDetailVO();
        MemberProfileVO profileVO = toProfileVO(profile,
                loadUserMap(Set.of(profile.getUserId())),
                loadLevelMap(Set.of(profile.getLevelId())));
        BeanUtil.copyProperties(profileVO, detailVO);
        detailVO.setTotalPointsEarned(profile.getTotalPointsEarned());
        detailVO.setTotalPointsUsed(profile.getTotalPointsUsed());

        MemberLevel level = memberLevelMapper.selectById(profile.getLevelId());
        if (level != null) {
            detailVO.setCurrentLevelPointsRate(level.getPointsRate());
            detailVO.setCurrentLevelDiscountRate(level.getDiscountRate());
            detailVO.setCurrentLevelBenefitConfig(level.getBenefitConfig());
        }

        com.scaffold.modules.member.dto.MemberPointsRecordQueryDTO pointsQueryDTO = new com.scaffold.modules.member.dto.MemberPointsRecordQueryDTO();
        pointsQueryDTO.setMemberId(profile.getId());
        pointsQueryDTO.setPageNum(1);
        pointsQueryDTO.setPageSize(5);
        com.scaffold.modules.member.dto.MemberGrowthRecordQueryDTO growthQueryDTO = new com.scaffold.modules.member.dto.MemberGrowthRecordQueryDTO();
        growthQueryDTO.setMemberId(profile.getId());
        growthQueryDTO.setPageNum(1);
        growthQueryDTO.setPageSize(5);
        detailVO.setRecentPointsRecords(memberPointsService.pageAdmin(pointsQueryDTO).getList());
        detailVO.setRecentGrowthRecords(memberGrowthService.pageAdmin(growthQueryDTO).getList());
        detailVO.setRecentLevelChangeLogs(queryRecentLevelChangeLogs(profile.getId()));
        detailVO.setRecentOrderContributions(queryRecentOrderContributions(profile.getId()));
        return detailVO;
    }

    /**
     * 获取或创建会员档案
     *
     * @param userId 用户ID
     * @return 会员档案
     * @author Henfon
     * @date 2026-06-30
     * @description 当用户首次消费或查看会员中心时自动初始化会员档案
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberProfile getOrCreateByUserId(Long userId) {
        MemberProfile exist = memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getUserId, userId)
                .last("LIMIT 1"));
        if (exist != null) {
            return exist;
        }

        MemberLevel defaultLevel = memberLevelService.getDefaultLevel();
        MemberProfile profile = new MemberProfile();
        profile.setUserId(userId);
        profile.setMemberNo(generateMemberNo());
        profile.setLevelId(defaultLevel.getId());
        profile.setGrowthValue(0);
        profile.setPointsBalance(0);
        profile.setTotalPointsEarned(0);
        profile.setTotalPointsUsed(0);
        profile.setTotalAmountConsumed(BigDecimal.ZERO);
        profile.setStatus(1);
        profile.setRegisterSource("miniapp");

        try {
            memberProfileMapper.insert(profile);
            return profile;
        } catch (DuplicateKeyException e) {
            log.warn("会员档案并发创建命中唯一索引: userId={}", userId);
            return memberProfileMapper.selectOne(new LambdaQueryWrapper<MemberProfile>()
                    .eq(MemberProfile::getUserId, userId)
                    .last("LIMIT 1"));
        }
    }

    @Override
    public MemberProfile getByIdForUpdate(Long id) {
        MemberProfile profile = memberProfileMapper.selectByIdForUpdate(id);
        if (profile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会员不存在");
        }
        return profile;
    }

    /**
     * 查询当前登录用户的会员中心信息
     *
     * @param userId 用户ID
     * @return 会员中心信息
     * @author Henfon
     * @date 2026-06-30
     * @description 自动补齐下一等级信息，供小程序直接展示进度和权益
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppMemberCenterVO getCurrentMemberCenter(Long userId) {
        MemberProfile profile = getOrCreateByUserId(userId);
        MemberLevel currentLevel = memberLevelMapper.selectById(profile.getLevelId());
        List<MemberLevel> levels = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevel>()
                .eq(MemberLevel::getStatus, 1)
                .orderByAsc(MemberLevel::getGrowthThreshold)
                .orderByAsc(MemberLevel::getSort));

        AppMemberCenterVO vo = new AppMemberCenterVO();
        vo.setMemberId(profile.getId());
        vo.setMemberNo(profile.getMemberNo());
        vo.setLevelId(profile.getLevelId());
        vo.setLevelName(currentLevel == null ? null : currentLevel.getLevelName());
        vo.setGrowthValue(profile.getGrowthValue());
        vo.setPointsBalance(profile.getPointsBalance());
        vo.setTotalAmountConsumed(profile.getTotalAmountConsumed());
        if (currentLevel != null) {
            vo.setPointsRate(currentLevel.getPointsRate());
            vo.setDiscountRate(currentLevel.getDiscountRate());
        }

        MemberLevel nextLevel = levels.stream()
                .filter(level -> level.getGrowthThreshold() != null
                        && level.getGrowthThreshold() > (profile.getGrowthValue() == null ? 0 : profile.getGrowthValue()))
                .findFirst()
                .orElse(null);
        if (nextLevel != null) {
            vo.setNextLevelName(nextLevel.getLevelName());
            vo.setNextLevelThreshold(nextLevel.getGrowthThreshold());
            vo.setPointsToNextLevel(nextLevel.getGrowthThreshold() - (profile.getGrowthValue() == null ? 0 : profile.getGrowthValue()));
        } else {
            vo.setNextLevelName(currentLevel == null ? null : currentLevel.getLevelName());
            vo.setNextLevelThreshold(profile.getGrowthValue());
            vo.setPointsToNextLevel(0);
        }
        return vo;
    }

    /**
     * 查询会员统计总览
     *
     * @return 会员统计数据
     * @author Henfon
     * @date 2026-07-01
     * @description 汇总会员规模、等级分布、积分资产和近7天新增趋势，供管理端统计页直接展示
     */
    @Override
    public MemberOverviewVO getOverview() {
        List<MemberProfile> profiles = memberProfileMapper.selectList(new LambdaQueryWrapper<MemberProfile>()
                .eq(MemberProfile::getDeleted, 0));
        MemberOverviewVO vo = new MemberOverviewVO();
        if (profiles.isEmpty()) {
            vo.setTotalMembers(0L);
            vo.setActiveMembers(0L);
            vo.setFrozenMembers(0L);
            vo.setTotalPointsBalance(0L);
            vo.setTotalGrowthValue(0L);
            vo.setTotalAmountConsumed(BigDecimal.ZERO);
            vo.setRecentNewMembers(0L);
            vo.setLevelDistribution(Collections.emptyList());
            vo.setRecentTrend(Collections.emptyList());
            return vo;
        }

        long totalMembers = profiles.size();
        long activeMembers = profiles.stream().filter(item -> item.getStatus() != null && item.getStatus() == 1).count();
        long frozenMembers = profiles.stream().filter(item -> item.getStatus() != null && item.getStatus() == 0).count();
        long totalPointsBalance = profiles.stream().mapToLong(item -> item.getPointsBalance() == null ? 0 : item.getPointsBalance()).sum();
        long totalGrowthValue = profiles.stream().mapToLong(item -> item.getGrowthValue() == null ? 0 : item.getGrowthValue()).sum();
        BigDecimal totalAmountConsumed = profiles.stream()
                .map(item -> item.getTotalAmountConsumed() == null ? BigDecimal.ZERO : item.getTotalAmountConsumed())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate sevenDaysAgo = LocalDate.now().minusDays(6);
        long recentNewMembers = profiles.stream()
                .filter(item -> item.getCreateTime() != null && !item.getCreateTime().toLocalDate().isBefore(sevenDaysAgo))
                .count();

        vo.setTotalMembers(totalMembers);
        vo.setActiveMembers(activeMembers);
        vo.setFrozenMembers(frozenMembers);
        vo.setTotalPointsBalance(totalPointsBalance);
        vo.setTotalGrowthValue(totalGrowthValue);
        vo.setTotalAmountConsumed(totalAmountConsumed);
        vo.setRecentNewMembers(recentNewMembers);
        vo.setLevelDistribution(buildLevelDistribution(profiles));
        vo.setRecentTrend(buildRecentTrend(profiles, sevenDaysAgo));
        return vo;
    }

    /**
     * 生成会员编号
     *
     * @return 会员编号
     * @author Henfon
     * @date 2026-06-30
     * @description 使用时间戳加短雪花序列，生成具备可读性的会员编号
     */
    private String generateMemberNo() {
        String suffix = String.valueOf(IdUtil.getSnowflakeNextId()).substring(7, 13);
        return "MEM" + LocalDateTime.now().format(MEMBER_NO_FORMATTER) + suffix;
    }

    /**
     * 构建等级分布
     *
     * @param profiles 会员列表
     * @return 等级分布
     * @author Henfon
     * @date 2026-07-01
     * @description 按等级聚合会员数量，并补齐等级名称，便于前端直接展示柱状分布
     */
    private List<MemberLevelDistributionVO> buildLevelDistribution(List<MemberProfile> profiles) {
        Map<Long, Long> levelCountMap = profiles.stream()
                .filter(item -> item.getLevelId() != null)
                .collect(Collectors.groupingBy(MemberProfile::getLevelId, Collectors.counting()));
        if (levelCountMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, MemberLevel> levelMap = loadLevelMap(levelCountMap.keySet());
        return levelCountMap.entrySet().stream()
                .map(entry -> {
                    MemberLevelDistributionVO item = new MemberLevelDistributionVO();
                    item.setLevelId(entry.getKey());
                    MemberLevel level = levelMap.get(entry.getKey());
                    item.setLevelName(level == null ? "未命名等级" : level.getLevelName());
                    item.setMemberCount(entry.getValue());
                    return item;
                })
                .sorted((left, right) -> Long.compare(
                        right.getMemberCount() == null ? 0 : right.getMemberCount(),
                        left.getMemberCount() == null ? 0 : left.getMemberCount()))
                .toList();
    }

    /**
     * 构建近7天新增趋势
     *
     * @param profiles 会员列表
     * @param startDate 起始日期
     * @return 趋势列表
     * @author Henfon
     * @date 2026-07-01
     * @description 以天为维度输出近7天新增会员数，缺失日期补0
     */
    private List<MemberTrendPointVO> buildRecentTrend(List<MemberProfile> profiles, LocalDate startDate) {
        Map<LocalDate, Long> trendMap = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            trendMap.put(startDate.plusDays(i), 0L);
        }

        for (MemberProfile profile : profiles) {
            if (profile.getCreateTime() == null) {
                continue;
            }
            LocalDate date = profile.getCreateTime().toLocalDate();
            if (date.isBefore(startDate) || !trendMap.containsKey(date)) {
                continue;
            }
            trendMap.put(date, trendMap.get(date) + 1);
        }

        List<MemberTrendPointVO> result = new ArrayList<>();
        trendMap.forEach((date, count) -> {
            MemberTrendPointVO point = new MemberTrendPointVO();
            point.setDate(date.toString());
            point.setNewMemberCount(count);
            result.add(point);
        });
        return result;
    }

    private MemberProfileVO toProfileVO(MemberProfile profile, Map<Long, SysUser> userMap, Map<Long, MemberLevel> levelMap) {
        MemberProfileVO vo = BeanUtil.copyProperties(profile, MemberProfileVO.class);
        SysUser user = userMap.get(profile.getUserId());
        MemberLevel level = levelMap.get(profile.getLevelId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setPhone(user.getPhone());
        }
        if (level != null) {
            vo.setLevelName(level.getLevelName());
        }
        return vo;
    }

    private List<Long> queryUserIdsByCondition(String nickname, String phone) {
        if (StrUtil.isBlank(nickname) && StrUtil.isBlank(phone)) {
            return null;
        }
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .like(StrUtil.isNotBlank(nickname), SysUser::getNickname, nickname)
                        .like(StrUtil.isNotBlank(phone), SysUser::getPhone, phone))
                .stream()
                .map(SysUser::getId)
                .toList();
    }

    private Map<Long, SysUser> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sysUserMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
    }

    private Map<Long, MemberLevel> loadLevelMap(Set<Long> levelIds) {
        if (levelIds == null || levelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return memberLevelMapper.selectBatchIds(levelIds).stream()
                .collect(Collectors.toMap(MemberLevel::getId, Function.identity()));
    }

    /**
     * 查询最近等级变更日志
     *
     * @param memberId 会员ID
     * @return 等级变更日志列表
     * @author Henfon
     * @date 2026-07-01
     * @description 会员详情页展示最近升级轨迹，帮助运营快速定位等级变化来源
     */
    private List<MemberLevelChangeLogVO> queryRecentLevelChangeLogs(Long memberId) {
        List<MemberLevelChangeLog> logs = memberLevelChangeLogMapper.selectList(new LambdaQueryWrapper<MemberLevelChangeLog>()
                .eq(MemberLevelChangeLog::getMemberId, memberId)
                .orderByDesc(MemberLevelChangeLog::getCreateTime)
                .last("LIMIT 5"));
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> levelIds = logs.stream()
                .flatMap(item -> java.util.stream.Stream.of(item.getOldLevelId(), item.getNewLevelId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, MemberLevel> levelMap = loadLevelMap(levelIds);

        return logs.stream().map(item -> {
            MemberLevelChangeLogVO vo = BeanUtil.copyProperties(item, MemberLevelChangeLogVO.class);
            MemberLevel oldLevel = levelMap.get(item.getOldLevelId());
            MemberLevel newLevel = levelMap.get(item.getNewLevelId());
            vo.setOldLevelName(oldLevel == null ? null : oldLevel.getLevelName());
            vo.setNewLevelName(newLevel == null ? null : newLevel.getLevelName());
            return vo;
        }).toList();
    }

    /**
     * 查询最近订单贡献
     *
     * @param memberId 会员ID
     * @return 最近订单贡献列表
     * @author Henfon
     * @date 2026-07-01
     * @description 从支付奖励类积分与成长流水反查订单，帮助运营快速判断会员最近消费贡献
     */
    private List<MemberOrderContributionVO> queryRecentOrderContributions(Long memberId) {
        List<MemberPointsRecord> pointRecords = memberPointsRecordMapper.selectList(new LambdaQueryWrapper<MemberPointsRecord>()
                .eq(MemberPointsRecord::getMemberId, memberId)
                .eq(MemberPointsRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                .orderByDesc(MemberPointsRecord::getCreateTime)
                .last("LIMIT 10"));
        if (pointRecords.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, MemberPointsRecord> pointsRecordMap = pointRecords.stream()
                .filter(item -> item.getBizId() != null)
                .collect(Collectors.toMap(MemberPointsRecord::getBizId, Function.identity(), (left, right) -> left));
        if (pointsRecordMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = pointRecords.stream()
                .map(MemberPointsRecord::getBizId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, MemberGrowthRecord> growthRecordMap = memberGrowthRecordMapper.selectList(new LambdaQueryWrapper<MemberGrowthRecord>()
                        .eq(MemberGrowthRecord::getMemberId, memberId)
                        .eq(MemberGrowthRecord::getBizType, MemberBizTypeConstants.ORDER_PAY)
                        .in(MemberGrowthRecord::getBizId, orderIds))
                .stream()
                .filter(item -> item.getBizId() != null)
                .collect(Collectors.toMap(MemberGrowthRecord::getBizId, Function.identity(), (left, right) -> left));
        Map<Long, Order> orderMap = orderService.listByIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity(), (left, right) -> left));

        return orderIds.stream().map(orderId -> {
            MemberPointsRecord pointsRecord = pointsRecordMap.get(orderId);
            MemberGrowthRecord growthRecord = growthRecordMap.get(orderId);
            Order order = orderMap.get(orderId);
            MemberOrderContributionVO vo = new MemberOrderContributionVO();
            vo.setOrderId(orderId);
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setActualAmount(order.getActualAmount());
                vo.setPaidAmount(order.getPaidAmount());
                vo.setCreateTime(order.getCreateTime() == null ? null : order.getCreateTime().toString());
            }
            vo.setPointsReward(pointsRecord == null ? 0 : pointsRecord.getChangeAmount());
            vo.setGrowthReward(growthRecord == null ? 0 : growthRecord.getChangeAmount());
            return vo;
        }).limit(5).toList();
    }
}
