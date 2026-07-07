const memberApi = require('../../api/member');

Page({
  data: {
    ok: true,
    type: 'payment',
    orderId: null,
    rewardSummary: null,
    rewardLoading: false
  },

  onLoad(query) {
    this.setData({
      ok: query.ok === '1',
      type: query.type || 'payment',
      orderId: query.orderId ? Number(query.orderId) : null
    });
  },

  onShow() {
    this.loadRewardSummary();
  },

  async loadRewardSummary() {
    if (!this.data.ok || this.data.type === 'review' || !this.data.orderId) {
      return;
    }
    this.setData({ rewardLoading: true });
    try {
      const rewardSummary = await memberApi.getMemberRewardSummary(this.data.orderId);
      this.setData({ rewardSummary: rewardSummary || null });
    } catch (err) {
      this.setData({ rewardSummary: null });
    } finally {
      this.setData({ rewardLoading: false });
    }
  },

  backHome() {
    wx.reLaunch({ url: '/pages/index/index' });
  },

  goOrder() {
    wx.switchTab({
      url: '/pages/order/index',
      fail: () => {
        wx.reLaunch({ url: '/pages/order/index' });
      }
    });
  },

  goMember() {
    wx.navigateTo({ url: '/pages/member/index' });
  }
});
