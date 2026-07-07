package com.scaffold.modules.feedback.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.feedback.dto.FeedbackCreateDTO;
import com.scaffold.modules.feedback.dto.FeedbackQueryDTO;
import com.scaffold.modules.feedback.dto.FeedbackReplyDTO;
import com.scaffold.modules.feedback.entity.UserFeedback;
import com.scaffold.modules.feedback.mapper.UserFeedbackMapper;
import com.scaffold.modules.feedback.service.FeedbackService;
import com.scaffold.modules.feedback.vo.AdminFeedbackListVO;
import com.scaffold.modules.feedback.vo.FeedbackVO;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 反馈服务实现
 *
 * @author Henfon
 */
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final UserFeedbackMapper userFeedbackMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：提交用户反馈并快照用户手机号
     *
     * @param openid 当前用户 openid
     * @param dto 反馈参数
     * @return 反馈信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FeedbackVO submitFeedback(String openid, FeedbackCreateDTO dto) {
        UserFeedback feedback = new UserFeedback();
        feedback.setCustomerOpenid(openid);
        feedback.setContactPhone(dto.getContactPhone());
        feedback.setContent(dto.getContent());
        feedback.setStatus(0);

        // 快照当前用户手机号，方便后台定位反馈人。
        SysUser user = getUserByOpenid(openid);
        if (user != null) {
            feedback.setCustomerPhone(user.getPhone());
        }

        userFeedbackMapper.insert(feedback);
        return toFeedbackVO(feedback);
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询当前用户反馈记录
     *
     * @param openid 当前用户 openid
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 反馈分页列表
     */
    @Override
    public PageResult<FeedbackVO> listMyFeedback(String openid, int pageNum, int pageSize) {
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFeedback::getCustomerOpenid, openid)
                .orderByDesc(UserFeedback::getCreateTime);

        Page<UserFeedback> pageResult = userFeedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<FeedbackVO> voList = pageResult.getRecords().stream()
                .map(this::toFeedbackVO)
                .toList();

        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：分页查询管理端反馈列表，并补充用户昵称
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param queryDTO 查询参数
     * @return 反馈分页列表
     */
    @Override
    public PageResult<AdminFeedbackListVO> listFeedbackForAdmin(int pageNum, int pageSize, FeedbackQueryDTO queryDTO) {
        LambdaQueryWrapper<UserFeedback> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            wrapper.eq(queryDTO.getStatus() != null, UserFeedback::getStatus, queryDTO.getStatus())
                    .like(StringUtils.hasText(queryDTO.getKeyword()), UserFeedback::getContent, queryDTO.getKeyword())
                    .like(StringUtils.hasText(queryDTO.getContactPhone()), UserFeedback::getContactPhone, queryDTO.getContactPhone());
            if (queryDTO.getStartDate() != null) {
                wrapper.ge(UserFeedback::getCreateTime, queryDTO.getStartDate().atStartOfDay());
            }
            if (queryDTO.getEndDate() != null) {
                wrapper.lt(UserFeedback::getCreateTime, queryDTO.getEndDate().plusDays(1).atStartOfDay());
            }
        }
        wrapper.orderByAsc(UserFeedback::getStatus)
                .orderByDesc(UserFeedback::getCreateTime);

        Page<UserFeedback> pageResult = userFeedbackMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, SysUser> userMap = loadUserMap(pageResult.getRecords());

        List<AdminFeedbackListVO> voList = pageResult.getRecords().stream()
                .map(item -> toAdminVO(item, userMap.get(item.getCustomerOpenid())))
                .toList();

        return PageResult.of(voList, pageResult.getCurrent(), pageResult.getSize(), pageResult.getTotal());
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：回复指定反馈
     *
     * @param feedbackId 反馈ID
     * @param dto 回复参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replyFeedback(Long feedbackId, FeedbackReplyDTO dto) {
        UserFeedback feedback = userFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 直接覆盖回复内容，便于后台修正措辞。
        feedback.setReplyContent(dto.getReplyContent());
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus(1);
        userFeedbackMapper.updateById(feedback);
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：根据 openid 查询用户
     *
     * @param openid 用户 openid
     * @return 用户信息
     */
    private SysUser getUserByOpenid(String openid) {
        if (!StringUtils.hasText(openid)) {
            return null;
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getOpenid, openid).last("LIMIT 1");
        return sysUserMapper.selectOne(wrapper);
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：批量加载反馈人的用户信息
     *
     * @param feedbackList 反馈列表
     * @return openid 到用户的映射
     */
    private Map<String, SysUser> loadUserMap(List<UserFeedback> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> openids = feedbackList.stream()
                .map(UserFeedback::getCustomerOpenid)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (openids.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getOpenid, openids);
        return sysUserMapper.selectList(wrapper).stream()
                .filter(item -> StringUtils.hasText(item.getOpenid()))
                .collect(Collectors.toMap(SysUser::getOpenid, Function.identity(), (left, right) -> left));
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：转换小程序反馈 VO
     *
     * @param feedback 反馈实体
     * @return 反馈 VO
     */
    private FeedbackVO toFeedbackVO(UserFeedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setContent(feedback.getContent());
        vo.setContactPhone(feedback.getContactPhone());
        vo.setStatus(feedback.getStatus());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setCreateTime(feedback.getCreateTime());
        return vo;
    }

    /**
     * 作者：Henfon
     * 日期：2026/06/27
     * 描述：转换管理端反馈列表 VO
     *
     * @param feedback 反馈实体
     * @param user 用户信息
     * @return 管理端反馈 VO
     */
    private AdminFeedbackListVO toAdminVO(UserFeedback feedback, SysUser user) {
        AdminFeedbackListVO vo = new AdminFeedbackListVO();
        vo.setId(feedback.getId());
        vo.setContent(feedback.getContent());
        vo.setContactPhone(feedback.getContactPhone());
        vo.setCustomerPhone(feedback.getCustomerPhone());
        vo.setCustomerOpenid(feedback.getCustomerOpenid());
        vo.setStatus(feedback.getStatus());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setCreateTime(feedback.getCreateTime());
        if (user != null) {
            vo.setCustomerNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            if (!StringUtils.hasText(vo.getCustomerPhone())) {
                vo.setCustomerPhone(user.getPhone());
            }
        }
        return vo;
    }
}
