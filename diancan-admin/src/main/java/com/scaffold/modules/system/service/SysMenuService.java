package com.scaffold.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.modules.system.dto.MenuCreateDTO;
import com.scaffold.modules.system.dto.MenuUpdateDTO;
import com.scaffold.modules.system.entity.SysMenu;
import com.scaffold.modules.system.vo.MenuTreeVO;
import com.scaffold.modules.system.vo.MenuVO;
import com.scaffold.modules.system.vo.UserRouteVO;

import java.util.List;

/**
 * 菜单服务接口
 *
 * @author Henfon
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 创建菜单
     *
     * @param dto 创建参数
     */
    void createMenu(MenuCreateDTO dto);

    /**
     * 更新菜单
     *
     * @param dto 更新参数
     */
    void updateMenu(MenuUpdateDTO dto);

    /**
     * 删除菜单
     *
     * @param menuId 菜单ID
     */
    void deleteMenu(Long menuId);

    /**
     * 获取菜单列表
     *
     * @return 菜单列表
     */
    List<MenuVO> listAll();

    /**
     * 获取菜单树（管理用）
     *
     * @return 菜单树
     */
    List<MenuTreeVO> getMenuTree();

    /**
     * 获取权限树（授权用，包含按钮）
     *
     * @return 权限树
     */
    List<MenuTreeVO> getPermissionTree();

    /**
     * 获取当前用户菜单树
     *
     * @return 菜单树
     */
    List<MenuTreeVO> getCurrentUserMenuTree();

    /**
     * 获取用户权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 获取用户路由（适配Soybean Admin）
     *
     * @param userId 用户ID
     * @return 用户路由
     */
    UserRouteVO getUserRoutes(Long userId);
}
