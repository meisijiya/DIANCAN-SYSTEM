<script setup lang="ts">
import { onMounted, ref } from 'vue';
import {
  NButton, NCard, NDataTable, NDescriptions, NDescriptionsItem, NInput, NModal,
  NSpace, NSpin, NTag, useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { useRoute } from 'vue-router';
import { fetchOrderDetail, refundOrder } from '@/service/api';

defineOptions({ name: 'OrderDetail' });

const route = useRoute();
const message = useMessage();
const loading = ref(false);
const order = ref<Api.Business.OrderDetail | null>(null);
const refunding = ref(false);
const refundModalVisible = ref(false);
const refundReason = ref('');

const statusMap: Record<number, { label: string; type: 'warning' | 'success' | 'error' | 'info' }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'error' },
  3: { label: '已退款', type: 'info' }
};
const paymentMethodMap: Record<number, string> = { 0: '微信', 1: '支付宝', 2: '现金' };

const itemStatusMap: Record<number, string> = { 0: '待制作', 1: '制作中', 2: '已完成' };

const itemColumns: DataTableColumns<Api.Business.OrderItem> = [
  { title: '菜品名称', key: 'dishName', width: 150 },
  { title: '单价', key: 'price', width: 80, render(row) { return `¥${row.price}`; } },
  { title: '数量', key: 'quantity', width: 60 },
  { title: '金额', key: 'amount', width: 80, render(row) { return `¥${row.amount}`; } },
  { title: '备注', key: 'remark', width: 120, render(row) { return row.remark || '-'; } },
  { title: '状态', key: 'status', width: 80, render(row) { return itemStatusMap[row.status] || '未知'; } },
  { title: '赠送', key: 'isGift', width: 60, render(row) { return row.isGift === 1 ? '是' : '-'; } }
];

const logColumns: DataTableColumns<Api.Business.OrderOperationLog> = [
  { title: '操作类型', key: 'operationType', width: 100 },
  { title: '操作人', key: 'operatorName', width: 100 },
  { title: '原因', key: 'reason', width: 150, render(row) { return row.reason || '-'; } },
  { title: '详情', key: 'detail', width: 200, render(row) { return row.detail || '-'; } },
  { title: '操作时间', key: 'createTime', width: 170 }
];

async function loadData() {
  const idParam = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id;
  if (!idParam) return;
  loading.value = true;
  try {
    const { data: result, error } = await fetchOrderDetail(String(idParam));
    if (!error && result) order.value = result;
  } finally { loading.value = false; }
}

function openRefundModal() {
  refundReason.value = '';
  refundModalVisible.value = true;
}

async function handleRefundOrder() {
  if (!order.value) return;
  if (!refundReason.value.trim()) {
    message.warning('请输入退款原因');
    return;
  }

  refunding.value = true;
  try {
    const { error } = await refundOrder(order.value.id, { reason: refundReason.value.trim() });
    if (!error) {
      message.success('订单退款成功');
      refundModalVisible.value = false;
      await loadData();
    }
  } finally {
    refunding.value = false;
  }
}

onMounted(() => { loadData(); });
</script>

<template>
  <NSpin :show="loading">
    <NSpace v-if="order" vertical :size="16">
      <NCard :bordered="false" class="detail-hero">
        <div class="detail-hero__eyebrow">订单详情</div>
        <div class="detail-hero__head">
          <div>
            <h2 class="detail-hero__title">把订单金额、菜品明细和操作日志集中到一张详情面板里</h2>
            <p class="detail-hero__desc">适合在对账、售后和异常处理时快速回看订单全链路信息，不再需要在多个业务页之间来回切换。</p>
          </div>
          <div class="detail-hero__badge">
            <span>订单状态</span>
            <strong>{{ (statusMap[order.status] || {}).label || '未知' }}</strong>
          </div>
        </div>
      </NCard>

      <NCard :bordered="false" title="订单信息">
        <template #header-extra>
          <NButton v-if="order.status === 1" type="error" secondary @click="openRefundModal">
            整单退款
          </NButton>
        </template>
        <NDescriptions :column="3" label-placement="left">
          <NDescriptionsItem label="订单编号">{{ order.orderNo }}</NDescriptionsItem>
          <NDescriptionsItem label="桌台">{{ order.tableCode }}</NDescriptionsItem>
          <NDescriptionsItem label="区域">{{ order.areaName || '未分区' }}</NDescriptionsItem>
          <NDescriptionsItem label="状态">
            <NTag :type="(statusMap[order.status] || {}).type || 'info'">
              {{ (statusMap[order.status] || {}).label || '未知' }}
            </NTag>
          </NDescriptionsItem>
          <NDescriptionsItem label="原价">¥{{ order.originalAmount }}</NDescriptionsItem>
          <NDescriptionsItem label="折扣">{{ order.discountRate ? `${order.discountRate}折` : '无' }}</NDescriptionsItem>
          <NDescriptionsItem label="实付金额">¥{{ order.actualAmount }}</NDescriptionsItem>
          <NDescriptionsItem label="订单类型">{{ order.orderType === 0 ? '堂食' : '外卖' }}</NDescriptionsItem>
          <NDescriptionsItem label="支付模式">{{ order.paymentMode === 0 ? '餐前付' : '餐后付' }}</NDescriptionsItem>
          <NDescriptionsItem label="支付类型">{{ order.paymentMethod != null ? (paymentMethodMap[order.paymentMethod] || '未知') : '-' }}</NDescriptionsItem>
          <NDescriptionsItem label="下单时间">{{ order.createTime }}</NDescriptionsItem>
          <NDescriptionsItem label="备注">{{ order.remark || '无' }}</NDescriptionsItem>
        </NDescriptions>
      </NCard>

      <NCard :bordered="false" title="订单项">
        <NDataTable :columns="itemColumns" :data="order.items || []" />
      </NCard>

      <NCard v-if="order.operationLogs && order.operationLogs.length" :bordered="false" title="操作日志">
        <NDataTable :columns="logColumns" :data="order.operationLogs" />
      </NCard>
    </NSpace>

    <NModal
      v-model:show="refundModalVisible"
      preset="card"
      title="整单退款"
      style="width: 520px"
      :mask-closable="false"
    >
      <NSpace vertical :size="12">
        <div>退款后将同步把订单状态改为“已退款”，支付记录改为“已退款”，并回退会员积分和成长值。</div>
        <NInput
          v-model:value="refundReason"
          type="textarea"
          :rows="4"
          maxlength="200"
          show-count
          placeholder="请输入退款原因，例如：顾客取消、菜品异常、线下纠纷处理"
        />
        <NSpace justify="end">
          <NButton @click="refundModalVisible = false">取消</NButton>
          <NButton type="error" :loading="refunding" @click="handleRefundOrder">确认退款</NButton>
        </NSpace>
      </NSpace>
    </NModal>
  </NSpin>
</template>

<style scoped>
.detail-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.detail-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.detail-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.detail-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.detail-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.detail-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.detail-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.detail-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}
</style>
