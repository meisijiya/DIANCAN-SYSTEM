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
  NModal,
  NSelect,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import { fetchFeedbackList, replyFeedback } from '@/service/api';

defineOptions({ name: 'DeviceFeedback' });

const message = useMessage();
const loading = ref(false);
const total = ref(0);
const data = ref<Api.Business.FeedbackRecord[]>([]);
const showReplyModal = ref(false);
const replyLoading = ref(false);
const dateRange = ref<[number, number] | null>(null);
const currentRow = ref<Api.Business.FeedbackRecord | null>(null);

const searchForm = ref<Api.Business.FeedbackQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10,
  status: undefined,
  keyword: '',
  contactPhone: ''
});

const replyForm = ref<Api.Business.FeedbackReply>({
  replyContent: ''
});

const statusOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '待回复', value: 0 },
  { label: '已回复', value: 1 }
];

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

const columns: DataTableColumns<Api.Business.FeedbackRecord> = [
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row =>
      h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '已回复' : '待回复') })
  },
  { title: '用户昵称', key: 'customerNickname', width: 120, render: row => row.customerNickname || '-' },
  { title: '用户手机', key: 'customerPhone', width: 130, render: row => row.customerPhone || '-' },
  { title: '联系手机', key: 'contactPhone', width: 130, render: row => row.contactPhone || '-' },
  { title: '反馈内容', key: 'content', minWidth: 260 },
  { title: '回复内容', key: 'replyContent', minWidth: 220, render: row => row.replyContent || '-' },
  { title: '反馈时间', key: 'createTime', width: 180 },
  { title: '回复时间', key: 'replyTime', width: 180, render: row => row.replyTime || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 110,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          type: row.status === 1 ? 'info' : 'primary',
          secondary: false,
          onClick: () => handleOpenReply(row)
        },
        { default: () => (row.status === 1 ? '修改回复' : '去回复') }
      )
  }
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
    const { data: result, error } = await fetchFeedbackList(searchForm.value);
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
  searchForm.value = {
    pageNum: 1,
    pageSize: 10,
    status: undefined,
    keyword: '',
    contactPhone: ''
  };
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

function handleOpenReply(row: Api.Business.FeedbackRecord) {
  currentRow.value = row;
  replyForm.value = {
    replyContent: row.replyContent || ''
  };
  showReplyModal.value = true;
}

async function handleSubmitReply() {
  if (!currentRow.value) {
    return;
  }
  if (!replyForm.value.replyContent.trim()) {
    message.warning('请输入回复内容');
    return;
  }

  replyLoading.value = true;
  try {
    const { error } = await replyFeedback(currentRow.value.id, replyForm.value);
    if (!error) {
      message.success('回复成功');
      showReplyModal.value = false;
      await loadData();
    }
  } finally {
    replyLoading.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="16">
          <NGi :span="4">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.status" :options="statusOptions" clearable placeholder="请选择状态" />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="联系手机">
              <NInput v-model:value="searchForm.contactPhone" clearable placeholder="请输入联系手机" />
            </NFormItem>
          </NGi>
          <NGi :span="5">
            <NFormItem label="关键词">
              <NInput v-model:value="searchForm.keyword" clearable placeholder="请输入反馈内容关键词" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="时间范围">
              <NDatePicker v-model:value="dateRange" clearable style="width: 100%" type="daterange" />
            </NFormItem>
          </NGi>
          <NGi :span="5">
            <NSpace justify="end" class="feedback-actions">
              <NButton type="primary" @click="handleSearch">查询</NButton>
              <NButton @click="handleReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="反馈列表">
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

    <NModal v-model:show="showReplyModal" preset="card" title="回复反馈" style="width: 620px;">
      <NSpace vertical :size="14">
        <NCard v-if="currentRow" size="small" :bordered="false" embedded>
          <div class="mb-8px text-14px font-600">原始反馈</div>
          <div class="text-14px text-#475467">{{ currentRow.content }}</div>
        </NCard>
        <NInput v-model:value="replyForm.replyContent" type="textarea" :rows="6" maxlength="500" show-count placeholder="请输入回复内容" />
        <NSpace justify="end">
          <NButton @click="showReplyModal = false">取消</NButton>
          <NButton type="primary" :loading="replyLoading" @click="handleSubmitReply">保存回复</NButton>
        </NSpace>
      </NSpace>
    </NModal>
  </NSpace>
</template>

<style scoped>
:deep(.n-data-table .n-button) {
  min-width: 82px;
}

.feedback-actions {
  width: 100%;
}
</style>

