package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.system.dto.RoleCreateDTO;
import com.scaffold.modules.system.dto.RoleQueryDTO;
import com.scaffold.modules.system.dto.RoleUpdateDTO;
import com.scaffold.modules.system.entity.SysRole;
import com.scaffold.modules.system.vo.RoleVO;

import java.util.List;

/**
 * 角色服务接口
 *
 * @author Henfon
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 创建角色
     *
     * @param dto 创建参数
     */
    void createRole(RoleCreateDTO dto);

    /**
     * 更新角色
     *
     * @param dto 更新参数
     */
    void updateRole(RoleUpdateDTO dto);

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    void deleteRole(Long roleId);

    /**
     * 分页查询角色
     *
     * @param dto 查询参数
     * @return 分页结果
     */
    PageResult<RoleVO> pageList(RoleQueryDTO dto);

    /**
     * 获取所有启用的角色列表
     *
     * @return 角色列表
     */
    List<RoleVO> listAll();

    /**
     * 分配角色权限
     *
     * @param roleId  角色ID
     * @param menuIds 菜单ID列表
     */
    void assignMenus(Long roleId, List<Long> menuIds);

    /**
     * 获取角色权限ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getRoleMenuIds(Long roleId);

    /**
     * 更新角色状态
     *
     * @param roleId 角色ID
     * @param status 状态
     */
    void updateStatus(Long roleId, Integer status);

    /**
     * 分配角色用户
     *
     * @param roleId  角色ID
     * @param userIds 用户ID列表
     */
    void assignUsers(Long roleId, List<Long> userIds);

    /**
     * 获取角色已分配的用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> getRoleUserIds(Long roleId);

}
