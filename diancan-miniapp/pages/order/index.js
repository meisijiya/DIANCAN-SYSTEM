const orderApi = require('../../api/order');
const reviewApi = require('../../api/review');
const { addSocketListener, connectSocket } = require('../../utils/socket');
const { KEYS, get, set } = require('../../utils/storage');
const { env } = require('../../config/env');

function pickId(obj) {
  if (!obj || typeof obj !== 'object') return '';
  const raw = obj.id ?? obj.orderId ?? '';
  return raw === null || raw === undefined ? '' : String(raw);
}

function normalizeId(v) {
  if (v === null || v === undefined) return '';
  const s = String(v).trim();
  return s && s !== '0' ? s : '';
}

function pickItemId(obj) {
  if (!obj || typeof obj !== 'object') return '';
  const raw = obj.id ?? obj.itemId ?? obj.orderItemId ?? '';
  return raw === null || raw === undefined ? '' : String(raw);
}

function mapOrderStatus(status) {
  if (status === 0) return '待支付';
  if (status === 1) return '已支付';
  return '已取消';
}

function mapItemStatus(status) {
  if (status === 0) return '待制作';
  if (status === 1) return '制作中';
  return '已完成';
}

function normalizeImageUrl(raw) {
  if (!raw) return '';
  let url = String(raw).trim();
  if (!url) return '';
  if (/^data:image\//i.test(url)) return url;
  if (/^https?:\/\//i.test(url)) {
    // MinIO 预签名 URL 的签名依赖完整地址，不能改写 host
    if (/x-amz-signature=/i.test(url)) {
      return url;
    }
    const hostMatch = String(env.apiHost || '').match(/^https?:\/\/([^/:]+)/i);
    const lanHost = hostMatch && hostMatch[1] ? hostMatch[1] : '';
    if (lanHost) {
      url = url.replace(/^(https?:\/\/)(127\.0\.0\.1|localhost)/i, `$1${lanHost}`);
    }
    return url;
  }
  if (url.startsWith('//')) return `https:${url}`;
  if (url.startsWith('/pages/')) return '';
  if (url.startsWith('/')) {
    if (url.startsWith('/api/')) return `${env.apiHost}${url}`;
    return `${env.apiHost}/api${url}`;
  }
  return `${env.apiHost}/${url}`;
}

function formatShortTime(v) {
  if (!v) return '--';
  const s = String(v).replace('T', ' ');
  return s.length >= 16 ? s.slice(0, 16) : s;
}

function stripUrlQuery(url) {
  if (!url) return '';
  return String(url).split('?')[0];
}

function preserveItemImageUrl(prevOrders, nextOrders) {
  const prevMap = new Map();
  (prevOrders || []).forEach((order) => {
    const orderId = String(order.id || '');
    (order.items || []).forEach((item) => {
      const itemId = String(item.id || '');
      if (orderId && itemId) {
        prevMap.set(`${orderId}:${itemId}`, item);
      }
    });
  });

  return (nextOrders || []).map((order) => {
    const orderId = String(order.id || '');
    const items = (order.items || []).map((item) => {
      const itemId = String(item.id || '');
      const prevItem = prevMap.get(`${orderId}:${itemId}`);
      if (!prevItem) return item;

      const prevUrl = prevItem.dishImageUrl || '';
      const nextUrl = item.dishImageUrl || '';
      if (!prevUrl || !nextUrl) return item;

      if (stripUrlQuery(prevUrl) === stripUrlQuery(nextUrl)) {
        return {
          ...item,
          dishImageUrl: prevUrl,
          imageError: !!prevItem.imageError
        };
      }

      return item;
    });
    return { ...order, items };
  });
}

function getReviewedSet() {
  const reviewedLocal = get(KEYS.REVIEWED_ORDER_IDS) || [];
  return new Set(
    (Array.isArray(reviewedLocal) ? reviewedLocal : [])
      .map((v) => normalizeId(v))
      .filter((v) => !!v)
  );
}

function saveReviewedSet(setObj) {
  set(KEYS.REVIEWED_ORDER_IDS, Array.from(setObj));
}

function normalizeOrder(order, mockPaidSet, reviewedSet) {
  const id = pickId(order);
  let status = order.status;

  if (id && status === 0 && mockPaidSet.has(id)) {
    status = 1;
  }

  return {
    ...order,
    id,
    status,
    reviewed: reviewedSet.has(normalizeId(id)),
    statusText: mapOrderStatus(status),
    timeText: formatShortTime(order.createTime || order.createdTime || order.orderTime),
    items: (order.items || []).map((it) => ({
      ...it,
      id: pickItemId(it),
      dishImageUrl: normalizeImageUrl(it.dishImage || it.image || it.thumbnail),
      imageError: false,
      statusText: mapItemStatus(it.status)
    }))
  };
}

Page({
  data: {
    tableId: null,
    orders: [],
    paidOrderCount: 0,
    unpaidOrderCount: 0,
    completedDishCount: 0,
    pollTimer: null,
    detailVisible: false,
    currentOrderDetail: null
  },

  onLoad() {
    const table = get(KEYS.TABLE) || {};
    this.setData({ tableId: Number(table.id || 0) });
  },

  onShow() {
    const table = get(KEYS.TABLE) || {};
    const tableId = Number(table.id || 0);
    if (tableId && tableId !== Number(this.data.tableId || 0)) {
      this.setData({ tableId });
    }
    this.loadOrders();
    connectSocket();
    this.unsubscribe = addSocketListener((msg) => {
      if (msg && (msg.eventType === 'ITEM_COMPLETED' || msg.eventType === 'ALL_COMPLETED')) {
        this.loadOrders();
      }
    });
    this.startPolling();
  },

  onHide() {
    this.stopPolling();
    if (this.unsubscribe) this.unsubscribe();
  },

  onUnload() {
    this.stopPolling();
    if (this.unsubscribe) this.unsubscribe();
  },

  startPolling() {
    this.stopPolling();
    const timer = setInterval(() => this.loadOrders(), 8000);
    this.setData({ pollTimer: timer });
  },

  stopPolling() {
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer);
      this.setData({ pollTimer: null });
    }
  },

  async loadOrders() {
    if (!this.data.tableId) return;
    try {
      const mockPaid = get(KEYS.MOCK_PAID_ORDER_IDS) || [];
      const mockPaidSet = new Set((Array.isArray(mockPaid) ? mockPaid : []).map((v) => String(v)));
      const reviewedSet = getReviewedSet();

      const list = await orderApi.getTableOrders(this.data.tableId);
      const orders = (list || [])
        .map((o) => normalizeOrder(o, mockPaidSet, reviewedSet))
        .filter((o) => !!o.id);
      const nextOrders = preserveItemImageUrl(this.data.orders, orders);
      const paidOrderCount = nextOrders.filter((item) => Number(item.status) === 1).length;
      const unpaidOrderCount = nextOrders.filter((item) => Number(item.status) === 0).length;
      const completedDishCount = nextOrders.reduce((sum, order) => {
        return sum + (order.items || []).filter((dish) => Number(dish.status) === 2).length;
      }, 0);

      this.setData({
        orders: nextOrders,
        paidOrderCount,
        unpaidOrderCount,
        completedDishCount
      });
    } catch (err) {
      wx.showToast({ title: err.message || '加载订单失败', icon: 'none' });
    }
  },

  async rushItem(e) {
    const orderId = String(e.currentTarget.dataset.orderId || '');
    const itemId = String(e.currentTarget.dataset.itemId || '');
    if (!orderId || !itemId) {
      wx.showToast({ title: '订单或菜品标识无效', icon: 'none' });
      return;
    }

    try {
      await orderApi.rushItem(orderId, itemId);
      wx.showToast({ title: '已发送催单', icon: 'none' });
    } catch (err) {
      wx.showToast({ title: err.message || '催单失败', icon: 'none' });
    }
  },

  onDishImageError(e) {
    const orderId = String(e.currentTarget.dataset.orderId || '');
    const itemId = String(e.currentTarget.dataset.itemId || '');
    if (!orderId || !itemId) return;

    const orders = (this.data.orders || []).map((order) => {
      if (String(order.id) !== orderId) return order;
      const items = (order.items || []).map((dish) =>
        String(dish.id) === itemId ? { ...dish, imageError: true } : dish
      );
      return { ...order, items };
    });

    this.setData({ orders });
  },

  openOrderDetail(e) {
    const orderId = String(e.currentTarget.dataset.orderId || '');
    if (!orderId) return;
    const currentOrderDetail = (this.data.orders || []).find(order => String(order.id) === orderId) || null;
    if (!currentOrderDetail) return;
    this.setData({ detailVisible: true, currentOrderDetail });
  },

  closeOrderDetail() {
    this.setData({ detailVisible: false, currentOrderDetail: null });
  },

  noop() {},

  goMenuForAddItem() {
    wx.switchTab({ url: '/pages/menu/index' });
  },

  goPayment(e) {
    const orderId = String(e.currentTarget.dataset.orderId || '');
    if (!orderId) {
      wx.showToast({ title: '订单无效', icon: 'none' });
      return;
    }
    set(KEYS.ORDER_ID, orderId);
    wx.navigateTo({ url: `/pages/payment/index?orderId=${orderId}` });
  },

  goMyReviews() {
    wx.navigateTo({ url: '/pages/my-review/index' });
  },

  async goReview(e) {
    const orderId = String(e.currentTarget.dataset.orderId || '');
    const status = Number(e.currentTarget.dataset.status);
    if (!orderId) {
      wx.showToast({ title: '订单无效', icon: 'none' });
      return;
    }
    if (status !== 1) {
      wx.showToast({ title: '请先完成支付再评价', icon: 'none' });
      return;
    }

    const reviewedSet = getReviewedSet();
    if (reviewedSet.has(normalizeId(orderId))) {
      wx.showToast({ title: '该订单已评价', icon: 'none' });
      return;
    }

    try {
      const review = await reviewApi.getOrderReview(orderId);
      if (review && review.id) {
        reviewedSet.add(normalizeId(orderId));
        saveReviewedSet(reviewedSet);
        this.loadOrders();
        wx.showToast({ title: '该订单已评价', icon: 'none' });
        return;
      }
    } catch (err) {
      // ignore and continue to review page
    }

    set(KEYS.ORDER_ID, orderId);
    wx.navigateTo({ url: `/pages/review/index?orderId=${orderId}` });
  }
});
