package com.scaffold.framework.satoken;

import java.util.*;

/**
 * 餐厅角色权限默认配置
 * <p>
 * 预置四种角色的默认权限集合，作为数据库配置的补充。
 * 当数据库中未配置权限时，使用此默认配置。
 *
 * @author Henfon
 */
public final class RolePermissionDefaults {

    /**
     * 角色编码常量
     */
    public static final String ROLE_ADMIN = "RESTAURANT_ADMIN";
    public static final String ROLE_WAITER = "WAITER";
    public static final String ROLE_CASHIER = "CASHIER";
    public static final String ROLE_KITCHEN = "KITCHEN";

    /**
     * 角色-权限映射（默认配置）
     */
    private static final Map<String, List<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // 服务员权限：桌台管理、订单查看/创建/加菜/催单、菜品查看、重新打印
        ROLE_PERMISSIONS.put(ROLE_WAITER, List.of(
                "table:list", "table:open", "table:clean", "table:change", "table:order", "table:qrcode:download",
                "table:area:list",
                "order:view", "order:create", "order:add-item", "order:rush",
                "dish:view",
                "print:reprint"
        ));

        // 收银员权限：支付操作、订单操作（打折/赠送/退菜/换菜）、桌台管理、菜品查看
        ROLE_PERMISSIONS.put(ROLE_CASHIER, List.of(
                "table:list", "table:open", "table:clean", "table:order", "table:qrcode:download",
                "table:area:list",
                "order:view", "order:discount", "order:gift", "order:return", "order:replace", "order:list",
                "dish:view",
                "payment:cash", "payment:qrcode", "payment:split-bill",
                "print:reprint"
        ));

        // 后厨权限：后厨任务（接单、划单）、估清管理、菜品查看
        ROLE_PERMISSIONS.put(ROLE_KITCHEN, List.of(
                "kitchen:tasks", "kitchen:accept", "kitchen:complete",
                "dish:view", "dish:sold-out"
        ));

        // 管理员权限：所有权限
        List<String> allPermissions = new ArrayList<>();
        ROLE_PERMISSIONS.values().forEach(allPermissions::addAll);
        // 管理员额外权限
        allPermissions.addAll(List.of(
                "table:manage", "table:qrcode:generate", "table:area:list", "table:area:manage",
                "order:create", "order:add-item", "order:rush", "order:list",
                "dish:manage",
                "payment:list",
                "feedback:list", "feedback:reply",
                "report:revenue", "report:dish-ranking", "report:table-turnover", "report:export",
                "print:manage",
                "review:list", "review:view",
                "audit:list", "audit:export"
        ));
        ROLE_PERMISSIONS.put(ROLE_ADMIN, allPermissions.stream().distinct().toList());
    }

    private RolePermissionDefaults() {
    }

    /**
     * 获取角色的默认权限列表
     *
     * @param roleCode 角色编码
     * @return 权限列表，角色不存在时返回空列表
     */
    public static List<String> getPermissions(String roleCode) {
        return ROLE_PERMISSIONS.getOrDefault(roleCode, Collections.emptyList());
    }

    /**
     * 根据角色列表获取合并后的权限集合
     *
     * @param roleCodes 角色编码列表
     * @return 去重后的权限列表
     */
    public static List<String> getMergedPermissions(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> permissions = new LinkedHashSet<>();
        for (String roleCode : roleCodes) {
            permissions.addAll(getPermissions(roleCode));
        }
        return new ArrayList<>(permissions);
    }

    /**
     * 获取所有预置角色编码
     */
    public static List<String> getAllRoleCodes() {
        return List.of(ROLE_ADMIN, ROLE_WAITER, ROLE_CASHIER, ROLE_KITCHEN);
    }
}
