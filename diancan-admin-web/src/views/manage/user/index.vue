<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NSpace, NInput, NForm, NFormItem, NGrid, NGi, NTag, NPopconfirm, NModal, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchUserList, updateUserStatus, resetUserPassword } from '@/service/api';

defineOptions({
  name: 'ManageUser'
});

const message = useMessage();
const loading = ref(false);
const searchForm = ref<Api.System.UserQuery>({
  username: '',
  status: undefined,
  userType: 'BACKEND',
  pageNum: 1,
  pageSize: 10
});

const data = ref<Api.System.User[]>([]);
const total = ref(0);
const showResetModal = ref(false);
const resetPassword = ref('');
const resetUserId = ref<number | null>(null);
const resetUsername = ref('');

const columns: DataTableColumns<Api.System.User> = [
  { title: '用户名', key: 'username', width: 120 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '邮箱', key: 'email', width: 180 },
  { title: '手机号', key: 'phone', width: 130 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error' }, { default: () => (row.status === 1 ? '启用' : '禁用') });
    }
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 250,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleOpenResetPassword(row) }, { default: () => '重置密码' }),
          h(
            NPopconfirm,
            {
              onPositiveClick: () => handleToggleStatus(row)
            },
            {
              trigger: () =>
                h(NButton, { size: 'small', type: row.status === 1 ? 'warning' : 'success' }, { default: () => (row.status === 1 ? '禁用' : '启用') }),
              default: () => `确定${row.status === 1 ? '禁用' : '启用'}该用户吗？`
            }
          )
        ]
      });
    }
  }
];

function handleOpenResetPassword(row: Api.System.User) {
  resetUserId.value = row.id;
  resetUsername.value = row.username;
  resetPassword.value = '';
  showResetModal.value = true;
}

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

async function handleResetPasswordSubmit() {
  if (!resetUserId.value) return;
  if (!resetPassword.value || resetPassword.value.length < 6 || resetPassword.value.length > 20) {
    message.warning('密码长度需在6-20位之间');
    return;
  }
  const { error } = await resetUserPassword(resetUserId.value, resetPassword.value);
  if (!error) {
    message.success('密码重置成功');
    showResetModal.value = false;
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
    userType: 'BACKEND',
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
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="user-hero">
      <div class="user-hero__eyebrow">账号目录</div>
      <div class="user-hero__head">
        <div>
          <h2 class="user-hero__title">把用户状态、检索和密码重置统一在一张账号管理台里</h2>
          <p class="user-hero__desc">适合日常运维处理账号启停、快速查人和重置密码，降低后台基础管理的操作割裂感。</p>
        </div>
        <div class="user-hero__badge">
          <span>当前用户数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="user-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="用户名">
              <NInput v-model:value="searchForm.username" placeholder="请输入用户名" clearable />
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
    <NCard :bordered="false" title="用户列表" class="user-list-card">
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

    <NModal v-model:show="showResetModal" title="重置密码" preset="card" style="width: 420px">
      <NForm label-placement="left" label-width="90">
        <NFormItem label="用户名">
          <NInput :value="resetUsername" disabled />
        </NFormItem>
        <NFormItem label="新密码" required>
          <NInput v-model:value="resetPassword" type="password" show-password-on="click" placeholder="请输入6-20位新密码" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showResetModal = false">取消</NButton>
          <NButton type="primary" @click="handleResetPasswordSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.user-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.user-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.user-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.user-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.user-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.user-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.user-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.user-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.user-filter-card,
.user-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.user-list-card :deep(.n-card-header) {
  padding-bottom: 14px;
  margin-bottom: 14px;
  border-bottom: 1px solid rgba(var(--admin-accent-rgb), 0.08);
}

.user-list-card :deep(.n-card-header__main) {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: color-mix(in srgb, var(--admin-accent-strong) 56%, #1b2d45);
}

.user-list-card :deep(.n-data-table) {
  border-radius: 20px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.08), transparent 20%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 249, 255, 0.98));
  box-shadow:
    0 18px 34px rgba(var(--admin-accent-rgb), 0.07),
    inset 0 1px 0 rgba(255, 255, 255, 0.78);
}

.user-list-card :deep(.n-data-table .n-data-table-base-table-header) {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(236, 245, 255, 0.98));
}

.user-list-card :deep(.n-data-table .n-data-table-th) {
  padding-top: 15px;
  padding-bottom: 15px;
  color: color-mix(in srgb, var(--admin-accent-strong) 64%, #20314b);
  font-size: 13px;
  font-weight: 700;
  border-bottom-color: rgba(var(--admin-accent-rgb), 0.08) !important;
}

.user-list-card :deep(.n-data-table .n-data-table-td) {
  padding-top: 14px;
  padding-bottom: 14px;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #26364d);
  border-bottom-color: rgba(var(--admin-accent-rgb), 0.06) !important;
}

.user-list-card :deep(.n-data-table .n-data-table-tr:not(.n-data-table-tr--summary):nth-child(even) td) {
  background: rgba(243, 249, 255, 0.78);
}

.user-list-card :deep(.n-data-table .n-data-table-tbody .n-data-table-tr:hover td) {
  background: rgba(var(--admin-accent-rgb), 0.06) !important;
}

.user-list-card :deep(.n-data-table .n-button) {
  box-shadow: none;
}

.user-list-card :deep(.n-data-table .n-pagination) {
  padding-top: 6px;
}

.search-actions {
  width: 100%;
}

html.dark .user-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .user-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .user-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .user-hero__badge strong {
  color: #dbe5ff;
}

html.dark .user-filter-card,
html.dark .user-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .user-list-card :deep(.n-card-header) {
  border-bottom-color: rgba(255, 255, 255, 0.06);
}

html.dark .user-list-card :deep(.n-card-header__main) {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .user-list-card :deep(.n-data-table) {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.1), transparent 18%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98));
  box-shadow:
    0 24px 40px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .user-list-card :deep(.n-data-table .n-data-table-base-table-header) {
  background: linear-gradient(180deg, rgba(16, 21, 33, 0.98), rgba(9, 13, 21, 0.98));
}

html.dark .user-list-card :deep(.n-data-table .n-data-table-th) {
  color: rgba(236, 242, 255, 0.92);
  border-bottom-color: rgba(255, 255, 255, 0.06) !important;
}

html.dark .user-list-card :deep(.n-data-table .n-data-table-td) {
  color: rgba(220, 228, 242, 0.9);
  border-bottom-color: rgba(255, 255, 255, 0.05) !important;
}

html.dark .user-list-card :deep(.n-data-table .n-data-table-tr:not(.n-data-table-tr--summary):nth-child(even) td) {
  background: rgba(255, 255, 255, 0.018);
}

html.dark .user-list-card :deep(.n-data-table .n-data-table-tbody .n-data-table-tr:hover td) {
  background: rgba(var(--admin-accent-rgb), 0.08) !important;
}
</style>

