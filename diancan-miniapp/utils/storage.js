const KEYS = {
  TOKEN: 'appToken',
  OPENID: 'appOpenid',
  USER_INFO: 'appUserInfo',
  TABLE: 'currentTable',
  ORDER_ID: 'currentOrderId',
  ORDERED_DISH_IDS: 'orderedDishIds',
  MOCK_PAID_ORDER_IDS: 'mockPaidOrderIds',
  REVIEWED_ORDER_IDS: 'reviewedOrderIds',
  PERSON_COUNT: 'personCount'
};

function get(key) {
  return wx.getStorageSync(key);
}

function set(key, value) {
  wx.setStorageSync(key, value);
}

function remove(key) {
  wx.removeStorageSync(key);
}

module.exports = {
  KEYS,
  get,
  set,
  remove
};
