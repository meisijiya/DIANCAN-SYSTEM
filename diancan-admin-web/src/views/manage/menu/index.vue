<script setup lang="ts">
import { h, ref, computed } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NSpace,
  NModal,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NSelect,
  NTreeSelect,
  NIcon,
  NTag,
  NPopconfirm,
  useMessage,
  type DataTableColumns
} from 'naive-ui';
import { fetchMenuTree, createMenu, updateMenu, deleteMenu } from '@/service/api/system';

defineOptions({
  name: 'ManageMenu'
});

const message = useMessage();
const loading = ref(false);
const showModal = ref(false);
const modalTitle = ref('新增菜单');
const menuData = ref<Api.System.MenuTree[]>([]);

// 菜单类型选项
const menuTypeOptions = [
  { label: '目录', value: 0 },
  { label: '菜单', value: 1 },
  { label: '按钮', value: 2 }
];

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
];

// 表单数据
const formData = ref<Api.System.MenuCreate & { id?: number }>({
  parentId: 0,
  name: '',
  path: '',
  component: '',
  permission: '',
  type: 1,
  icon: '',
  orderNum: 0,
  status: 1
});

// 树形选择数据
const treeSelectOptions = computed(() => {
  const buildOptions = (menus: Api.System.MenuTree[]): any[] => {
    return menus
      .filter(m => m.type !== 2) // 按钮不能作为父级
      .map(menu => ({
        key: menu.id,
        label: menu.name,
        children: menu.children ? buildOptions(menu.children) : undefined
      }));
  };
  return [{ key: 0, label: '根目录' }, ...buildOptions(menuData.value)];
});

// 表格列定义
const columns: DataTableColumns<Api.System.MenuTree> = [
  { title: '菜单名称', key: 'name', width: 180 },
  {
    title: '图标',
    key: 'icon',
    width: 80,
    render: row => (row.icon ? h('span', {}, row.icon) : '-')
  },
  {
    title: '类型',
    key: 'type',
    width: 80,
    render: row => {
      const types: Record<number, { text: string; type: 'info' | 'success' | 'warning' }> = {
        0: { text: '目录', type: 'info' },
        1: { text: '菜单', type: 'success' },
        2: { text: '按钮', type: 'warning' }
      };
      const t = types[row.type] || { text: '未知', type: 'info' };
      return h(NTag, { type: t.type, size: 'small' }, { default: () => t.text });
    }
  },
  { title: '路由地址', key: 'path', width: 150, ellipsis: { tooltip: true } },
  { title: '组件路径', key: 'component', width: 180, ellipsis: { tooltip: true } },
  { title: '权限标识', key: 'permission', width: 150, ellipsis: { tooltip: true } },
  { title: '排序', key: 'orderNum', width: 70 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row =>
      h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => (row.status === 1 ? '启用' : '禁用') })
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    render: row =>
      h(NSpace, {}, () => [
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
        row.type !== 2 &&
          h(NButton, { size: 'small', type: 'info', onClick: () => handleAddChild(row) }, { default: () => '新增子菜单' }),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDelete(row.id) },
          {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除该菜单吗？'
          }
        )
      ])
  }
];

// 获取菜单数据
async function fetchData() {
  loading.value = true;
  try {
    const res = await fetchMenuTree();
    if (res.data) {
      menuData.value = res.data;
    }
  } finally {
    loading.value = false;
  }
}

// 重置表单
function resetForm() {
  formData.value = {
    parentId: 0,
    name: '',
    path: '',
    component: '',
    permission: '',
    type: 1,
    icon: '',
    orderNum: 0,
    status: 1
  };
}

// 新增菜单
function handleAdd() {
  resetForm();
  modalTitle.value = '新增菜单';
  showModal.value = true;
}

// 新增子菜单
function handleAddChild(row: Api.System.MenuTree) {
  resetForm();
  formData.value.parentId = row.id;
  formData.value.type = row.type === 0 ? 1 : 2; // 目录下默认菜单，菜单下默认按钮
  modalTitle.value = '新增子菜单';
  showModal.value = true;
}

// 编辑菜单
function handleEdit(row: Api.System.MenuTree) {
  formData.value = {
    id: row.id,
    parentId: row.parentId,
    name: row.name,
    path: row.path || '',
    component: row.component || '',
    permission: row.permission || '',
    type: row.type,
    icon: row.icon || '',
    orderNum: row.orderNum,
    status: row.status
  };
  modalTitle.value = '编辑菜单';
  showModal.value = true;
}

// 删除菜单
async function handleDelete(id: number) {
  try {
    await deleteMenu(id);
    message.success('删除成功');
    fetchData();
  } catch {
    message.error('删除失败');
  }
}

// 提交表单
async function handleSubmit() {
  try {
    if (formData.value.id) {
      await updateMenu(formData.value as Api.System.MenuUpdate);
      message.success('更新成功');
    } else {
      await createMenu(formData.value);
      message.success('创建成功');
    }
    showModal.value = false;
    fetchData();
  } catch {
    message.error('操作失败');
  }
}

// 初始化
fetchData();
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="manage-hero">
      <div class="manage-hero__eyebrow">MENU GOVERNANCE</div>
      <div class="manage-hero__head">
        <div>
          <h2 class="manage-hero__title">把目录、菜单和按钮权限统一维护在一张结构化菜单树里</h2>
          <p class="manage-hero__desc">适合在系统扩展、模块上新和权限梳理时快速查看层级关系，减少配置散落的问题。</p>
        </div>
        <div class="manage-hero__badge">
          <span>当前根节点</span>
          <strong>{{ menuData.length }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="菜单管理" class="manage-list-card">
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">新增菜单</NButton>
      </template>
      <NDataTable
        :columns="columns"
        :data="menuData"
        :loading="loading"
        :row-key="(row: Api.System.MenuTree) => row.id"
        default-expand-all
      />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 600px">
      <NForm :model="formData" label-placement="left" label-width="100">
        <NFormItem label="上级菜单">
          <NTreeSelect
            v-model:value="formData.parentId"
            :options="treeSelectOptions"
            key-field="key"
            label-field="label"
            children-field="children"
            placeholder="请选择上级菜单"
            clearable
            default-expand-all
          />
        </NFormItem>
        <NFormItem label="菜单类型" required>
          <NSelect v-model:value="formData.type" :options="menuTypeOptions" />
        </NFormItem>
        <NFormItem label="菜单名称" required>
          <NInput v-model:value="formData.name" placeholder="请输入菜单名称" />
        </NFormItem>
        <NFormItem v-if="formData.type !== 2" label="路由地址">
          <NInput v-model:value="formData.path" placeholder="请输入路由地址" />
        </NFormItem>
        <NFormItem v-if="formData.type === 1" label="组件路径">
          <NInput v-model:value="formData.component" placeholder="请输入组件路径，如 view.manage_user" />
        </NFormItem>
        <NFormItem v-if="formData.type !== 0" label="权限标识">
          <NInput v-model:value="formData.permission" placeholder="请输入权限标识，如 system:user:list" />
        </NFormItem>
        <NFormItem v-if="formData.type !== 2" label="图标">
          <NInput v-model:value="formData.icon" placeholder="请输入图标名称" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="formData.orderNum" :min="0" style="width: 100%" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="formData.status" :options="statusOptions" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.manage-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.manage-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.manage-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.manage-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.manage-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.manage-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.manage-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.manage-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.manage-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.manage-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

html.dark .manage-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .manage-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(10, 14, 22, 0.96), rgba(14, 19, 30, 0.96)) !important;
  box-shadow:
    0 22px 40px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>
