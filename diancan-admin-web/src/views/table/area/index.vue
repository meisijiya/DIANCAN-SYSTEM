<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst } from 'naive-ui';
import { createTableArea, deleteTableArea, fetchTableAreaList, updateTableArea } from '@/service/api';

defineOptions({ name: 'TableAreaManage' });

const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.TableArea[]>([]);
const filterKeyword = ref('');
const filterStatus = ref<string>('all');
const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.TableAreaCreate & { id?: number }>({ name: '', sort: 0, status: 1, remark: '' });

const rules = {
  name: { required: true, message: '请输入区域名称', trigger: 'blur' }
};

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '启用', value: '1' },
  { label: '停用', value: '0' }
];

const formStatusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const filteredData = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase();
  return data.value.filter(item => {
    const matchKeyword = !keyword || item.name.toLowerCase().includes(keyword) || (item.remark || '').toLowerCase().includes(keyword);
    const matchStatus = filterStatus.value === 'all' || String(item.status) === filterStatus.value;
    return matchKeyword && matchStatus;
  });
});

const enabledCount = computed(() => data.value.filter(item => item.status === 1).length);

const columns: DataTableColumns<Api.Business.TableArea> = [
  { title: '区域名称', key: 'name', width: 160 },
  { title: '排序', key: 'sort', width: 90 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '启用' : '停用') });
    }
  },
  { title: '备注', key: 'remark', minWidth: 220, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => '确定删除该区域吗？'
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
    const { data: result, error } = await fetchTableAreaList();
    if (!error && result) {
      data.value = result;
    }
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { name: '', sort: 0, status: 1, remark: '' };
  showModal.value = true;
}

function handleEdit(row: Api.Business.TableArea) {
  isEdit.value = true;
  formModel.value = { id: row.id, name: row.name, sort: row.sort, status: row.status, remark: row.remark || '' };
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (isEdit.value && formModel.value.id) {
    const { error } = await updateTableArea(formModel.value as Api.Business.TableAreaUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }

  const { error } = await createTableArea(formModel.value);
  if (!error) {
    message.success('创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteTableArea(id);
  if (!error) {
    message.success('删除成功');
    loadData();
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="table-area-hero">
      <div class="table-area-hero__eyebrow">AREA GOVERNANCE</div>
      <div class="table-area-hero__head">
        <div>
          <h2 class="table-area-hero__title">把大厅、包间和主题分区统一维护到一张区域主数据清单里</h2>
          <p class="table-area-hero__desc">桌台录入、看板分组和二维码打包都直接复用这套区域资料，避免名称随手录入导致后续筛选混乱。</p>
        </div>
        <div class="table-area-hero__badge">
          <span>当前启用区域</span>
          <strong>{{ enabledCount }}</strong>
          <small>{{ data.length }} 个区域主数据节点</small>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="区域管理" class="table-area-card">
      <template #header-extra>
        <NSpace :size="8">
          <NInput v-model:value="filterKeyword" placeholder="搜索区域名称/备注" clearable style="width: 220px" />
          <NSelect v-model:value="filterStatus" :options="statusOptions" style="width: 120px" />
          <NButton type="primary" @click="handleAdd">新增区域</NButton>
        </NSpace>
      </template>
      <NDataTable :columns="columns" :data="filteredData" :loading="loading" />
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="dialog"
      :title="isEdit ? '编辑区域' : '新增区域'"
      positive-text="确定"
      negative-text="取消"
      @positive-click="handleSubmit"
    >
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="80">
        <NFormItem label="区域名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="请输入区域名称" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="formModel.sort" :min="0" style="width: 100%" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="formModel.status" :options="formStatusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="formModel.remark" type="textarea" placeholder="可填写区域说明" :rows="3" />
        </NFormItem>
      </NForm>
    </NModal>
  </NSpace>
</template>

<style scoped>
.table-area-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.table-area-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.table-area-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.table-area-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.table-area-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.table-area-hero__badge {
  min-width: 190px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.table-area-hero__badge span,
.table-area-hero__badge small {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.table-area-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  color: var(--admin-accent-strong);
}

.table-area-hero__badge small {
  margin-top: 8px;
}

.table-area-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

html.dark .table-area-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .table-area-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(10, 14, 22, 0.96), rgba(14, 19, 30, 0.96)) !important;
  box-shadow:
    0 22px 40px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>
