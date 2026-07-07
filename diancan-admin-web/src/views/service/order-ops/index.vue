<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  NCard, NSpace, NButton, NInput, NInputNumber, NSelect, NDataTable,
  NTag, NSpin, NModal, NForm, NFormItem, NEmpty, NPagination, useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import {
  fetchOrderList, fetchOrderDetail,
  discountOrder, giftOrderItem, returnOrderItem, replaceOrderItem,
  addOrderItem, rushOrderItem, fetchDishList, fetchTableAreaList
} from '@/service/api';

const message = useMessage();
const route = useRoute();
const loading = ref(false);

// ==================== 订单列表 ====================
const orders = ref<Api.Business.Order[]>([]);
const selectedOrder = ref<Api.Business.OrderDetail | null>(null);
const orderLoading = ref(false);
const orderKeyword = ref('');
const selectedArea = ref<string | null>(null);
const orderPage = ref(1);
const orderPageSize = ref(10);
const areaOptions = ref<SelectOption[]>([{ label: '全部区域', value: null }]);

const orderStatusMap: Record<number, { label: string; type: 'default' | 'warning' | 'success' | 'error' }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'error' },
  3: { label: '已退款', type: 'default' }
};

const itemStatusMap: Record<number, string> = {
  0: '待制作', 1: '制作中', 2: '已完成', 3: '已退菜'
};

async function loadOrders() {
  loading.value = true;
  try {
    const { data, error } = await fetchOrderList({ status: 0, pageNum: 1, pageSize: 200 });
    if (!error && data) {
      orders.value = data.list || [];
      const maxPage = Math.max(1, Math.ceil(filteredOrders.value.length / orderPageSize.value));
      if (orderPage.value > maxPage) orderPage.value = maxPage;
    }
  } finally { loading.value = false; }
}

async function loadAreaOptions() {
  const { data, error } = await fetchTableAreaList();
  if (!error && data) {
    areaOptions.value = [
      { label: '全部区域', value: null },
      { label: '未分区', value: '未分区' },
      ...data.map(item => ({ label: item.name, value: item.name }))
    ];
  }
}

const filteredOrders = computed(() => {
  const keyword = orderKeyword.value.trim().toLowerCase();
  return orders.value.filter(order => {
    const orderNo = (order.orderNo || '').toLowerCase();
    const tableCode = (order.tableCode || '').toLowerCase();
    const areaName = order.areaName || '未分区';
    const matchKeyword = !keyword || orderNo.includes(keyword) || tableCode.includes(keyword);
    const matchArea = !selectedArea.value || areaName === selectedArea.value;
    return matchKeyword && matchArea;
  });
});

const pagedOrders = computed(() => {
  const start = (orderPage.value - 1) * orderPageSize.value;
  return filteredOrders.value.slice(start, start + orderPageSize.value);
});

function handleOrderKeywordChange(value: string) {
  orderKeyword.value = value;
  orderPage.value = 1;
}

function handleAreaChange(value: string | null) {
  selectedArea.value = value;
  orderPage.value = 1;
}

function handleOrderPageChange(page: number) {
  orderPage.value = page;
}

function handleOrderPageSizeChange(pageSize: number) {
  orderPageSize.value = pageSize;
  orderPage.value = 1;
}

async function initOrderFromRoute() {
  const tableCode = String(route.query.tableCode || '').trim();
  if (!tableCode) return;
  handleOrderKeywordChange(tableCode);
  const targetOrder = orders.value.find(order => order.tableCode === tableCode);
  if (targetOrder) {
    await selectOrder(targetOrder.id);
  }
}

/** 选中订单加载详情 */
async function selectOrder(orderId: Api.Business.IdType) {
  orderLoading.value = true;
  try {
    const { data, error } = await fetchOrderDetail(orderId);
    if (!error && data) selectedOrder.value = data;
  } finally { orderLoading.value = false; }
}

// ==================== 订单项表格列 ====================
const itemColumns: DataTableColumns<Api.Business.OrderItem> = [
  { title: '菜品', key: 'dishName', width: 140 },
  { title: '单价', key: 'price', width: 80, render: row => `¥${row.price.toFixed(2)}` },
  { title: '数量', key: 'quantity', width: 60 },
  { title: '金额', key: 'amount', width: 80, render: row => `¥${row.amount.toFixed(2)}` },
  {
    title: '状态', key: 'status', width: 80,
    render: row => {
      if (row.isGift === 1) return '赠送';
      return itemStatusMap[row.status] || '未知';
    }
  },
  {
    title: '操作', key: 'actions', width: 280,
    render: row => {
      if (row.status === 3 || row.isGift === 1) return null;
      return [
        h(NButton, { size: 'tiny', type: 'primary', style: 'margin-right:4px', onClick: () => handleRush(row) }, () => '催菜'),
        h(NButton, { size: 'tiny', type: 'info', style: 'margin-right:4px', onClick: () => openGift(row) }, () => '赠送'),
        h(NButton, { size: 'tiny', type: 'warning', style: 'margin-right:4px', onClick: () => openReturn(row) }, () => '退菜'),
        h(NButton, { size: 'tiny', type: 'default', onClick: () => openReplace(row) }, () => '换菜')
      ];
    }
  }
];

// ==================== 加菜与催菜 ====================
const showAddItemModal = ref(false);
const addItemForm = ref({ dishId: null as number | null, quantity: 1, remark: '' });
const addDishOptions = ref<SelectOption[]>([]);

async function openAddItemModal() {
  if (!selectedOrder.value) {
    message.warning('请先选择订单');
    return;
  }
  addItemForm.value = { dishId: null, quantity: 1, remark: '' };
  const { data, error } = await fetchDishList({ status: 1, pageNum: 1, pageSize: 200 });
  if (!error && data) {
    addDishOptions.value = (data.list || []).map(d => ({ label: `${d.name} ¥${d.price}`, value: d.id }));
  }
  showAddItemModal.value = true;
}

async function handleAddItem() {
  if (!selectedOrder.value) return;
  if (!addItemForm.value.dishId) {
    message.warning('请选择菜品');
    return;
  }
  const { error } = await addOrderItem(selectedOrder.value.id, {
    dishId: addItemForm.value.dishId,
    quantity: addItemForm.value.quantity,
    remark: addItemForm.value.remark || undefined
  });
  if (!error) {
    message.success('加菜成功');
    showAddItemModal.value = false;
    await selectOrder(selectedOrder.value.id);
  }
}

async function handleRush(item: Api.Business.OrderItem) {
  if (!selectedOrder.value) return;
  const { error } = await rushOrderItem(selectedOrder.value.id, item.id);
  if (!error) {
    message.success('已催菜');
  }
}

// ==================== 打折操作 ====================
const showDiscountModal = ref(false);
const discountForm = ref({ discountRate: 0.9, reason: '' });

async function handleDiscount() {
  if (!selectedOrder.value) return;
  const { error } = await discountOrder(selectedOrder.value.id, {
    discountRate: discountForm.value.discountRate,
    reason: discountForm.value.reason || undefined
  });
  if (!error) {
    message.success('打折成功');
    showDiscountModal.value = false;
    await selectOrder(selectedOrder.value.id);
  }
}

// ==================== 赠送操作 ====================
async function openGift(item: Api.Business.OrderItem) {
  const { error } = await giftOrderItem(item.id);
  if (!error) {
    message.success('赠送成功');
    if (selectedOrder.value) await selectOrder(selectedOrder.value.id);
  }
}

// ==================== 退菜操作（需授权密码） ====================
const showReturnModal = ref(false);
const returnForm = ref<{ itemId: Api.Business.IdType | null; authPassword: string; reason: string }>({
  itemId: null,
  authPassword: '',
  reason: ''
});

function openReturn(item: Api.Business.OrderItem) {
  returnForm.value = { itemId: item.id, authPassword: '', reason: '' };
  showReturnModal.value = true;
}

async function handleReturn() {
  if (!returnForm.value.authPassword) {
    message.warning('请输入授权密码');
    return;
  }
  if (!returnForm.value.reason) {
    message.warning('请输入退菜原因');
    return;
  }
  if (!returnForm.value.itemId) return;
  const { error } = await returnOrderItem(returnForm.value.itemId, {
    authPassword: returnForm.value.authPassword,
    reason: returnForm.value.reason
  });
  if (!error) {
    message.success('退菜成功');
    showReturnModal.value = false;
    if (selectedOrder.value) await selectOrder(selectedOrder.value.id);
  }
}

// ==================== 换菜操作（需授权密码） ====================
const showReplaceModal = ref(false);
const replaceForm = ref<{ itemId: Api.Business.IdType | null; newDishId: number | null; quantity: number; remark: string; authPassword: string; reason: string }>({
  itemId: null,
  newDishId: null,
  quantity: 1,
  remark: '',
  authPassword: '',
  reason: ''
});
const dishOptions = ref<SelectOption[]>([]);

async function openReplace(item: Api.Business.OrderItem) {
  replaceForm.value = { itemId: item.id, newDishId: null, quantity: item.quantity, remark: '', authPassword: '', reason: '' };
  showReplaceModal.value = true;
  // 加载菜品列表作为选项
  const { data, error } = await fetchDishList({ status: 1, pageNum: 1, pageSize: 200 });
  if (!error && data) {
    dishOptions.value = (data.list || []).map(d => ({ label: `${d.name} ¥${d.price}`, value: d.id }));
  }
}

async function handleReplace() {
  if (!replaceForm.value.newDishId) { message.warning('请选择新菜品'); return; }
  if (!replaceForm.value.authPassword) { message.warning('请输入授权密码'); return; }
  if (!replaceForm.value.reason) { message.warning('请输入换菜原因'); return; }
  if (!replaceForm.value.itemId) return;
  const { error } = await replaceOrderItem(replaceForm.value.itemId, {
    newDishId: replaceForm.value.newDishId,
    quantity: replaceForm.value.quantity,
    remark: replaceForm.value.remark || undefined,
    authPassword: replaceForm.value.authPassword,
    reason: replaceForm.value.reason
  });
  if (!error) {
    message.success('换菜成功');
    showReplaceModal.value = false;
    if (selectedOrder.value) await selectOrder(selectedOrder.value.id);
  }
}

onMounted(async () => {
  await loadAreaOptions();
  await loadOrders();
  await initOrderFromRoute();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="ops-hero">
      <div class="ops-hero__eyebrow">订单指挥台</div>
      <div class="ops-hero__head">
        <div>
          <h2 class="ops-hero__title">把催菜、赠送、退菜、换菜这些高频操作收成一张订单指挥台</h2>
          <p class="ops-hero__desc">适合营业中快速处理异常需求，减少切换页面和重复查找订单的时间成本。</p>
        </div>
        <div class="ops-hero__badge">
          <span>待处理订单</span>
          <strong>{{ filteredOrders.length }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="待处理订单" size="small">
      <template #header-extra>
        <NSpace :size="8" align="center">
          <NInput
            :value="orderKeyword"
            size="small"
            clearable
            placeholder="搜索订单号/桌台"
            style="width: 220px;"
            @update:value="handleOrderKeywordChange"
          />
          <NSelect
            :value="selectedArea"
            size="small"
            clearable
            placeholder="全部区域"
            style="width: 160px;"
            :options="areaOptions"
            @update:value="handleAreaChange"
          />
          <NButton size="small" @click="loadOrders">刷新</NButton>
        </NSpace>
      </template>
      <NSpin :show="loading">
        <div class="order-grid" v-if="pagedOrders.length > 0">
          <button
            v-for="order in pagedOrders"
            :key="order.id"
            type="button"
            class="order-pill"
            :class="{ active: selectedOrder?.id === order.id }"
            @click="selectOrder(order.id)"
          >
            <div class="pill-top">
              <span class="order-no">{{ order.orderNo }}</span>
              <span class="table-code">{{ order.tableCode }}</span>
            </div>
            <div class="pill-bottom">
              <span class="area-name">{{ order.areaName || '未分区' }}</span>
              <span>应付 ¥{{ order.actualAmount?.toFixed(2) }}</span>
            </div>
          </button>
        </div>
        <NEmpty v-else-if="!loading" description="暂无待处理订单" />

        <NPagination
          v-if="filteredOrders.length > 0"
          style="margin-top: 12px; justify-content: flex-end;"
          :page="orderPage"
          :page-size="orderPageSize"
          :item-count="filteredOrders.length"
          :page-sizes="[10, 20, 50, 100, 200]"
          show-size-picker
          show-quick-jumper
          :prefix="({ itemCount }) => `共 ${itemCount} 条`"
          @update:page="handleOrderPageChange"
          @update:page-size="handleOrderPageSizeChange"
        />
      </NSpin>
    </NCard>

    <!-- 订单详情与操作 -->
    <template v-if="selectedOrder">
      <NCard :bordered="false" size="small">
        <template #header>
          <NSpace align="center" :size="12">
            <span>订单 {{ selectedOrder.orderNo }}</span>
            <NTag :type="orderStatusMap[selectedOrder.status]?.type || 'default'" size="small">
              {{ orderStatusMap[selectedOrder.status]?.label || '未知' }}
            </NTag>
            <span style="color: #999; font-size: 13px;">桌台：{{ selectedOrder.tableCode }}</span>
            <span style="color: #999; font-size: 13px;">区域：{{ selectedOrder.areaName || '未分区' }}</span>
          </NSpace>
        </template>
        <template #header-extra>
          <NSpace :size="8">
            <NButton type="primary" size="small" @click="openAddItemModal">加菜</NButton>
            <NButton type="warning" size="small" @click="showDiscountModal = true">整单打折</NButton>
          </NSpace>
        </template>

        <NSpace vertical :size="8">
          <NSpace :size="24">
            <span>原价：¥{{ selectedOrder.originalAmount?.toFixed(2) }}</span>
            <span>折扣：{{ (selectedOrder.discountRate * 10).toFixed(1) }}折</span>
            <span style="font-weight: 600; color: #d03050;">实付：¥{{ selectedOrder.actualAmount?.toFixed(2) }}</span>
          </NSpace>

          <NSpin :show="orderLoading">
            <NDataTable
              :columns="itemColumns"
              :data="selectedOrder.items || []"
              :bordered="false"
              size="small"
              :pagination="false"
            />
          </NSpin>
        </NSpace>
      </NCard>
    </template>

    <!-- 打折弹窗 -->
    <NModal v-model:show="showDiscountModal" preset="card" title="整单打折" style="width: 400px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="折扣">
          <NInputNumber v-model:value="discountForm.discountRate" :min="0.1" :max="1" :step="0.05" style="width: 100%;">
            <template #suffix>（{{ (discountForm.discountRate * 10).toFixed(1) }}折）</template>
          </NInputNumber>
        </NFormItem>
        <NFormItem label="原因">
          <NInput v-model:value="discountForm.reason" placeholder="打折原因（可选）" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDiscountModal = false">取消</NButton>
          <NButton type="primary" @click="handleDiscount">确认打折</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 退菜弹窗 -->
    <NModal v-model:show="showReturnModal" preset="card" title="退菜" style="width: 400px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="授权密码">
          <NInput v-model:value="returnForm.authPassword" type="password" placeholder="请输入管理员授权密码" />
        </NFormItem>
        <NFormItem label="退菜原因">
          <NInput v-model:value="returnForm.reason" type="textarea" placeholder="请输入退菜原因" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showReturnModal = false">取消</NButton>
          <NButton type="warning" @click="handleReturn">确认退菜</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 换菜弹窗 -->
    <NModal v-model:show="showReplaceModal" preset="card" title="换菜" style="width: 480px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="新菜品">
          <NSelect v-model:value="replaceForm.newDishId" :options="dishOptions" filterable placeholder="选择新菜品" />
        </NFormItem>
        <NFormItem label="数量">
          <NInputNumber v-model:value="replaceForm.quantity" :min="1" style="width: 100%;" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="replaceForm.remark" placeholder="备注（可选）" />
        </NFormItem>
        <NFormItem label="授权密码">
          <NInput v-model:value="replaceForm.authPassword" type="password" placeholder="请输入管理员授权密码" />
        </NFormItem>
        <NFormItem label="换菜原因">
          <NInput v-model:value="replaceForm.reason" type="textarea" placeholder="请输入换菜原因" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showReplaceModal = false">取消</NButton>
          <NButton type="primary" @click="handleReplace">确认换菜</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 加菜弹窗 -->
    <NModal v-model:show="showAddItemModal" preset="card" title="加菜" style="width: 460px;">
      <NForm label-placement="left" label-width="80">
        <NFormItem label="菜品">
          <NSelect v-model:value="addItemForm.dishId" :options="addDishOptions" filterable placeholder="请选择菜品" />
        </NFormItem>
        <NFormItem label="数量">
          <NInputNumber v-model:value="addItemForm.quantity" :min="1" style="width: 100%;" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="addItemForm.remark" placeholder="备注（可选）" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showAddItemModal = false">取消</NButton>
          <NButton type="primary" @click="handleAddItem">确认加菜</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.order-pill {
  border: 1px solid rgba(15, 111, 255, 0.1);
  border-radius: 18px;
  padding: 10px 12px;
  background: linear-gradient(180deg, rgba(255,255,255,0.98), rgba(239,247,255,0.98));
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 10px 20px rgba(15, 111, 255, 0.05);
}

.order-pill:hover {
  border-color: rgba(15, 111, 255, 0.18);
  box-shadow: 0 16px 28px rgba(15, 111, 255, 0.1);
}

.order-pill.active {
  border-color: #0f6fff;
  background: linear-gradient(180deg, rgba(233,242,255,0.98), rgba(219,236,255,0.98));
  box-shadow: 0 16px 30px rgba(15, 111, 255, 0.14);
}

.pill-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.pill-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: #64748b;
  font-size: 12px;
}

.area-name {
  color: #64748b;
}

.order-no {
  font-size: 13px;
  font-weight: 600;
  color: #123055;
}

.table-code {
  font-size: 12px;
  color: #163a70;
  background: #edf5ff;
  border-radius: 999px;
  padding: 1px 8px;
}

.ops-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.ops-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.ops-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.ops-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.ops-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.ops-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.ops-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.ops-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}

html.dark .order-pill {
  border-color: rgba(255, 255, 255, 0.06);
  background: linear-gradient(180deg, rgba(13, 18, 29, 0.98), rgba(9, 13, 21, 0.98));
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.2);
}

html.dark .order-pill:hover {
  border-color: rgba(var(--admin-accent-rgb), 0.22);
  box-shadow: 0 16px 28px rgba(0, 0, 0, 0.3);
}

html.dark .order-pill.active {
  border-color: rgba(var(--admin-accent-rgb), 0.32);
  background: linear-gradient(180deg, rgba(22, 28, 42, 0.98), rgba(14, 19, 30, 0.98));
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.34);
}

html.dark .pill-bottom {
  color: rgba(169, 184, 213, 0.76);
}

html.dark .area-name {
  color: rgba(169, 184, 213, 0.76);
}

html.dark .order-no {
  color: rgba(241, 246, 255, 0.94);
}

html.dark .table-code {
  color: #dbe5ff;
  background: rgba(var(--admin-accent-rgb), 0.16);
}

html.dark .ops-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.18), transparent 24%),
    linear-gradient(135deg, rgba(8, 12, 20, 0.98), rgba(14, 19, 30, 0.98)) !important;
}

html.dark .ops-hero__eyebrow {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .ops-hero__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .ops-hero__desc {
  color: rgba(206, 216, 236, 0.76);
}

html.dark .ops-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .ops-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .ops-hero__badge strong {
  color: #dbe5ff;
}
</style>
