<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NDatePicker, NForm, NFormItem, NGi, NGrid, NInput, NInputNumber, NSpace, NTag } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import dayjs from 'dayjs';
import { fetchMemberGrowthRecordPage } from '@/service/api';

defineOptions({ name: 'MarketingMemberGrowthRecord' });

type RowData = Record<string, any>;

const loading = ref(false);
const total = ref(0);
const data = ref<RowData[]>([]);
const dateRange = ref<[number, number] | null>(null);

const searchForm = ref({
  memberId: undefined as number | undefined,
  userId: undefined as number | undefined,
  bizType: '',
  pageNum: 1,
  pageSize: 10
});

const columns: DataTableColumns<RowData> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '会员编号', key: 'memberNo', width: 200 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '业务类型', key: 'bizType', width: 120 },
  { title: '业务ID', key: 'bizId', width: 180 },
  { title: '成长值变动', key: 'changeAmount', width: 110, render: row => h(NTag, { type: 'success' }, { default: () => `+${row.changeAmount || 0}` }) },
  { title: '当前成长值', key: 'growthAfter', width: 110 },
  { title: '备注', key: 'remark', minWidth: 180 }
];

async function loadData() {
  loading.value = true;
  try {
    const params: Record<string, any> = { ...searchForm.value };
    if (dateRange.value) {
      params.startDate = dayjs(dateRange.value[0]).format('YYYY-MM-DD');
      params.endDate = dayjs(dateRange.value[1]).format('YYYY-MM-DD');
    }
    const { data: result, error } = await fetchMemberGrowthRecordPage(params);
    if (!error && result) {
      data.value = result.list || [];
      total.value = result.total || 0;
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
  searchForm.value = { memberId: undefined, userId: undefined, bizType: '', pageNum: 1, pageSize: 10 };
  dateRange.value = null;
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="growth-hero">
      <div class="growth-hero__eyebrow">GROWTH TRACKING</div>
      <div class="growth-hero__head">
        <div>
          <h2 class="growth-hero__title">成长值流水更像一条会员升级轨迹</h2>
          <p class="growth-hero__desc">适合追踪升级门槛、活动带来的成长增量，以及消费驱动的会员层级变化节奏。</p>
        </div>
        <div class="growth-hero__badge">
          <span>当前结果</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="90">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="5"><NFormItem label="会员ID"><NInputNumber v-model:value="searchForm.memberId" placeholder="请输入会员ID" clearable style="width: 100%" /></NFormItem></NGi>
          <NGi :span="5"><NFormItem label="用户ID"><NInputNumber v-model:value="searchForm.userId" placeholder="请输入用户ID" clearable style="width: 100%" /></NFormItem></NGi>
          <NGi :span="6"><NFormItem label="业务类型"><NInput v-model:value="searchForm.bizType" placeholder="例如 ORDER_PAY" clearable /></NFormItem></NGi>
          <NGi :span="8"><NFormItem label="时间范围"><NDatePicker v-model:value="dateRange" type="daterange" clearable style="width: 100%" /></NFormItem></NGi>
        </NGrid>
        <NSpace><NButton type="primary" @click="handleSearch">搜索</NButton><NButton @click="handleReset">重置</NButton></NSpace>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="成长流水">
      <NDataTable remote :columns="columns" :data="data" :loading="loading" :pagination="{ page: searchForm.pageNum, pageSize: searchForm.pageSize, itemCount: total, showSizePicker: true, prefix: ({ itemCount }) => `共 ${itemCount} 条`, pageSizes: [10, 20, 50, 100], onChange: (page: number) => { searchForm.pageNum = page; loadData(); }, onUpdatePageSize: (pageSize: number) => { searchForm.pageSize = pageSize; searchForm.pageNum = 1; loadData(); } }" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.growth-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.growth-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.growth-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.growth-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.growth-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.growth-hero__badge {
  min-width: 150px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.growth-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.growth-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}
</style>

