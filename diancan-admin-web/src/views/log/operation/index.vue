<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NSpace, NInput, NForm, NFormItem, NGrid, NGi, NTag } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchOperationLogList } from '@/service/api';

defineOptions({
  name: 'LogOperation'
});

const loading = ref(false);
const searchForm = ref<Api.System.OperationLogQuery>({
  module: '',
  pageNum: 1,
  pageSize: 10
});

const data = ref<Api.System.OperationLog[]>([]);
const total = ref(0);

const columns: DataTableColumns<Api.System.OperationLog> = [
  { title: '模块', key: 'module', width: 100 },
  { title: '操作', key: 'operation', width: 120 },
  { title: '请求方式', key: 'requestMethod', width: 90 },
  { title: '操作人', key: 'username', width: 100 },
  { title: 'IP地址', key: 'ip', width: 130 },
  { title: '耗时(ms)', key: 'duration', width: 90 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error' }, { default: () => (row.status === 1 ? '成功' : '失败') });
    }
  },
  { title: '操作时间', key: 'createTime', width: 180 }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchOperationLogList(searchForm.value);
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
  searchForm.value = { module: '', pageNum: 1, pageSize: 10 };
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
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="模块名称">
              <NInput v-model:value="searchForm.module" placeholder="请输入模块名称" clearable />
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
    <NCard :bordered="false" title="操作日志">
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
          showQuickJumper: true,
          prefix: ({ itemCount, pageCount }) => `共 ${itemCount} 条 / ${pageCount} 页`,
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

