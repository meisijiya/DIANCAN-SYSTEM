<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NDatePicker, NForm, NFormItem, NGi, NGrid, NInput, NSelect, NSpace, NTag } from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import { fetchPaymentRecordList, fetchTableAreaList } from '@/service/api';

defineOptions({ name: 'DevicePayment' });

const loading = ref(false);
const data = ref<Api.Business.PaymentRecord[]>([]);
const total = ref(0);
const dateRange = ref<[number, number] | null>(null);

const searchForm = ref<Api.Business.PaymentRecordQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20
});

const paymentMethodOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '微信', value: 0 },
  { label: '支付宝', value: 1 },
  { label: '现金', value: 2 }
];

const statusOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已退款', value: 2 },
  { label: '失败', value: 3 }
];
const areaOptions = ref<SelectOption[]>([{ label: '全部区域', value: undefined }]);

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function paymentMethodLabel(method: number) {
  if (method === 0) return '微信';
  if (method === 1) return '支付宝';
  if (method === 2) return '现金';
  return '-';
}

function statusMeta(status: number): { label: string; type: 'warning' | 'success' | 'info' | 'error' } {
  if (status === 0) return { label: '待支付', type: 'warning' };
  if (status === 1) return { label: '已支付', type: 'success' };
  if (status === 2) return { label: '已退款', type: 'info' };
  if (status === 3) return { label: '失败', type: 'error' };
  return { label: '未知', type: 'warning' };
}

const columns: DataTableColumns<Api.Business.PaymentRecord> = [
  { title: '支付单号', key: 'paymentNo', width: 220 },
  { title: '订单号', key: 'orderNo', width: 200 },
  { title: '桌台', key: 'tableCode', width: 100, render: row => row.tableCode || '-' },
  { title: '区域', key: 'areaName', width: 100, render: row => row.areaName || '未分区' },
  { title: '支付方式', key: 'paymentMethod', width: 100, render: row => paymentMethodLabel(row.paymentMethod) },
  { title: '金额', key: 'amount', width: 100 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => {
      const meta = statusMeta(row.status);
      return h(NTag, { type: meta.type }, { default: () => meta.label });
    }
  },
  { title: '付款人', key: 'payerName', width: 180, render: row => row.payerName || row.payerOpenid || '-' },
  { title: '时间', key: 'createTime', width: 180 }
];

async function loadData() {
  loading.value = true;
  if (dateRange.value) {
    searchForm.value.startDate = formatDate(dateRange.value[0]);
    searchForm.value.endDate = formatDate(dateRange.value[1]);
  } else {
    searchForm.value.startDate = undefined;
    searchForm.value.endDate = undefined;
  }

  try {
    const { data: result, error } = await fetchPaymentRecordList(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

async function loadAreaOptions() {
  const { data: result, error } = await fetchTableAreaList();
  if (!error && result) {
    areaOptions.value = [
      { label: '全部区域', value: undefined },
      { label: '未分区', value: '未分区' },
      ...result.map(item => ({ label: item.name, value: item.name }))
    ];
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = { pageNum: 1, pageSize: 20, areaName: undefined };
  dateRange.value = null;
  loadData();
}

function handlePageChange(page: number) {
  searchForm.value.pageNum = page;
  loadData();
}

function handlePageSizeChange(pageSize: number) {
  searchForm.value.pageSize = pageSize;
  searchForm.value.pageNum = 1;
  loadData();
}

onMounted(() => {
  loadAreaOptions();
  loadData();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="16">
          <NGi :span="6">
            <NFormItem label="支付单号">
              <NInput v-model:value="searchForm.paymentNo" clearable placeholder="请输入支付单号" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="订单ID">
              <NInput v-model:value="searchForm.orderId" clearable placeholder="请输入订单ID" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="支付方式">
              <NSelect v-model:value="searchForm.paymentMethod" :options="paymentMethodOptions" clearable placeholder="全部" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.status" :options="statusOptions" clearable placeholder="全部" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="区域">
              <NSelect v-model:value="searchForm.areaName" :options="areaOptions" clearable placeholder="全部区域" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="时间范围">
              <NDatePicker v-model:value="dateRange" clearable style="width: 100%" type="daterange" />
            </NFormItem>
          </NGi>
        </NGrid>
        <NSpace justify="end" class="search-actions">
          <NButton type="primary" @click="handleSearch">查询</NButton>
          <NButton @click="handleReset">重置</NButton>
        </NSpace>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="支付记录">
      <NDataTable
        remote
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="{
          page: searchForm.pageNum,
          pageSize: searchForm.pageSize,
          itemCount: total,
          showSizePicker: true,
          prefix: ({ itemCount }) => `共 ${itemCount} 条`,
          pageSizes: [10, 20, 50, 100, 200],
          onChange: handlePageChange,
          onUpdatePageSize: handlePageSizeChange
        }"
      />
    </NCard>
  </NSpace>
</template>

<style scoped>
.search-actions {
  width: 100%;
}
</style>

