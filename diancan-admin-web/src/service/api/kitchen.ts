import { request } from '../request';

/** 后厨任务VO类型 */
export interface KitchenTask {
  id: number;
  orderId: number;
  orderNo: string;
  tableCode: string;
  areaName: string | null;
  paymentMode: number;
  dishId: number;
  dishName: string;
  dishImage: string | null;
  quantity: number;
  remark: string | null;
  status: number;
  addedAt: string;
  preparationTime: number | null;
  overtime: boolean;
}

/** 获取后厨任务列表 */
export function fetchKitchenTasks() {
  return request<KitchenTask[]>({ url: '/app/kitchen/tasks', method: 'get' });
}

/** 接单 */
export function acceptKitchenTask(itemId: number) {
  return request<void>({ url: `/app/kitchen/task/${itemId}/accept`, method: 'put' });
}

/** 划单 */
export function completeKitchenTask(itemId: number) {
  return request<void>({ url: `/app/kitchen/task/${itemId}/complete`, method: 'put' });
}

/** 估清/取消估清（后厨） */
export function markKitchenDishSoldOut(dishId: number, soldOut: 0 | 1) {
  return request<void>({ url: `/app/dish/${dishId}/sold-out`, method: 'put', params: { soldOut } });
}

/** 获取后厨自动接单开关 */
export function fetchKitchenAutoAcceptEnabled() {
  return request<boolean>({ url: '/app/kitchen/auto-accept', method: 'get' });
}

/** 更新后厨自动接单开关 */
export function updateKitchenAutoAcceptEnabled(enabled: boolean) {
  return request<void>({ url: '/app/kitchen/auto-accept', method: 'put', params: { enabled } });
}
