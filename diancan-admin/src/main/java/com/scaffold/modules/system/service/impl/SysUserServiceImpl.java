package com.scaffold.modules.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scaffold.common.enums.StatusEnum;
import com.scaffold.common.exception.BusinessException;
import com.scaffold.common.result.PageResult;
import com.scaffold.common.result.ResultCode;
import com.scaffold.modules.system.dto.*;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.entity.SysUserRole;
import com.scaffold.modules.system.mapper.SysUserMapper;
import com.scaffold.modules.system.mapper.SysUserRoleMapper;
import com.scaffold.modules.system.service.PermissionCacheService;
import com.scaffold.modules.system.service.SysLoginLogService;
import com.scaffold.modules.system.service.SysUserService;
import com.scaffold.modules.system.service.WechatApiService;
import com.scaffold.modules.system.vo.LoginVO;
import com.scaffold.modules.system.vo.UserInfoVO;
import com.scaffold.modules.system.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现
 *
 * @author Henfon
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final String USER_TYPE_BACKEND = "BACKEND";
    private static final String USER_TYPE_APP = "APP";
    private static final String USER_TYPE_STRESS = "STRESS";

    private final SysUserRoleMapper userRoleMapper;
    private final PermissionCacheService permissionCacheService;
    private final SysLoginLogService loginLogService;
    private final WechatApiService wechatApiService;

    /**
     * 默认角色ID（普通用户）
     */
    private static final Long DEFAULT_ROLE_ID = 2L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO dto) {
        // 检查用户名是否存在
        if (existsByUsername(dto.getUsername())) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getUsername());
        user.setStatus(StatusEnum.ENABLED.getValue());
        user.setUserType(USER_TYPE_BACKEND);
        save(user);

        // 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(DEFAULT_ROLE_ID);
        userRoleMapper.insert(userRole);

        log.info("用户注册成功: {}", dto.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 查询用户
        SysUser user = getByUsername(dto.getUsername());
        if (user == null) {
            loginLogService.recordLoginLog(dto.getUsername(), 0, "user_not_found");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            loginLogService.recordLoginLog(dto.getUsername(), 0, "password_error");
            throw new BusinessException(ResultCode.LOGIN_ERROR);
        }

        // 检查状态
        if (!StatusEnum.isEnabled(user.getStatus())) {
            loginLogService.recordLoginLog(dto.getUsername(), 0, "account_disabled");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 登录
        StpUtil.login(user.getId());

        // 缓存用户权限
        permissionCacheService.cacheUserPermissions(user.getId());

        // 构建返回结果
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setToken(StpUtil.getTokenValue());

        loginLogService.recordLoginLog(dto.getUsername(), 1, "login_success");

        log.info("用户登录成功: {}", dto.getUsername());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO loginByPhone(PhoneLoginDTO dto) {
        // 1. 调用微信 code2Session 获取 openid
        WechatApiService.WechatSessionInfo sessionInfo = wechatApiService.code2Session(dto.getCode());

        // 2. 调用微信接口获取手机号
        String phone = wechatApiService.getPhoneNumber(dto.getPhoneCode());

        // 3. 根据手机号查找或创建用户
        SysUser user = getByPhone(phone);
        boolean isNewUser = (user == null);

        if (isNewUser) {
            user = createWechatUser(phone);
            log.info("新用户通过手机号注册: phone={}", phone);
        }

        // 4. 更新 openid（每次登录都更新，因为 openid 可能变化）
        user.setOpenid(sessionInfo.openid());
        updateById(user);

        // 5. 检查状态
        if (!StatusEnum.isEnabled(user.getStatus())) {
            loginLogService.recordLoginLog(user.getUsername(), 0, "account_disabled");
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 6. 登录（以 userId 为 loginId）
        StpUtil.login(user.getId());
        // 将 openid 存入 Session，方便 app 端接口获取
        StpUtil.getSession().set("openid", sessionInfo.openid());

        // 7. 缓存用户权限
        permissionCacheService.cacheUserPermissions(user.getId());

        // 8. 构建返回
        LoginVO vo = new LoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setToken(StpUtil.getTokenValue());

        loginLogService.recordLoginLog(user.getUsername(), 1, "phone_login_success");
        log.info("用户手机号登录成功: phone={}, userId={}", phone, user.getId());
        return vo;
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(getUserRoles(userId));
        vo.setPermissions(getUserPermissions(userId));
        return vo;
    }

    @Override
    public void updateUserInfo(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = new SysUser();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        updateById(user);
        log.info("用户信息更新成功: {}", userId);
    }

    @Override
    public void updatePassword(PasswordUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        // 验证原密码
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }

        // 更新密码
        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        updateById(updateUser);
        log.info("用户密码修改成功: {}", userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(newPassword));
        updateById(updateUser);

        // 重置密码后踢下线，要求重新登录
        StpUtil.kickout(userId);
        permissionCacheService.clearUserCache(userId);
        log.info("管理员重置用户密码成功: userId={}", userId);
    }

    @Override
    public PageResult<UserVO> pageList(UserQueryDTO dto) {
        Page<SysUser> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(dto.getUsername()), SysUser::getUsername, dto.getUsername())
                .eq(dto.getStatus() != null, SysUser::getStatus, dto.getStatus())
                .eq(StrUtil.isNotBlank(dto.getUserType()), SysUser::getUserType, dto.getUserType())
                .in(Boolean.TRUE.equals(dto.getMemberOnly()) && StrUtil.isBlank(dto.getUserType()),
                        SysUser::getUserType, USER_TYPE_APP, USER_TYPE_STRESS)
                .ge(dto.getStartTime() != null, SysUser::getCreateTime, dto.getStartTime())
                .le(dto.getEndTime() != null, SysUser::getCreateTime, dto.getEndTime())
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = page(page, wrapper);
        List<UserVO> voList = BeanUtil.copyToList(result.getRecords(), UserVO.class);
        return PageResult.of(voList, result.getCurrent(), result.getSize(), result.getTotal());
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        // 不能禁用自己
        if (currentUserId.equals(userId) && StatusEnum.DISABLED.getValue().equals(status)) {
            throw new BusinessException(ResultCode.CANNOT_DISABLE_SELF);
        }

        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(status);
        updateById(user);

        // 如果是禁用，踢下线
        if (StatusEnum.DISABLED.getValue().equals(status)) {
            StpUtil.kickout(userId);
            permissionCacheService.clearUserCache(userId);
        }
        log.info("用户状态更新: userId={}, status={}", userId, status);
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        return baseMapper.selectPermissionsByUserId(userId);
    }

    /**
     * 根据用户名查询用户
     */
    private SysUser getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    /**
     * 检查用户名是否存在
     */
    private boolean existsByUsername(String username) {
        return count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
    }

    /**
     * 根据手机号查询用户
     */
    private SysUser getByPhone(String phone) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, phone));
    }

    /**
     * 为微信小程序用户创建账号（手机号即用户名）
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUser createWechatUser(String phone) {
        SysUser user = new SysUser();
        user.setUsername(phone);
        // 微信登录用户无密码，用随机串占位防止通过密码登录
        user.setPassword(BCrypt.hashpw(StrUtil.uuid()));
        user.setPhone(phone);
        user.setNickname("微信用户");
        user.setStatus(StatusEnum.ENABLED.getValue());
        user.setUserType(USER_TYPE_APP);
        save(user);

        // 分配默认角色
        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(DEFAULT_ROLE_ID);
        userRoleMapper.insert(userRole);

        return user;
    }
}
