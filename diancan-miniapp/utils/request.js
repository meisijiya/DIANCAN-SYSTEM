const { getBaseURL } = require('../config/env');
const { KEYS, get } = require('./storage');

function serializeQuery(params) {
  if (!params) return '';
  const query = Object.keys(params)
    .filter((key) => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
  return query ? `?${query}` : '';
}

function normalizeError(err) {
  if (typeof err === 'string') return new Error(err);
  if (err instanceof Error) return err;
  if (err && typeof err === 'object') {
    const errMsg = err.errMsg || err.message || '';
    if (errMsg) return new Error(String(errMsg));
  }
  return new Error('请求失败');
}

function requestRaw({ url, method = 'GET', data, params, header = {}, withPrefix = true }) {
  const token = get(KEYS.TOKEN);
  const baseURL = getBaseURL(withPrefix);
  const fullUrl = `${baseURL}${url}${serializeQuery(params)}`;

  return new Promise((resolve, reject) => {
    wx.request({
      url: fullUrl,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: token } : {}),
        ...header
      },
      success: (res) => {
        const payload = res.data || {};
        if (res.statusCode < 200 || res.statusCode >= 300) {
          reject(new Error(payload.message || `HTTP ${res.statusCode}`));
          return;
        }
        if (payload.code !== undefined && payload.code !== 200) {
          reject(new Error(payload.message || '业务处理失败'));
          return;
        }
        resolve(payload);
      },
      fail: (err) => reject(normalizeError(err))
    });
  });
}

function request(options) {
  return requestRaw(options).then((res) => (res.data !== undefined ? res.data : res));
}

module.exports = {
  request,
  requestRaw
};
