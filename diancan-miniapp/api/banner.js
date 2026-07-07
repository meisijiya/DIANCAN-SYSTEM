const { request } = require('../utils/request');

function getBannerList() {
  return request({ url: '/banner/list', method: 'GET' });
}

module.exports = {
  getBannerList
};
