package com.scaffold.modules.payment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scaffold.common.result.PageResult;
import com.scaffold.modules.payment.dto.AdminPaymentQueryDTO;
import com.scaffold.modules.payment.dto.*;
import com.scaffold.modules.payment.entity.PaymentRecord;
import com.scaffold.modules.payment.vo.AdminPaymentRecordVO;
import com.scaffold.modules.payment.vo.CashPayVO;
import com.scaffold.modules.payment.vo.PaymentStatusVO;
import com.scaffold.modules.payment.vo.PaymentVO;

/**
 * 支付服务接口
 *
 * @author Henfon
 */
public interface PaymentService extends IService<PaymentRecord> {

    /**
     * 微信支付（stub：创建支付记录，状态为待支付）
     *
     * @param openid 用户openid
     * @param dto    微信支付参数
     * @return 支付记录信息
     */
    PaymentVO wechatPay(String openid, WechatPayDTO dto);

    /**
     * 支付宝支付（stub：创建支付记录，状态为待支付）
     *
     * @param openid 用户openid
     * @param dto    支付宝支付参数
     * @return 支付记录信息
     */
    PaymentVO alipay(String openid, AlipayDTO dto);

    /**
     * 现金支付（立即完成，计算找零）
     *
     * @param dto 现金支付参数
     * @return 支付记录信息（含找零金额）
     */
    CashPayVO cashPay(CashPayDTO dto);

    /**
     * AA分摊支付（部分支付，累加 paidAmount）
     *
     * @param openid 用户openid
     * @param dto    AA支付参数
     * @return 支付记录信息
     */
    PaymentVO aaPay(String openid, AAPayDTO dto);

    /**
     * 查询支付状态
     *
     * @param paymentId 支付记录ID
     * @return 支付状态信息
     */
    PaymentStatusVO getPaymentStatus(Long paymentId);

    /**
     * 生成收银二维码（stub：创建待支付记录，返回支付信息）
     *
     * @param orderId 订单ID
     * @return 支付记录信息
     */
    PaymentVO generateQrCode(Long orderId);

    /**
     * 分单结账（按订单项拆分创建多个支付记录）
     *
     * @param dto 分单结账参数
     */
    void splitBill(SplitBillDTO dto);

    /**
     * 管理端整单退款
     *
     * @param orderId 订单ID
     * @param dto 退款参数
     */
    void refundOrder(Long orderId, OrderRefundDTO dto);

    /**
     * 管理端分页查询支付记录
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @param queryDTO 查询条件
     * @return 支付记录分页
     */
    PageResult<AdminPaymentRecordVO> listPaymentsForAdmin(int pageNum, int pageSize, AdminPaymentQueryDTO queryDTO);

    /**
     * 处理微信支付回调
     *
     * @param callbackHeaders 回调请求头
     * @param callbackData    回调原始数据
     */
    void handleWechatCallback(java.util.Map<String, String> callbackHeaders, String callbackData);

    /**
     * 处理支付宝支付回调（stub）
     *
     * @param callbackData 回调原始数据
     */
    void handleAlipayCallback(String callbackData);

}
