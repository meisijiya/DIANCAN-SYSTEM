import { request } from '../request';

export function fetchPaymentRecordList(params: Api.Business.PaymentRecordQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.PaymentRecord>>({
    url: '/admin/payment/list',
    method: 'get',
    params
  });
}

export function fetchReviewList(params: Api.Business.ReviewQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.ReviewRecord>>({
    url: '/admin/review/list',
    method: 'get',
    params
  });
}

export function fetchReviewDetail(orderId: Api.IdType) {
  return request<Api.Business.ReviewDetail>({
    url: `/admin/review/order/${orderId}`,
    method: 'get'
  });
}
