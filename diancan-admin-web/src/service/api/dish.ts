import { request } from '../request';

// ==================== 菜品分类 ====================

/** 获取所有分类列表 */
export function fetchDishCategoryList() {
  return request<Api.Business.DishCategory[]>({
    url: '/admin/dish/category/list',
    method: 'get'
  });
}

/** 创建菜品分类 */
export function createDishCategory(data: Api.Business.DishCategoryCreate) {
  return request({
    url: '/admin/dish/category',
    method: 'post',
    data
  });
}

/** 更新菜品分类 */
export function updateDishCategory(id: string | number, data: Api.Business.DishCategoryUpdate) {
  return request({
    url: `/admin/dish/category/${id}`,
    method: 'put',
    data
  });
}

/** 删除菜品分类 */
export function deleteDishCategory(id: string | number) {
  return request({
    url: `/admin/dish/category/${id}`,
    method: 'delete'
  });
}

/** 批量更新分类排序 */
export function updateDishCategorySort(data: { items: Api.Business.DishCategorySortItem[] }) {
  return request({
    url: '/admin/dish/category/sort',
    method: 'put',
    data
  });
}

/** 获取规格组列表 */
export function fetchDishSpecGroupList() {
  return request<Api.Business.DishSpecGroup[]>({
    url: '/admin/dish/spec/list',
    method: 'get'
  });
}

/** 创建规格组 */
export function createDishSpecGroup(data: Api.Business.DishSpecGroupCreate) {
  return request({
    url: '/admin/dish/spec',
    method: 'post',
    data
  });
}

/** 更新规格组 */
export function updateDishSpecGroup(id: string | number, data: Api.Business.DishSpecGroupUpdate) {
  return request({
    url: `/admin/dish/spec/${id}`,
    method: 'put',
    data
  });
}

/** 删除规格组 */
export function deleteDishSpecGroup(id: string | number) {
  return request({
    url: `/admin/dish/spec/${id}`,
    method: 'delete'
  });
}

// ==================== 菜品 ====================

/** 获取菜品列表（管理端分页） */
export function fetchDishList(params: Api.Business.DishQuery & { pageNum?: number; pageSize?: number }) {
  return request<Api.System.PageResult<Api.Business.Dish>>({
    url: '/admin/dish/list',
    method: 'get',
    params
  });
}

/** 获取菜品详情 */
export function fetchDishDetail(id: string | number) {
  return request<Api.Business.Dish>({
    url: `/admin/dish/${id}`,
    method: 'get'
  });
}

/** 创建菜品 */
export function createDish(data: Api.Business.DishCreate) {
  return request({
    url: '/admin/dish',
    method: 'post',
    data
  });
}

/** 更新菜品 */
export function updateDish(id: string | number, data: Api.Business.DishUpdate) {
  return request({
    url: `/admin/dish/${id}`,
    method: 'put',
    data
  });
}

/** 更新菜品状态（上架/下架） */
export function updateDishStatus(id: string | number, status: number) {
  return request({
    url: `/admin/dish/${id}/status`,
    method: 'put',
    params: { status }
  });
}

/** 设置菜品估清状态（1估清 0取消估清） */
export function updateDishSoldOut(id: string | number, soldOut: 0 | 1) {
  return request({
    url: `/app/dish/${id}/sold-out`,
    method: 'put',
    params: { soldOut }
  });
}

/** 上传菜品图片 */
export function uploadDishImage(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request<Api.Business.FileUploadResult>({
    url: '/admin/file/upload/dish-image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
