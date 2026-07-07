package com.scaffold.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应状态码枚举
 *
 * @author Henfon
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 操作失败
     */
    FAIL(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),

    /**
     * 未登录或登录已过期
     */
    UNAUTHORIZED(401, "未登录或登录已过期"),

    /**
     * 没有操作权限
     */
    FORBIDDEN(403, "没有操作权限"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 用户名或密码错误
     */
    LOGIN_ERROR(1001, "用户名或密码错误"),

    /**
     * 账号已被禁用
     */
    ACCOUNT_DISABLED(1002, "账号已被禁用"),

    /**
     * 用户名已存在
     */
    USERNAME_EXISTS(1003, "用户名已存在"),

    /**
     * 角色编码已存在
     */
    ROLE_CODE_EXISTS(1004, "角色编码已存在"),

    /**
     * 角色已被分配，无法删除
     */
    ROLE_IN_USE(1005, "角色已被分配，无法删除"),

    /**
     * 存在子菜单，无法删除
     */
    MENU_HAS_CHILDREN(1006, "存在子菜单，无法删除"),

    /**
     * 字典类型编码已存在
     */
    DICT_TYPE_EXISTS(1007, "字典类型编码已存在"),

    /**
     * 配置键已存在
     */
    CONFIG_KEY_EXISTS(1008, "配置键已存在"),

    /**
     * 原密码错误
     */
    OLD_PASSWORD_ERROR(1009, "原密码错误"),

    /**
     * 不能禁用自己的账号
     */
    CANNOT_DISABLE_SELF(1010, "不能禁用自己的账号"),

    // ==================== 菜品相关错误码 ====================

    /**
     * 菜品已售罄
     */
    DISH_SOLD_OUT(2001, "菜品已售罄"),

    /**
     * 菜品已下架
     */
    DISH_OFF_SHELF(2002, "菜品已下架"),

    /**
     * 库存不足
     */
    DISH_STOCK_NOT_ENOUGH(2003, "库存不足"),

    /**
     * 购物车为空
     */
    CART_EMPTY(2004, "购物车为空"),

    // ==================== 桌台相关错误码 ====================

    /**
     * 桌台不可用
     */
    TABLE_NOT_AVAILABLE(3001, "桌台不可用"),

    /**
     * 换桌失败
     */
    TABLE_CHANGE_FAILED(3002, "换桌失败"),

    /**
     * 桌台状态异常
     */
    TABLE_STATUS_ERROR(3003, "桌台状态异常"),

    // ==================== 订单相关错误码 ====================

    /**
     * 订单不存在
     */
    ORDER_NOT_FOUND(4001, "订单不存在"),

    /**
     * 订单状态异常
     */
    ORDER_STATUS_ERROR(4002, "订单状态异常"),

    /**
     * 催单频率限制
     */
    RUSH_ORDER_LIMIT(4003, "催单频率限制"),

    /**
     * 退菜授权失败
     */
    RETURN_DISH_AUTH_FAILED(4004, "退菜授权失败"),

    /**
     * 订单项不存在
     */
    ORDER_ITEM_NOT_FOUND(4005, "订单项不存在"),

    /**
     * 折扣比例无效
     */
    ORDER_DISCOUNT_INVALID(4006, "折扣比例无效"),

    // ==================== 支付相关错误码 ====================

    /**
     * 支付发起失败
     */
    PAYMENT_INIT_FAILED(5001, "支付发起失败"),

    /**
     * 支付签名验证失败
     */
    PAYMENT_SIGN_VERIFY_FAILED(5002, "支付签名验证失败"),

    /**
     * 支付金额不匹配
     */
    PAYMENT_AMOUNT_MISMATCH(5003, "支付金额不匹配"),

    /**
     * 分摊金额异常
     */
    PAYMENT_SPLIT_AMOUNT_ERROR(5004, "分摊金额异常"),

    // ==================== 评价相关错误码 ====================

    /**
     * 重复评价
     */
    REVIEW_DUPLICATE(6001, "重复评价"),

    /**
     * 评分超出范围
     */
    REVIEW_RATING_OUT_OF_RANGE(6002, "评分超出范围"),

    // ==================== 打印相关错误码 ====================

    /**
     * 打印机离线
     */
    PRINTER_OFFLINE(7001, "打印机离线"),

    /**
     * 打印任务失败
     */
    PRINT_TASK_FAILED(7002, "打印任务失败"),

    // ==================== 文件相关错误码 ====================

    /**
     * 文件上传失败
     */
    FILE_UPLOAD_FAILED(8001, "文件上传失败");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;
}
