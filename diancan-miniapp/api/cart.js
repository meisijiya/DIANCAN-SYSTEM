const { request } = require('../utils/request');

function getCart(tableId) {
  return request({ url: '/cart', params: { tableId } });
}

function addCartItem(tableId, dishId, quantity = 1, remark = '') {
  return request({
    url: '/cart/item',
    method: 'POST',
    params: { tableId },
    data: { dishId, quantity, remark }
  });
}

function updateCartItem(dishId, tableId, quantity, remark) {
  return request({
    url: `/cart/item/${dishId}`,
    method: 'PUT',
    params: { tableId, quantity, remark }
  });
}

function removeCartItem(dishId, tableId) {
  return request({
    url: `/cart/item/${dishId}`,
    method: 'DELETE',
    params: { tableId }
  });
}

function clearCart(tableId) {
  return request({ url: '/cart', method: 'DELETE', params: { tableId } });
}

module.exports = {
  getCart,
  addCartItem,
  updateCartItem,
  removeCartItem,
  clearCart
};
