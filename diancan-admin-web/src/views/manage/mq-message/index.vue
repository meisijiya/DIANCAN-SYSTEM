<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGi, NGrid, NInput, NSelect, NSpace, NTag, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchMqMessagePage, retryMqMessage } from '@/service/api';

defineOptions({
  name: 'ManageMqMessage'
});

const message = useMessage();
const loading = ref(false);
const searchForm = ref<Api.Mq.MessageQuery>({
  bizType: '',
  messageKey: '',
  deliverStatus: undefined,
  pageNum: 1,
  pageSize: 10
});
const data = ref<Api.Mq.Message[]>([]);
const total = ref(0);

const bizTypeLabelMap: Record<string, string> = {
  COUPON_GRANT_DISPATCH: '发券任务分发',
  COUPON_GRANT_BATCH: '发券任务批处理'
};

const statusOptions = [
  { label: '待投递', value: 0 },
  { label: '投递中', value: 1 },
  { label: '已投递', value: 2 },
  { label: '投递失败', value: 3 },
  { label: '死信', value: 4 }
];

const columns: DataTableColumns<Api.Mq.Message> = [
  { title: '消息键', key: 'messageKey', width: 240, ellipsis: { tooltip: true }, fixed: 'left' },
  { title: '主题', key: 'topic', width: 180 },
  { title: '标签', key: 'tag', width: 120 },
  {
    title: '业务类型',
    key: 'bizType',
    width: 180,
    render: row => bizTypeLabelMap[row.bizType] || row.bizType
  },
  {
    title: '状态',
    key: 'deliverStatus',
    width: 100,
    render: row => {
      const statusMap: Record<number, { label: string; type: 'default' | 'success' | 'warning' | 'error' | 'info' }> = {
        0: { label: '待投递', type: 'warning' },
        1: { label: '投递中', type: 'info' },
        2: { label: '已投递', type: 'success' },
        3: { label: '投递失败', type: 'error' },
        4: { label: '死信', type: 'default' }
      };
      const current = statusMap[row.deliverStatus] || statusMap[0];
      return h(NTag, { type: current.type }, { default: () => current.label });
    }
  },
  {
    title: '业务结果',
    key: 'bizStatusText',
    width: 240,
    render: row => {
      if (!row.bizStatusText) return '-';
      const type =
        row.bizStatusText === '业务成功'
          ? 'success'
          : row.bizStatusText === '部分成功'
            ? 'warning'
            : row.bizStatusText === '业务失败'
              ? 'error'
              : 'default';
      return h('div', { class: 'biz-result-cell' }, [
        h(NTag, { type, size: 'small' }, { default: () => row.bizStatusText || '-' }),
        row.bizStatusDetail ? h('div', { class: 'biz-result-cell__detail' }, row.bizStatusDetail) : null
      ]);
    }
  },
  { title: '重试次数', key: 'retryCount', width: 100 },
  { title: '下次重试', key: 'nextRetryTime', width: 180 },
  { title: '最后错误', key: 'lastError', width: 220, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          type: 'primary',
          disabled: row.deliverStatus !== 3 && row.deliverStatus !== 4,
          onClick: () => handleRetry(row.id)
        },
        { default: () => '重试' }
      )
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchMqMessagePage(searchForm.value);
    if (!error && result) {
      data.value = result.list || [];
      total.value = result.total || 0;
    }
  } finally {
    loading.value = false;
  }
}

async function handleRetry(id: string) {
  const { error } = await retryMqMessage(id);
  if (!error) {
    message.success('消息已重新进入待发送队列');
    loadData();
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = {
    bizType: '',
    messageKey: '',
    deliverStatus: undefined,
    pageNum: 1,
    pageSize: 10
  };
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="mq-hero">
      <div class="mq-hero__eyebrow">MESSAGE RELAY</div>
      <div class="mq-hero__head">
        <div>
          <h2 class="mq-hero__title">把消息投递状态、业务结果和重试入口统一放进一张消息看板里</h2>
          <p class="mq-hero__desc">适合排查发券链路、死信消息和投递异常，让队列状态、业务结果和人工补救动作保持在同一个视角下。</p>
        </div>
        <div class="mq-hero__badge">
          <span>当前消息数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="mq-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="业务类型">
              <NInput v-model:value="searchForm.bizType" placeholder="例如：COUPON_GRANT_BATCH" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="消息键">
              <NInput v-model:value="searchForm.messageKey" placeholder="请输入消息键" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.deliverStatus" :options="statusOptions" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NSpace justify="end" class="search-actions">
              <NButton type="primary" @click="handleSearch">搜索</NButton>
              <NButton @click="handleReset">重置</NButton>
              <NButton @click="loadData">刷新</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="消息记录" class="mq-list-card">
      <NDataTable
        remote
        :columns="columns"
        :data="data"
        :loading="loading"
        :scroll-x="1780"
        :pagination="{
          page: searchForm.pageNum,
          pageSize: searchForm.pageSize,
          itemCount: total,
          showSizePicker: true,
          prefix: ({ itemCount }) => `共 ${itemCount} 条`,
          pageSizes: [10, 20, 50, 100, 200],
          onChange: (page: number) => { searchForm.pageNum = page; loadData(); },
          onUpdatePageSize: (pageSize: number) => { searchForm.pageSize = pageSize; searchForm.pageNum = 1; loadData(); }
        }"
      />
    </NCard>
  </NSpace>
</template>

<style scoped>
.search-actions {
  width: 100%;
}

.mq-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.mq-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.mq-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.mq-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.mq-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.mq-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.mq-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.mq-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.mq-filter-card,
.mq-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.mq-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

.biz-result-cell {
  display: grid;
  gap: 6px;
}

.biz-result-cell__detail {
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

html.dark .mq-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .mq-hero__title {
  color: rgba(243, 247, 255, 0.96);
}

html.dark .mq-hero__desc {
  color: rgba(209, 220, 241, 0.72);
}

html.dark .mq-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .mq-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .mq-hero__badge strong {
  color: #dbe5ff;
}

html.dark .mq-filter-card,
html.dark .mq-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .biz-result-cell__detail {
  color: rgba(206, 216, 236, 0.68);
}
</style>

