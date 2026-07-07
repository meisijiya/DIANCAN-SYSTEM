<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import {
  NButton, NCard, NSpace, NDataTable, NForm, NFormItem, NInput, NSelect,
  NGrid, NGi, NTag, NDatePicker, useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import { useRouter } from 'vue-router';
import { fetchOrderList, fetchTableAreaList } from '@/service/api';

defineOptions({ name: 'OrderList' });

const router = useRouter();
const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.Order[]>([]);
const total = ref(0);

const searchForm = ref<Api.Business.OrderQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 10, startDate: undefined, endDate: undefined, status: undefined, orderNo: ''
});

const dateRange = ref<[number, number] | null>(null);

const statusOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '待支付', value: 0 },
  { label: '已支付', value: 1 },
  { label: '已取消', value: 2 },
  { label: '已退款', value: 3 }
];
const areaOptions = ref<SelectOption[]>([{ label: '全部区域', value: undefined }]);

const statusMap: Record<number, { label: string; type: 'warning' | 'success' | 'error' | 'info' }> = {
  0: { label: '待支付', type: 'warning' },
  1: { label: '已支付', type: 'success' },
  2: { label: '已取消', type: 'error' },
  3: { label: '已退款', type: 'info' }
};

const columns: DataTableColumns<Api.Business.Order> = [
  { title: '订单编号', key: 'orderNo', width: 180 },
  { title: '桌台', key: 'tableCode', width: 80 },
  { title: '区域', key: 'areaName', width: 100, render(row) { return row.areaName || '未分区'; } },
  { title: '原价', key: 'originalAmount', width: 90, render(row) { return `¥${row.originalAmount}`; } },
  { title: '折扣', key: 'discountRate', width: 70, render(row) { return row.discountRate ? `${row.discountRate}折` : '-'; } },
  { title: '实付', key: 'actualAmount', width: 90, render(row) { return `¥${row.actualAmount}`; } },
  {
    title: '状态', key: 'status', width: 90,
    render(row) {
      const s = statusMap[row.status] || { label: '未知', type: 'warning' as const };
      return h(NTag, { type: s.type }, { default: () => s.label });
    }
  },
  {
    title: '类型', key: 'orderType', width: 70,
    render(row) { return row.orderType === 0 ? '堂食' : '外卖'; }
  },
  { title: '下单时间', key: 'createTime', width: 170 },
  {
    title: '操作', key: 'actions', width: 100,
    render(row) {
      return h(NButton, {
        size: 'small', type: 'primary',
        onClick: () => router.push(`/order/detail/${row.id}`)
      }, { default: () => '详情' });
    }
  }
];

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

async function loadData() {
  loading.value = true;
  // 处理日期范围
  if (dateRange.value) {
    searchForm.value.startDate = formatDate(dateRange.value[0]);
    searchForm.value.endDate = formatDate(dateRange.value[1]);
  } else {
    searchForm.value.startDate = undefined;
    searchForm.value.endDate = undefined;
  }
  try {
    const { data: result, error } = await fetchOrderList(searchForm.value);
    if (!error && result) { data.value = result.list; total.value = result.total; }
  } finally { loading.value = false; }
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

function handleSearch() { searchForm.value.pageNum = 1; loadData(); }
function handleReset() {
  searchForm.value = { pageNum: 1, pageSize: 10, startDate: undefined, endDate: undefined, status: undefined, orderNo: '', areaName: undefined };
  dateRange.value = null;
  loadData();
}
function handlePageChange(page: number) { searchForm.value.pageNum = page; loadData(); }
function handlePageSizeChange(pageSize: number) { searchForm.value.pageSize = pageSize; searchForm.value.pageNum = 1; loadData(); }

onMounted(() => {
  loadAreaOptions();
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="order-list-hero">
      <div class="order-list-hero__eyebrow">订单目录</div>
      <div class="order-list-hero__head">
        <div>
          <h2 class="order-list-hero__title">把订单检索、状态筛选和明细入口做成一张统一的订单目录</h2>
          <p class="order-list-hero__desc">适合快速查单、核对支付状态和按日期回看订单流转，不需要再从多个业务页回跳。</p>
        </div>
        <div class="order-list-hero__badge">
          <span>当前订单数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="订单编号">
              <NInput v-model:value="searchForm.orderNo" placeholder="请输入订单编号" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="5">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.status" :options="statusOptions" placeholder="全部" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="区域">
              <NSelect v-model:value="searchForm.areaName" :options="areaOptions" placeholder="全部区域" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="7">
            <NFormItem label="日期范围">
              <NDatePicker v-model:value="dateRange" type="daterange" clearable style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NSpace justify="end" class="search-actions">
              <NButton type="primary" @click="handleSearch">搜索</NButton>
              <NButton @click="handleReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>
    <NCard :bordered="false" title="订单列表">
      <NDataTable remote :columns="columns" :data="data" :loading="loading" :pagination="{
        page: searchForm.pageNum, pageSize: searchForm.pageSize, itemCount: total,
        showSizePicker: true, showQuickJumper: true,
        prefix: ({ itemCount, pageCount }) => `共 ${itemCount} 条 / ${pageCount} 页`,
        pageSizes: [10, 20, 50, 100, 200],
        onChange: handlePageChange, onUpdatePageSize: handlePageSizeChange
      }" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.order-list-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.order-list-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.order-list-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.order-list-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.order-list-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.order-list-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.order-list-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.order-list-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}

.search-actions {
  width: 100%;
}
</style>

