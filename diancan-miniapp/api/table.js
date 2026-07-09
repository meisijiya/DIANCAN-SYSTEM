const { request } = require('../utils/request');

function getTableByCode(code) {
  return request({ url: `/table/${code}` });
}

function openTable(id) {
  return request({ url: `/table/${id}/open`, method: 'PUT' });
}

module.exports = {
  getTableByCode,
  openTable
};
