import { request } from '../request';

/** 获取订单列表（管理端分页） */
export function fetchOrderList(params: Api.Business.OrderQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.Order>>({
    url: '/admin/order/list',
    method: 'get',
    params
  });
}

/** 获取订单详情 */
export function fetchOrderDetail(id: Api.Business.IdType) {
  return request<Api.Business.OrderDetail>({
    url: `/admin/order/${id}`,
    method: 'get'
  });
}
