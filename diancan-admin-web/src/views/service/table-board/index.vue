<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import {
  NCard, NSpace, NTag, NSpin,
  NDrawer, NDrawerContent, NDescriptions, NDescriptionsItem, NButton, NEmpty, useMessage,
  NInputNumber, NInput
} from 'naive-ui';
import {
  fetchTableAreaList,
  fetchTableList,
  fetchDishCategoryList,
  fetchOrderList,
  fetchOrderDetail,
  fetchDishList,
  createAdminOrder,
  addOrderItem,
  cashPay,
  splitBill,
  checkoutTable,
  releaseTable
} from '@/service/api';
import { connectWebSocket, subscribe } from '@/service/websocket';
import type { WsMessage, TableStatusData } from '@/service/websocket';

const message = useMessage();
const loading = ref(false);
const cleanLoadingTableId = ref<Api.Business.IdType | null>(null);
const tables = ref<Api.Business.DiningTable[]>([]);
const areaList = ref<Api.Business.TableArea[]>([]);
const filterArea = ref<string | null>(null);
const boardViewMode = ref<'overview' | 'standard' | 'focus'>('overview');

/** 桌台状态配置 */
const statusConfig: Record<number, { label: string; shortLabel: string; color: string }> = {
  0: { label: '空闲', shortLabel: '闲', color: '#2f8f6b' },
  1: { label: '占用', shortLabel: '占', color: '#d9485f' },
  2: { label: '已结账', shortLabel: '结', color: '#14a3ff' },
  3: { label: '待清洁', shortLabel: '洁', color: '#dd8b1c' }
};

const areaMetaMap = computed(() => {
  const map = new Map<string, Api.Business.TableArea>();
  areaList.value.forEach(item => {
    map.set(item.name, item);
  });
  return map;
});

function resolveAreaSort(areaName: string) {
  const areaMeta = areaMetaMap.value.get(areaName);
  if (areaMeta) return areaMeta.sort ?? 0;
  return Number.MAX_SAFE_INTEGER;
}

function sortAreaNames(a: string, b: string) {
  const sortDiff = resolveAreaSort(a) - resolveAreaSort(b);
  if (sortDiff !== 0) return sortDiff;
  return a.localeCompare(b, 'zh-Hans-CN');
}

/** 区域筛选列表 */
const areaOptions = computed(() => {
  const areaSet = new Set<string>();
  areaList.value.forEach(item => areaSet.add(item.name));
  tables.value.forEach(item => areaSet.add(item.areaName || '未分区'));
  const areas = [...areaSet].sort(sortAreaNames);
  return ['全部区域', ...areas];
});

/** 区域桌台统计 */
const areaCounts = computed(() => {
  const counts: Record<string, number> = {};
  tables.value.forEach(t => {
    const area = t.areaName || '未分区';
    counts[area] = (counts[area] || 0) + 1;
  });
  return counts;
});

const filteredTables = computed(() => (
  filterArea.value
    ? tables.value.filter(t => (t.areaName || '未分区') === filterArea.value)
    : tables.value
));

/** 按区域分组 */
const groupedData = computed(() => {
  const groups: Record<string, Api.Business.DiningTable[]> = {};
  for (const t of filteredTables.value) {
    const area = t.areaName || '未分区';
    if (!groups[area]) groups[area] = [];
    groups[area].push(t);
  }
  return groups;
});

const groupedEntries = computed(() =>
  Object.entries(groupedData.value)
    .map(([area, areaTables]) => [
      area,
      [...areaTables].sort((a, b) => a.code.localeCompare(b.code, 'zh-Hans-CN'))
    ] as const)
    .sort(([areaA], [areaB]) => sortAreaNames(areaA, areaB))
);

const overviewTables = computed(() => (
  [...filteredTables.value].sort((a, b) => {
    const areaA = a.areaName || '未分区';
    const areaB = b.areaName || '未分区';
    if (areaA !== areaB) return sortAreaNames(areaA, areaB);
    if (a.status !== b.status) return a.status - b.status;
    return a.code.localeCompare(b.code, 'zh-Hans-CN');
  })
));

/** 统计数据 */
const stats = computed(() => ({
  total: tables.value.length,
  free: tables.value.filter(t => t.status === 0).length,
  occupied: tables.value.filter(t => t.status === 1).length,
  paid: tables.value.filter(t => t.status === 2).length,
  cleaning: tables.value.filter(t => t.status === 3).length
}));

const occupancyRate = computed(() => {
  if (!stats.value.total) return 0;
  return Math.round((stats.value.occupied / stats.value.total) * 100);
});

const visibleAreaCount = computed(() => Object.keys(groupedData.value).length);

function getStatus(s: number) {
  return statusConfig[s] || statusConfig[0];
}

function getTableDisplayName(table: Api.Business.DiningTable) {
  const code = (table.code || table.name || '').trim();
  if (!code) return '未命名桌';
  return code.endsWith('桌') ? code : `${code}桌`;
}

function resolveActiveOrder() {
  return (
    tableOrders.value.find(order => order.status === 0)
    || tableOrders.value.find(order => order.status === 1)
    || tableOrders.value[0]
    || null
  );
}

function filterOrdersByCurrentSession(orders: Api.Business.Order[]) {
  const currentSessionCode = String(selectedTable.value?.currentSessionCode || '').trim();
  if (!currentSessionCode) {
    return orders;
  }
  return orders.filter(order => String(order.tableSessionCode || '').trim() === currentSessionCode);
}

/** 抽屉数据 */
const showOrderDrawer = ref(false);
const selectedTable = ref<Api.Business.DiningTable | null>(null);
const drawerMode = ref<'overview' | 'order' | 'checkout'>('overview');
const orderLoading = ref(false);
const dishLoading = ref(false);
const actionLoading = ref(false);
const tableOrders = ref<Api.Business.Order[]>([]);
const currentOrderDetail = ref<Api.Business.OrderDetail | null>(null);
const drawerDishes = ref<Api.Business.Dish[]>([]);

const orderStatusMap: Record<number, string> = {
  0: '待支付', 1: '已支付', 2: '已取消', 3: '已退款'
};

function getPaymentStatusMeta(status?: number) {
  return status === 2
    ? { label: '已支付', type: 'success' as const }
    : { label: '未支付', type: 'warning' as const };
}

const previewOrder = computed(() => {
  if (currentOrderDetail.value) return currentOrderDetail.value;
  return resolveActiveOrder();
});

const dishCategories = ref<Api.Business.DishCategory[]>([]);
const activeDishCategoryId = ref<Api.Business.IdType | null>(null);
const drawerDishKeyword = ref('');

interface DrawerCartItem {
  dishId: Api.Business.IdType;
  dishName: string;
  price: number;
  quantity: number;
  remark: string;
}

const quickOrderForm = ref({ dishId: null as Api.Business.IdType | null });
const quickAddForm = ref({ dishId: null as Api.Business.IdType | null });
const quickOrderCart = ref<DrawerCartItem[]>([]);
const quickAddCart = ref<DrawerCartItem[]>([]);
const receivedAmount = ref<number | null>(null);
const checkoutMode = ref<'full' | 'split'>('full');
const splitCheckedItemIds = ref<Api.Business.IdType[]>([]);

const selectedQuickOrderDish = computed(() => (
  drawerDishes.value.find(dish => String(dish.id) === String(quickOrderForm.value.dishId))
));
const selectedQuickAddDish = computed(() => (
  drawerDishes.value.find(dish => String(dish.id) === String(quickAddForm.value.dishId))
));
const activeDraftCart = computed(() => (selectedTable.value?.status === 0 ? quickOrderCart.value : quickAddCart.value));
const activeDraftDishCountMap = computed(() => {
  const map = new Map<string, number>();
  activeDraftCart.value.forEach(item => {
    map.set(String(item.dishId), (map.get(String(item.dishId)) || 0) + item.quantity);
  });
  return map;
});
const filteredDrawerDishes = computed(() => {
  const keyword = drawerDishKeyword.value.trim().toLowerCase();
  return drawerDishes.value.filter(dish => {
    if (dish.soldOut === 1) return false;
    const matchCategory = !activeDishCategoryId.value || String(dish.categoryId) === String(activeDishCategoryId.value);
    const matchKeyword = !keyword
      || dish.name.toLowerCase().includes(keyword)
      || (dish.categoryName || '').toLowerCase().includes(keyword);
    return matchCategory && matchKeyword;
  });
});
const quickOrderCartTotal = computed(() => quickOrderCart.value.reduce((sum, item) => sum + item.price * item.quantity, 0));
const quickAddCartTotal = computed(() => quickAddCart.value.reduce((sum, item) => sum + item.price * item.quantity, 0));
const splitSelectableItems = computed(() =>
  (currentOrderDetail.value?.items || []).filter(item => item.status !== 3 && item.paymentStatus !== 2)
);
const splitTotal = computed(() =>
  splitSelectableItems.value
    .filter(item => splitCheckedItemIds.value.includes(item.id))
    .reduce((sum, item) => sum + item.amount, 0)
);
const currentOrderRemainingAmount = computed(() => {
  const actualAmount = currentOrderDetail.value?.actualAmount || 0;
  const paidAmount = currentOrderDetail.value?.paidAmount || 0;
  return Math.max(actualAmount - paidAmount, 0);
});
const canReleaseSelectedTable = computed(() => {
  const status = selectedTable.value?.status;
  if (status === 2 || status === 3) return true;
  return status === 1 && !orderLoading.value && tableOrders.value.length === 0;
});

function isSameId(left: Api.Business.IdType | null | undefined, right: Api.Business.IdType | null | undefined) {
  return String(left ?? '') === String(right ?? '');
}

function resetQuickForms() {
  quickOrderForm.value = { dishId: null };
  quickAddForm.value = { dishId: null };
  drawerDishKeyword.value = '';
  activeDishCategoryId.value = null;
}

function resetQuickCarts() {
  quickOrderCart.value = [];
  quickAddCart.value = [];
}

function pushDishToCart(
  cart: typeof quickOrderCart.value,
  dish: Api.Business.Dish,
  quantity: number,
  remark: string
) {
  const existing = cart.find(item => String(item.dishId) === String(dish.id) && item.remark === remark);
  if (existing) {
    existing.quantity += quantity;
    return;
  }
  cart.push({
    dishId: dish.id,
    dishName: dish.name,
    price: dish.price,
    quantity,
    remark
  });
}

function addDrawerDishToCart(mode: 'order' | 'add', dish: Api.Business.Dish) {
  const isQuickOrder = mode === 'order';
  const cart = isQuickOrder ? quickOrderCart.value : quickAddCart.value;
  const quantity = 1;
  const remark = '';

  pushDishToCart(cart, dish, quantity, remark);
  message.success(`已加入${isQuickOrder ? '点单' : '加菜'}清单：${dish.name} × ${quantity}`);

  // 保留当前菜品高亮，方便继续加同款。
  if (isQuickOrder) {
    quickOrderForm.value = { dishId: dish.id };
    return;
  }

  quickAddForm.value = { dishId: dish.id };
}

function removeCartItem(cart: typeof quickOrderCart.value, index: number) {
  cart.splice(index, 1);
}

async function loadDrawerDishes() {
  dishLoading.value = true;
  try {
    const [{ data: categoryData, error: categoryError }, { data, error }] = await Promise.all([
      fetchDishCategoryList(),
      fetchDishList({ status: 1, pageNum: 1, pageSize: 200 })
    ]);
    if (!categoryError && categoryData) {
      dishCategories.value = categoryData.filter(category => category.status === 1);
    }
    if (!error && data) drawerDishes.value = data.list || [];
  } finally {
    dishLoading.value = false;
  }
}

function selectDrawerDish(dish: Api.Business.Dish) {
  if (selectedTable.value?.status === 0) {
    quickOrderForm.value.dishId = dish.id;
    addDrawerDishToCart('order', dish);
    return;
  }
  quickAddForm.value.dishId = dish.id;
  addDrawerDishToCart('add', dish);
}

async function loadCurrentOrderDetail() {
  const targetOrder = resolveActiveOrder();
  if (!targetOrder) {
    currentOrderDetail.value = null;
    return;
  }
  const { data, error } = await fetchOrderDetail(targetOrder.id);
  if (!error && data) {
    currentOrderDetail.value = data;
    receivedAmount.value = Math.max((data.actualAmount || 0) - (data.paidAmount || 0), 0);
    splitCheckedItemIds.value = [];
  }
}

async function refreshDrawerData() {
  if (!selectedTable.value) return;
  orderLoading.value = true;
  try {
    if (selectedTable.value.status === 0) {
      tableOrders.value = [];
      currentOrderDetail.value = null;
      receivedAmount.value = null;
      return;
    }
    const { data, error } = await fetchOrderList({ tableId: selectedTable.value.id, pageNum: 1, pageSize: 200 });
    if (!error && data) {
      tableOrders.value = filterOrdersByCurrentSession(data.list || []);
      await loadCurrentOrderDetail();
    }
  } finally {
    orderLoading.value = false;
  }
}

async function syncSelectedTableStatus() {
  if (!selectedTable.value) return;
  await loadData();
  const nextTable = tables.value.find(t => isSameId(t.id, selectedTable.value?.id)) || null;
  selectedTable.value = nextTable;
}

function resolveDrawerMode(table: Api.Business.DiningTable) {
  if (table.status === 0) return 'order';
  if (table.status === 3) return 'overview';
  if (table.status === 2) return 'overview';
  return 'order';
}

/** 点击桌台查看详情 */
async function handleTableClick(table: Api.Business.DiningTable) {
  selectedTable.value = table;
  drawerMode.value = resolveDrawerMode(table);
  showOrderDrawer.value = true;
  tableOrders.value = [];
  currentOrderDetail.value = null;
  receivedAmount.value = null;
  resetQuickForms();
  resetQuickCarts();
  await Promise.all([loadDrawerDishes(), refreshDrawerData()]);
}

async function loadData(showLoading = true) {
  if (showLoading) loading.value = true;
  try {
    const [{ data: tableData, error: tableError }, { data: areaData, error: areaError }] = await Promise.all([
      fetchTableList(),
      fetchTableAreaList()
    ]);
    if (!tableError && tableData) tables.value = tableData;
    if (!areaError && areaData) areaList.value = areaData;
  } finally {
    if (showLoading) loading.value = false;
  }
}

async function handleReleaseTable(table: Api.Business.DiningTable) {
  if (!canReleaseSelectedTable.value || cleanLoadingTableId.value) return;
  cleanLoadingTableId.value = table.id;
  try {
    const { error } = await releaseTable(Number(table.id));
    if (!error) {
      message.success(`桌台 ${table.code} 已释放为空闲`);
      table.status = 0;
      if (isSameId(selectedTable.value?.id, table.id)) {
        drawerMode.value = 'order';
        await refreshDrawerData();
      }
    }
  } finally {
    cleanLoadingTableId.value = null;
  }
}

async function handleQuickPlaceOrder() {
  if (!selectedTable.value || quickOrderCart.value.length === 0) {
    message.warning('请先加入菜品');
    return;
  }
  actionLoading.value = true;
  try {
    const { error } = await createAdminOrder({
      tableId: selectedTable.value.id,
      tableCode: selectedTable.value.code,
      items: quickOrderCart.value.map(item => ({
        dishId: item.dishId,
        quantity: item.quantity,
        remark: item.remark || undefined
      })),
      paymentMode: 1,
      orderType: 0,
      preOrder: false
    });
    if (!error) {
      message.success('已在当前桌台完成点单');
      resetQuickForms();
      resetQuickCarts();
      await syncSelectedTableStatus();
      await refreshDrawerData();
      drawerMode.value = 'overview';
    }
  } finally {
    actionLoading.value = false;
  }
}

async function handleQuickAddDish() {
  if (quickAddCart.value.length === 0) {
    message.warning('请先加入加菜清单');
    return;
  }
  if (!currentOrderDetail.value) {
    await refreshDrawerData();
  }
  if (!currentOrderDetail.value) {
    if (!selectedTable.value) {
      message.warning('当前桌台暂无可加菜订单');
      return;
    }
    actionLoading.value = true;
    try {
      const { error } = await createAdminOrder({
        tableId: Number(selectedTable.value.id),
        tableCode: selectedTable.value.code,
        items: quickAddCart.value.map(item => ({
          dishId: item.dishId,
          quantity: item.quantity,
          remark: item.remark || undefined
        })),
        paymentMode: 1,
        orderType: 0,
        preOrder: false
      });
      if (!error) {
        message.success('当前桌台未找到活动订单，已自动补挂新单');
        resetQuickForms();
        resetQuickCarts();
        await syncSelectedTableStatus();
        await refreshDrawerData();
        drawerMode.value = 'overview';
      }
    } finally {
      actionLoading.value = false;
    }
    return;
  }
  actionLoading.value = true;
  try {
    for (const item of quickAddCart.value) {
      const { error } = await addOrderItem(currentOrderDetail.value.id, {
        dishId: item.dishId,
        quantity: item.quantity,
        remark: item.remark || undefined
      });
      if (error) {
        return;
      }
    }
    message.success('加菜成功');
    resetQuickForms();
    resetQuickCarts();
    await refreshDrawerData();
  } finally {
    actionLoading.value = false;
  }
}

async function handleQuickCheckout() {
  if (!selectedTable.value || !currentOrderDetail.value) {
    message.warning('暂无可结账订单');
    return;
  }

  // 小程序已付清本桌全部订单时，管理端只需要确认结台，不再重复收款。
  if (currentOrderDetail.value.status !== 0 || currentOrderRemainingAmount.value <= 0) {
    actionLoading.value = true;
    try {
      const { error } = await checkoutTable(Number(selectedTable.value.id));
      if (!error) {
        message.success('结台成功，桌台已进入待清洁');
        await syncSelectedTableStatus();
        await refreshDrawerData();
        drawerMode.value = 'overview';
      }
    } finally {
      actionLoading.value = false;
    }
    return;
  }

  if ((receivedAmount.value || 0) < currentOrderRemainingAmount.value) {
    message.warning('收款金额不足');
    return;
  }
  actionLoading.value = true;
  try {
    const { error } = await cashPay({
      orderId: currentOrderDetail.value.id,
      receivedAmount: receivedAmount.value || currentOrderRemainingAmount.value
    });
    if (!error) {
      await syncSelectedTableStatus();
      await refreshDrawerData();
      if (selectedTable.value?.status === 3) {
        message.success('结账成功，桌台已进入待清洁');
      } else {
        message.success('本单收款成功，请继续处理本桌其他待支付订单');
      }
      drawerMode.value = 'overview';
    }
  } finally {
    actionLoading.value = false;
  }
}

function toggleSplitItem(itemId: Api.Business.IdType) {
  if (splitCheckedItemIds.value.includes(itemId)) {
    splitCheckedItemIds.value = splitCheckedItemIds.value.filter(id => id !== itemId);
    return;
  }
  splitCheckedItemIds.value = [...splitCheckedItemIds.value, itemId];
}

async function handleQuickSplitCheckout() {
  if (!currentOrderDetail.value) {
    message.warning('暂无可结账订单');
    return;
  }
  if (splitCheckedItemIds.value.length === 0) {
    message.warning('请选择要先结账的菜品');
    return;
  }
  actionLoading.value = true;
  try {
    const { error } = await splitBill({
      orderId: currentOrderDetail.value.id,
      items: splitCheckedItemIds.value.map(id => ({
        orderItemIds: [String(id)],
        paymentMethod: 2
      }))
    });
    if (!error) {
      message.success('分单结账成功');
      splitCheckedItemIds.value = [];
      await syncSelectedTableStatus();
      await refreshDrawerData();
      drawerMode.value = 'overview';
    }
  } finally {
    actionLoading.value = false;
  }
}

/** WebSocket 桌台状态实时更新 */
function handleTableStatus(msg: WsMessage<TableStatusData>) {
  const { tableId, newStatus } = msg.data;
  const table = tables.value.find(t => isSameId(t.id, tableId));
  if (table) table.status = newStatus;
  if (isSameId(selectedTable.value?.id, tableId)) {
    selectedTable.value.status = newStatus;
  }
  void loadData(false);
}

let unsubscribe: (() => void) | null = null;

onMounted(() => {
  loadData();
  connectWebSocket();
  unsubscribe = subscribe('/topic/table-status', handleTableStatus);
});

onUnmounted(() => {
  unsubscribe?.();
});
</script>

<template>
  <NSpin :show="loading">
    <NSpace vertical :size="12">
      <NCard :bordered="false" class="board-card" :class="[`board-card--${boardViewMode}`]">
        <div class="board-surface-head">
          <div>
            <div class="board-card__eyebrow">TABLE ZONES</div>
            <div class="board-card__title">桌台分区视图</div>
            <div class="board-card__subline">{{ visibleAreaCount }} 个区域 · {{ filterArea || '全部区域' }} · 实时同步桌态变化</div>
          </div>
          <div class="board-surface-head__actions">
            <div class="board-view-switch board-view-switch--header">
              <button
                type="button"
                class="board-view-switch__item"
                :class="{ 'board-view-switch__item--active': boardViewMode === 'overview' }"
                @click="boardViewMode = 'overview'"
              >
                全览
              </button>
              <button
                type="button"
                class="board-view-switch__item"
                :class="{ 'board-view-switch__item--active': boardViewMode === 'standard' }"
                @click="boardViewMode = 'standard'"
              >
                标准
              </button>
              <button
                type="button"
                class="board-view-switch__item"
                :class="{ 'board-view-switch__item--active': boardViewMode === 'focus' }"
                @click="boardViewMode = 'focus'"
              >
                聚焦
              </button>
            </div>
            <NButton class="board-refresh-btn board-refresh-btn--inline" @click="loadData">刷新桌态</NButton>
            <NSpace :size="10" class="board-card__legend">
              <NSpace v-for="(cfg, key) in statusConfig" :key="key" align="center" :size="6" class="board-card__legend-item">
                <span :style="{ width: '10px', height: '10px', borderRadius: '999px', background: cfg.color, display: 'inline-block' }" />
                <span>{{ cfg.label }}</span>
              </NSpace>
            </NSpace>
          </div>
        </div>
        <div class="board-surface-divider" />

        <div class="board-stage">
          <div class="board-top-toolbar">
            <div class="board-filter-list board-filter-list--compact">
              <button
                v-for="area in areaOptions"
                :key="area"
                class="board-filter-item board-filter-item--compact"
                :class="{ 'board-filter-item--active': (area === '全部区域' && !filterArea) || filterArea === area }"
                type="button"
                @click="filterArea = area === '全部区域' ? null : area"
              >
                <strong>{{ area }}</strong>
                <span>{{ area === '全部区域' ? `${tables.length} 张桌台` : `${areaCounts[area] || 0} 张桌台` }}</span>
              </button>
            </div>
          </div>

          <div class="board-surface">
            <div class="board-stage__main">
              <div class="board-surface__main">
                <div class="board-surface__aurora board-surface__aurora--primary" />
                <div class="board-surface__aurora board-surface__aurora--secondary" />

                <div v-if="boardViewMode === 'overview'" class="board-panorama">
                  <div class="board-zone__header">
                    <div>
                      <div class="board-zone__title">全部桌台总览</div>
                      <div class="board-zone__meta">{{ overviewTables.length }} 张桌台连续展开，适合前厅一屏扫视</div>
                    </div>
                    <div class="board-zone__pill">{{ filterArea || '全部区域' }}</div>
                  </div>

                  <div class="board-zone__grid board-zone__grid--overview">
                    <div v-for="t in overviewTables" :key="t.id" class="board-zone__cell">
                      <div
                        class="table-card table-card--overview"
                        :data-status="t.status"
                        @click="handleTableClick(t)"
                      >
                        <span class="table-card__area">{{ t.areaName || '未分区' }}</span>
                        <NTag
                          size="small"
                          class="table-card__status"
                          :bordered="false"
                          :style="{ background: getStatus(t.status).color, color: '#fff', fontSize: '11px', borderRadius: '999px', padding: '0 10px' }"
                        >
                          {{ getStatus(t.status).shortLabel }}
                        </NTag>
                        <div class="table-card__halo" />
                        <div class="table-icon" :style="{ color: getStatus(t.status).color }">
                          <svg viewBox="0 0 24 24" width="40" height="40" fill="currentColor">
                            <path d="M4 11h16a1 1 0 011 1v1a1 1 0 01-1 1H4a1 1 0 01-1-1v-1a1 1 0 011-1zm1 4h2v5H5v-5zm12 0h2v5h-2v-5zm-7-9h4a1 1 0 011 1v4H9V7a1 1 0 011-1z"/>
                          </svg>
                        </div>
                        <div class="table-name">{{ getTableDisplayName(t) }}</div>
                        <div class="table-meta">
                          <span>{{ t.capacity }}人位</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <template v-else>
                  <div v-for="[area, areaTables] in groupedEntries" :key="area" class="board-zone">
                    <div class="board-zone__header">
                      <div>
                        <div class="board-zone__title">{{ area }}</div>
                        <div class="board-zone__meta">{{ areaTables.length }} 张桌台 · 保持前厅扫视效率</div>
                      </div>
                      <div class="board-zone__pill">桌态分区</div>
                    </div>

                    <div class="board-zone__grid">
                      <div v-for="t in areaTables" :key="t.id" class="board-zone__cell">
                        <div
                          class="table-card"
                          :class="[`table-card--${boardViewMode}`]"
                          :data-status="t.status"
                          @click="handleTableClick(t)"
                        >
                          <NTag
                            size="small"
                            class="table-card__status"
                            :bordered="false"
                            :style="{ background: getStatus(t.status).color, color: '#fff', fontSize: '11px', borderRadius: '999px', padding: '0 10px' }"
                          >
                            {{ getStatus(t.status).shortLabel }}
                          </NTag>
                          <div class="table-card__halo" />
                          <div class="table-icon" :style="{ color: getStatus(t.status).color }">
                            <svg viewBox="0 0 24 24" width="40" height="40" fill="currentColor">
                              <path d="M4 11h16a1 1 0 011 1v1a1 1 0 01-1 1H4a1 1 0 01-1-1v-1a1 1 0 011-1zm1 4h2v5H5v-5zm12 0h2v5h-2v-5zm-7-9h4a1 1 0 011 1v4H9V7a1 1 0 011-1z"/>
                            </svg>
                          </div>
                          <div class="table-name">{{ getTableDisplayName(t) }}</div>
                          <div class="table-meta">
                            <span>{{ t.capacity }}人位</span>
                          </div>
                          <div class="table-action">点击进入桌台操作</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </template>

                <div v-if="!loading && tables.length === 0" class="board-empty">
                  暂无桌台数据
                </div>
              </div>

              <aside class="board-insight-panel">
                <div class="board-insight-focus">
                  <span>当前在台率</span>
                  <strong>{{ occupancyRate }}%</strong>
                  <small>{{ stats.occupied }} / {{ stats.total || 0 }} 张桌台正在接待</small>
                </div>

                <div class="board-insight-stats">
                  <div class="board-insight-stat">
                    <span>总数</span>
                    <strong>{{ stats.total }}</strong>
                  </div>
                  <div class="board-insight-stat" data-tone="success">
                    <span>空闲</span>
                    <strong>{{ stats.free }}</strong>
                  </div>
                  <div class="board-insight-stat" data-tone="error">
                    <span>占用</span>
                    <strong>{{ stats.occupied }}</strong>
                  </div>
                  <div class="board-insight-stat" data-tone="info">
                    <span>已结账</span>
                    <strong>{{ stats.paid }}</strong>
                  </div>
                  <div class="board-insight-stat" data-tone="warning">
                    <span>待清洁</span>
                    <strong>{{ stats.cleaning }}</strong>
                  </div>
                </div>
              </aside>
            </div>
          </div>
        </div>
      </NCard>
    </NSpace>

    <NDrawer v-model:show="showOrderDrawer" :width="560" placement="right">
      <NDrawerContent :title="`${selectedTable?.name || ''} · ${selectedTable?.code || ''}`" closable>
        <NSpace vertical :size="12">
          <div v-if="selectedTable" class="drawer-summary">
            <div class="drawer-summary__status">
              <span class="drawer-summary__label">当前状态</span>
              <NTag :bordered="false" :style="{ background: getStatus(selectedTable.status).color, color: '#fff' }">
                {{ getStatus(selectedTable.status).label }}
              </NTag>
            </div>
            <div class="drawer-summary__meta">
              <span>容纳 {{ selectedTable.capacity }} 人</span>
              <span>{{ selectedTable.areaName || '未分区' }}</span>
            </div>
            <div v-if="canReleaseSelectedTable" class="drawer-summary__clean-actions">
              <NButton
                type="warning"
                :loading="cleanLoadingTableId === selectedTable.id"
                @click="handleReleaseTable(selectedTable)"
              >
                {{ selectedTable.status === 1 ? '释放空桌' : '释放桌台' }}
              </NButton>
            </div>
            <div v-if="selectedTable.status === 3" class="drawer-summary__notice">
              当前桌台已处于待清洁状态，确认收尾完成后可直接释放为空闲。
            </div>
            <div v-else-if="selectedTable.status === 2" class="drawer-summary__notice">
              当前桌台已结账，如已完成现场收尾，可直接释放桌台。
            </div>
            <div v-else-if="selectedTable.status === 1 && !orderLoading && tableOrders.length === 0" class="drawer-summary__notice">
              当前桌次尚未产生订单，可直接释放空桌。
            </div>
          </div>

          <div class="drawer-mode-switch">
            <button class="drawer-mode-switch__item" :class="{ 'drawer-mode-switch__item--active': drawerMode === 'overview' }" @click="drawerMode = 'overview'">
              概览
            </button>
            <button
              class="drawer-mode-switch__item"
              :class="{ 'drawer-mode-switch__item--active': drawerMode === 'order' }"
              :disabled="selectedTable?.status === 2 || selectedTable?.status === 3"
              @click="drawerMode = 'order'"
            >
              {{ selectedTable?.status === 0 ? '点单' : '加菜' }}
            </button>
            <button
              class="drawer-mode-switch__item"
              :class="{ 'drawer-mode-switch__item--active': drawerMode === 'checkout' }"
              :disabled="!currentOrderDetail || selectedTable?.status === 2 || selectedTable?.status === 3"
              @click="drawerMode = 'checkout'"
            >
              结账
            </button>
          </div>

          <NSpin :show="orderLoading || dishLoading || actionLoading">
            <template v-if="drawerMode === 'overview'">
              <template v-if="previewOrder">
                <NSpace vertical :size="12">
                  <NCard size="small" :bordered="true" class="drawer-order-card">
                    <NDescriptions :column="2" label-placement="left" size="small">
                      <NDescriptionsItem label="订单号">{{ previewOrder.orderNo }}</NDescriptionsItem>
                      <NDescriptionsItem label="状态">
                        <NTag :type="previewOrder.status === 1 ? 'success' : previewOrder.status === 0 ? 'warning' : 'default'" size="small">
                          {{ orderStatusMap[previewOrder.status] || '未知' }}
                        </NTag>
                      </NDescriptionsItem>
                      <NDescriptionsItem label="原价">¥{{ previewOrder.originalAmount?.toFixed(2) }}</NDescriptionsItem>
                      <NDescriptionsItem label="实付">¥{{ previewOrder.actualAmount?.toFixed(2) }}</NDescriptionsItem>
                      <NDescriptionsItem label="下单时间" :span="2">{{ previewOrder.createTime }}</NDescriptionsItem>
                    </NDescriptions>

                    <div v-if="previewOrder.items?.length" class="drawer-order-items">
                      <div class="drawer-order-items__title">本单菜品</div>
                      <div class="drawer-order-items__list">
                        <div
                          v-for="item in previewOrder.items"
                          :key="item.id"
                          class="drawer-order-items__item"
                        >
                          <div>
                            <strong>{{ item.dishName }}</strong>
                            <span v-if="item.remark">{{ item.remark }}</span>
                          </div>
                          <div class="drawer-order-items__side">
                            <span>x{{ item.quantity }}</span>
                            <small>¥{{ item.amount.toFixed(2) }}</small>
                            <NTag size="small" :type="getPaymentStatusMeta(item.paymentStatus).type">
                              {{ getPaymentStatusMeta(item.paymentStatus).label }}
                            </NTag>
                          </div>
                        </div>
                      </div>
                    </div>
                  </NCard>
                </NSpace>
              </template>
              <NEmpty v-else description="当前桌台暂无主订单" />
            </template>

            <template v-else-if="drawerMode === 'order'">
              <div class="drawer-panel">
                <div class="drawer-panel__title">{{ selectedTable?.status === 0 ? '快捷点单' : '快捷加菜' }}</div>
                <div class="drawer-panel__desc">先筛分类、再搜菜名，直接点菜卡即可加入清单；数量统一在下方清单里调整。</div>

                <div class="drawer-dish-browser">
                  <NInput
                    v-model:value="drawerDishKeyword"
                    placeholder="搜索菜名或分类"
                    clearable
                  />
                  <div class="drawer-dish-browser__categories">
                    <button
                      type="button"
                      class="drawer-category-chip"
                      :class="{ 'drawer-category-chip--active': !activeDishCategoryId }"
                      @click="activeDishCategoryId = null"
                    >
                      全部
                    </button>
                    <button
                      v-for="category in dishCategories"
                      :key="category.id"
                      type="button"
                      class="drawer-category-chip"
                      :class="{ 'drawer-category-chip--active': String(activeDishCategoryId) === String(category.id) }"
                      @click="activeDishCategoryId = category.id"
                    >
                      {{ category.name }}
                    </button>
                  </div>

                  <div v-if="filteredDrawerDishes.length > 0" class="drawer-dish-grid">
                    <button
                      v-for="dish in filteredDrawerDishes"
                      :key="dish.id"
                      type="button"
                      class="drawer-dish-card"
                      :class="{
                        'drawer-dish-card--active': String((selectedTable?.status === 0 ? quickOrderForm.dishId : quickAddForm.dishId)) === String(dish.id),
                        'drawer-dish-card--in-cart': activeDraftDishCountMap.has(String(dish.id))
                      }"
                      @click="selectDrawerDish(dish)"
                    >
                      <div class="drawer-dish-card__title">{{ dish.name }}</div>
                      <div class="drawer-dish-card__meta">
                        <span>¥{{ dish.price.toFixed(2) }}</span>
                        <span>{{ dish.categoryName || '未分类' }}</span>
                      </div>
                      <div v-if="activeDraftDishCountMap.has(String(dish.id))" class="drawer-dish-card__feedback">
                        已入清单 {{ activeDraftDishCountMap.get(String(dish.id)) }} 份
                      </div>
                      <div
                        v-else-if="String((selectedTable?.status === 0 ? quickOrderForm.dishId : quickAddForm.dishId)) === String(dish.id)"
                        class="drawer-dish-card__feedback"
                      >
                        当前菜品，可继续点卡快速加入
                      </div>
                    </button>
                  </div>
                  <NEmpty v-else description="没有匹配到可用菜品" style="padding: 20px 0 8px;" />
                </div>

                <div class="drawer-quick-compose">
                  <div class="drawer-dish-preview" v-if="selectedTable?.status === 0 ? selectedQuickOrderDish : selectedQuickAddDish">
                    <div>
                      <strong>{{ (selectedTable?.status === 0 ? selectedQuickOrderDish : selectedQuickAddDish)?.name }}</strong>
                      <span>¥{{ (selectedTable?.status === 0 ? selectedQuickOrderDish : selectedQuickAddDish)?.price.toFixed(2) }}</span>
                    </div>
                    <NTag v-if="(selectedTable?.status === 0 ? selectedQuickOrderDish : selectedQuickAddDish)?.categoryName" size="small">
                      {{ (selectedTable?.status === 0 ? selectedQuickOrderDish : selectedQuickAddDish)?.categoryName }}
                    </NTag>
                  </div>
                </div>

                <div class="drawer-cart" v-if="selectedTable?.status === 0 ? quickOrderCart.length > 0 : quickAddCart.length > 0">
                  <div class="drawer-cart__head">
                    <strong>{{ selectedTable?.status === 0 ? '点单清单' : '加菜清单' }}</strong>
                    <span>
                      ¥{{ (selectedTable?.status === 0 ? quickOrderCartTotal : quickAddCartTotal).toFixed(2) }}
                    </span>
                  </div>

                  <div
                    v-for="(item, index) in selectedTable?.status === 0 ? quickOrderCart : quickAddCart"
                    :key="`${item.dishId}-${item.remark}-${index}`"
                    class="drawer-cart__item"
                  >
                    <div>
                      <strong>{{ item.dishName }}</strong>
                      <span>¥{{ item.price.toFixed(2) }} · {{ item.quantity }}份</span>
                      <span v-if="item.remark">{{ item.remark }}</span>
                    </div>
                    <div class="drawer-cart__actions">
                      <NInputNumber v-model:value="item.quantity" :min="1" size="small" style="width: 92px;" />
                      <NButton quaternary type="error" size="small" @click="removeCartItem(selectedTable?.status === 0 ? quickOrderCart : quickAddCart, index)">
                        删除
                      </NButton>
                    </div>
                  </div>
                </div>

                <NButton
                  type="success"
                  block
                  class="drawer-quick-compose__button drawer-quick-compose__confirm"
                  @click="selectedTable?.status === 0 ? handleQuickPlaceOrder() : handleQuickAddDish()"
                >
                  {{ selectedTable?.status === 0 ? '确认点单' : '确认加菜' }}
                </NButton>

              </div>
            </template>

            <template v-else>
              <div class="drawer-panel" v-if="currentOrderDetail">
                <div class="drawer-panel__title">快捷结账</div>
                <div class="drawer-panel__desc">
                  {{ currentOrderDetail.status === 0
                    ? '支持整单结账，也支持先勾选部分菜品做分单结账。'
                    : '本桌当前桌次订单均已支付，确认结台后桌台将进入待清洁。' }}
                </div>

                <div v-if="currentOrderDetail.status === 0" class="drawer-mode-switch drawer-mode-switch--checkout" style="margin-top: 16px;">
                  <button class="drawer-mode-switch__item" :class="{ 'drawer-mode-switch__item--active': checkoutMode === 'full' }" @click="checkoutMode = 'full'">
                    整单结账
                  </button>
                  <button class="drawer-mode-switch__item" :class="{ 'drawer-mode-switch__item--active': checkoutMode === 'split' }" @click="checkoutMode = 'split'">
                    分单结账
                  </button>
                </div>

                <NCard size="small" :bordered="true" class="drawer-order-card" style="margin-top: 16px;">
                  <NDescriptions :column="2" label-placement="left" size="small">
                    <NDescriptionsItem label="订单号">{{ currentOrderDetail.orderNo }}</NDescriptionsItem>
                    <NDescriptionsItem label="订单状态">{{ orderStatusMap[currentOrderDetail.status] || '未知' }}</NDescriptionsItem>
                    <NDescriptionsItem label="原价">¥{{ currentOrderDetail.originalAmount?.toFixed(2) }}</NDescriptionsItem>
                    <NDescriptionsItem :label="currentOrderDetail.status === 0 ? (checkoutMode === 'full' ? '待收' : '已选小计') : '支付状态'">
                      <template v-if="currentOrderDetail.status === 0">
                        ¥{{ (checkoutMode === 'full' ? currentOrderRemainingAmount : splitTotal).toFixed(2) }}
                      </template>
                      <NTag v-else type="success" size="small">已付清</NTag>
                    </NDescriptionsItem>
                  </NDescriptions>
                </NCard>

                <div v-if="currentOrderDetail.status !== 0">
                  <NButton type="warning" block style="margin-top: 16px;" @click="handleQuickCheckout">
                    确认结台
                  </NButton>
                </div>

                <div v-else-if="checkoutMode === 'full'">
                  <div class="checkout-amount">
                    <span>收款金额</span>
                    <NInputNumber v-model:value="receivedAmount" :min="0" :precision="2" style="width: 100%;">
                      <template #prefix>¥</template>
                    </NInputNumber>
                  </div>

                  <NButton type="warning" block style="margin-top: 16px;" @click="handleQuickCheckout">
                    确认结账
                  </NButton>
                </div>

                <div v-else class="split-checkout">
                  <div v-if="splitSelectableItems.length > 0" class="split-checkout__list">
                    <button
                      v-for="item in splitSelectableItems"
                      :key="item.id"
                      type="button"
                      class="split-checkout__item"
                      :class="{ 'split-checkout__item--active': splitCheckedItemIds.includes(item.id) }"
                      @click="toggleSplitItem(item.id)"
                    >
                      <div>
                        <strong>{{ item.dishName }}</strong>
                        <span>¥{{ item.price.toFixed(2) }} · {{ item.quantity }}份</span>
                      </div>
                      <div class="split-checkout__side">
                        <span>¥{{ item.amount.toFixed(2) }}</span>
                        <NTag size="small" :type="splitCheckedItemIds.includes(item.id) ? 'warning' : getPaymentStatusMeta(item.paymentStatus).type">
                          {{ splitCheckedItemIds.includes(item.id) ? '已选' : getPaymentStatusMeta(item.paymentStatus).label }}
                        </NTag>
                      </div>
                    </button>
                  </div>
                  <NEmpty v-else description="当前可分单菜品已全部支付" style="margin-top: 12px;" />

                  <NButton type="warning" block style="margin-top: 16px;" @click="handleQuickSplitCheckout">
                    确认分单结账
                  </NButton>
                </div>
              </div>
              <NEmpty v-else description="当前没有可结账订单" />
            </template>
          </NSpin>
        </NSpace>
      </NDrawerContent>
    </NDrawer>
  </NSpin>
</template>

<style scoped>
.board-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.board-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.board-hero__title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #123055;
}

.board-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.board-hero__focus {
  min-width: 220px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
  box-shadow:
    0 18px 34px rgba(15, 57, 119, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.board-hero__focus span,
.board-hero__focus small {
  display: block;
}

.board-hero__focus span {
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.board-hero__focus strong {
  display: block;
  margin-top: 8px;
  font-size: 32px;
  line-height: 1;
  color: #0f6fff;
}

.board-hero__focus small {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.board-hero__stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.board-hero__stat {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow:
    0 16px 30px rgba(15, 57, 119, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.board-hero__stat span,
.board-hero__stat strong {
  display: block;
}

.board-hero__stat span {
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.board-hero__stat strong {
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  color: #123055;
}

.board-hero__stat[data-tone='success'] strong {
  color: #2f8f6b;
}

.board-hero__stat[data-tone='error'] strong {
  color: #d9485f;
}

.board-hero__stat[data-tone='info'] strong {
  color: #14a3ff;
}

.board-hero__stat[data-tone='warning'] strong {
  color: #dd8b1c;
}

.board-card {
  --board-surface-bg:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 24%),
    linear-gradient(180deg, rgba(251, 253, 255, 0.94), color-mix(in srgb, var(--admin-layout-start) 46%, white));
  --board-panel-bg:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), color-mix(in srgb, var(--admin-layout-start) 62%, white));
  --board-panel-border: rgba(var(--admin-accent-rgb), 0.1);
  --board-panel-shadow:
    0 18px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
  --board-panel-text: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
  --board-panel-muted: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
  --board-chip-bg: rgba(var(--admin-accent-rgb), 0.08);
  --board-chip-border: rgba(var(--admin-accent-rgb), 0.12);
  --board-chip-text: var(--admin-accent-strong);
  --table-status-free-bg:
    radial-gradient(circle at top, rgba(47, 143, 107, 0.18) 0%, transparent 54%),
    linear-gradient(180deg, rgba(239, 249, 244, 0.98), rgba(229, 245, 237, 0.98));
  --table-status-busy-bg:
    radial-gradient(circle at top, rgba(217, 72, 95, 0.16) 0%, transparent 54%),
    linear-gradient(180deg, rgba(255, 245, 247, 0.98), rgba(255, 237, 241, 0.98));
  --table-status-paid-bg:
    radial-gradient(circle at top, rgba(20, 163, 255, 0.16) 0%, transparent 54%),
    linear-gradient(180deg, rgba(239, 249, 255, 0.98), rgba(231, 244, 255, 0.98));
  --table-status-cleaning-bg:
    radial-gradient(circle at top, rgba(221, 139, 28, 0.18) 0%, transparent 54%),
    linear-gradient(180deg, rgba(255, 248, 239, 0.98), rgba(255, 242, 225, 0.98));
  --table-status-free-border: rgba(47, 143, 107, 0.34);
  --table-status-busy-border: rgba(217, 72, 95, 0.3);
  --table-status-paid-border: rgba(20, 163, 255, 0.3);
  --table-status-cleaning-border: rgba(221, 139, 28, 0.34);
  --table-status-free-chip: rgba(240, 251, 246, 0.84);
  --table-status-busy-chip: rgba(255, 244, 246, 0.84);
  --table-status-paid-chip: rgba(241, 250, 255, 0.84);
  --table-status-cleaning-chip: rgba(255, 247, 237, 0.84);
  --table-status-free-chip-text: #1d6f52;
  --table-status-busy-chip-text: #b93a51;
  --table-status-paid-chip-text: #0f78b8;
  --table-status-cleaning-chip-text: #b06a11;
  --table-status-free-glow: rgba(47, 143, 107, 0.2);
  --table-status-busy-glow: rgba(217, 72, 95, 0.2);
  --table-status-paid-glow: rgba(20, 163, 255, 0.22);
  --table-status-cleaning-glow: rgba(221, 139, 28, 0.22);
  overflow: hidden;
  backdrop-filter: blur(16px);
  background: var(--board-surface-bg) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
  box-shadow:
    0 28px 60px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.board-control-card {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 22%),
    linear-gradient(180deg, rgba(252, 254, 255, 0.96), rgba(244, 249, 255, 0.94)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  box-shadow:
    0 18px 34px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.board-card__header {
  display: grid;
  gap: 18px;
}

.board-card__header--compact {
  gap: 12px;
}

.board-card__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding-top: 4px;
}

.board-card__toolbar--compact {
  gap: 14px;
  padding-top: 0;
}

.board-card__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(15, 62, 124, 0.64);
}

.board-card__title {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: #123e7c;
}

.board-card__subline {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.board-card__actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.board-surface-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.board-surface-divider {
  height: 1px;
  margin-bottom: 14px;
  background:
    linear-gradient(90deg,
      rgba(15, 111, 255, 0),
      rgba(15, 111, 255, 0.14) 12%,
      rgba(15, 111, 255, 0.18) 50%,
      rgba(15, 111, 255, 0.14) 88%,
      rgba(15, 111, 255, 0)
    );
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.7);
}

.board-surface-head__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: nowrap;
  min-width: 0;
}

.board-view-switch {
  display: inline-grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  padding: 4px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.board-view-switch--header {
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
}

.board-view-switch__item {
  min-width: 64px;
  min-height: 34px;
  padding: 7px 12px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: rgba(21, 44, 76, 0.68);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, background-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.board-view-switch__item:hover {
  color: #123e7c;
}

.board-view-switch__item--active {
  color: #0f6fff;
  background: rgba(15, 111, 255, 0.12);
  box-shadow:
    0 10px 20px rgba(15, 57, 119, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.board-card__legend {
  flex-wrap: nowrap;
  justify-content: flex-end;
  align-items: center;
  min-width: 0;
}

.board-card__legend-item {
  min-height: 34px;
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(15, 111, 255, 0.1);
  color: rgba(21, 44, 76, 0.74);
  font-size: 12px;
}

.board-refresh-btn {
  flex-shrink: 0;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  --n-border: 1px solid rgba(15, 111, 255, 0.1);
  --n-border-hover: 1px solid rgba(15, 111, 255, 0.16);
  --n-border-pressed: 1px solid rgba(15, 111, 255, 0.16);
  --n-border-focus: 1px solid rgba(15, 111, 255, 0.16);
  --n-color: rgba(255, 255, 255, 0.7);
  --n-color-hover: rgba(255, 255, 255, 0.82);
  --n-color-pressed: rgba(255, 255, 255, 0.82);
  --n-text-color: rgba(21, 44, 76, 0.74);
  --n-text-color-hover: #123e7c;
  --n-text-color-pressed: #123e7c;
}

.board-refresh-btn--inline {
  width: auto;
}

.board-hero__stats--compact {
  margin-top: 0;
}

.board-control-card .board-hero__title {
  font-size: 22px;
}

.board-control-card .board-hero__desc {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.board-control-card .board-hero__focus {
  min-width: 200px;
  padding: 14px 16px;
  border-radius: 18px;
}

.board-control-card .board-hero__focus strong {
  font-size: 26px;
}

.board-control-card .board-hero__stats {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.board-control-card .board-hero__stat {
  padding: 10px 12px;
  border-radius: 16px;
}

.board-control-card .board-hero__stat strong {
  margin-top: 6px;
  font-size: 22px;
}

.board-control-card .board-card__subline {
  margin-top: 4px;
}

.board-stage {
  display: grid;
  gap: 14px;
}

.board-top-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;
}

.board-stage__main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 232px;
  gap: 14px;
  align-items: start;
}

.board-surface__main {
  min-width: 0;
}

.board-insight-panel {
  display: grid;
  gap: 10px;
}

.board-insight-focus,
.board-insight-stat {
  padding: 12px 14px;
  border-radius: 18px;
  background: var(--board-panel-bg);
  border: 1px solid var(--board-panel-border);
  box-shadow: var(--board-panel-shadow);
}

.board-insight-focus span,
.board-insight-focus small,
.board-insight-stat span,
.board-insight-stat strong {
  display: block;
}

.board-insight-focus span,
.board-insight-focus small,
.board-insight-stat span {
  color: var(--board-panel-muted);
}

.board-insight-focus span,
.board-insight-stat span {
  font-size: 12px;
}

.board-insight-focus strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  line-height: 1;
  color: var(--admin-accent-strong);
}

.board-insight-focus small {
  margin-top: 8px;
  font-size: 11px;
  line-height: 1.6;
}

.board-insight-stats {
  display: grid;
  gap: 8px;
  grid-template-columns: 1fr;
}

.board-insight-stat strong {
  margin-top: 6px;
  font-size: 22px;
  line-height: 1;
  color: var(--board-panel-text);
}

.board-insight-stat[data-tone='success'] strong {
  color: #2f8f6b;
}

.board-insight-stat[data-tone='error'] strong {
  color: #d9485f;
}

.board-insight-stat[data-tone='info'] strong {
  color: #14a3ff;
}

.board-insight-stat[data-tone='warning'] strong {
  color: #dd8b1c;
}

.board-filter-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.board-filter-list--compact {
  width: 100%;
}

.board-filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 7px 12px;
  border: 1px solid rgba(15, 111, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.board-filter-item--compact {
  justify-content: space-between;
  min-width: 168px;
}

.board-filter-item:hover,
.board-filter-item--active {
  transform: translateY(-2px);
  border-color: rgba(15, 111, 255, 0.16);
  box-shadow:
    0 12px 22px rgba(15, 57, 119, 0.1),
    0 6px 16px rgba(8, 27, 58, 0.05);
}

.board-filter-item strong,
.board-filter-item span {
  display: block;
}

.board-filter-item strong {
  font-size: 12px;
  color: #173d69;
}

.board-filter-item span {
  font-size: 12px;
  font-weight: 700;
  color: rgba(23, 61, 105, 0.58);
}

.board-surface {
  position: relative;
  overflow: hidden;
  padding: 4px 2px 2px;
}

.board-surface__aurora {
  position: absolute;
  z-index: 0;
  border-radius: 999px;
  filter: blur(28px);
  opacity: 0.55;
  pointer-events: none;
}

.board-surface__aurora--primary {
  top: -40px;
  right: 12%;
  width: 240px;
  height: 240px;
  background: radial-gradient(circle, rgba(15, 111, 255, 0.18) 0%, rgba(15, 111, 255, 0) 72%);
}

.board-surface__aurora--secondary {
  left: 8%;
  bottom: -32px;
  width: 220px;
  height: 220px;
  background: radial-gradient(circle, rgba(20, 163, 255, 0.14) 0%, rgba(20, 163, 255, 0) 72%);
}

.board-panorama {
  position: relative;
  z-index: 1;
  padding: 16px;
  border-radius: 24px;
  background: var(--board-panel-bg);
  border: 1px solid var(--board-panel-border);
  box-shadow: var(--board-panel-shadow);
}

.board-zone {
  position: relative;
  z-index: 1;
  margin-bottom: 14px;
  padding: 16px;
  border-radius: 24px;
  background: var(--board-panel-bg);
  border: 1px solid var(--board-panel-border);
  box-shadow: var(--board-panel-shadow);
}

.board-zone:last-child {
  margin-bottom: 0;
}

.board-zone__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.board-zone__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--board-panel-text);
}

.board-zone__meta {
  margin-top: 4px;
  font-size: 12px;
  color: var(--board-panel-muted);
}

.board-zone__pill {
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--board-chip-bg);
  border: 1px solid var(--board-chip-border);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--board-chip-text);
}

.board-zone__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 144px);
  justify-content: start;
  gap: 14px;
}

.board-zone__cell {
  min-width: 0;
}

.board-zone__grid--overview {
  grid-template-columns: repeat(auto-fill, 116px);
  gap: 10px;
}

.table-card {
  --table-accent: var(--table-status-free-accent);
  --table-card-bg: var(--table-status-free-bg);
  --table-card-border: var(--table-status-free-border);
  --table-chip-bg: var(--table-status-free-chip);
  --table-chip-text: var(--table-status-free-chip-text);
  --table-glow: var(--table-status-free-glow);
  position: relative;
  border: 2px solid;
  border-radius: 24px;
  padding: 14px 10px 10px;
  text-align: center;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  height: 132px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  background: var(--table-card-bg);
  border-color: var(--table-card-border);
  box-shadow:
    0 24px 54px rgba(15, 57, 119, 0.16),
    0 10px 24px rgba(8, 27, 58, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
  isolation: isolate;
}

.table-card[data-status='0'] {
  --table-accent: var(--table-status-free-accent);
  --table-card-bg: var(--table-status-free-bg);
  --table-card-border: var(--table-status-free-border);
  --table-chip-bg: var(--table-status-free-chip);
  --table-chip-text: var(--table-status-free-chip-text);
  --table-glow: var(--table-status-free-glow);
}

.table-card[data-status='1'] {
  --table-accent: var(--table-status-busy-accent);
  --table-card-bg: var(--table-status-busy-bg);
  --table-card-border: var(--table-status-busy-border);
  --table-chip-bg: var(--table-status-busy-chip);
  --table-chip-text: var(--table-status-busy-chip-text);
  --table-glow: var(--table-status-busy-glow);
}

.table-card[data-status='2'] {
  --table-accent: var(--table-status-paid-accent);
  --table-card-bg: var(--table-status-paid-bg);
  --table-card-border: var(--table-status-paid-border);
  --table-chip-bg: var(--table-status-paid-chip);
  --table-chip-text: var(--table-status-paid-chip-text);
  --table-glow: var(--table-status-paid-glow);
}

.table-card[data-status='3'] {
  --table-accent: var(--table-status-cleaning-accent);
  --table-card-bg: var(--table-status-cleaning-bg);
  --table-card-border: var(--table-status-cleaning-border);
  --table-chip-bg: var(--table-status-cleaning-chip);
  --table-chip-text: var(--table-status-cleaning-chip-text);
  --table-glow: var(--table-status-cleaning-glow);
}

.table-card--overview {
  height: 116px;
  padding: 12px 9px 9px;
  border-radius: 20px;
}

.table-card--focus {
  height: 148px;
  padding: 16px 12px 12px;
}

.table-card:hover {
  transform: translateY(-8px);
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--table-accent) 16%, transparent),
    0 0 24px var(--table-glow),
    0 30px 62px rgba(15, 57, 119, 0.2),
    0 16px 30px rgba(8, 27, 58, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.table-card__halo {
  position: absolute;
  top: -28px;
  left: 50%;
  width: 96px;
  height: 96px;
  border-radius: 999px;
  background: radial-gradient(circle, color-mix(in srgb, var(--table-accent) 20%, white) 0%, transparent 72%);
  transform: translateX(-50%);
  opacity: 0.8;
  pointer-events: none;
}

.table-card__status {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 2;
}

.table-card__area {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  max-width: calc(100% - 84px);
  padding: 3px 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--table-chip-bg);
  border: 1px solid color-mix(in srgb, var(--table-accent) 18%, rgba(255, 255, 255, 0.62));
  color: var(--table-chip-text);
  font-size: 10px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-icon {
  position: relative;
  z-index: 1;
  margin-bottom: 4px;
}

.table-card--overview .table-icon :deep(svg) {
  width: 32px;
  height: 32px;
}

.table-card--focus .table-icon :deep(svg) {
  width: 44px;
  height: 44px;
}

.table-name {
  position: relative;
  z-index: 1;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.2;
  color: #123055;
}

.table-meta {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
  margin-top: 6px;
}

.table-meta span {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 10px;
  color: var(--table-chip-text);
  background: color-mix(in srgb, var(--table-chip-bg) 90%, white);
  border: 1px solid color-mix(in srgb, var(--table-accent) 16%, rgba(255, 255, 255, 0.56));
}

.table-action {
  position: relative;
  z-index: 1;
  margin-top: 6px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: rgba(21, 44, 76, 0.52);
}

.board-card--overview .board-surface {
  padding-top: 2px;
}

.board-card--overview .board-zone {
  margin-bottom: 10px;
  padding: 14px;
}

.board-card--overview .board-panorama {
  padding: 14px;
}

.board-card--overview .board-zone__header {
  margin-bottom: 10px;
}

.board-card--overview .board-zone__grid {
  grid-template-columns: repeat(auto-fill, 112px);
  gap: 10px;
}

.board-card--overview .board-zone__title {
  font-size: 16px;
}

.board-card--overview .board-zone__meta {
  font-size: 11px;
}

.board-card--overview .board-zone__grid--overview {
  grid-template-columns: repeat(auto-fill, 108px);
  gap: 9px;
}

.board-card--focus .board-zone {
  padding: 18px;
}

.board-card--focus .board-zone__grid {
  grid-template-columns: repeat(auto-fill, 168px);
  gap: 16px;
}

.board-empty {
  padding: 56px 0 34px;
  text-align: center;
  color: #7a8ca8;
}

.drawer-summary {
  padding: 18px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(250, 253, 255, 0.96), rgba(242, 248, 255, 0.88));
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow:
    0 20px 40px rgba(15, 57, 119, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.drawer-summary__status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.drawer-summary__label {
  font-size: 12px;
  font-weight: 600;
  color: #173d69;
}

.drawer-summary__meta {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.drawer-summary__meta span {
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  color: rgba(23, 61, 105, 0.72);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.drawer-summary__notice {
  margin-top: 10px;
  padding: 8px 10px;
  border-radius: 14px;
  font-size: 11px;
  line-height: 1.55;
  color: rgba(133, 74, 12, 0.92);
  background: rgba(255, 245, 232, 0.92);
  border: 1px solid rgba(221, 139, 28, 0.16);
}

.drawer-summary__clean-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.drawer-summary__clean-actions :deep(.n-button) {
  min-width: 0;
  height: 32px;
  padding: 0 12px;
  border-radius: 12px;
}

.drawer-mode-switch {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.drawer-mode-switch--checkout {
  grid-template-columns: repeat(2, 1fr);
}

.drawer-mode-switch__item {
  padding: 10px 12px;
  border: 1px solid rgba(15, 111, 255, 0.1);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  color: #173d69;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.drawer-mode-switch__item:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.drawer-mode-switch__item--active {
  transform: translateY(-2px);
  border-color: rgba(15, 111, 255, 0.18);
  box-shadow: 0 16px 30px rgba(15, 57, 119, 0.1);
}

.drawer-panel {
  padding: 18px;
  border-radius: 24px;
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.1), transparent 28%),
    linear-gradient(180deg, rgba(250, 253, 255, 0.96), rgba(242, 248, 255, 0.88));
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.drawer-panel__title {
  font-size: 18px;
  font-weight: 700;
  color: #123e7c;
}

.drawer-panel__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.7;
  color: rgba(21, 44, 76, 0.68);
}

.drawer-dish-browser {
  margin-top: 16px;
}

.drawer-dish-browser__categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.drawer-category-chip {
  padding: 7px 12px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.76);
  color: #173d69;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.drawer-category-chip--active {
  transform: translateY(-1px);
  color: var(--admin-accent-strong);
  border-color: rgba(var(--admin-accent-rgb), 0.18);
  box-shadow: 0 10px 18px rgba(var(--admin-accent-rgb), 0.08);
}

.drawer-dish-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  max-height: 280px;
  margin-top: 14px;
  overflow-y: auto;
}

.drawer-dish-card {
  padding: 12px 12px 11px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  border-radius: 18px;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    rgba(255, 255, 255, 0.78);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.drawer-dish-card:hover,
.drawer-dish-card--active {
  transform: translateY(-2px);
  border-color: rgba(var(--admin-accent-rgb), 0.18);
  box-shadow: 0 14px 24px rgba(var(--admin-accent-rgb), 0.1);
}

.drawer-dish-card--in-cart {
  border-color: rgba(var(--admin-accent-rgb), 0.22);
  box-shadow:
    0 16px 28px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 0 0 1px rgba(var(--admin-accent-rgb), 0.08);
}

.drawer-dish-card__title {
  font-size: 14px;
  font-weight: 700;
  color: #123055;
}

.drawer-dish-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 6px;
  font-size: 11px;
  color: #5b7195;
}

.drawer-dish-card__feedback {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--admin-accent-strong);
}

.drawer-dish-preview {
  padding: 12px 14px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.drawer-dish-preview strong,
.drawer-dish-preview span {
  display: block;
}

.drawer-dish-preview strong {
  font-size: 14px;
  color: #123055;
}

.drawer-dish-preview span {
  margin-top: 4px;
  font-size: 12px;
  color: #5b7195;
}

.drawer-quick-compose {
  position: sticky;
  bottom: 0;
  z-index: 3;
  margin-top: 16px;
  padding-top: 12px;
  background:
    linear-gradient(180deg, rgba(250, 253, 255, 0) 0%, rgba(250, 253, 255, 0.92) 16%, rgba(250, 253, 255, 0.98) 100%);
  backdrop-filter: blur(8px);
}

.drawer-quick-compose__button {
  width: 100%;
}

.drawer-quick-compose__confirm {
  margin-top: 16px;
}

.drawer-order-card {
  border-radius: 18px;
  box-shadow: 0 14px 28px rgba(15, 57, 119, 0.06);
}

.drawer-order-items {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(var(--admin-accent-rgb), 0.08);
}

.drawer-order-items__title {
  font-size: 13px;
  font-weight: 700;
  color: #123055;
}

.drawer-order-items__list {
  display: grid;
  gap: 10px;
  margin-top: 10px;
}

.drawer-order-items__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.08);
}

.drawer-order-items__item strong,
.drawer-order-items__item span,
.drawer-order-items__side span,
.drawer-order-items__side small {
  display: block;
}

.drawer-order-items__item strong {
  font-size: 13px;
  color: #123055;
}

.drawer-order-items__item span {
  margin-top: 4px;
  font-size: 11px;
  color: #5b7195;
}

.drawer-order-items__side {
  text-align: right;
  flex-shrink: 0;
}

.drawer-order-items__side span {
  font-size: 13px;
  font-weight: 700;
  color: var(--admin-accent-strong);
}

.drawer-order-items__side small {
  margin-top: 4px;
  font-size: 11px;
  color: #7a8ca8;
}

.drawer-cart {
  margin-top: 16px;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.drawer-cart__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.drawer-cart__head strong {
  font-size: 14px;
  color: #123055;
}

.drawer-cart__head span {
  font-size: 13px;
  font-weight: 700;
  color: #0f6fff;
}

.drawer-cart__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid rgba(15, 111, 255, 0.08);
}

.drawer-cart__item:first-of-type {
  border-top: 0;
  padding-top: 0;
}

.drawer-cart__item strong,
.drawer-cart__item span {
  display: block;
}

.drawer-cart__item strong {
  font-size: 13px;
  color: #123055;
}

.drawer-cart__item span {
  margin-top: 4px;
  font-size: 11px;
  color: #5b7195;
}

.drawer-cart__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.checkout-amount {
  margin-top: 16px;
}

.checkout-amount span {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #173d69;
}

.split-checkout__list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.split-checkout__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  text-align: left;
  border: 1px solid rgba(15, 111, 255, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.split-checkout__item--active {
  transform: translateY(-2px);
  border-color: rgba(221, 139, 28, 0.22);
  box-shadow: 0 16px 28px rgba(221, 139, 28, 0.12);
}

.split-checkout__item strong,
.split-checkout__item span {
  display: block;
}

.split-checkout__item strong {
  font-size: 13px;
  color: #123055;
}

.split-checkout__item span {
  margin-top: 4px;
  font-size: 11px;
  color: #5b7195;
}

.split-checkout__side {
  text-align: right;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .board-surface-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-surface-head__actions {
    width: 100%;
    justify-content: flex-start;
  }

  .board-top-toolbar {
    gap: 10px;
  }

  .board-stage__main {
    grid-template-columns: 1fr;
  }

  .board-insight-stats {
    width: 100%;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .board-card__actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .board-card__actions {
    width: 100%;
  }

  .board-view-switch {
    width: 100%;
  }

  .board-card__legend {
    flex-wrap: wrap;
  }

  .board-filter-item {
    width: 100%;
    min-width: 0;
  }

  .board-zone__grid,
  .board-zone__grid--overview,
  .board-card--overview .board-zone__grid,
  .board-card--overview .board-zone__grid--overview,
  .board-card--focus .board-zone__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .board-zone {
    padding: 14px;
  }

  .drawer-dish-grid {
    grid-template-columns: 1fr;
  }

  .drawer-mode-switch {
    grid-template-columns: 1fr;
  }

  .drawer-summary__clean-actions {
    flex-direction: column;
  }

  .split-checkout__item {
    align-items: flex-start;
    flex-direction: column;
  }

  .split-checkout__side {
    text-align: left;
  }
}

html.dark .board-card {
  --board-surface-bg:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(180deg, rgba(8, 12, 20, 0.98), color-mix(in srgb, #0a1018 84%, var(--admin-layout-end)));
  --board-panel-bg:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.1), transparent 26%),
    linear-gradient(180deg, rgba(13, 18, 28, 0.98), rgba(9, 13, 21, 0.98));
  --board-panel-border: rgba(255, 255, 255, 0.06);
  --board-panel-shadow:
    0 18px 36px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
  --board-panel-text: rgba(241, 246, 255, 0.96);
  --board-panel-muted: rgba(173, 188, 216, 0.72);
  --board-chip-bg: rgba(var(--admin-accent-rgb), 0.16);
  --board-chip-border: rgba(var(--admin-accent-rgb), 0.2);
  --board-chip-text: #dbe5ff;
  --table-status-free-bg:
    radial-gradient(circle at top, rgba(47, 143, 107, 0.2) 0%, transparent 50%),
    linear-gradient(180deg, rgba(10, 24, 22, 0.96), rgba(7, 16, 18, 0.98));
  --table-status-busy-bg:
    radial-gradient(circle at top, rgba(217, 72, 95, 0.22) 0%, transparent 52%),
    linear-gradient(180deg, rgba(28, 14, 20, 0.96), rgba(18, 9, 14, 0.98));
  --table-status-paid-bg:
    radial-gradient(circle at top, rgba(20, 163, 255, 0.22) 0%, transparent 52%),
    linear-gradient(180deg, rgba(10, 18, 30, 0.96), rgba(7, 12, 21, 0.98));
  --table-status-cleaning-bg:
    radial-gradient(circle at top, rgba(221, 139, 28, 0.22) 0%, transparent 52%),
    linear-gradient(180deg, rgba(30, 20, 10, 0.96), rgba(20, 13, 7, 0.98));
  --table-status-free-border: color-mix(in srgb, var(--table-status-free-accent) 48%, rgba(255, 255, 255, 0.16));
  --table-status-busy-border: color-mix(in srgb, var(--table-status-busy-accent) 48%, rgba(255, 255, 255, 0.16));
  --table-status-paid-border: color-mix(in srgb, var(--table-status-paid-accent) 48%, rgba(255, 255, 255, 0.16));
  --table-status-cleaning-border: color-mix(in srgb, var(--table-status-cleaning-accent) 48%, rgba(255, 255, 255, 0.16));
  --table-status-free-chip: rgba(10, 32, 27, 0.72);
  --table-status-busy-chip: rgba(37, 14, 21, 0.72);
  --table-status-paid-chip: rgba(10, 20, 36, 0.72);
  --table-status-cleaning-chip: rgba(39, 24, 10, 0.72);
  --table-status-free-chip-text: rgba(194, 237, 218, 0.92);
  --table-status-busy-chip-text: rgba(255, 204, 214, 0.92);
  --table-status-paid-chip-text: rgba(190, 228, 255, 0.92);
  --table-status-cleaning-chip-text: rgba(255, 222, 181, 0.92);
  --table-status-free-glow: rgba(47, 143, 107, 0.18);
  --table-status-busy-glow: rgba(217, 72, 95, 0.18);
  --table-status-paid-glow: rgba(20, 163, 255, 0.2);
  --table-status-cleaning-glow: rgba(221, 139, 28, 0.2);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.14), transparent 24%),
    linear-gradient(180deg, rgba(8, 12, 20, 0.96), rgba(14, 19, 30, 0.98)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 28px 60px rgba(0, 0, 0, 0.34),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .board-hero__eyebrow,
html.dark .board-card__eyebrow,
html.dark .board-card__subline,
html.dark .board-hero__focus span,
html.dark .board-hero__focus small,
html.dark .board-hero__stat span,
html.dark .board-zone__meta,
html.dark .table-action,
html.dark .drawer-panel__desc,
html.dark .drawer-dish-preview span,
html.dark .drawer-cart__item span,
html.dark .split-checkout__item span {
  color: rgba(173, 188, 216, 0.72);
}

html.dark .board-hero__title,
html.dark .board-card__title,
html.dark .board-zone__title,
html.dark .table-name,
html.dark .board-insight-focus strong,
html.dark .board-insight-stat strong,
html.dark .board-hero__stat strong,
html.dark .drawer-panel__title,
html.dark .drawer-dish-preview strong,
html.dark .drawer-cart__head strong,
html.dark .drawer-cart__item strong,
html.dark .split-checkout__item strong {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .board-hero__focus,
html.dark .board-hero__stat,
html.dark .board-insight-focus,
html.dark .board-insight-stat,
html.dark .board-card__legend-item,
html.dark .board-filter-panel,
html.dark .board-panorama,
html.dark .board-zone,
html.dark .drawer-summary,
html.dark .drawer-panel,
html.dark .drawer-cart,
html.dark .drawer-dish-preview,
html.dark .split-checkout__item,
html.dark .drawer-mode-switch__item,
html.dark .board-view-switch {
  background: linear-gradient(180deg, rgba(12, 17, 28, 0.96), rgba(8, 12, 20, 0.96));
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 18px 36px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .board-card__legend-item,
html.dark .board-empty,
html.dark .board-insight-focus span,
html.dark .board-insight-focus small,
html.dark .board-insight-stat span,
html.dark .drawer-summary__label,
html.dark .drawer-summary__meta span,
html.dark .drawer-mode-switch__item,
html.dark .board-view-switch__item,
html.dark .checkout-amount span {
  color: rgba(217, 226, 241, 0.86);
}

html.dark .board-view-switch__item--active {
  color: #f6fbff;
  background: rgba(var(--admin-accent-rgb), 0.22);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.26);
}

html.dark .board-surface-divider {
  background:
    linear-gradient(90deg,
      rgba(var(--admin-accent-rgb), 0),
      rgba(var(--admin-accent-rgb), 0.18) 12%,
      rgba(var(--admin-accent-rgb), 0.28) 50%,
      rgba(var(--admin-accent-rgb), 0.18) 88%,
      rgba(var(--admin-accent-rgb), 0)
    );
  box-shadow: 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .board-filter-item {
  background: rgba(255, 255, 255, 0.04);
}

html.dark .drawer-category-chip,
html.dark .drawer-dish-card {
  background: linear-gradient(180deg, rgba(12, 17, 28, 0.96), rgba(8, 12, 20, 0.96));
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 12px 24px rgba(0, 0, 0, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .drawer-order-items {
  border-top-color: rgba(255, 255, 255, 0.06);
}

html.dark .drawer-order-items__title,
html.dark .drawer-order-items__item strong {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .drawer-order-items__item,
html.dark .drawer-order-items__side small {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.06);
  color: rgba(170, 186, 216, 0.72);
}

html.dark .drawer-order-items__item span {
  color: rgba(170, 186, 216, 0.72);
}

html.dark .drawer-quick-compose {
  background:
    linear-gradient(180deg, rgba(9, 13, 21, 0) 0%, rgba(9, 13, 21, 0.92) 16%, rgba(9, 13, 21, 0.98) 100%);
}

html.dark .drawer-dish-card--in-cart {
  border-color: rgba(var(--admin-accent-rgb), 0.26);
  box-shadow:
    0 16px 28px rgba(0, 0, 0, 0.24),
    inset 0 0 0 1px rgba(var(--admin-accent-rgb), 0.14);
}

html.dark .drawer-category-chip,
html.dark .drawer-dish-card__meta {
  color: rgba(173, 188, 216, 0.78);
}

html.dark .drawer-dish-card__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .drawer-dish-card__feedback {
  color: #dbe5ff;
}

html.dark .board-filter-item strong {
  color: rgba(232, 238, 250, 0.92);
}

html.dark .board-filter-item span {
  color: rgba(170, 186, 216, 0.7);
}

html.dark .table-meta span {
  color: rgba(235, 241, 255, 0.94);
  border-color: rgba(255, 255, 255, 0.12);
}

html.dark .table-card__area {
  color: rgba(244, 247, 255, 0.96);
  border-color: rgba(255, 255, 255, 0.12);
}

html.dark .board-filter-item:hover,
html.dark .board-filter-item--active,
html.dark .drawer-mode-switch__item--active,
html.dark .split-checkout__item--active {
  border-color: rgba(var(--admin-accent-rgb), 0.24);
  box-shadow: 0 20px 34px rgba(0, 0, 0, 0.34);
}

html.dark .board-zone__pill,
html.dark .drawer-cart__head span {
  color: #dbe5ff;
  background: rgba(var(--admin-accent-rgb), 0.16);
  border-color: rgba(var(--admin-accent-rgb), 0.2);
}

html.dark .table-card {
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--table-accent) 18%, transparent),
    0 24px 54px rgba(0, 0, 0, 0.28),
    0 10px 24px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .table-card__halo {
  background: radial-gradient(circle, color-mix(in srgb, var(--table-accent) 36%, white) 0%, transparent 74%);
  opacity: 0.52;
}

html.dark .table-card :deep(.n-tag) {
  background: color-mix(in srgb, var(--table-accent) 68%, rgba(7, 12, 20, 0.92)) !important;
  color: #f8fbff !important;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.22);
}

html.dark .table-name {
  color: rgba(246, 249, 255, 0.98);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.28);
}

html.dark .table-action {
  color: rgba(214, 225, 247, 0.86);
}

html.dark .table-icon {
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.22));
}

html.dark .table-card:hover {
  border-color: color-mix(in srgb, var(--table-accent) 64%, rgba(255, 255, 255, 0.22)) !important;
  box-shadow:
    0 0 0 1px color-mix(in srgb, var(--table-accent) 24%, transparent),
    0 0 26px var(--table-glow),
    0 30px 62px rgba(0, 0, 0, 0.36),
    0 16px 30px rgba(0, 0, 0, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

html.dark .drawer-summary__meta span {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .drawer-summary__notice {
  color: #ffd7a1;
  background: rgba(76, 49, 16, 0.54);
  border-color: rgba(221, 139, 28, 0.2);
}

html.dark .drawer-cart__item {
  border-top-color: rgba(255, 255, 255, 0.06);
}
</style>
