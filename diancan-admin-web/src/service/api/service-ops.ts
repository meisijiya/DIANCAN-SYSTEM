import { request } from '../request';

// ==================== 服务端订单操作 ====================

/** 管理端创建订单 */
export function createAdminOrder(data: Api.Business.AdminOrderCreate) {
  return request<Api.Business.Order>({
    url: '/admin/order',
    method: 'post',
    data
  });
}

/** 管理端订单试算 */
export function estimateAdminOrder(data: Api.Business.AdminOrderEstimate) {
  return request<Api.Business.AdminOrderEstimateResult>({
    url: '/admin/order/estimate',
    method: 'post',
    data
  });
}

/** 加菜 */
export function addOrderItem(orderId: Api.Business.IdType, data: Api.Business.AddItemRequest) {
  return request<Api.Business.Order>({
    url: `/admin/order/${orderId}/add-item`,
    method: 'post',
    data
  });
}

/** 催单 */
export function rushOrderItem(orderId: Api.Business.IdType, itemId: Api.Business.IdType) {
  return request({
    url: `/admin/order/${orderId}/rush/${itemId}`,
    method: 'post'
  });
}

/** 整单打折 */
export function discountOrder(orderId: Api.Business.IdType, data: Api.Business.DiscountRequest) {
  return request<Api.Business.Order>({
    url: `/admin/order/${orderId}/discount`,
    method: 'put',
    data
  });
}

/** 赠送订单项 */
export function giftOrderItem(itemId: Api.Business.IdType) {
  return request<Api.Business.Order>({
    url: `/admin/order/item/${itemId}/gift`,
    method: 'put'
  });
}

/** 退菜 */
export function returnOrderItem(itemId: Api.Business.IdType, data: Api.Business.ReturnItemRequest) {
  return request<Api.Business.Order>({
    url: `/admin/order/item/${itemId}/return`,
    method: 'put',
    data
  });
}

/** 换菜 */
export function replaceOrderItem(itemId: Api.Business.IdType, data: Api.Business.ReplaceItemRequest) {
  return request<Api.Business.Order>({
    url: `/admin/order/item/${itemId}/replace`,
    method: 'put',
    data
  });
}

// ==================== 支付操作 ====================

/** 现金支付 */
export function cashPay(data: Api.Business.CashPayRequest) {
  return request<Api.Business.CashPayResult>({
    url: '/admin/payment/cash',
    method: 'post',
    data
  });
}

/** 生成收银二维码 */
export function generatePayQrCode(orderId: Api.Business.IdType) {
  return request<Api.Business.PaymentResult>({
    url: '/admin/payment/qrcode',
    method: 'post',
    params: { orderId }
  });
}

/** 分单结账 */
export function splitBill(data: Api.Business.SplitBillRequest) {
  return request({
    url: '/admin/payment/split-bill',
    method: 'post',
    data
  });
}

/** 整单退款 */
export function refundOrder(orderId: Api.Business.IdType, data: Api.Business.OrderRefundRequest) {
  return request({
    url: `/admin/payment/order/${orderId}/refund`,
    method: 'post',
    data
  });
}

/** 查询支付状态 */
export function fetchPaymentStatus(paymentId: Api.Business.IdType) {
  return request<Api.Business.PaymentStatusResult>({
    url: `/admin/payment/${paymentId}/status`,
    method: 'get'
  });
}
