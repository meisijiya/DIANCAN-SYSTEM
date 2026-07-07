import { request } from '../request';

/** 获取打印机列表 */
export function fetchPrinterList() {
  return request<Api.Business.Printer[]>({
    url: '/admin/print/printer/list',
    method: 'get'
  });
}

/** 创建打印机 */
export function createPrinter(data: Api.Business.PrinterCreate) {
  return request({
    url: '/admin/print/printer',
    method: 'post',
    data
  });
}

/** 更新打印机 */
export function updatePrinter(id: number, data: Api.Business.PrinterUpdate) {
  return request({
    url: `/admin/print/printer/${id}`,
    method: 'put',
    data
  });
}

/** 删除打印机 */
export function deletePrinter(id: number) {
  return request({
    url: `/admin/print/printer/${id}`,
    method: 'delete'
  });
}

/** 更新打印机-分类映射 */
export function updateCategoryMapping(data: { mappings: Api.Business.CategoryMappingItem[]; printerIds?: number[] }) {
  return request({
    url: '/admin/print/category-mapping',
    method: 'put',
    data
  });
}

/** 重新打印订单 */
export function reprintOrder(orderId: number) {
  return request({
    url: `/admin/print/reprint/${orderId}`,
    method: 'post'
  });
}
