<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NGrid, NGi, NInput, NPopconfirm, NSpace, NTag, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchUserList, updateUserStatus } from '@/service/api';

defineOptions({
  name: 'ManageMemberUser'
});

const message = useMessage();
const loading = ref(false);
const searchForm = ref<Api.System.UserQuery>({
  username: '',
  status: undefined,
  userType: undefined,
  memberOnly: true,
  pageNum: 1,
  pageSize: 10
});

const data = ref<Api.System.User[]>([]);
const total = ref(0);

const columns: DataTableColumns<Api.System.User> = [
  { title: '用户名', key: 'username', width: 150 },
  { title: '昵称', key: 'nickname', width: 130, render: row => row.nickname || '-' },
  { title: '手机号', key: 'phone', width: 140, render: row => row.phone || '-' },
  { title: 'OpenID', key: 'openid', minWidth: 220, render: row => row.openid || '-' },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error', bordered: false }, { default: () => (row.status === 1 ? '启用' : '禁用') });
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render(row) {
      return h(
        NPopconfirm,
        {
          onPositiveClick: () => handleToggleStatus(row)
        },
        {
          trigger: () =>
            h(NButton, { size: 'small', type: row.status === 1 ? 'warning' : 'success' }, { default: () => (row.status === 1 ? '禁用' : '启用') }),
          default: () => `确定${row.status === 1 ? '禁用' : '启用'}该用户吗？`
        }
      );
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchUserList(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

async function handleToggleStatus(row: Api.System.User) {
  const newStatus = row.status === 1 ? 0 : 1;
  const { error } = await updateUserStatus(row.id, newStatus);
  if (!error) {
    message.success('操作成功');
    loadData();
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = {
    username: '',
    status: undefined,
    userType: undefined,
    memberOnly: true,
    pageNum: 1,
    pageSize: 10
  };
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
  <NSpace vertical :size="12" class="member-account-view">
    <NCard :bordered="false" class="member-hero">
      <div class="member-hero__eyebrow">会员档案</div>
      <div class="member-hero__head">
        <div>
          <h2 class="member-hero__title">会员用户管理</h2>
          <p class="member-hero__desc">仅展示会员侧账号，后台员工账号请在“用户管理”中维护。</p>
        </div>
        <div class="member-hero__badge">
          <span>当前会员账号数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="member-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="8">
            <NFormItem label="用户名">
              <NInput v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NSpace justify="end" class="search-actions">
              <NButton type="primary" @click="handleSearch">搜索</NButton>
              <NButton @click="handleReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="会员账号列表" class="member-list-card">
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
.member-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.18), transparent 24%),
    radial-gradient(circle at left bottom, rgba(var(--admin-accent-rgb), 0.1), transparent 22%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.99), rgba(232, 241, 255, 0.96)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.member-hero :deep(.n-card__content) {
  padding: 14px 22px;
}

.member-hero__eyebrow {
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.78);
}

.member-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.member-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.member-hero__desc {
  max-width: 760px;
  margin: 8px 0 0;
  line-height: 1.7;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.member-hero__badge {
  min-width: 170px;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.member-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.member-hero__badge strong {
  display: block;
  margin-top: 6px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.member-filter-card,
.member-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.member-list-card :deep(.n-card-header) {
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.08);
}

.member-list-card :deep(.n-card-header__main) {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: color-mix(in srgb, var(--admin-accent-strong) 56%, #1b2d45);
}

.member-list-card :deep(.n-data-table) {
  border-radius: 20px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.08), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.98));
  box-shadow:
    0 18px 34px rgba(var(--admin-accent-rgb), 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.member-list-card :deep(.n-data-table .n-data-table-base-table-header) {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(236, 245, 255, 0.98));
}

.member-list-card :deep(.n-data-table .n-data-table-th) {
  padding-top: 15px;
  padding-bottom: 15px;
  color: color-mix(in srgb, var(--admin-accent-strong) 64%, #20314b);
  font-size: 13px;
  font-weight: 700;
  border-bottom-color: rgba(var(--admin-accent-rgb), 0.08) !important;
}

.member-list-card :deep(.n-data-table .n-data-table-td) {
  padding-top: 14px;
  padding-bottom: 14px;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #26364d);
  border-bottom-color: rgba(var(--admin-accent-rgb), 0.06) !important;
}

.member-list-card :deep(.n-data-table .n-data-table-tr:not(.n-data-table-tr--summary):nth-child(even) td) {
  background: rgba(243, 249, 255, 0.78);
}

.member-list-card :deep(.n-data-table .n-data-table-tbody .n-data-table-tr:hover td) {
  background: rgba(var(--admin-accent-rgb), 0.06) !important;
}

.search-actions {
  width: 100%;
}

@media (max-width: 960px) {
  .member-hero :deep(.n-card__content) {
    padding: 12px 18px;
  }

  .member-hero__head {
    align-items: flex-start;
    flex-direction: column;
  }

  .member-hero__badge {
    min-width: 0;
    width: 100%;
  }
}

html.dark .member-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .member-hero__title,
html.dark .member-list-card :deep(.n-card-header__main) {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .member-hero__desc {
  color: rgba(184, 198, 222, 0.78);
}

html.dark .member-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .member-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .member-hero__badge strong {
  color: #dbe5ff;
}

html.dark .member-filter-card,
html.dark .member-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .member-list-card :deep(.n-card-header) {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

html.dark .member-list-card :deep(.n-data-table) {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.1), transparent 18%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98));
  box-shadow:
    0 24px 40px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .member-list-card :deep(.n-data-table .n-data-table-base-table-header) {
  background: linear-gradient(180deg, rgba(16, 21, 33, 0.98), rgba(9, 13, 21, 0.98));
}

html.dark .member-list-card :deep(.n-data-table .n-data-table-th) {
  color: rgba(236, 242, 255, 0.92);
  border-bottom-color: rgba(255, 255, 255, 0.06) !important;
}

html.dark .member-list-card :deep(.n-data-table .n-data-table-td) {
  color: rgba(220, 228, 242, 0.9);
  border-bottom-color: rgba(255, 255, 255, 0.05) !important;
}

html.dark .member-list-card :deep(.n-data-table .n-data-table-tr:not(.n-data-table-tr--summary):nth-child(even) td) {
  background: rgba(255, 255, 255, 0.018);
}

html.dark .member-list-card :deep(.n-data-table .n-data-table-tbody .n-data-table-tr:hover td) {
  background: rgba(var(--admin-accent-rgb), 0.08) !important;
}
</style>
