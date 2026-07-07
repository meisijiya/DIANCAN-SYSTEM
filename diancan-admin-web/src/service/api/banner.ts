import { request } from '../request';

/** 获取轮播图分页列表 */
export function fetchBannerPage(params: Api.Banner.HomeBannerQuery) {
  return request<Api.System.PageResult<Api.Banner.HomeBanner>>({
    url: '/admin/banner/page',
    method: 'get',
    params
  });
}

/** 创建轮播图 */
export function createBanner(data: Api.Banner.HomeBannerSubmit) {
  return request({
    url: '/admin/banner',
    method: 'post',
    data
  });
}

/** 更新轮播图 */
export function updateBanner(id: number, data: Api.Banner.HomeBannerSubmit) {
  return request({
    url: `/admin/banner/${id}`,
    method: 'put',
    data
  });
}

/** 更新轮播图状态 */
export function updateBannerStatus(id: number, status: number) {
  return request({
    url: `/admin/banner/${id}/status`,
    method: 'put',
    params: { status }
  });
}

/** 上传轮播图图片 */
export function uploadBannerImage(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return request<Api.Business.FileUploadResult>({
    url: '/admin/file/upload/banner-image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}
