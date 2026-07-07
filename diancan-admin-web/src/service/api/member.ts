import { request } from '../request';

/** 获取会员分页列表 */
export function fetchMemberPage(params?: Record<string, any>) {
  return request({
    url: '/admin/member/page',
    method: 'get',
    params
  });
}

/** 获取会员详情 */
export function fetchMemberDetail(id: string | number) {
  return request({
    url: `/admin/member/${id}`,
    method: 'get'
  });
}

/** 获取会员统计总览 */
export function fetchMemberOverview() {
  return request({
    url: '/admin/member/overview',
    method: 'get'
  });
}

/** 获取会员等级列表 */
export function fetchMemberLevelList() {
  return request({
    url: '/admin/member/level/list',
    method: 'get'
  });
}

/** 创建会员等级 */
export function createMemberLevel(data: Record<string, any>) {
  return request({
    url: '/admin/member/level',
    method: 'post',
    data
  });
}

/** 更新会员等级 */
export function updateMemberLevel(id: string | number, data: Record<string, any>) {
  return request({
    url: `/admin/member/level/${id}`,
    method: 'put',
    data
  });
}

/** 更新会员等级状态 */
export function updateMemberLevelStatus(id: string | number, status: number) {
  return request({
    url: `/admin/member/level/${id}/status`,
    method: 'put',
    data: { status }
  });
}

/** 获取积分流水分页 */
export function fetchMemberPointsRecordPage(params?: Record<string, any>) {
  return request({
    url: '/admin/member/points-record/page',
    method: 'get',
    params
  });
}

/** 获取成长值流水分页 */
export function fetchMemberGrowthRecordPage(params?: Record<string, any>) {
  return request({
    url: '/admin/member/growth-record/page',
    method: 'get',
    params
  });
}

/** 手工调整会员积分 */
export function adjustMemberPoints(id: string | number, data: Record<string, any>) {
  return request({
    url: `/admin/member/${id}/points-adjust`,
    method: 'post',
    data
  });
}

/** 获取会员权益配置 */
export function fetchMemberBenefitConfig() {
  return request({
    url: '/admin/member/benefit-config',
    method: 'get'
  });
}

/** 保存会员权益配置 */
export function saveMemberBenefitConfig(data: Record<string, any>) {
  return request({
    url: '/admin/member/benefit-config',
    method: 'put',
    data
  });
}

/** 获取积分兑换优惠券配置列表 */
export function fetchMemberExchangeList() {
  return request({
    url: '/admin/member/exchange/list',
    method: 'get'
  });
}

/** 保存积分兑换优惠券配置 */
export function saveMemberExchange(data: Record<string, any>) {
  return request({
    url: '/admin/member/exchange',
    method: 'post',
    data
  });
}

/** 删除积分兑换优惠券配置 */
export function deleteMemberExchange(id: string | number) {
  return request({
    url: `/admin/member/exchange/${id}`,
    method: 'delete'
  });
}
