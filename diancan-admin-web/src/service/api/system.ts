import { request } from '../request';

// ==================== 用户管理 ====================

/** 获取用户分页列表 */
export function fetchUserList(params: Api.System.UserQuery) {
  return request<Api.System.PageResult<Api.System.User>>({
    url: '/system/user/page',
    method: 'get',
    params
  });
}

/** 更新用户状态 */
export function updateUserStatus(userId: number, status: number) {
  return request({
    url: `/system/user/${userId}/status/${status}`,
    method: 'put'
  });
}

/** 管理员重置用户密码 */
export function resetUserPassword(userId: number, newPassword: string) {
  return request({
    url: `/system/user/${userId}/password/reset`,
    method: 'put',
    data: { newPassword }
  });
}

// ==================== 角色管理 ====================

/** 获取角色分页列表 */
export function fetchRoleList(params: Api.System.RoleQuery) {
  return request<Api.System.PageResult<Api.System.Role>>({
    url: '/system/role/page',
    method: 'get',
    params
  });
}

/** 获取所有角色 */
export function fetchAllRoles() {
  return request<Api.System.Role[]>({
    url: '/system/role/list',
    method: 'get'
  });
}

/** 创建角色 */
export function createRole(data: Api.System.RoleCreate) {
  return request({
    url: '/system/role',
    method: 'post',
    data
  });
}

/** 更新角色 */
export function updateRole(data: Api.System.RoleUpdate) {
  return request({
    url: '/system/role',
    method: 'put',
    data
  });
}

/** 删除角色 */
export function deleteRole(id: number) {
  return request({
    url: `/system/role/${id}`,
    method: 'delete'
  });
}

/** 获取角色菜单ID列表 */
export function fetchRoleMenuIds(roleId: number) {
  return request<number[]>({
    url: `/system/role/${roleId}/menus`,
    method: 'get'
  });
}

/** 分配角色菜单 */
export function assignRoleMenus(roleId: number, menuIds: number[]) {
  return request({
    url: `/system/role/${roleId}/menus`,
    method: 'post',
    data: menuIds
  });
}

/** 获取角色已分配的用户ID列表 */
export function fetchRoleUserIds(roleId: number) {
  return request<number[]>({
    url: `/system/role/${roleId}/users`,
    method: 'get'
  });
}

/** 分配角色用户 */
export function assignRoleUsers(roleId: number, userIds: number[]) {
  return request({
    url: `/system/role/${roleId}/users`,
    method: 'post',
    data: userIds
  });
}

// ==================== 菜单管理 ====================

/** 获取菜单树 */
export function fetchMenuTree() {
  return request<Api.System.MenuTree[]>({
    url: '/system/menu/tree',
    method: 'get'
  });
}

/** 获取权限树（含按钮） */
export function fetchPermissionTree() {
  return request<Api.System.MenuTree[]>({
    url: '/system/menu/permission/tree',
    method: 'get'
  });
}

/** 获取菜单列表 */
export function fetchMenuList() {
  return request<Api.System.Menu[]>({
    url: '/system/menu/list',
    method: 'get'
  });
}

/** 创建菜单 */
export function createMenu(data: Api.System.MenuCreate) {
  return request({
    url: '/system/menu',
    method: 'post',
    data
  });
}

/** 更新菜单 */
export function updateMenu(data: Api.System.MenuUpdate) {
  return request({
    url: '/system/menu',
    method: 'put',
    data
  });
}

/** 删除菜单 */
export function deleteMenu(id: number) {
  return request({
    url: `/system/menu/${id}`,
    method: 'delete'
  });
}

// ==================== 字典管理 ====================

/** 获取字典类型分页列表 */
export function fetchDictTypeList(params: Api.System.DictTypeQuery) {
  return request<Api.System.PageResult<Api.System.DictType>>({
    url: '/system/dict/type/page',
    method: 'get',
    params
  });
}

/** 创建字典类型 */
export function createDictType(data: Api.System.DictTypeCreate) {
  return request({
    url: '/system/dict/type',
    method: 'post',
    data
  });
}

/** 更新字典类型 */
export function updateDictType(data: Api.System.DictTypeUpdate) {
  return request({
    url: '/system/dict/type',
    method: 'put',
    data
  });
}

/** 删除字典类型 */
export function deleteDictType(id: number) {
  return request({
    url: `/system/dict/type/${id}`,
    method: 'delete'
  });
}

/** 获取字典数据列表 */
export function fetchDictDataList(typeId: number) {
  return request<Api.System.DictData[]>({
    url: `/system/dict/data/type/${typeId}`,
    method: 'get'
  });
}

/** 创建字典数据 */
export function createDictData(data: Api.System.DictDataCreate) {
  return request({
    url: '/system/dict/data',
    method: 'post',
    data
  });
}

/** 更新字典数据 */
export function updateDictData(data: Api.System.DictDataUpdate) {
  return request({
    url: '/system/dict/data',
    method: 'put',
    data
  });
}

/** 删除字典数据 */
export function deleteDictData(id: number) {
  return request({
    url: `/system/dict/data/${id}`,
    method: 'delete'
  });
}

// ==================== 配置管理 ====================

/** 获取配置分页列表 */
export function fetchConfigList(params: Api.System.ConfigQuery) {
  return request<Api.System.PageResult<Api.System.Config>>({
    url: '/system/config/page',
    method: 'get',
    params
  });
}

/** 创建配置 */
export function createConfig(data: Api.System.ConfigCreate) {
  return request({
    url: '/system/config',
    method: 'post',
    data
  });
}

/** 更新配置 */
export function updateConfig(data: Api.System.ConfigUpdate) {
  return request({
    url: '/system/config',
    method: 'put',
    data
  });
}

/** 删除配置 */
export function deleteConfig(id: number) {
  return request({
    url: `/system/config/${id}`,
    method: 'delete'
  });
}

/** 根据配置键获取配置值 */
export function fetchConfigValueByKey(configKey: string) {
  return request<string | null>({
    url: `/system/config/key/${configKey}`,
    method: 'get'
  });
}

/** 获取管理端主题预设 */
export function fetchAdminThemePreset() {
  return request<string | null>({
    url: '/system/config/theme-preset',
    method: 'get'
  });
}

/** 保存管理端主题预设 */
export function saveAdminThemePreset(presetId: string) {
  return request({
    url: `/system/config/theme-preset/${presetId}`,
    method: 'put'
  });
}

// ==================== 日志管理 ====================

/** 获取登录日志分页列表 */
export function fetchLoginLogList(params: Api.System.LoginLogQuery) {
  return request<Api.System.PageResult<Api.System.LoginLog>>({
    url: '/system/log/login/page',
    method: 'get',
    params
  });
}

/** 获取操作日志分页列表 */
export function fetchOperationLogList(params: Api.System.OperationLogQuery) {
  return request<Api.System.PageResult<Api.System.OperationLog>>({
    url: '/system/log/operation/page',
    method: 'get',
    params
  });
}
