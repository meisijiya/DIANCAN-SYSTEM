<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NDatePicker, NForm, NFormItem, NGi, NGrid, NInput, NInputNumber, NSelect, NSpace, NTag } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import dayjs from 'dayjs';
import { fetchMemberPointsRecordPage } from '@/service/api';

defineOptions({ name: 'MarketingMemberPointsRecord' });

type RowData = Record<string, any>;

const loading = ref(false);
const total = ref(0);
const data = ref<RowData[]>([]);
const dateRange = ref<[number, number] | null>(null);

const searchForm = ref({
  memberId: undefined as number | undefined,
  userId: undefined as number | undefined,
  bizType: '',
  changeType: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
});

const changeTypeOptions = [
  { label: '增加', value: 1 },
  { label: '扣减', value: 2 },
  { label: '过期', value: 3 },
  { label: '调整', value: 4 },
  { label: '退款回退', value: 5 }
];

const columns: DataTableColumns<RowData> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '会员编号', key: 'memberNo', width: 200 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '业务类型', key: 'bizType', width: 120 },
  { title: '业务ID', key: 'bizId', width: 180 },
  {
    title: '变动类型',
    key: 'changeType',
    width: 100,
    render(row) {
      const map: Record<number, { label: string; type: 'success' | 'error' | 'warning' | 'info' | 'default' }> = { 1: { label: '增加', type: 'success' }, 2: { label: '扣减', type: 'error' }, 3: { label: '过期', type: 'warning' }, 4: { label: '调整', type: 'info' }, 5: { label: '退款回退', type: 'default' } };
      const current = map[row.changeType] || map[4];
      return h(NTag, { type: current.type }, { default: () => current.label });
    }
  },
  {
    title: '变动积分',
    key: 'changeAmount',
    width: 100,
    render(row) {
      const positive = Number(row.changeAmount || 0) >= 0;
      return h(NTag, { type: positive ? 'success' : 'error' }, { default: () => `${positive ? '+' : ''}${row.changeAmount || 0}` });
    }
  },
  { title: '变动后余额', key: 'balanceAfter', width: 110 },
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
    const { data: result, error } = await fetchMemberPointsRecordPage(params);
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
  searchForm.value = { memberId: undefined, userId: undefined, bizType: '', changeType: undefined, pageNum: 1, pageSize: 10 };
  dateRange.value = null;
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="record-hero">
      <div class="record-hero__eyebrow">POINTS LEDGER</div>
      <div class="record-hero__head">
        <div>
          <h2 class="record-hero__title">积分流水更适合做成运营账本</h2>
          <p class="record-hero__desc">筛选会员、业务类型和变动区间，快速定位异常补分、扣减或退款回退记录。</p>
        </div>
        <div class="record-hero__badge">
          <span>当前结果</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="90">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="4"><NFormItem label="会员ID"><NInputNumber v-model:value="searchForm.memberId" placeholder="请输入会员ID" clearable style="width: 100%" /></NFormItem></NGi>
          <NGi :span="4"><NFormItem label="用户ID"><NInputNumber v-model:value="searchForm.userId" placeholder="请输入用户ID" clearable style="width: 100%" /></NFormItem></NGi>
          <NGi :span="5"><NFormItem label="业务类型"><NInput v-model:value="searchForm.bizType" placeholder="例如 ORDER_PAY" clearable /></NFormItem></NGi>
          <NGi :span="4"><NFormItem label="变动类型"><NSelect v-model:value="searchForm.changeType" :options="changeTypeOptions" clearable /></NFormItem></NGi>
          <NGi :span="7"><NFormItem label="时间范围"><NDatePicker v-model:value="dateRange" type="daterange" clearable style="width: 100%" /></NFormItem></NGi>
        </NGrid>
        <NSpace><NButton type="primary" @click="handleSearch">搜索</NButton><NButton @click="handleReset">重置</NButton></NSpace>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="积分流水">
      <NDataTable remote :columns="columns" :data="data" :loading="loading" :pagination="{ page: searchForm.pageNum, pageSize: searchForm.pageSize, itemCount: total, showSizePicker: true, prefix: ({ itemCount }) => `共 ${itemCount} 条`, pageSizes: [10, 20, 50, 100], onChange: (page: number) => { searchForm.pageNum = page; loadData(); }, onUpdatePageSize: (pageSize: number) => { searchForm.pageSize = pageSize; searchForm.pageNum = 1; loadData(); } }" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.record-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.record-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.record-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.record-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.record-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.record-hero__badge {
  min-width: 150px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.record-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.record-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}
</style>

