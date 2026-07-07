<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NSpace,
  NInput,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NTag,
  NPopconfirm,
  NModal,
  NTree,
  NTransfer,
  useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import {
  fetchRoleList,
  createRole,
  updateRole,
  deleteRole,
  fetchPermissionTree,
  fetchRoleMenuIds,
  assignRoleMenus,
  fetchUserList,
  fetchRoleUserIds,
  assignRoleUsers
} from '@/service/api';

defineOptions({
  name: 'ManageRole'
});

const message = useMessage();
const loading = ref(false);
const searchForm = ref<Api.System.RoleQuery>({
  name: '',
  pageNum: 1,
  pageSize: 10
});

const data = ref<Api.System.Role[]>([]);
const total = ref(0);

// 编辑弹窗
const showModal = ref(false);
const modalTitle = ref('新增角色');
const formData = ref<Api.System.RoleCreate & { id?: number }>({
  name: '',
  code: '',
  status: 1,
  remark: ''
});

// 分配菜单弹窗
const showMenuModal = ref(false);
const currentRoleId = ref<number>(0);
const menuTree = ref<Api.System.MenuTree[]>([]);
const checkedMenuKeys = ref<number[]>([]);

// 分配用户弹窗
const showUserModal = ref(false);
const allUsers = ref<{ label: string; value: number }[]>([]);
const selectedUserIds = ref<number[]>([]);

const columns: DataTableColumns<Api.System.Role> = [
  { title: '角色名称', key: 'name', width: 150 },
  { title: '角色编码', key: 'code', width: 150 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error' }, { default: () => (row.status === 1 ? '启用' : '禁用') });
    }
  },
  { title: '备注', key: 'remark', width: 200 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(NButton, { size: 'small', type: 'info', onClick: () => handleAssignMenu(row) }, { default: () => '分配菜单' }),
          h(NButton, { size: 'small', type: 'warning', onClick: () => handleAssignUser(row) }, { default: () => '分配用户' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => '确定删除该角色吗？'
            }
          )
        ]
      });
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchRoleList(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  modalTitle.value = '新增角色';
  formData.value = { name: '', code: '', status: 1, remark: '' };
  showModal.value = true;
}

function handleEdit(row: Api.System.Role) {
  modalTitle.value = '编辑角色';
  formData.value = { ...row };
  showModal.value = true;
}

async function handleSubmit() {
  if (formData.value.id) {
    const { error } = await updateRole(formData.value as Api.System.RoleUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
  } else {
    const { error } = await createRole(formData.value);
    if (!error) {
      message.success('创建成功');
      showModal.value = false;
      loadData();
    }
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteRole(id);
  if (!error) {
    message.success('删除成功');
    loadData();
  }
}

async function handleAssignMenu(row: Api.System.Role) {
  currentRoleId.value = row.id;
  // 加载菜单树
  const { data: tree, error: treeError } = await fetchPermissionTree();
  if (!treeError && tree) {
    menuTree.value = tree;
  }
  // 加载已分配的菜单
  const { data: menuIds, error: menuError } = await fetchRoleMenuIds(row.id);
  if (!menuError && menuIds) {
    checkedMenuKeys.value = menuIds;
  }
  showMenuModal.value = true;
}

async function handleSaveMenus() {
  const { error } = await assignRoleMenus(currentRoleId.value, checkedMenuKeys.value);
  if (!error) {
    message.success('分配成功');
    showMenuModal.value = false;
  }
}

async function handleAssignUser(row: Api.System.Role) {
  currentRoleId.value = row.id;
  // 加载所有用户
  const { data: userResult, error: userError } = await fetchUserList({ pageNum: 1, pageSize: 9999, userType: 'BACKEND' });
  if (!userError && userResult) {
    allUsers.value = userResult.list.map(u => ({
      label: `${u.nickname || u.username}（${u.username}）`,
      value: u.id
    }));
  }
  // 加载已分配的用户
  const { data: userIds, error: idsError } = await fetchRoleUserIds(row.id);
  if (!idsError && userIds) {
    selectedUserIds.value = userIds;
  }
  showUserModal.value = true;
}

async function handleSaveUsers() {
  const { error } = await assignRoleUsers(currentRoleId.value, selectedUserIds.value);
  if (!error) {
    message.success('分配成功');
    showUserModal.value = false;
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = { name: '', pageNum: 1, pageSize: 10 };
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
    <NCard :bordered="false" class="role-hero">
      <div class="role-hero__eyebrow">ROLE CONTROL</div>
      <div class="role-hero__head">
        <div>
          <h2 class="role-hero__title">把角色、菜单授权和用户绑定统一在一张权限工作台里</h2>
          <p class="role-hero__desc">适合在组织调整、岗位分工和新模块上线时快速梳理角色权限，不用在多个系统页之间来回切换。</p>
        </div>
        <div class="role-hero__badge">
          <span>角色总数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="role-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="角色名称">
              <NInput v-model:value="searchForm.name" placeholder="请输入角色名称" clearable />
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
    <NCard :bordered="false" title="角色列表" class="role-list-card">
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">新增角色</NButton>
      </template>
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

    <!-- 编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 500px">
      <NForm :model="formData" label-placement="left" label-width="80">
        <NFormItem label="角色名称" required>
          <NInput v-model:value="formData.name" placeholder="请输入角色名称" />
        </NFormItem>
        <NFormItem label="角色编码" required>
          <NInput v-model:value="formData.code" placeholder="请输入角色编码" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 分配菜单弹窗 -->
    <NModal v-model:show="showMenuModal" title="分配菜单" preset="card" style="width: 500px">
      <NTree
        :data="menuTree"
        :checked-keys="checkedMenuKeys"
        checkable
        cascade
        key-field="id"
        label-field="name"
        children-field="children"
        @update:checked-keys="(keys: number[]) => (checkedMenuKeys = keys)"
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showMenuModal = false">取消</NButton>
          <NButton type="primary" @click="handleSaveMenus">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 分配用户弹窗 -->
    <NModal v-model:show="showUserModal" title="分配用户" preset="card" style="width: 600px">
      <NTransfer
        v-model:value="selectedUserIds"
        :options="allUsers"
        source-filterable
        target-filterable
        :source-filter-placeholder="'搜索用户'"
        :target-filter-placeholder="'搜索已选用户'"
        style="height: 400px"
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showUserModal = false">取消</NButton>
          <NButton type="primary" @click="handleSaveUsers">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.role-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.role-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.role-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.role-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.role-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.role-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.role-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.role-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.role-filter-card,
.role-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.role-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

.search-actions {
  width: 100%;
}

html.dark .role-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .role-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .role-hero__badge span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .role-hero__badge strong {
  color: #dbe5ff;
}

html.dark .role-filter-card,
html.dark .role-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>

