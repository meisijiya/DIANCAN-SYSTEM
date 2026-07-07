import { localStg } from '@/utils/storage';
import { getServiceBaseURL } from '@/utils/service';
import { request } from '../request';

/** 查询审计日志列表 */
export function fetchAuditLogList(params: Api.Business.AuditLogQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.AuditLog>>({
    url: '/admin/audit-log/list',
    method: 'get',
    params
  });
}

/** 提交审计日志导出任务 */
export function submitAuditLogExportTask(data: Api.Business.AuditLogQuery) {
  return request<Api.Business.AuditLogExportTask>({
    url: '/admin/audit-log/export/task',
    method: 'post',
    data
  });
}

/** 查询审计日志导出任务列表 */
export function fetchAuditLogExportTaskPage(params: Api.Business.AuditLogExportTaskQuery) {
  return request<Api.System.PageResult<Api.Business.AuditLogExportTask>>({
    url: '/admin/audit-log/export/task/page',
    method: 'get',
    params
  });
}

/** 下载审计日志导出任务文件 */
export async function downloadAuditLogExportTaskFile(taskId: number, fallbackFileName?: string) {
  const isHttpProxy = import.meta.env.DEV && import.meta.env.VITE_HTTP_PROXY === 'Y';
  const { baseURL } = getServiceBaseURL(import.meta.env, isHttpProxy);

  const token = localStg.get('token');
  const response = await fetch(`${baseURL}/admin/audit-log/export/task/${taskId}/download`, {
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
  const fileName = parseFileName(disposition) || fallbackFileName || `audit-log-${taskId}.xlsx`;

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
