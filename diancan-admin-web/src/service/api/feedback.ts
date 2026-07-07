import { request } from '../request';

/** 获取反馈分页列表 */
export function fetchFeedbackList(params: Api.Business.FeedbackQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.FeedbackRecord>>({
    url: '/admin/feedback/list',
    method: 'get',
    params
  });
}

/** 回复反馈 */
export function replyFeedback(feedbackId: number | string, data: Api.Business.FeedbackReply) {
  return request({
    url: `/admin/feedback/${feedbackId}/reply`,
    method: 'put',
    data
  });
}
