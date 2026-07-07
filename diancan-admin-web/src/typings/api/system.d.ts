declare namespace Api {
  namespace System {
    // 分页结果
    interface PageResult<T> {
      list: T[];
      pageNum: number;
      pageSize: number;
      total: number;
    }

    // 分页查询基础参数
    interface PageQuery {
      pageNum?: number;
      pageSize?: number;
    }

    // ==================== 用户 ====================
    interface User {
      id: number;
      username: string;
      nickname: string;
      email: string | null;
      phone: string | null;
      openid: string | null;
      avatar: string | null;
      status: number;
      userType: 'BACKEND' | 'APP' | 'STRESS';
      createTime: string;
    }

    interface UserQuery extends PageQuery {
      username?: string;
      status?: number;
      userType?: 'BACKEND' | 'APP' | 'STRESS';
      memberOnly?: boolean;
      startTime?: string;
      endTime?: string;
    }

    // ==================== 角色 ====================
    interface Role {
      id: number;
      name: string;
      code: string;
      status: number;
      remark: string | null;
      createTime: string;
    }

    interface RoleQuery extends PageQuery {
      name?: string;
      code?: string;
      status?: number;
    }

    interface RoleCreate {
      name: string;
      code: string;
      status?: number;
      remark?: string;
    }

    interface RoleUpdate extends RoleCreate {
      id: number;
    }

    // ==================== 菜单 ====================
    interface Menu {
      id: number;
      parentId: number;
      name: string;
      path: string | null;
      component: string | null;
      permission: string | null;
      type: number;
      icon: string | null;
      orderNum: number;
      status: number;
      createTime: string;
    }

    interface MenuTree extends Menu {
      children?: MenuTree[];
    }

    interface MenuCreate {
      parentId?: number;
      name: string;
      path?: string;
      component?: string;
      permission?: string;
      type: number;
      icon?: string;
      orderNum?: number;
      status?: number;
    }

    interface MenuUpdate extends MenuCreate {
      id: number;
    }

    // ==================== 字典类型 ====================
    interface DictType {
      id: number;
      name: string;
      code: string;
      status: number;
      remark: string | null;
      createTime: string;
    }

    interface DictTypeQuery extends PageQuery {
      name?: string;
      code?: string;
      status?: number;
    }

    interface DictTypeCreate {
      name: string;
      code: string;
      status?: number;
      remark?: string;
    }

    interface DictTypeUpdate extends DictTypeCreate {
      id: number;
    }

    // ==================== 字典数据 ====================
    interface DictData {
      id: number;
      typeId: number;
      label: string;
      value: string;
      orderNum: number;
      status: number;
      remark: string | null;
      createTime: string;
    }

    interface DictDataCreate {
      typeId: number;
      label: string;
      value: string;
      orderNum?: number;
      status?: number;
      remark?: string;
    }

    interface DictDataUpdate extends DictDataCreate {
      id: number;
    }

    // ==================== 配置 ====================
    interface Config {
      id: number;
      name: string;
      configKey: string;
      configValue: string | null;
      remark: string | null;
      createTime: string;
    }

    interface ConfigQuery extends PageQuery {
      name?: string;
      configKey?: string;
    }

    interface ConfigCreate {
      name: string;
      configKey: string;
      configValue?: string;
      remark?: string;
    }

    interface ConfigUpdate extends ConfigCreate {
      id: number;
    }

    // ==================== 登录日志 ====================
    interface LoginLog {
      id: number;
      username: string;
      ip: string;
      location: string | null;
      browser: string | null;
      os: string | null;
      status: number;
      message: string | null;
      loginTime: string;
    }

    interface LoginLogQuery extends PageQuery {
      username?: string;
      status?: number;
      startTime?: string;
      endTime?: string;
    }

    // ==================== 操作日志 ====================
    interface OperationLog {
      id: number;
      module: string;
      operation: string;
      method: string;
      requestUrl: string;
      requestMethod: string;
      requestParams: string | null;
      responseResult: string | null;
      userId: number;
      username: string;
      ip: string;
      duration: number;
      status: number;
      errorMsg: string | null;
      createTime: string;
    }

    interface OperationLogQuery extends PageQuery {
      module?: string;
      username?: string;
      status?: number;
      startTime?: string;
      endTime?: string;
    }
  }
}
