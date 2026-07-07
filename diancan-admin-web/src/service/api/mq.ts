import { request } from '../request';

/** 获取消息记录分页列表 */
export function fetchMqMessagePage(params: Api.Mq.MessageQuery) {
  return request<Api.Mq.PageResult<Api.Mq.Message>>({
    url: '/admin/mq/message/page',
    method: 'get',
    params
  });
}

/** 手动重试消息 */
export function retryMqMessage(id: string | number) {
  return request({
    url: `/admin/mq/message/${id}/retry`,
    method: 'post'
  });
}
