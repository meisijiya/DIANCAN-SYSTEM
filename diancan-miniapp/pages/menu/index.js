const dishApi = require('../../api/dish');
const cartApi = require('../../api/cart');
const orderApi = require('../../api/order');
const tableApi = require('../../api/table');
const { KEYS, get } = require('../../utils/storage');
const { isLoggedIn, wxLogin, phoneLogin } = require('../../utils/auth');
const { formatPrice } = require('../../utils/format');

const SPICE_LABEL_MAP = {
  0: '不辣',
  1: '微辣',
  2: '中辣',
  3: '重辣'
};

function normalizeId(v) {
  if (v === null || v === undefined) return '';
  return String(v);
}

function normalizeCategoryId(...values) {
  for (const value of values) {
    const normalized = normalizeId(value);
    if (normalized && normalized !== '0' && normalized !== 'null' && normalized !== 'undefined') {
      return normalized;
    }
  }
  return '0';
}

function parseIngredients(raw) {
  if (!raw) return [];

  if (Array.isArray(raw)) {
    return raw.map(item => String(item).trim()).filter(Boolean);
  }

  if (typeof raw === 'string') {
    const text = raw.trim();
    if (!text) return [];

    if ((text.startsWith('[') && text.endsWith(']')) || (text.startsWith('"') && text.endsWith('"'))) {
      try {
        const parsed = JSON.parse(text);
        if (Array.isArray(parsed)) {
          return parsed.map(item => String(item).trim()).filter(Boolean);
        }
      } catch (err) {
        // ignore json parse error and fallback to plain text split
      }
    }

    return text
      .split(/[、,，/]/)
      .map(item => item.trim())
      .filter(Boolean);
  }

  return [];
}

function extractDishIdFromOrderItem(item) {
  if (!item || typeof item !== 'object') return '';
  return normalizeId(item.dishId ?? item.id ?? '');
}

function buildSearchText(dish) {
  return [
    dish.name,
    dish.categoryName,
    dish.description,
    dish.spiceLabel,
    dish.ingredientsText
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase();
}

function buildCategoryShortLabel(name) {
  const text = String(name || '').trim();
  if (!text) return '分';
  return text.length <= 2 ? text : text.slice(0, 2);
}

function pickCategoryImage(category) {
  if (!category || typeof category !== 'object') return '';
  return category.image || category.imageUrl || category.icon || '';
}

function pickDishImage(dish) {
  if (!dish || typeof dish !== 'object') return '';
  return dish.image || dish.imageUrl || dish.thumbnail || '';
}

function buildDishBrief(dish) {
  if (!dish || typeof dish !== 'object') return '点击查看菜品详情';
  return dish.description || dish.ingredientsText || '点击查看菜品详情';
}

Page({
  data: {
    statusBarHeight: 0,
    navBarHeight: 44,
    loggedIn: false,
    tableCode: '',
    table: null,
    showTableInput: false,
    categories: [],
    categoryScrollIntoView: 'category-all',
    dishMap: {},
    allDishList: [],
    recommendDishIds: [],
    activeCategoryId: null,
    activeCategoryName: '全部菜品',
    activeCategoryCount: 0,
    totalDishCount: 0,
    recommendDishCount: 0,
    orderedDishCount: 0,
    visibleDishCount: 0,
    dishList: [],
    keyword: '',
    quickFilter: 'all',
    orderedDishIds: [],
    cartSummary: { totalCount: 0, totalPrice: '0.00' },
    heroStatusText: '等待绑定桌台',
    menuHeroTitle: '选好菜，再确认下单',
    tableDisplayCode: '未绑定',
    emptyDishText: '没有找到匹配的菜品，换个关键词试试',
    detailVisible: false,
    detailDish: null,
    detailHasIngredients: false,
    detailQty: 1,
    detailRemark: '',
    cartFeedbackVisible: false,
    cartFeedbackText: '',
    showLoginPanel: false,
    agreeProtocol: false,
    loginCallback: null
  },

  onLoad(options) {
    this.initNavBar();
    const code = this.extractTableCode(options);
    if (code) this.setData({ tableCode: code, showTableInput: false });
  },

  onShow() {
    const loggedIn = isLoggedIn();
    const table = get(KEYS.TABLE);
    this.setData({
      loggedIn,
      table: table || null,
      heroStatusText: table ? '桌台已就绪' : '等待绑定桌台',
      menuHeroTitle: table ? `${table.name || table.code || '当前桌台'} 正在点餐` : '选好菜，再确认下单',
      tableDisplayCode: table && table.code ? table.code : '未绑定'
    });

    if (table) {
      this.setData({ tableCode: table.code || '', showTableInput: false });
      this.loadMenu();
      this.loadOrderedDishIds();
      if (loggedIn) this.loadCart();
    } else if (this.data.tableCode) {
      this.loadTable(this.data.tableCode);
    } else {
      this.loadMenu();
      this.setData({ orderedDishIds: [], orderedDishCount: 0, cartSummary: { totalCount: 0, totalPrice: '0.00' } }, () => {
        this.updateDishListFromState();
      });
    }
  },

  onUnload() {
    if (this.cartFeedbackTimer) {
      clearTimeout(this.cartFeedbackTimer);
      this.cartFeedbackTimer = null;
    }
  },

  initNavBar() {
    const { statusBarHeight } = wx.getWindowInfo();
    const menuBtn = wx.getMenuButtonBoundingClientRect();
    this.setData({
      statusBarHeight,
      navBarHeight: (menuBtn.top - statusBarHeight) * 2 + menuBtn.height
    });
  },

  extractTableCode(options) {
    if (!options) return '';
    if (options.scene) {
      const decoded = decodeURIComponent(options.scene);
      if (decoded.includes('code=')) return decoded.split('code=')[1].split('&')[0];
      return decoded;
    }
    if (options.code) return String(options.code);
    if (options.q) {
      const decoded = decodeURIComponent(options.q);
      if (decoded.includes('code=')) return decoded.split('code=')[1].split('&')[0];
      return decoded;
    }
    return '';
  },

  onTableCodeInput(e) {
    this.setData({ tableCode: e.detail.value.trim() });
  },

  async loadTable(code) {
    if (!code) {
      wx.showToast({ title: '请输入桌号编码', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '加载桌台' });
    try {
      const table = await tableApi.getTableByCode(code);
      this.setData({ table, showTableInput: false });
      wx.setStorageSync(KEYS.TABLE, table);
      if (table.status === 0) await tableApi.openTable(table.id);
      await this.loadMenu();
      await this.loadOrderedDishIds();
      if (isLoggedIn()) {
        await this.loadCart();
      }
    } catch (err) {
      wx.showToast({ title: err.message || '桌台不存在', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  toggleTableInput() {
    this.setData({ showTableInput: !this.data.showTableInput });
  },

  async loadMenu() {
    try {
      const [categoryList, dishPayload] = await Promise.all([dishApi.getCategoryList(), dishApi.getDishList()]);
      const catList = Array.isArray(categoryList) ? categoryList : [];
      const categoryNameMap = catList.reduce((acc, item) => {
        acc[normalizeId(item.id)] = item.name;
        return acc;
      }, {});

      const groupedMap = {};
      const categoryCountMap = {};
      const allDishList = [];
      const sourceMap = Array.isArray(dishPayload) ? null : dishPayload;

      const pushDish = (dish, fallbackCategoryId = '0') => {
        const categoryKey = normalizeCategoryId(dish.categoryId, dish.category_id, fallbackCategoryId);
        const ingredientsList = parseIngredients(dish.ingredients);
        const spiceLevel = Number(dish.spiceLevel || 0);
        const normalizedDish = {
          ...dish,
          categoryId: categoryKey,
          categoryKey,
          categoryName: dish.categoryName || categoryNameMap[categoryKey] || '未分类',
          spiceLabel: SPICE_LABEL_MAP[spiceLevel] || '口味',
          ingredientsList,
          ingredientsText: ingredientsList.join(' '),
          imageView: pickDishImage(dish),
          briefText: buildDishBrief({
            ...dish,
            ingredientsText: ingredientsList.join(' ')
          }),
          soldOutFlag: Number(dish.status) === 0 || Number(dish.soldOut) === 1,
          _idStr: normalizeId(dish.id)
        };
        normalizedDish._searchText = buildSearchText(normalizedDish);

        if (!groupedMap[categoryKey]) groupedMap[categoryKey] = [];
        groupedMap[categoryKey].push(normalizedDish);
        categoryCountMap[categoryKey] = Number(categoryCountMap[categoryKey] || 0) + 1;
        allDishList.push(normalizedDish);
      };

      if (Array.isArray(dishPayload)) {
        dishPayload.forEach(item => pushDish(item));
      } else if (sourceMap && typeof sourceMap === 'object') {
        Object.keys(sourceMap).forEach(key => {
          const categoryId = normalizeCategoryId(key);
          (sourceMap[key] || []).forEach(item => pushDish(item, categoryId));
        });
      }

      const recommendDishIds = this.buildRecommendDishIds(allDishList);
      const categoryViewList = catList.map(item => {
        const id = normalizeId(item.id);
        return {
          ...item,
          _idStr: id,
          imageView: pickCategoryImage(item),
          shortLabel: buildCategoryShortLabel(item.name),
          dishCount: Number(categoryCountMap[id] || 0)
        };
      });
      const activeCategoryId = catList.length ? normalizeId(catList[0].id) : null;
      this.setData(
        {
          categories: categoryViewList,
          categoryScrollIntoView: activeCategoryId ? `category-${activeCategoryId}` : 'category-all',
          dishMap: groupedMap,
          allDishList,
          recommendDishIds,
          activeCategoryId,
          activeCategoryName: catList.length ? catList[0].name : '全部菜品',
          totalDishCount: allDishList.length,
          recommendDishCount: recommendDishIds.length
        },
        () => {
          this.updateDishListFromState();
        }
      );
    } catch (err) {
      console.error('加载菜单失败:', err);
      wx.showToast({ title: '菜单加载失败', icon: 'none' });
    }
  },

  async loadOrderedDishIds() {
    if (!this.data.table) {
      this.setData({ orderedDishIds: [], orderedDishCount: 0 }, () => {
        this.updateDishListFromState();
      });
      return;
    }

    try {
      const list = await orderApi.getTableOrders(this.data.table.id);
      const orderedSet = new Set();
      (list || []).forEach(order => {
        (order.items || []).forEach(item => {
          const dishId = extractDishIdFromOrderItem(item);
          if (dishId) {
            orderedSet.add(dishId);
          }
        });
      });

      this.setData({ orderedDishIds: Array.from(orderedSet), orderedDishCount: orderedSet.size }, () => {
        this.updateDishListFromState();
      });
    } catch (err) {
      this.setData({ orderedDishIds: [], orderedDishCount: 0 }, () => {
        this.updateDishListFromState();
      });
    }
  },

  async loadCart() {
    if (!this.data.table) return;
    try {
      const cart = await cartApi.getCart(this.data.table.id);
      this.setData({
        cartSummary: {
          totalCount: cart.totalCount || 0,
          totalPrice: formatPrice(cart.totalPrice)
        }
      });
    } catch (err) {
      this.setData({
        cartSummary: {
          totalCount: 0,
          totalPrice: '0.00'
        }
      });
    }
  },

  selectCategory(e) {
    const id = normalizeId(e.currentTarget.dataset.id);
    const selected = (this.data.categories || []).find(item => normalizeId(item.id) === id);
    this.setData(
      {
        activeCategoryId: id,
        activeCategoryName: selected ? selected.name : '全部菜品',
        categoryScrollIntoView: `category-${id}`
      },
      () => this.updateDishListFromState()
    );
  },

  selectAllCategory() {
    this.setData(
      {
        activeCategoryId: null,
        activeCategoryName: '全部菜品',
        categoryScrollIntoView: 'category-all'
      },
      () => this.updateDishListFromState()
    );
  },

  selectQuickFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    if (!filter || filter === this.data.quickFilter) return;
    this.setData({ quickFilter: filter }, () => this.updateDishListFromState());
  },

  mergeDishList() {
    return this.data.allDishList || [];
  },

  getBaseDishList() {
    const id = this.data.activeCategoryId;
    if (id === null) return this.mergeDishList();
    return this.data.dishMap[id] || [];
  },

  isRecommended(dish) {
    if (!dish) return false;
    if (dish.recommend === 1 || dish.isRecommend === 1 || dish.recommended === 1) return true;
    const text = `${dish.tags || ''}${dish.tag || ''}${dish.label || ''}`;
    if (text.includes('推荐')) return true;
    return (this.data.recommendDishIds || []).includes(dish._idStr);
  },

  buildRecommendDishIds(list) {
    const explicit = (list || [])
      .filter(dish => dish.recommend === 1 || dish.isRecommend === 1 || dish.recommended === 1 || `${dish.tags || ''}${dish.tag || ''}${dish.label || ''}`.includes('推荐'))
      .map(dish => dish._idStr)
      .filter(Boolean);

    if (explicit.length) {
      return explicit;
    }

    return (list || [])
      .filter(dish => !dish.soldOutFlag)
      .sort((left, right) => {
        const leftPrep = Number(left.preparationTime || 999);
        const rightPrep = Number(right.preparationTime || 999);
        if (leftPrep !== rightPrep) return leftPrep - rightPrep;
        return Number(right.price || 0) - Number(left.price || 0);
      })
      .slice(0, 8)
      .map(dish => dish._idStr);
  },

  applyQuickFilter(list) {
    const orderedSet = new Set((this.data.orderedDishIds || []).map(normalizeId));
    const markedList = (list || []).map(dish => ({
      ...dish,
      _ordered: orderedSet.has(dish._idStr),
      _recommended: this.isRecommended(dish)
    }));

    if (this.data.quickFilter === 'recommend') {
      return markedList.filter(item => item._recommended);
    }

    if (this.data.quickFilter === 'ordered') {
      return markedList.filter(item => item._ordered);
    }

    return markedList;
  },

  applyKeywordFilter(list) {
    const keyword = (this.data.keyword || '').trim().toLowerCase();
    if (!keyword) {
      return list;
    }

    return (list || []).filter(item => {
      return item._searchText.includes(keyword);
    });
  },

  updateDishListFromState() {
    const baseList = this.getBaseDishList();
    const filterList = this.applyQuickFilter(baseList);
    const dishList = this.applyKeywordFilter(filterList);
    this.setData({
      dishList,
      visibleDishCount: dishList.length,
      activeCategoryCount: baseList.length
    });
  },

  onKeywordInput(e) {
    this.setData({ keyword: e.detail.value.trim() }, () => {
      this.updateDishListFromState();
    });
  },

  findDishById(dishId) {
    const normalizedId = normalizeId(dishId);
    if (!normalizedId) return null;

    // 优先从当前列表中取，避免弹层数据和页面展示数据不一致
    const currentDish = (this.data.dishList || []).find(item => normalizeId(item.id) === normalizedId);
    if (currentDish) return currentDish;

    return (this.data.allDishList || []).find(item => normalizeId(item.id) === normalizedId) || null;
  },

  openDetail(e) {
    const dishId = e.currentTarget.dataset.id;
    const dish = this.findDishById(dishId);
    if (!dish || dish.soldOutFlag) return;
    this.setData({
      detailVisible: true,
      detailDish: {
        ...dish,
        imageView: dish.imageView || pickDishImage(dish)
      },
      detailHasIngredients: !!(dish.ingredientsList && dish.ingredientsList.length),
      detailQty: 1,
      detailRemark: ''
    });
  },

  closeDetail() {
    this.setData({ detailVisible: false, detailDish: null, detailHasIngredients: false, detailQty: 1, detailRemark: '' });
  },

  showCartFeedback(text) {
    if (this.cartFeedbackTimer) {
      clearTimeout(this.cartFeedbackTimer);
    }

    this.setData({
      cartFeedbackVisible: true,
      cartFeedbackText: text || '已加入购物车'
    });

    this.cartFeedbackTimer = setTimeout(() => {
      this.setData({ cartFeedbackVisible: false, cartFeedbackText: '' });
      this.cartFeedbackTimer = null;
    }, 1400);
  },

  increaseDetailQty() {
    this.setData({ detailQty: Number(this.data.detailQty || 1) + 1 });
  },

  decreaseDetailQty() {
    const qty = Number(this.data.detailQty || 1);
    this.setData({ detailQty: Math.max(1, qty - 1) });
  },

  onDetailRemarkInput(e) {
    this.setData({ detailRemark: e.detail.value });
  },

  requireLogin(callback) {
    this.setData({ showLoginPanel: true, loginCallback: callback, agreeProtocol: false });
  },

  closeLoginPanel() {
    this.setData({ showLoginPanel: false, loginCallback: null, agreeProtocol: false });
  },

  toggleAgreeProtocol() {
    this.setData({ agreeProtocol: !this.data.agreeProtocol });
  },

  openUserAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '登录前请阅读并同意《用户协议》。当前先使用说明弹窗占位，后续可接正式协议页。',
      showCancel: false
    });
  },

  openPrivacyPolicy() {
    wx.showModal({
      title: '隐私政策',
      content: '登录前请阅读并同意《隐私政策》。当前先使用说明弹窗占位，后续可接正式隐私政策页。',
      showCancel: false
    });
  },

  async handlePhoneLogin(e) {
    if (!this.data.agreeProtocol) {
      wx.showToast({ title: '请先勾选用户协议与隐私政策', icon: 'none' });
      return;
    }
    if (!e.detail.code) {
      wx.showToast({ title: e.detail.errMsg || '请授权手机号', icon: 'none' });
      return;
    }
    wx.showLoading({ title: '登录中', mask: true });
    try {
      const code = await wxLogin();
      await phoneLogin(code, e.detail.code);
      this.setData({ loggedIn: true, showLoginPanel: false });
      wx.showToast({ title: '登录成功', icon: 'none' });
      this.loadCart();
      const cb = this.data.loginCallback;
      if (cb) {
        this.setData({ loginCallback: null });
        setTimeout(cb, 300);
      }
    } catch (err) {
      wx.showToast({ title: err.message || '登录失败', icon: 'none', duration: 2000 });
    } finally {
      wx.hideLoading();
    }
  },

  async confirmDetailAdd() {
    if (!isLoggedIn()) {
      this.setData({ detailVisible: false });
      this.requireLogin(async () => {
        this.setData({ detailVisible: true });
        try {
          await this.addDetailDishToCart();
          this.showCartFeedback('已加入购物车');
          this.closeDetail();
        } catch (err) {
          wx.showToast({ title: err.message || '加入失败', icon: 'none' });
        }
      });
      return;
    }

    try {
      await this.addDetailDishToCart();
      this.showCartFeedback('已加入购物车');
      this.closeDetail();
    } catch (err) {
      wx.showToast({ title: err.message || '加入失败', icon: 'none' });
    }
  },

  async submitDetailOrder() {
    if (!isLoggedIn()) {
      this.setData({ detailVisible: false });
      this.requireLogin(async () => {
        this.setData({ detailVisible: true });
        try {
          await this.addDetailDishToCart();
          this.closeDetail();
          wx.navigateTo({ url: '/pages/cart/index' });
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' });
        }
      });
      return;
    }

    try {
      await this.addDetailDishToCart();
      this.closeDetail();
      wx.navigateTo({ url: '/pages/cart/index' });
    } catch (err) {
      wx.showToast({ title: err.message || '操作失败', icon: 'none' });
    }
  },

  /**
   * 将当前弹窗菜品加入购物车
   * @returns {Promise<void>}
   */
  async addDetailDishToCart() {
    const dish = this.data.detailDish;
    if (!dish || dish.soldOutFlag) {
      throw new Error('该菜品已售罄');
    }

    if (!this.data.table) {
      throw new Error('请先关联桌台');
    }

    await cartApi.addCartItem(this.data.table.id, dish.id, Number(this.data.detailQty || 1), this.data.detailRemark || '');
    await this.loadCart();
  },

  goCart() {
    if (!isLoggedIn()) {
      this.requireLogin(() => {
        wx.navigateTo({ url: '/pages/cart/index' });
      });
      return;
    }
    wx.navigateTo({ url: '/pages/cart/index' });
  },

  noop() {},

  onPullDownRefresh() {
    const tasks = [this.loadMenu()];
    if (this.data.table) {
      tasks.push(this.loadOrderedDishIds());
      if (this.data.loggedIn) {
        tasks.push(this.loadCart());
      }
    }

    Promise.allSettled(tasks).finally(() => {
      wx.stopPullDownRefresh();
    });
  }
});
