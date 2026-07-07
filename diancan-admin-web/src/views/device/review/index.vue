<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NRate,
  NSpace
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ReviewDetailPage from './detail/index.vue';
import { fetchReviewList } from '@/service/api';

defineOptions({ name: 'DeviceReview' });

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const data = ref<Api.Business.ReviewRecord[]>([]);
const total = ref(0);
const dateRange = ref<[number, number] | null>(null);

const searchForm = ref<Api.Business.ReviewQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 20
});
const detailOrderId = computed(() => String(route.query.orderId || ''));
const isDetailMode = computed(() => !!detailOrderId.value);

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const columns: DataTableColumns<Api.Business.ReviewRecord> = [
  { title: '订单号', key: 'orderNo', width: 200 },
  { title: '桌台', key: 'tableCode', width: 100, render: row => row.tableCode || '-' },
  {
    title: '评分',
    key: 'overallRating',
    width: 140,
    render: row => h(NRate, { value: row.overallRating, readonly: true, size: 'small' })
  },
  { title: '评价内容', key: 'content', minWidth: 220, render: row => row.content || '-' },
  { title: '用户标识', key: 'customerOpenid', width: 220, render: row => row.customerOpenid || '-' },
  { title: '时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          type: 'primary',
          secondary: true,
          onClick: () =>
            router.push({
              path: '/device/review',
              query: { orderId: String(row.orderId), mode: 'detail' }
            })
        },
        { default: () => '查看详情' }
      )
  }
];

function handleBackToList() {
  router.replace({ path: '/device/review' });
}

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
    const { data: result, error } = await fetchReviewList(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = { pageNum: 1, pageSize: 20 };
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

onMounted(loadData);
</script>

<template>
  <ReviewDetailPage v-if="isDetailMode" :order-id="detailOrderId" @back="handleBackToList" />

  <NSpace v-else vertical :size="16">
    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="16">
          <NGi :span="4">
            <NFormItem label="订单ID">
              <NInput v-model:value="searchForm.orderId" clearable placeholder="请输入订单ID" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="评分">
              <NInput v-model:value="searchForm.overallRating" clearable placeholder="1-5" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="用户标识">
              <NInput v-model:value="searchForm.customerOpenid" clearable placeholder="请输入用户标识" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="时间范围">
              <NDatePicker v-model:value="dateRange" clearable style="width: 100%" type="daterange" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NSpace justify="end" class="search-actions">
              <NButton type="primary" @click="handleSearch">查询</NButton>
              <NButton @click="handleReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="评价记录">
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

