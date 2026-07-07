import { request } from '../request';

/** 获取优惠券模板分页列表 */
export function fetchCouponTemplatePage(params: Api.Coupon.CouponTemplateQuery) {
  return request<Api.Coupon.PageResult<Api.Coupon.CouponTemplate>>({
    url: '/admin/coupon/template/page',
    method: 'get',
    params
  });
}

/** 创建优惠券模板 */
export function createCouponTemplate(data: Api.Coupon.CouponTemplateSubmit) {
  return request({
    url: '/admin/coupon/template',
    method: 'post',
    data
  });
}

/** 更新优惠券模板 */
export function updateCouponTemplate(id: string | number, data: Api.Coupon.CouponTemplateUpdate) {
  return request({
    url: `/admin/coupon/template/${id}`,
    method: 'put',
    data
  });
}

/** 更新优惠券模板状态 */
export function updateCouponTemplateStatus(id: string | number, status: number) {
  return request({
    url: `/admin/coupon/template/${id}/status`,
    method: 'put',
    params: { status }
  });
}

/** 发放优惠券 */
export function grantCoupon(data: Api.Coupon.CouponGrantSubmit) {
  return request<Api.Coupon.CouponGrantTask>({
    url: '/admin/coupon/grant',
    method: 'post',
    data,
    timeout: 120 * 1000
  });
}

/** 获取发券任务分页列表 */
export function fetchCouponGrantTaskPage(params: Api.Coupon.CouponGrantTaskQuery) {
  return request<Api.Coupon.PageResult<Api.Coupon.CouponGrantTask>>({
    url: '/admin/coupon/task/page',
    method: 'get',
    params
  });
}

/** 获取发券任务明细分页列表 */
export function fetchCouponGrantTaskDetailPage(params: Api.Coupon.CouponGrantTaskDetailQuery) {
  return request<Api.Coupon.PageResult<Api.Coupon.CouponGrantTaskDetail>>({
    url: '/admin/coupon/task/detail/page',
    method: 'get',
    params
  });
}

/** 获取用户优惠券分页列表 */
export function fetchUserCouponPage(params: Api.Coupon.UserCouponQuery) {
  return request<Api.Coupon.PageResult<Api.Coupon.UserCoupon>>({
    url: '/admin/coupon/user/page',
    method: 'get',
    params
  });
}
