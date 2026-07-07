<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import {
  NCard, NSpace, NButton, NInput, NInputNumber, NDataTable, NTag, NSpin,
  NModal, NEmpty, NDescriptions, NDescriptionsItem, NRadioGroup,
  NRadio, NDivider, NCheckbox, NPagination, useMessage, NResult, NSelect
} from 'naive-ui';
import type { DataTableColumns, DataTableRowKey, SelectOption } from 'naive-ui';
import {
  fetchOrderList, fetchOrderDetail,
  cashPay, generatePayQrCode, splitBill, fetchPaymentStatus, fetchTableAreaList
} from '@/service/api';

const message = useMessage();
const route = useRoute();
const loading = ref(false);
let lastVoiceAt = 0;
const VOICE_PREF_KEY = 'admin_voice_enabled';
const voiceEnabled = ref(true);

function playVoice(text: string, cooldownMs = 1200) {
  if (!voiceEnabled.value) return;
  if (!('speechSynthesis' in window)) return;
  const now = Date.now();
  if (now - lastVoiceAt < cooldownMs) return;
  lastVoiceAt = now;
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = 'zh-CN';
  utter.rate = 1;
  window.speechSynthesis.speak(utter);
}

function initVoicePreference() {
  try {
    const saved = window.localStorage.getItem(VOICE_PREF_KEY);
    if (saved === '0') voiceEnabled.value = false;
    if (saved === '1') voiceEnabled.value = true;
  } catch {
    // ignore
  }
}

function toggleVoice() {
  voiceEnabled.value = !voiceEnabled.value;
  try {
    window.localStorage.setItem(VOICE_PREF_KEY, voiceEnabled.value ? '1' : '0');
  } catch {
    // ignore
  }
  message.success(voiceEnabled.value ? '语音播报已开启' : '语音播报已关闭');
}

// ==================== 订单列表 ====================
const orders = ref<Api.Business.Order[]>([]);
const selectedOrder = ref<Api.Business.OrderDetail | null>(null);
const orderLoading = ref(false);
const orderKeyword = ref('');
const selectedArea = ref<string | null>(null);
const orderPage = ref(1);
const orderPageSize = ref(10);
const areaOptions = ref<SelectOption[]>([{ label: '全部区域', value: null }]);

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

async function selectOrder(orderId: Api.Business.IdType) {
  stopPaymentPolling();
  orderLoading.value = true;
  try {
    const { data, error } = await fetchOrderDetail(orderId);
    if (!error && data) {
      selectedOrder.value = data;
      paymentMethod.value = 'cash';
      receivedAmount.value = data.actualAmount;
      splitMode.value = false;
      checkedItemKeys.value = [];
      payResult.value = null;
      qrCodeResult.value = null;
    }
  } finally { orderLoading.value = false; }
}

// ==================== 支付方式 ====================
const paymentMethod = ref<'cash' | 'qrcode'>('cash');
const receivedAmount = ref(0);
const payResult = ref<Api.Business.CashPayResult | null>(null);
const qrCodeResult = ref<Api.Business.PaymentResult | null>(null);
const qrImageFailed = ref(false);
const paying = ref(false);
const pollingTimer = ref<number | null>(null);
const pollingCount = ref(0);
const pollingInFlight = ref(false);

const fallbackQrUrl = computed(() => {
  const payUrl = qrCodeResult.value?.payUrl;
  if (!payUrl) return '';
  return `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(payUrl)}`;
});

const displayQrUrl = computed(() => {
  const direct = qrCodeResult.value?.qrCodeUrl;
  if (direct && !qrImageFailed.value) return direct;
  return fallbackQrUrl.value;
});

/** 找零金额 */
const changeAmount = computed(() => {
  if (!selectedOrder.value) return 0;
  return Math.max(0, receivedAmount.value - selectedOrder.value.actualAmount);
});

/** 现金支付 */
async function handleCashPay() {
  if (!selectedOrder.value) return;
  if (receivedAmount.value < selectedOrder.value.actualAmount) {
    message.warning('收款金额不足');
    playVoice('收款金额不足，请检查金额');
    return;
  }
  paying.value = true;
  try {
    const { data, error } = await cashPay({
      orderId: selectedOrder.value.id,
      receivedAmount: receivedAmount.value
    });
    if (!error && data) {
      payResult.value = data;
      message.success('支付成功');
      playVoice(`现金支付成功，金额${data.amount?.toFixed(2) || '0'}元`);
      loadOrders();
    }
  } finally { paying.value = false; }
}

/** 扫码支付 - 生成二维码 */
async function handleQrCodePay() {
  if (!selectedOrder.value) return;
  paying.value = true;
  try {
    const { data, error } = await generatePayQrCode(selectedOrder.value.id);
    if (!error && data) {
      qrCodeResult.value = data;
      qrImageFailed.value = false;
      message.success('二维码已生成，请扫码支付');
      playVoice('收款码已生成，请扫码支付');
      if (data.id) {
        startPaymentPolling(data.id);
      }
    }
  } finally { paying.value = false; }
}

function stopPaymentPolling() {
  if (pollingTimer.value) {
    window.clearInterval(pollingTimer.value);
    pollingTimer.value = null;
  }
  pollingInFlight.value = false;
}

function handleQrImageError() {
  if (!qrImageFailed.value) {
    qrImageFailed.value = true;
    message.warning('直连二维码加载失败，已切换备用二维码');
  }
}

function startPaymentPolling(paymentId: Api.Business.IdType) {
  stopPaymentPolling();
  pollingCount.value = 0;

  pollingTimer.value = window.setInterval(async () => {
    if (pollingInFlight.value) return;
    pollingInFlight.value = true;
    pollingCount.value += 1;
    try {
      const { data, error } = await fetchPaymentStatus(paymentId);
      if (!error && data) {
        if (data.status === 1) {
          stopPaymentPolling();
          message.success('扫码支付已完成');
          playVoice('扫码支付成功');
          qrCodeResult.value = null;
          selectedOrder.value = null;
          await loadOrders();
          return;
        }
        if (data.status === 3) {
          stopPaymentPolling();
          message.error('支付失败，请重试');
          playVoice('支付失败，请重试');
          return;
        }
      }
    } finally {
      pollingInFlight.value = false;
    }

    if (pollingCount.value >= 40) {
      stopPaymentPolling();
      message.warning('支付状态轮询超时，请手动刷新确认');
      playVoice('支付确认超时，请手动刷新');
    }
  }, 3000);
}

// ==================== 分单结账 ====================
const splitMode = ref(false);
const checkedItemKeys = ref<DataTableRowKey[]>([]);

const itemColumns: DataTableColumns<Api.Business.OrderItem> = [
  { title: '菜品', key: 'dishName', width: 140 },
  { title: '单价', key: 'price', width: 80, render: row => `¥${row.price.toFixed(2)}` },
  { title: '数量', key: 'quantity', width: 60 },
  { title: '金额', key: 'amount', width: 90, render: row => `¥${row.amount.toFixed(2)}` },
  {
    title: '支付',
    key: 'paymentStatus',
    width: 88,
    render: row => row.paymentStatus === 2 ? '已支付' : '未支付'
  },
  {
    title: '状态', key: 'status', width: 80,
    render: row => row.isGift === 1 ? '赠送' : ({ 0: '待制作', 1: '制作中', 2: '已完成', 3: '已退菜' }[row.status] || '未知')
  }
];

const splitSelectableItems = computed(() =>
  (selectedOrder.value?.items || []).filter(item => item.status !== 3 && item.isGift !== 1 && item.paymentStatus !== 2)
);

const itemTableData = computed(() => (splitMode.value ? splitSelectableItems.value : (selectedOrder.value?.items || [])));

function handleSplitItemRowClick(itemId: DataTableRowKey) {
  if (!splitMode.value) return;
  if (checkedItemKeys.value.includes(itemId)) {
    checkedItemKeys.value = checkedItemKeys.value.filter(key => key !== itemId);
    return;
  }
  checkedItemKeys.value = [...checkedItemKeys.value, itemId];
}

function handleSplitModeChange(checked: boolean) {
  splitMode.value = checked;
  checkedItemKeys.value = [];
}

/** 分单选中项的小计 */
const splitTotal = computed(() => {
  return splitSelectableItems.value
    .filter(item => checkedItemKeys.value.includes(item.id))
    .reduce((sum, item) => sum + item.amount, 0);
});

/** 提交分单结账 */
async function handleSplitBill() {
  if (!selectedOrder.value) return;
  if (checkedItemKeys.value.length === 0) {
    message.warning('请选择要结账的菜品');
    playVoice('请选择需要结账的菜品');
    return;
  }
  paying.value = true;
  try {
    // 与后端 paymentMethod 定义对齐：0微信 1支付宝 2现金
    const methodCode = paymentMethod.value === 'cash' ? 2 : 0;
    const { error } = await splitBill({
      orderId: selectedOrder.value.id,
      items: checkedItemKeys.value.map(id => ({ orderItemIds: [String(id)], paymentMethod: methodCode }))
    });
    if (!error) {
      message.success('分单结账成功');
      playVoice('分单结账成功');
      loadOrders();
      selectedOrder.value = null;
    }
  } finally { paying.value = false; }
}

onMounted(async () => {
  initVoicePreference();
  await loadAreaOptions();
  await loadOrders();
  await initOrderFromRoute();
});
onUnmounted(() => stopPaymentPolling());
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="checkout-hero">
      <div class="checkout-hero__eyebrow">CHECKOUT DESK</div>
      <div class="checkout-hero__head">
        <div>
          <h2 class="checkout-hero__title">把待结账订单、收款方式和支付反馈收拢在一个结账工作台</h2>
          <p class="checkout-hero__desc">适合收银台快速切换订单、确认实付金额、生成收款码和处理分单结账，不打断收银动作。</p>
        </div>
        <div class="checkout-hero__badge">
          <span>待结账订单</span>
          <strong>{{ filteredOrders.length }}</strong>
        </div>
      </div>
    </NCard>

    <!-- 待结账订单列表 -->
    <NCard :bordered="false" title="待结账订单" size="small">
      <template #header-extra>
        <NSpace :size="8" align="center">
          <NButton size="small" secondary @click="toggleVoice">
            语音: {{ voiceEnabled ? '开' : '关' }}
          </NButton>
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
              <span class="amount">应付 ¥{{ order.actualAmount?.toFixed(2) }}</span>
            </div>
          </button>
        </div>
        <NEmpty v-else-if="!loading" description="暂无待结账订单" />

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

    <template v-if="selectedOrder && !payResult && !qrCodeResult">
      <NCard :bordered="false" size="small">
        <template #header>
          <NSpace align="center" :size="12">
            <span>订单 {{ selectedOrder.orderNo }}</span>
            <span style="color: #999;">桌台：{{ selectedOrder.tableCode }}</span>
            <span style="color: #999;">区域：{{ selectedOrder.areaName || '未分区' }}</span>
          </NSpace>
        </template>

        <NSpace vertical :size="12">
          <!-- 订单项列表 -->
          <NDataTable
            :columns="itemColumns"
            :data="itemTableData"
            :bordered="false"
            size="small"
            :pagination="false"
            :row-key="(row: Api.Business.OrderItem) => row.id"
            :row-props="(row: Api.Business.OrderItem) => splitMode ? {
              class: checkedItemKeys.includes(row.id) ? 'checkout-item-row checkout-item-row--active' : 'checkout-item-row',
              onClick: () => handleSplitItemRowClick(row.id)
            } : {}"
          />

          <NDivider />

          <!-- 金额汇总 -->
          <NDescriptions :column="3" label-placement="left" size="small">
            <NDescriptionsItem label="原价">¥{{ selectedOrder.originalAmount?.toFixed(2) }}</NDescriptionsItem>
            <NDescriptionsItem label="折扣">{{ (selectedOrder.discountRate * 10).toFixed(1) }}折</NDescriptionsItem>
            <NDescriptionsItem label="应付">
              <span style="font-size: 18px; font-weight: 700; color: #3f6b8a;">
                ¥{{ selectedOrder.actualAmount?.toFixed(2) }}
              </span>
            </NDescriptionsItem>
          </NDescriptions>

          <NDivider />

          <!-- 支付方式选择 -->
          <NSpace align="center" :size="16">
            <span>支付方式：</span>
            <NRadioGroup v-model:value="paymentMethod">
              <NRadio value="cash">现金支付</NRadio>
              <NRadio value="qrcode">扫码支付</NRadio>
            </NRadioGroup>
            <NCheckbox :checked="splitMode" @update:checked="handleSplitModeChange">分单结账</NCheckbox>
          </NSpace>

          <!-- 现金支付区域 -->
          <template v-if="paymentMethod === 'cash' && !splitMode">
            <NSpace align="center" :size="16">
              <span>收款金额：</span>
              <NInputNumber v-model:value="receivedAmount" :min="0" :precision="2" style="width: 160px;">
                <template #prefix>¥</template>
              </NInputNumber>
              <span style="font-size: 16px; font-weight: 600; color: #4e7a77;">
                找零：¥{{ changeAmount.toFixed(2) }}
              </span>
            </NSpace>
            <NButton type="primary" :loading="paying" block @click="handleCashPay">确认收款</NButton>
          </template>

          <!-- 扫码支付区域 -->
          <template v-if="paymentMethod === 'qrcode' && !splitMode">
            <NButton type="primary" :loading="paying" block @click="handleQrCodePay">生成收款二维码</NButton>
          </template>

          <!-- 分单结账区域 -->
          <template v-if="splitMode">
            <NSpace align="center" :size="16">
              <span>已选菜品小计：</span>
              <span style="font-size: 16px; font-weight: 600; color: #3f6b8a;">¥{{ splitTotal.toFixed(2) }}</span>
            </NSpace>
            <div style="font-size: 12px; color: #6b7d91;">
              仅支持选择未退菜、非赠送且未支付的菜品；已支付菜品会直接显示状态，不再允许重复分单。
            </div>
            <NButton type="primary" :loading="paying" block @click="handleSplitBill">分单结账</NButton>
          </template>
        </NSpace>
      </NCard>
    </template>

    <!-- 现金支付成功结果 -->
    <NCard v-if="payResult" :bordered="false">
      <NResult status="success" title="支付成功" :description="`订单 ${payResult.paymentNo}`">
        <template #footer>
          <NDescriptions :column="2" label-placement="left">
            <NDescriptionsItem label="支付金额">¥{{ payResult.amount?.toFixed(2) }}</NDescriptionsItem>
            <NDescriptionsItem label="找零">¥{{ payResult.changeAmount?.toFixed(2) }}</NDescriptionsItem>
          </NDescriptions>
          <NButton type="primary" style="margin-top: 16px;" @click="payResult = null; selectedOrder = null;">返回</NButton>
        </template>
      </NResult>
    </NCard>

    <!-- 扫码支付二维码展示 -->
    <NCard v-if="qrCodeResult" :bordered="false">
      <NResult status="info" title="请扫码支付" :description="`支付单号：${qrCodeResult.paymentNo}`">
        <template #footer>
          <div v-if="displayQrUrl" style="display: flex; justify-content: center; margin-bottom: 12px;">
            <img
              :src="displayQrUrl"
              alt="收款二维码"
              style="width: 220px; height: 220px; object-fit: contain;"
              @error="handleQrImageError"
            />
          </div>
          <NDescriptions :column="1" label-placement="left" style="max-width: 560px; margin: 0 auto;">
            <NDescriptionsItem label="支付金额">¥{{ qrCodeResult.amount?.toFixed(2) }}</NDescriptionsItem>
            <NDescriptionsItem v-if="qrCodeResult.payUrl" label="支付链接">
              <a :href="qrCodeResult.payUrl" target="_blank" rel="noopener noreferrer">{{ qrCodeResult.payUrl }}</a>
            </NDescriptionsItem>
            <NDescriptionsItem label="状态">
              <NTag type="warning">等待支付</NTag>
            </NDescriptionsItem>
          </NDescriptions>
          <NButton type="primary" style="margin-top: 16px;" @click="qrCodeResult = null; selectedOrder = null;">返回</NButton>
        </template>
      </NResult>
    </NCard>
  </NSpace>
</template>

<style scoped>
.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
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
  border-color: rgba(15, 111, 255, 0.22);
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

.amount {
  color: #0f6fff;
  font-weight: 600;
}

.area-name {
  color: #64748b;
}

.checkout-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.checkout-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.checkout-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.checkout-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.checkout-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.checkout-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.checkout-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.checkout-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}

:deep(.checkout-item-row td) {
  cursor: pointer;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}

:deep(.checkout-item-row--active td) {
  background: rgba(15, 111, 255, 0.12) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.42);
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

html.dark .order-no {
  color: rgba(241, 246, 255, 0.94);
}

html.dark .table-code {
  color: #dbe5ff;
  background: rgba(var(--admin-accent-rgb), 0.16);
}

html.dark .amount {
  color: #dbe5ff;
}

html.dark .area-name {
  color: rgba(169, 184, 213, 0.76);
}

html.dark .checkout-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.18), transparent 24%),
    linear-gradient(135deg, rgba(8, 12, 20, 0.98), rgba(14, 19, 30, 0.98)) !important;
}

html.dark .checkout-hero__eyebrow {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .checkout-hero__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .checkout-hero__desc {
  color: rgba(206, 216, 236, 0.76);
}

html.dark .checkout-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .checkout-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .checkout-hero__badge strong {
  color: #dbe5ff;
}

html.dark :deep(.checkout-item-row--active td) {
  background: rgba(var(--admin-accent-rgb), 0.2) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);
}
</style>
