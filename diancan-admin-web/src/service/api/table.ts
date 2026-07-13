import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { request } from '../request';

/** 获取桌台列表 */
export function fetchTableList() {
  return request<Api.Business.DiningTable[]>({ url: '/admin/table/list', method: 'get' });
}

/** 获取桌台区域列表 */
export function fetchTableAreaList() {
  return request<Api.Business.TableArea[]>({ url: '/admin/table/area/list', method: 'get' });
}

/** 获取启用中的桌台区域列表 */
export function fetchEnabledTableAreaList() {
  return request<Api.Business.TableArea[]>({ url: '/admin/table/area/enabled-list', method: 'get' });
}

/** 创建桌台区域 */
export function createTableArea(data: Api.Business.TableAreaCreate) {
  return request<void>({ url: '/admin/table/area', method: 'post', data });
}

/** 更新桌台区域 */
export function updateTableArea(data: Api.Business.TableAreaUpdate) {
  return request<void>({ url: `/admin/table/area/${data.id}`, method: 'put', data });
}

/** 删除桌台区域 */
export function deleteTableArea(id: number) {
  return request<void>({ url: `/admin/table/area/${id}`, method: 'delete' });
}


/** 创建桌台 */
export function createTable(data: Api.Business.TableCreate) {
  return request<void>({ url: '/admin/table', method: 'post', data });
}

/** 更新桌台 */
export function updateTable(data: Api.Business.TableUpdate) {
  return request<void>({ url: `/admin/table/${data.id}`, method: 'put', data });
}

/** 删除桌台 */
export function deleteTable(id: number) {
  return request<void>({ url: `/admin/table/${id}`, method: 'delete' });
}

/** 生成桌台二维码 */
export function generateTableQrCode(id: number) {
  return request<{ qrCodeUrl: string }>({ url: `/admin/table/${id}/qrcode`, method: 'post' });
}

/** 标记桌台清洁完成（待清洁 -> 空闲） */
export function markTableClean(id: number) {
  return request<void>({ url: `/admin/table/${id}/clean`, method: 'put' });
}

/** 当前桌次全部结清后确认结台（占用 -> 待清洁） */
export function checkoutTable(id: number) {
  return request<void>({ url: `/admin/table/${id}/checkout`, method: 'put' });
}

/** 已结账推进为待清洁 */
export function markTableToClean(id: number) {
  return request<void>({ url: `/admin/table/${id}/to-clean`, method: 'put' });
}

/** 一键释放桌台 */
export function releaseTable(id: number) {
  return request<void>({ url: `/admin/table/${id}/release`, method: 'put' });
}

/** 生成所有桌台二维码 */
export function generateAllTableQrCodes() {
  return request<number>({ url: '/admin/table/qrcode/generate-all', method: 'post' });
}

/** 异步生成所有桌台二维码 */
export function submitGenerateAllTableQrCodesTask() {
  return request<Api.Business.QrCodeTask>({ url: '/admin/table/qrcode/generate-all/task', method: 'post' });
}

/** 异步按区域打包所有桌台二维码 */
export function submitDownloadAllTableQrCodesTask() {
  return request<Api.Business.QrCodeTask>({ url: '/admin/table/qrcode/download-all/task', method: 'post' });
}

/** 查询二维码任务状态 */
export function fetchQrCodeTask(taskId: string) {
  return request<Api.Business.QrCodeTask>({ url: `/admin/table/qrcode/task/${taskId}`, method: 'get' });
}

/** 下载桌台二维码 */
export async function downloadTableQrCode(id: number, fallbackFileName?: string) {
  const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
  const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

  const token = localStg.get('token');
  const response = await fetch(`${baseURL}/admin/table/${id}/qrcode/download`, {
    method: 'GET',
    headers: token ? { Authorization: token } : undefined
  });

  if (!response.ok) {
    throw new Error(`下载失败(${response.status})`);
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const errorBody = (await response.json()) as { code?: number | string; message?: string };
    if (String(errorBody?.code || '') !== import.meta.env.VITE_SERVICE_SUCCESS_CODE) {
      throw new Error(errorBody?.message || '下载失败：没有权限或服务异常');
    }
    throw new Error('下载失败：服务返回了非文件数据');
  }

  const blob = await response.blob();
  if (!blob.size) {
    throw new Error('下载失败：文件内容为空');
  }

  const disposition = response.headers.get('content-disposition');
  const nameFromHeader = parseFileName(disposition);
  const fileName = nameFromHeader || fallbackFileName || `table-${id}-qrcode.png`;

  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(objectUrl);
}

/** 下载二维码任务结果文件 */
export async function downloadQrCodeTaskFile(taskId: string, fallbackFileName?: string) {
  const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
  const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

  const token = localStg.get('token');
  const response = await fetch(`${baseURL}/admin/table/qrcode/task/${taskId}/download`, {
    method: 'GET',
    headers: token ? { Authorization: token } : undefined
  });

  if (!response.ok) {
    throw new Error(`下载失败(${response.status})`);
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const errorBody = (await response.json()) as { code?: number | string; message?: string };
    if (String(errorBody?.code || '') !== import.meta.env.VITE_SERVICE_SUCCESS_CODE) {
      throw new Error(errorBody?.message || '下载失败：没有权限或服务异常');
    }
    throw new Error('下载失败：服务返回了非文件数据');
  }

  const blob = await response.blob();
  if (!blob.size) {
    throw new Error('下载失败：文件内容为空');
  }

  const disposition = response.headers.get('content-disposition');
  const nameFromHeader = parseFileName(disposition);
  const fileName = nameFromHeader || fallbackFileName || `qrcode-task-${taskId}.zip`;

  const objectUrl = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(objectUrl);
}

function parseFileName(contentDisposition: string | null) {
  if (!contentDisposition) return '';

  const filenameStarMatch = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (filenameStarMatch?.[1]) {
    return decodeURIComponent(filenameStarMatch[1].trim().replace(/["']/g, ''));
  }

  const filenameMatch = contentDisposition.match(/filename=([^;]+)/i);
  if (filenameMatch?.[1]) {
    const rawName = filenameMatch[1].trim().replace(/["']/g, '');
    try {
      return decodeURIComponent(rawName);
    } catch {
      return rawName;
    }
  }

  return '';
}
