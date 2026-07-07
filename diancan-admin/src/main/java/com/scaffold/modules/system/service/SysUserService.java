package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.*;
import com.scaffold.modules.system.entity.SysUser;
import com.scaffold.modules.system.vo.LoginVO;
import com.scaffold.modules.system.vo.UserInfoVO;
import com.scaffold.modules.system.vo.UserVO;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author Henfon
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户注册
     *
     * @param dto 注册信息
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录
     *
     * @param dto 登录信息
     * @return 登录结果
     */
    LoginVO login(LoginDTO dto);

    /**
     * 小程序手机号登录（微信手机号授权）
     *
     * @param dto 登录信息（code + phoneCode）
     * @return 登录结果
     */
    LoginVO loginByPhone(PhoneLoginDTO dto);

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    UserInfoVO getCurrentUserInfo();

    /**
     * 更新用户信息
     *
     * @param dto 用户信息
     */
    void updateUserInfo(UserUpdateDTO dto);

    /**
     * 修改密码
     *
     * @param dto 密码信息
     */
    void updatePassword(PasswordUpdateDTO dto);

    /**
     * 管理员重置用户密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 分页查询用户列表
     *
     * @param dto 查询条件
     * @return 用户列表
     */
    PageResult<UserVO> pageList(UserQueryDTO dto);

    /**
     * 启用/禁用用户
     *
     * @param userId 用户ID
     * @param status 状态
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 获取用户角色编码列表
     *
     * @param userId 用户ID
     * @return 角色编码列表
     */
    List<String> getUserRoles(Long userId);

    /**
     * 获取用户权限标识列表
     *
     * @param userId 用户ID
     * @return 权限标识列表
     */
    List<String> getUserPermissions(Long userId);
}
