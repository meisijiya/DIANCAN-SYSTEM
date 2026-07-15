<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import {
  NCard, NSpace, NGrid, NGi, NButton, NInput, NSelect,
  NTag, NSpin, NEmpty, NBadge, NModal, NList, NListItem, NThing, NImage,
  NCheckbox,
  NScrollbar, NDivider, NSkeleton, useMessage
} from 'naive-ui';
import type { SelectOption } from 'naive-ui';
import { useRoute } from 'vue-router';
import {
  fetchTableList, fetchDishCategoryList, fetchDishList, createAdminOrder
} from '@/service/api';
import {
  countOfflineAdminOrders,
  enqueueOfflineAdminOrder,
  listOfflineAdminOrders,
  markOfflineAdminOrderRetry,
  removeOfflineAdminOrder
} from '@/utils/offline-order-queue';

const message = useMessage();
const route = useRoute();
const loading = ref(false);
const isOffline = ref(!window.navigator.onLine);
const syncingOfflineOrders = ref(false);
const pendingOfflineCount = ref(0);

// ==================== 桌台选择 ====================
const tables = ref<Api.Business.DiningTable[]>([]);
const selectedTableId = ref<number | null>(null);

const tableOptions = computed<SelectOption[]>(() =>
  tables.value.map(t => ({
    label: `${t.areaName || '未分区'} · ${t.name}（${t.code}）- ${{ 0: '空闲', 1: '占用', 2: '已结账', 3: '待清洁' }[t.status] || '未知'}`,
    value: t.id
  }))
);

const selectedTable = computed(() => tables.value.find(t => t.id === selectedTableId.value));
const selectedTableHasActiveOrder = computed(() => selectedTable.value?.status === 1);
const selectedTableIsPaid = computed(() => selectedTable.value?.status === 2);
const selectedTableIsToClean = computed(() => selectedTable.value?.status === 3);

function initSelectedTableFromRoute() {
  const tableId = Number(route.query.tableId || 0);
  if (!tableId) return;
  const targetTable = tables.value.find(t => t.id === tableId);
  if (targetTable) selectedTableId.value = targetTable.id;
}

// ==================== 菜品分类与列表 ====================
const categories = ref<Api.Business.DishCategory[]>([]);
const activeCategoryId = ref<Api.Business.IdType | null>(null);
const allDishes = ref<Api.Business.Dish[]>([]);
const dishLoading = ref(false);
const searchKeyword = ref('');
const pageInitialized = ref(false);

const categoryDishCountMap = computed(() => {
  const map = new Map<string, number>();
  allDishes.value.forEach(dish => {
    const key = String(dish.categoryId ?? '');
    map.set(key, (map.get(key) || 0) + 1);
  });
  return map;
});

const dishList = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase();
  return allDishes.value.filter(dish => {
    const matchCategory = !activeCategoryId.value || String(dish.categoryId) === String(activeCategoryId.value);
    if (!matchCategory) return false;
    if (!keyword) return true;
    const searchText = [
      dish.name,
      dish.categoryName,
      dish.description,
      dish.ingredients
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return searchText.includes(keyword);
  });
});

/** 首屏加载全部菜品，后续分类与搜索统一走前端过滤 */
async function loadAllDishes(showSectionLoading = false) {
  if (showSectionLoading) {
    dishLoading.value = true;
  }
  try {
    const params: Api.Business.DishQuery & { pageNum: number; pageSize: number } = {
      pageNum: 1,
      pageSize: 500,
      status: 1
    };
    const { data, error } = await fetchDishList(params);
    if (!error && data) allDishes.value = data.list || [];
  } finally {
    if (showSectionLoading) {
      dishLoading.value = false;
    }
  }
}

/** 切换分类 */
function selectCategory(id: Api.Business.IdType | null) {
  activeCategoryId.value = id;
}

function handleSearch(val: string) {
  searchKeyword.value = val;
}

// ==================== 购物车（本地） ====================
interface CartItem {
  dishId: Api.Business.IdType;
  dishName: string;
  price: number;
  quantity: number;
  remark: string;
}

const cart = ref<CartItem[]>([]);
const cartButtonRef = ref<HTMLElement | null>(null);
const flyToCartToken = ref({
  visible: false,
  active: false,
  label: '',
  x: 0,
  y: 0,
  targetX: 0,
  targetY: 0
});
let flyTokenTimer: number | null = null;

const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + item.price * item.quantity, 0));
const cartCount = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0));
const cartTotalText = computed(() => cartTotal.value.toFixed(2));
const cartDishCountMap = computed(() => {
  const map = new Map<string, number>();
  cart.value.forEach(item => {
    map.set(String(item.dishId), item.quantity);
  });
  return map;
});

async function playFlyToCartEffect(dish: Api.Business.Dish, event?: MouseEvent) {
  const startElement = event?.currentTarget as HTMLElement | null;
  const cartElement = cartButtonRef.value;
  if (!startElement || !cartElement) return;

  const startRect = startElement.getBoundingClientRect();
  const cartRect = cartElement.getBoundingClientRect();

  if (flyTokenTimer) {
    window.clearTimeout(flyTokenTimer);
    flyTokenTimer = null;
  }

  flyToCartToken.value = {
    visible: true,
    active: false,
    label: `+1 ${dish.name}`,
    x: startRect.left + startRect.width / 2 - 42,
    y: startRect.top + 12,
    targetX: cartRect.left + cartRect.width / 2 - 42,
    targetY: cartRect.top + 6
  };

  await nextTick();
  requestAnimationFrame(() => {
    flyToCartToken.value.active = true;
  });

  flyTokenTimer = window.setTimeout(() => {
    flyToCartToken.value.visible = false;
    flyToCartToken.value.active = false;
    flyTokenTimer = null;
  }, 720);
}

/** 添加菜品到购物车 */
function addToCart(dish: Api.Business.Dish, event?: MouseEvent) {
  if (dish.soldOut === 1) {
    message.warning('该菜品已售罄');
    return;
  }
  const existing = cart.value.find(c => c.dishId === dish.id);
  if (existing) {
    existing.quantity++;
  } else {
    cart.value.push({ dishId: dish.id, dishName: dish.name, price: dish.price, quantity: 1, remark: '' });
  }
  void playFlyToCartEffect(dish, event);
}

/** 修改数量 */
function updateQuantity(dishId: Api.Business.IdType, qty: number) {
  if (qty <= 0) {
    cart.value = cart.value.filter(c => c.dishId !== dishId);
  } else {
    const item = cart.value.find(c => c.dishId === dishId);
    if (item) item.quantity = qty;
  }
}

/** 清空购物车 */
function clearCart() { cart.value = []; }

// ==================== 提交订单 ====================
const submitting = ref(false);
const showCartModal = ref(false);
const preOrderMode = ref(false);
function generateClientOrderNo() {
  return `OFF${Date.now()}${Math.floor(Math.random() * 9000 + 1000)}`;
}

function isNetworkLikeError(error: unknown) {
  if (!window.navigator.onLine) return true;
  const msg = String(error ?? '').toLowerCase();
  return msg.includes('network') || msg.includes('timeout') || msg.includes('failed to fetch');
}

async function refreshPendingOfflineCount() {
  pendingOfflineCount.value = await countOfflineAdminOrders();
}

async function saveOrderToOfflineQueue(payload: Api.Business.AdminOrderCreate, clientOrderNo: string) {
  await enqueueOfflineAdminOrder(payload, clientOrderNo);
  await refreshPendingOfflineCount();
}

async function syncOfflineOrders(showToast = false) {
  if (syncingOfflineOrders.value || isOffline.value) return;
  syncingOfflineOrders.value = true;
  try {
    const pendingOrders = await listOfflineAdminOrders();
    if (!pendingOrders.length) return;

    let successCount = 0;
    for (const record of pendingOrders) {
      const { error } = await createAdminOrder(record.payload);
      if (!error) {
        await removeOfflineAdminOrder(record.id!);
        successCount += 1;
        continue;
      }

      if (isNetworkLikeError(error)) {
        await markOfflineAdminOrderRetry(record.id!, String(error));
        break;
      }

      await markOfflineAdminOrderRetry(record.id!, String(error));
    }

    await refreshPendingOfflineCount();
    if (showToast && successCount > 0) {
      message.success(`已自动补传 ${successCount} 笔离线订单`);
    }
  } finally {
    syncingOfflineOrders.value = false;
  }
}

function handleOnline() {
  isOffline.value = false;
  syncOfflineOrders(true);
}

function handleOffline() {
  isOffline.value = true;
}

async function submitOrder() {
  if (!selectedTableId.value) {
    message.warning('请先选择桌台');
    return;
  }
  if (!preOrderMode.value && selectedTableIsPaid.value) {
    message.warning(`桌台 ${selectedTable.value?.code || ''} 已结账，请先完成清洁流转后再开台点单`);
    return;
  }
  if (!preOrderMode.value && selectedTableIsToClean.value) {
    message.warning(`桌台 ${selectedTable.value?.code || ''} 待清洁，请先完成清洁后再开台点单`);
    return;
  }
  if (!preOrderMode.value && selectedTableHasActiveOrder.value) {
    message.warning(`桌台 ${selectedTable.value?.code || ''} 已有进行中订单，请前往桌台看板执行加菜`);
    return;
  }
  if (cart.value.length === 0) {
    message.warning('请先添加菜品');
    return;
  }
  submitting.value = true;
  try {
    const table = selectedTable.value;
    const clientOrderNo = generateClientOrderNo();
    const payload: Api.Business.AdminOrderCreate = {
      tableId: selectedTableId.value,
      tableCode: table?.code,
      clientOrderNo,
      items: cart.value.map(c => ({ dishId: c.dishId, quantity: c.quantity, remark: c.remark || undefined })),
      paymentMode: 1, // 餐后付
      orderType: 0,
      preOrder: preOrderMode.value
    };

    if (isOffline.value) {
      await saveOrderToOfflineQueue(payload, clientOrderNo);
      message.warning('当前离线，订单已缓存，联网后自动补传');
      clearCart();
      showCartModal.value = false;
      preOrderMode.value = false;
      return;
    }

    const { error } = await createAdminOrder(payload);
    if (!error) {
      message.success(preOrderMode.value ? '预订单已保存' : '下单成功');
      clearCart();
      showCartModal.value = false;
      preOrderMode.value = false;
      await refreshPendingOfflineCount();
      return;
    }

    if (isNetworkLikeError(error)) {
      await saveOrderToOfflineQueue(payload, clientOrderNo);
      message.warning('网络异常，订单已转入离线队列，恢复后自动补传');
      clearCart();
      showCartModal.value = false;
      preOrderMode.value = false;
    }
  } finally { submitting.value = false; }
}

onMounted(async () => {
  window.addEventListener('online', handleOnline);
  window.addEventListener('offline', handleOffline);
  loading.value = true;
  try {
    const [tableRes, catRes] = await Promise.all([fetchTableList(), fetchDishCategoryList()]);
    if (!tableRes.error && tableRes.data) tables.value = tableRes.data;
    if (!catRes.error && catRes.data) categories.value = catRes.data;
    initSelectedTableFromRoute();
    await loadAllDishes();
    if (!isOffline.value) {
      void syncOfflineOrders();
    }
    void refreshPendingOfflineCount();
    pageInitialized.value = true;
  } finally { loading.value = false; }
});

onUnmounted(() => {
  window.removeEventListener('online', handleOnline);
  window.removeEventListener('offline', handleOffline);
});
</script>

<template>
  <div class="service-place-order-page">
    <NSpace vertical :size="12">
      <!-- 顶部：桌台选择 + 搜索 -->
      <NCard :bordered="false" class="order-toolbar">
        <NSpace :size="16" align="center">
          <NTag :type="isOffline ? 'error' : 'success'">
            {{ isOffline ? '离线模式' : '在线模式' }}
          </NTag>
          <NTag v-if="pendingOfflineCount > 0" type="warning">
            待补传 {{ pendingOfflineCount }} 单
          </NTag>
          <NButton
            v-if="!isOffline && pendingOfflineCount > 0"
            size="small"
            :loading="syncingOfflineOrders"
            @click="syncOfflineOrders(true)"
          >
            立即补传
          </NButton>
          <NSelect
            v-model:value="selectedTableId"
            :options="tableOptions"
            placeholder="选择桌台（含区域）"
            filterable
            :disabled="loading"
            style="width: 280px;"
          />
          <NInput
            :value="searchKeyword"
            placeholder="搜索菜品..."
            clearable
            :disabled="loading"
            style="width: 240px;"
            @update:value="handleSearch"
          />
          <NBadge :value="cartCount" :max="99">
            <NButton ref="cartButtonRef" type="primary" @click="showCartModal = true">
              购物车 ¥{{ cartTotal.toFixed(2) }}
            </NButton>
          </NBadge>
        </NSpace>
      </NCard>

      <NGrid :cols="24" :x-gap="16">
        <!-- 左侧：分类导航 -->
        <NGi :span="4">
          <NCard :bordered="false" title="菜品分类" size="small">
            <NScrollbar style="max-height: calc(100vh - 240px);">
              <NSpace v-if="pageInitialized" vertical :size="4">
                <NButton
                  :type="activeCategoryId === null ? 'primary' : 'default'"
                  block
                  size="small"
                  @click="selectCategory(null)"
                >
                  全部（{{ allDishes.length }}）
                </NButton>
                <NButton
                  v-for="cat in categories"
                  :key="cat.id"
                  :type="activeCategoryId === cat.id ? 'primary' : 'default'"
                  block
                  size="small"
                  @click="selectCategory(cat.id)"
                >
                  {{ cat.name }}（{{ categoryDishCountMap.get(String(cat.id)) || 0 }}）
                </NButton>
              </NSpace>
              <NSpace v-else vertical :size="8">
                <NSkeleton v-for="idx in 6" :key="idx" height="34px" :sharp="false" style="border-radius: 12px;" />
              </NSpace>
            </NScrollbar>
          </NCard>
        </NGi>

        <!-- 右侧：菜品列表 -->
        <NGi :span="20">
          <NSpin :show="pageInitialized && dishLoading">
            <div v-if="!pageInitialized" class="dish-skeleton-grid">
              <div v-for="idx in 8" :key="idx" class="dish-skeleton-card">
                <NSkeleton height="20px" width="42%" :sharp="false" />
                <NSkeleton text :repeat="2" :sharp="false" style="margin-top: 10px;" />
                <div class="dish-skeleton-card__foot">
                  <NSkeleton height="18px" width="72px" :sharp="false" />
                  <NSkeleton circle height="58px" width="58px" />
                </div>
              </div>
            </div>
            <NGrid :cols="4" :x-gap="12" :y-gap="12" responsive="screen" :item-responsive="true">
              <NGi v-for="dish in dishList" :key="dish.id" span="4 m:2 l:1">
                <NCard
                  class="dish-card"
                  :class="{ 'dish-card--active': cartDishCountMap.has(String(dish.id)) }"
                  size="small"
                  hoverable
                  :style="{ opacity: dish.soldOut === 1 ? 0.5 : 1, cursor: dish.soldOut === 1 ? 'not-allowed' : 'pointer' }"
                  @click="addToCart(dish, $event)"
                >
                  <div class="dish-card__body">
                    <div class="dish-card__content">
                      <div class="dish-card__name">{{ dish.name }}</div>
                      <NSpace :size="8" align="center" class="dish-card__tags">
                        <span class="dish-card__price">¥{{ dish.price.toFixed(2) }}</span>
                        <NTag v-if="dish.soldOut === 1" type="error" size="small">售罄</NTag>
                        <NTag v-if="dish.spiceLevel > 0" type="warning" size="small">
                          辣度 {{ dish.spiceLevel }}
                        </NTag>
                      </NSpace>
                      <div v-if="dish.categoryName" class="dish-card__meta">{{ dish.categoryName }}</div>
                      <div v-if="cartDishCountMap.has(String(dish.id))" class="dish-card__feedback">
                        已加入 {{ cartDishCountMap.get(String(dish.id)) }} 份
                      </div>
                    </div>
                    <div class="dish-card__media">
                      <NImage
                        v-if="dish.image"
                        class="dish-card__image"
                        :src="dish.image"
                        object-fit="cover"
                        preview-disabled
                      />
                      <div v-else class="dish-card__placeholder">
                        {{ dish.name.slice(0, 2) }}
                      </div>
                    </div>
                  </div>
                </NCard>
              </NGi>
            </NGrid>
            <NEmpty v-if="!dishLoading && dishList.length === 0" description="暂无菜品" style="padding: 40px;" />
          </NSpin>
        </NGi>
      </NGrid>
    </NSpace>

    <div
      v-if="flyToCartToken.visible"
      class="cart-fly-token"
      :class="{ 'cart-fly-token--active': flyToCartToken.active }"
      :style="{
        left: `${flyToCartToken.active ? flyToCartToken.targetX : flyToCartToken.x}px`,
        top: `${flyToCartToken.active ? flyToCartToken.targetY : flyToCartToken.y}px`
      }"
    >
      {{ flyToCartToken.label }}
    </div>

    <!-- 购物车弹窗 -->
    <NModal v-model:show="showCartModal" preset="card" title="购物车" style="width: 560px;">
      <template v-if="cart.length > 0">
        <NList bordered>
          <NListItem v-for="item in cart" :key="item.dishId">
            <NThing :title="item.dishName" :description="`¥${item.price.toFixed(2)}`">
              <template #header-extra>
                <NSpace align="center" :size="8">
                  <NButton size="tiny" @click="updateQuantity(item.dishId, item.quantity - 1)">-</NButton>
                  <span style="min-width: 20px; text-align: center;">{{ item.quantity }}</span>
                  <NButton size="tiny" @click="updateQuantity(item.dishId, item.quantity + 1)">+</NButton>
                  <span style="font-weight: 600; min-width: 60px; text-align: right;">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                </NSpace>
              </template>
            </NThing>
            <NInput v-model:value="item.remark" placeholder="备注（可选）" size="small" style="margin-top: 4px;" />
          </NListItem>
        </NList>
        <NDivider />
        <NSpace justify="space-between" align="center">
          <NButton @click="clearCart">清空</NButton>
          <NSpace align="center" :size="16">
            <NCheckbox v-model:checked="preOrderMode">预订单（不立即下发后厨）</NCheckbox>
            <span style="font-size: 16px; font-weight: 600;">合计：¥{{ cartTotalText }}</span>
            <NButton type="primary" :loading="submitting" @click="submitOrder">
              {{ preOrderMode ? '保存预订单' : '提交订单' }}
            </NButton>
          </NSpace>
        </NSpace>
      </template>
      <NEmpty v-else description="购物车为空" />
    </NModal>
  </div>
</template>

<style scoped>
.order-toolbar {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(239, 247, 255, 0.96)) !important;
}

.dish-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.dish-skeleton-card {
  padding: 14px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.05), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(243, 249, 255, 0.82));
  border: 1px solid rgba(15, 111, 255, 0.08);
}

.dish-skeleton-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
}

.dish-card {
  overflow: hidden;
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.08), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(243, 249, 255, 0.82)) !important;
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow:
    0 18px 34px rgba(15, 57, 119, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
}

.dish-card:hover {
  transform: translateY(-5px);
  box-shadow:
    0 24px 44px rgba(15, 57, 119, 0.14),
    0 10px 24px rgba(8, 27, 58, 0.06);
}

.dish-card--active {
  border-color: rgba(var(--admin-accent-rgb), 0.2);
  box-shadow:
    0 24px 42px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 0 0 1px rgba(var(--admin-accent-rgb), 0.08);
}

.dish-card__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.dish-card__content {
  min-width: 0;
  flex: 1;
}

.dish-card__name {
  font-size: 14px;
  font-weight: 700;
  color: #123055;
}

.dish-card__tags {
  margin-top: 8px;
  flex-wrap: wrap;
}

.dish-card__price {
  color: #d03050;
  font-weight: 700;
}

.dish-card__meta {
  margin-top: 8px;
  font-size: 11px;
  color: #7a8ca8;
}

.dish-card__feedback {
  margin-top: 8px;
  font-size: 11px;
  font-weight: 700;
  color: var(--admin-accent-strong);
}

.dish-card__media {
  flex-shrink: 0;
}

.dish-card__image,
.dish-card__placeholder {
  width: 58px;
  height: 58px;
  border-radius: 16px;
}

.dish-card__placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, rgba(15, 111, 255, 0.14), rgba(20, 163, 255, 0.08));
  border: 1px solid rgba(15, 111, 255, 0.12);
  font-size: 14px;
  font-weight: 700;
  color: #0f6fff;
}

.cart-fly-token {
  position: fixed;
  z-index: 1200;
  padding: 8px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--admin-accent-gradient-start), var(--admin-accent-gradient-end));
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  pointer-events: none;
  box-shadow: 0 16px 28px rgba(var(--admin-accent-rgb), 0.24);
  opacity: 0.92;
  transform: scale(0.96);
  transition:
    left 0.68s cubic-bezier(0.2, 0.8, 0.2, 1),
    top 0.68s cubic-bezier(0.2, 0.8, 0.2, 1),
    transform 0.68s cubic-bezier(0.2, 0.8, 0.2, 1),
    opacity 0.68s ease;
}

.cart-fly-token--active {
  opacity: 0.2;
  transform: scale(0.72);
}

@media (max-width: 960px) {
  .dish-skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

html.dark .order-toolbar {
  background: linear-gradient(180deg, rgba(9, 13, 21, 0.96), rgba(14, 19, 30, 0.98)) !important;
}

html.dark .dish-card {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 28%),
    linear-gradient(180deg, rgba(12, 17, 28, 0.96), rgba(8, 12, 20, 0.96)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 18px 34px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .dish-card:hover {
  box-shadow:
    0 24px 44px rgba(0, 0, 0, 0.34),
    0 10px 24px rgba(0, 0, 0, 0.18);
}

html.dark .dish-card--active {
  border-color: rgba(var(--admin-accent-rgb), 0.24);
  box-shadow:
    0 22px 40px rgba(0, 0, 0, 0.3),
    inset 0 0 0 1px rgba(var(--admin-accent-rgb), 0.14);
}

html.dark .dish-card__name {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .dish-card__meta {
  color: rgba(170, 186, 216, 0.72);
}

html.dark .dish-card__feedback {
  color: #dbe5ff;
}

html.dark .dish-card__placeholder {
  background: linear-gradient(180deg, rgba(var(--admin-accent-rgb), 0.18), rgba(var(--admin-accent-rgb), 0.08));
  border-color: rgba(var(--admin-accent-rgb), 0.14);
  color: #dbe5ff;
}

html.dark .cart-fly-token {
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.3);
}

</style>
