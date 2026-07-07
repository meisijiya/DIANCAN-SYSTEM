import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { request } from '../request';

/** 营业额统计 */
export function fetchRevenue(params: { dimension?: string; startDate: string; endDate: string }) {
  return request<Api.Business.Revenue[]>({
    url: '/admin/report/revenue',
    method: 'get',
    params
  });
}

/** 首页经营概览 */
export function fetchDashboardOverview() {
  return request<Api.Business.DashboardOverview>({
    url: '/admin/report/dashboard-overview',
    method: 'get'
  });
}

/** 菜品销售排行 */
export function fetchDishRanking(params: { startDate: string; endDate: string; limit?: number }) {
  return request<Api.Business.DishRanking[]>({
    url: '/admin/report/dish-ranking',
    method: 'get',
    params
  });
}

/** 翻台率统计 */
export function fetchTableTurnover(params: { startDate: string; endDate: string }) {
  return request<Api.Business.TableTurnover[]>({
    url: '/admin/report/table-turnover',
    method: 'get',
    params
  });
}

/** 导出营业额报表 */
export async function exportRevenue(params: { dimension?: string; startDate: string; endDate: string }) {
  const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
  const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);
  const query = new URLSearchParams(params as Record<string, string>).toString();
  const token = localStg.get('token');

  const response = await fetch(`${baseURL}/admin/report/export?${query}`, {
    method: 'GET',
    headers: token ? { Authorization: token } : undefined
  });

  if (!response.ok) {
    throw new Error(`导出失败(${response.status})`);
  }

  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    const errorBody = (await response.json()) as { code?: number | string; message?: string };
    if (String(errorBody?.code || '') !== import.meta.env.VITE_SERVICE_SUCCESS_CODE) {
      throw new Error(errorBody?.message || '导出失败：没有权限或服务异常');
    }
    throw new Error('导出失败：服务返回了非文件数据');
  }

  const blob = await response.blob();
  if (!blob.size) {
    throw new Error('导出失败：文件内容为空');
  }

  const disposition = response.headers.get('content-disposition');
  const fileName = parseFileName(disposition) || '营业额报表.xlsx';
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
