<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue';
import {
  NButton, NCard, NSpace, NDataTable, NForm, NFormItem, NInput, NSelect,
  NGrid, NGi, NDatePicker, NTabPane, NTabs, NTag, useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import {
  downloadAuditLogExportTaskFile,
  fetchAuditLogExportTaskPage,
  fetchAuditLogList,
  submitAuditLogExportTask
} from '@/service/api';

defineOptions({ name: 'DeviceAuditLog' });

const message = useMessage();
const activeTab = ref('audit');
const loading = ref(false);
const data = ref<Api.Business.AuditLog[]>([]);
const total = ref(0);
const dateRange = ref<[number, number] | null>(null);
const exportTaskLoading = ref(false);
const exportTaskData = ref<Api.Business.AuditLogExportTask[]>([]);
const exportTaskTotal = ref(0);
const latestTaskId = ref<number | null>(null);
const exportTaskSearch = ref<Api.Business.AuditLogExportTaskQuery>({
  pageNum: 1,
  pageSize: 5
});
let exportTaskTimer: number | null = null;
const exportTaskStatusMap = new Map<number, number>();

const searchForm = ref<Api.Business.AuditLogQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1, pageSize: 20, operatorName: '', operationType: undefined
});

const operationTypeOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '退菜', value: 'RETURN' },
  { label: '换菜', value: 'REPLACE' },
  { label: '赠送', value: 'GIFT' },
  { label: '打折', value: 'DISCOUNT' },
  { label: '催单', value: 'RUSH' }
];

const columns: DataTableColumns<Api.Business.AuditLog> = [
  { title: '订单ID', key: 'orderId', width: 80 },
  { title: '操作类型', key: 'operationType', width: 80 },
  { title: '操作人', key: 'operatorName', width: 100 },
  { title: '原因', key: 'reason', width: 150, render(row) { return row.reason || '-'; } },
  { title: '详情', key: 'detail', width: 200, render(row) { return row.detail || '-'; } },
  { title: '操作时间', key: 'createTime', width: 170 }
];

const exportTaskColumns: DataTableColumns<Api.Business.AuditLogExportTask> = [
  {
    title: '状态',
    key: 'taskStatus',
    width: 96,
    render(row) {
      const statusMap = {
        0: { type: 'warning' as const, label: '待处理' },
        1: { type: 'info' as const, label: '处理中' },
        2: { type: 'success' as const, label: '成功' },
        3: { type: 'error' as const, label: '失败' }
      };
      const status = statusMap[row.taskStatus as 0 | 1 | 2 | 3] || { type: 'default' as const, label: '未知' };
      return h(NTag, { type: status.type, size: 'small', bordered: false }, { default: () => status.label });
    }
  },
  {
    title: '筛选条件',
    key: 'filters',
    minWidth: 220,
    render(row) {
      const parts = [
        row.operatorName ? `操作人：${row.operatorName}` : '',
        row.operationType ? `类型：${row.operationType}` : '',
        row.startDate || row.endDate ? `日期：${row.startDate || '-'} ~ ${row.endDate || '-'}` : ''
      ].filter(Boolean);
      return parts.join(' / ') || '全部数据';
    }
  },
  {
    title: '进度',
    key: 'progress',
    width: 100,
    render(row) {
      return `${row.exportedCount || 0}/${row.totalCount || 0}`;
    }
  },
  { title: '提交时间', key: 'createTime', width: 168 },
  {
    title: '结果',
    key: 'lastError',
    minWidth: 180,
    render(row) {
      return row.taskStatus === 3 ? row.lastError || '导出失败' : row.fileName || '-';
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render(row) {
      return h(
        NButton,
        {
          text: true,
          type: 'primary',
          disabled: row.taskStatus !== 2,
          onClick: () => handleDownloadTask(row)
        },
        { default: () => '下载' }
      );
    }
  }
];

const hasRunningTask = computed(() => exportTaskData.value.some(item => item.taskStatus === 0 || item.taskStatus === 1));

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
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
    const { data: result, error } = await fetchAuditLogList(searchForm.value);
    if (!error && result) { data.value = result.list; total.value = result.total; }
  } finally { loading.value = false; }
}

async function loadExportTasks() {
  exportTaskLoading.value = true;
  try {
    const { data: result, error } = await fetchAuditLogExportTaskPage(exportTaskSearch.value);
    if (!error && result) {
      notifyExportTaskStatus(result.list);
      exportTaskData.value = result.list;
      exportTaskTotal.value = result.total;
      latestTaskId.value = result.list[0]?.id ?? null;
      restartTaskPolling();
    }
  } finally {
    exportTaskLoading.value = false;
  }
}

function notifyExportTaskStatus(tasks: Api.Business.AuditLogExportTask[]) {
  const nextStatusMap = new Map<number, number>();
  tasks.forEach(task => {
    nextStatusMap.set(task.id, task.taskStatus);
    const previousStatus = exportTaskStatusMap.get(task.id);
    if ((previousStatus === 0 || previousStatus === 1) && task.taskStatus === 2) {
      message.success(`导出完成：${task.fileName || `任务#${task.id}`}`);
    }
    if ((previousStatus === 0 || previousStatus === 1) && task.taskStatus === 3) {
      message.error(`导出失败：${task.lastError || `任务#${task.id}`}`);
    }
  });
  exportTaskStatusMap.clear();
  nextStatusMap.forEach((status, id) => exportTaskStatusMap.set(id, status));
}

function handleSearch() { searchForm.value.pageNum = 1; loadData(); }
function handleReset() {
  searchForm.value = { pageNum: 1, pageSize: 20, operatorName: '', operationType: undefined };
  dateRange.value = null;
  loadData();
}

async function handleExport() {
  const params: Api.Business.AuditLogQuery = { ...searchForm.value };
  if (dateRange.value) {
    params.startDate = formatDate(dateRange.value[0]);
    params.endDate = formatDate(dateRange.value[1]);
  }
  const { data: task, error } = await submitAuditLogExportTask(params);
  if (!error && task) {
    message.success('导出任务已提交');
    exportTaskSearch.value.pageNum = 1;
    await loadExportTasks();
  }
}
function handlePageChange(page: number) { searchForm.value.pageNum = page; loadData(); }
function handlePageSizeChange(pageSize: number) { searchForm.value.pageSize = pageSize; searchForm.value.pageNum = 1; loadData(); }

function handleExportTaskPageChange(page: number) {
  exportTaskSearch.value.pageNum = page;
  loadExportTasks();
}

function handleExportTaskPageSizeChange(pageSize: number) {
  exportTaskSearch.value.pageSize = pageSize;
  exportTaskSearch.value.pageNum = 1;
  loadExportTasks();
}

async function handleDownloadTask(row: Api.Business.AuditLogExportTask) {
  try {
    await downloadAuditLogExportTaskFile(row.id, row.fileName || undefined);
    message.success('导出文件下载已开始');
  } catch (error) {
    message.error(error instanceof Error ? error.message : '下载失败');
  }
}

function exportTaskRowClassName(row: Api.Business.AuditLogExportTask) {
  if (row.id === latestTaskId.value) {
    return 'audit-export-task-row-latest';
  }
  return '';
}

function clearTaskPolling() {
  if (exportTaskTimer !== null) {
    window.clearTimeout(exportTaskTimer);
    exportTaskTimer = null;
  }
}

function restartTaskPolling() {
  clearTaskPolling();
  if (!hasRunningTask.value) {
    return;
  }
  exportTaskTimer = window.setTimeout(() => {
    void loadExportTasks();
  }, 2000);
}

onMounted(() => {
  loadData();
  loadExportTasks();
});

onBeforeUnmount(() => {
  clearTaskPolling();
});
</script>

<template>
  <NCard :bordered="false">
    <NTabs v-model:value="activeTab" type="line" animated>
      <NTabPane name="audit" tab="审计日志">
        <NSpace vertical :size="16">
          <NForm :model="searchForm" label-placement="left" label-width="80">
            <NGrid :cols="24" :x-gap="18">
              <NGi :span="5">
                <NFormItem label="操作人">
                  <NInput v-model:value="searchForm.operatorName" placeholder="请输入操作人" clearable />
                </NFormItem>
              </NGi>
              <NGi :span="5">
                <NFormItem label="操作类型">
                  <NSelect v-model:value="searchForm.operationType" :options="operationTypeOptions" placeholder="全部" clearable />
                </NFormItem>
              </NGi>
              <NGi :span="7">
                <NFormItem label="日期范围">
                  <NDatePicker v-model:value="dateRange" type="daterange" clearable style="width: 100%" />
                </NFormItem>
              </NGi>
              <NGi :span="7">
                <NSpace justify="end" class="search-actions">
                  <NButton type="primary" @click="handleSearch">搜索</NButton>
                  <NButton @click="handleReset">重置</NButton>
                  <NButton type="warning" @click="handleExport">导出</NButton>
                </NSpace>
              </NGi>
            </NGrid>
          </NForm>
          <NDataTable remote :columns="columns" :data="data" :loading="loading" :pagination="{
            page: searchForm.pageNum, pageSize: searchForm.pageSize, itemCount: total,
            showSizePicker: true, showQuickJumper: true,
            prefix: ({ itemCount, pageCount }) => `共 ${itemCount} 条 / ${pageCount} 页`,
            pageSizes: [10, 20, 50, 100, 200],
            onChange: handlePageChange, onUpdatePageSize: handlePageSizeChange
          }" />
        </NSpace>
      </NTabPane>
      <NTabPane name="task" tab="导出任务">
        <NDataTable remote :columns="exportTaskColumns" :data="exportTaskData" :loading="exportTaskLoading" :row-class-name="exportTaskRowClassName" :pagination="{
          page: exportTaskSearch.pageNum, pageSize: exportTaskSearch.pageSize, itemCount: exportTaskTotal,
          showSizePicker: true, showQuickJumper: true,
          prefix: ({ itemCount, pageCount }) => `共 ${itemCount} 条 / ${pageCount} 页`,
          pageSizes: [5, 10, 20],
          onChange: handleExportTaskPageChange, onUpdatePageSize: handleExportTaskPageSizeChange
        }" />
      </NTabPane>
    </NTabs>
  </NCard>
</template>

<style scoped>
.search-actions {
  width: 100%;
}

:deep(.audit-export-task-row-latest td) {
  background: rgba(245, 158, 11, 0.08);
}
</style>

