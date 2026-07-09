const tableApi = require('../../api/table');
const { KEYS, get, setCurrentTable } = require('../../utils/storage');
const { isLoggedIn, wxLogin, phoneLogin } = require('../../utils/auth');

function normalizeTableCode(value) {
  if (value === null || value === undefined) return '';
  return String(value).trim().toUpperCase();
}

Page({
  data: {
    tableCode: '',
    table: null,
    targetTableInput: '',
    loggedIn: false,
    autoLoadFromSceneDone: false,
    agreeProtocol: false
  },

  onLoad(options) {
    const code = this.extractCodeFromOptions(options || {});
    if (!code) return;
    this.setData({ tableCode: code, autoLoadFromSceneDone: false });
  },

  onShow() {
    this.setData({ loggedIn: isLoggedIn() });

    const cachedTable = get(KEYS.TABLE);
    const sceneTableCode = normalizeTableCode(this.data.tableCode);
    const cachedTableCode = normalizeTableCode(cachedTable && cachedTable.code);
    if (sceneTableCode && sceneTableCode !== cachedTableCode) {
      this.setData({ table: null, autoLoadFromSceneDone: true });
      this.loadTable(this.data.tableCode);
      return;
    }

    if (cachedTable) {
      this.setData({ table: cachedTable, tableCode: cachedTable.code || '' });
      return;
    }

    if (this.data.tableCode && !this.data.autoLoadFromSceneDone) {
      this.setData({ autoLoadFromSceneDone: true });
      this.loadTable();
    }
  },

  onCodeInput(e) {
    this.setData({ tableCode: e.detail.value.trim() });
  },

  onTargetInput(e) {
    this.setData({ targetTableInput: e.detail.value.trim() });
  },

  /**
   * 手机号登录：open-type="getPhoneNumber" 的回调
   */
  async handlePhoneLogin(e) {
    if (!this.data.agreeProtocol) {
      wx.showToast({ title: '请先勾选用户协议与隐私政策', icon: 'none' });
      return;
    }
    if (!e.detail.code) {
      wx.showToast({ title: '请授权手机号登录', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '登录中', mask: true });
    try {
      const code = await wxLogin();
      await phoneLogin(code, e.detail.code);
      this.setData({ loggedIn: true });
      wx.showToast({ title: '登录成功', icon: 'none' });
    } catch (err) {
      wx.showToast({ title: err.message || '登录失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
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

  async scanCode() {
    wx.scanCode({
      onlyFromCamera: true,
      success: async (res) => {
        const content = res.result || '';
        const parsedCode = this.parseCode(content);
        this.setData({ tableCode: parsedCode });
        await this.loadTable();
      },
      fail: () => {
        wx.showToast({ title: '扫码失败', icon: 'none' });
      }
    });
  },

  parseCode(content) {
    if (!content) return '';
    if (content.includes('code=')) {
      const parts = content.split('code=');
      return decodeURIComponent(parts[1].split('&')[0]);
    }
    return content;
  },

  extractCodeFromOptions(options) {
    if (options.scene) {
      const decodedScene = decodeURIComponent(options.scene);
      const sceneCode = this.parseCode(decodedScene);
      if (sceneCode) return sceneCode;
    }
    if (options.code) {
      return this.parseCode(String(options.code));
    }
    if (options.q) {
      const decodedQ = decodeURIComponent(options.q);
      return this.parseCode(decodedQ);
    }
    return '';
  },

  async loadTable() {
    const tableCode = this.data.tableCode;
    if (!tableCode) {
      wx.showToast({ title: '请输入桌号编码', icon: 'none' });
      return;
    }
    const requestCode = normalizeTableCode(tableCode);
    wx.showLoading({ title: '加载中' });
    try {
      const table = await tableApi.getTableByCode(tableCode);
      if (requestCode !== normalizeTableCode(this.data.tableCode)) {
        return;
      }

      const originalStatus = Number(table.status);
      const boundTable = { ...table };
      if (originalStatus === 0) {
        await tableApi.openTable(boundTable.id);
        boundTable.status = 1;
        wx.showToast({ title: '空闲桌台已自动开台', icon: 'none' });
      }

      setCurrentTable(boundTable);
      this.setData({ table: boundTable, tableCode: boundTable.code || tableCode });

      if (originalStatus === 1) {
        wx.showModal({
          title: '桌台已占用',
          content: '可选择加入该桌继续点餐，或联系服务员。',
          cancelText: '联系服务员',
          confirmText: '加入该桌'
        });
      }
    } catch (err) {
      wx.showToast({ title: err.message || '获取桌台失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  enterMenu() {
    const { table } = this.data;
    if (!table || !table.id) {
      wx.showToast({ title: '请先关联桌台', icon: 'none' });
      return;
    }
    wx.switchTab({ url: '/pages/menu/index' });
  },

  async changeTable() {
    const { table, targetTableInput } = this.data;
    if (!table || !table.id || !targetTableInput) {
      wx.showToast({ title: '请先填写目标桌台', icon: 'none' });
      return;
    }

    try {
      let targetTableId = 0;
      if (/^\d+$/.test(targetTableInput)) {
        targetTableId = Number(targetTableInput);
      } else {
        const targetTable = await tableApi.getTableByCode(targetTableInput);
        targetTableId = Number(targetTable && targetTable.id);
      }

      if (!targetTableId) {
        wx.showToast({ title: '目标桌台不存在', icon: 'none' });
        return;
      }

      await tableApi.changeTable(table.id, targetTableId);
      wx.showToast({ title: '换桌成功', icon: 'none' });
      this.setData({ targetTableInput: '' });
    } catch (err) {
      wx.showToast({ title: err.message || '换桌失败', icon: 'none' });
    }
  }
});
