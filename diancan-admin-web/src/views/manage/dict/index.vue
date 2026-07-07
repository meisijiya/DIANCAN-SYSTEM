<script setup lang="ts">
import { h, ref, watch } from 'vue';
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
  NModal,
  NInputNumber,
  NSelect,
  NTag,
  NPopconfirm,
  NPagination,
  NDrawer,
  NDrawerContent,
  useMessage,
  type DataTableColumns
} from 'naive-ui';
import {
  fetchDictTypeList,
  createDictType,
  updateDictType,
  deleteDictType,
  fetchDictDataList,
  createDictData,
  updateDictData,
  deleteDictData
} from '@/service/api/system';

defineOptions({
  name: 'ManageDict'
});

const message = useMessage();

// ==================== 字典类型 ====================
const typeLoading = ref(false);
const typeData = ref<Api.System.DictType[]>([]);
const typePagination = ref({ page: 1, pageSize: 10, total: 0 });
const typeSearchForm = ref<Api.System.DictTypeQuery>({ name: '', code: '' });
const showTypeModal = ref(false);
const typeModalTitle = ref('新增字典类型');
const typeFormData = ref<Api.System.DictTypeCreate & { id?: number }>({
  name: '',
  code: '',
  status: 1,
  remark: ''
});

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
];

// 字典类型表格列
const typeColumns: DataTableColumns<Api.System.DictType> = [
  { title: '字典名称', key: 'name', width: 150 },
  { title: '字典编码', key: 'code', width: 150 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row =>
      h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => (row.status === 1 ? '启用' : '禁用') })
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render: row =>
      h(NSpace, {}, () => [
        h(NButton, { size: 'small', type: 'info', onClick: () => handleViewData(row) }, { default: () => '字典数据' }),
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleEditType(row) }, { default: () => '编辑' }),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDeleteType(row.id) },
          {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除该字典类型吗？'
          }
        )
      ])
  }
];

// 获取字典类型列表
async function fetchTypeData() {
  typeLoading.value = true;
  try {
    const res = await fetchDictTypeList({
      ...typeSearchForm.value,
      pageNum: typePagination.value.page,
      pageSize: typePagination.value.pageSize
    });
    if (res.data) {
      typeData.value = res.data.list;
      typePagination.value.total = res.data.total;
    }
  } finally {
    typeLoading.value = false;
  }
}

// 搜索
function handleTypeSearch() {
  typePagination.value.page = 1;
  fetchTypeData();
}

// 重置搜索
function handleTypeReset() {
  typeSearchForm.value = { name: '', code: '' };
  handleTypeSearch();
}

// 新增字典类型
function handleAddType() {
  typeFormData.value = { name: '', code: '', status: 1, remark: '' };
  typeModalTitle.value = '新增字典类型';
  showTypeModal.value = true;
}

// 编辑字典类型
function handleEditType(row: Api.System.DictType) {
  typeFormData.value = {
    id: row.id,
    name: row.name,
    code: row.code,
    status: row.status,
    remark: row.remark || ''
  };
  typeModalTitle.value = '编辑字典类型';
  showTypeModal.value = true;
}

// 删除字典类型
async function handleDeleteType(id: number) {
  try {
    await deleteDictType(id);
    message.success('删除成功');
    fetchTypeData();
  } catch {
    message.error('删除失败');
  }
}

// 提交字典类型表单
async function handleTypeSubmit() {
  try {
    if (typeFormData.value.id) {
      await updateDictType(typeFormData.value as Api.System.DictTypeUpdate);
      message.success('更新成功');
    } else {
      await createDictType(typeFormData.value);
      message.success('创建成功');
    }
    showTypeModal.value = false;
    fetchTypeData();
  } catch {
    message.error('操作失败');
  }
}

// ==================== 字典数据 ====================
const showDataDrawer = ref(false);
const currentType = ref<Api.System.DictType | null>(null);
const dataLoading = ref(false);
const dictDataList = ref<Api.System.DictData[]>([]);
const showDataModal = ref(false);
const dataModalTitle = ref('新增字典数据');
const dataFormData = ref<Api.System.DictDataCreate & { id?: number }>({
  typeId: 0,
  label: '',
  value: '',
  orderNum: 0,
  status: 1,
  remark: ''
});

// 字典数据表格列
const dataColumns: DataTableColumns<Api.System.DictData> = [
  { title: '数据标签', key: 'label', width: 150 },
  { title: '数据值', key: 'value', width: 150 },
  { title: '排序', key: 'orderNum', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row =>
      h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => (row.status === 1 ? '启用' : '禁用') })
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: row =>
      h(NSpace, {}, () => [
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleEditData(row) }, { default: () => '编辑' }),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDeleteData(row.id) },
          {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除该字典数据吗？'
          }
        )
      ])
  }
];

// 查看字典数据
function handleViewData(row: Api.System.DictType) {
  currentType.value = row;
  showDataDrawer.value = true;
  fetchDictData();
}

// 获取字典数据列表
async function fetchDictData() {
  if (!currentType.value) return;
  dataLoading.value = true;
  try {
    const res = await fetchDictDataList(currentType.value.id);
    if (res.data) {
      dictDataList.value = res.data;
    }
  } finally {
    dataLoading.value = false;
  }
}

// 新增字典数据
function handleAddData() {
  if (!currentType.value) return;
  dataFormData.value = {
    typeId: currentType.value.id,
    label: '',
    value: '',
    orderNum: 0,
    status: 1,
    remark: ''
  };
  dataModalTitle.value = '新增字典数据';
  showDataModal.value = true;
}

// 编辑字典数据
function handleEditData(row: Api.System.DictData) {
  dataFormData.value = {
    id: row.id,
    typeId: row.typeId,
    label: row.label,
    value: row.value,
    orderNum: row.orderNum,
    status: row.status,
    remark: row.remark || ''
  };
  dataModalTitle.value = '编辑字典数据';
  showDataModal.value = true;
}

// 删除字典数据
async function handleDeleteData(id: number) {
  try {
    await deleteDictData(id);
    message.success('删除成功');
    fetchDictData();
  } catch {
    message.error('删除失败');
  }
}

// 提交字典数据表单
async function handleDataSubmit() {
  try {
    if (dataFormData.value.id) {
      await updateDictData(dataFormData.value as Api.System.DictDataUpdate);
      message.success('更新成功');
    } else {
      await createDictData(dataFormData.value);
      message.success('创建成功');
    }
    showDataModal.value = false;
    fetchDictData();
  } catch {
    message.error('操作失败');
  }
}

// 初始化
fetchTypeData();
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="dict-hero">
      <div class="dict-hero__eyebrow">DICTIONARY CENTER</div>
      <div class="dict-hero__head">
        <div>
          <h2 class="dict-hero__title">把字典类型和字典数据做成一套统一维护的枚举中心</h2>
          <p class="dict-hero__desc">适合管理状态枚举、业务选项和展示标签，避免页面、接口和数据库之间定义不一致。</p>
        </div>
        <div class="dict-hero__badge">
          <span>当前字典数</span>
          <strong>{{ typePagination.total }}</strong>
        </div>
      </div>
    </NCard>

    <!-- 搜索表单 -->
    <NCard :bordered="false" class="dict-filter-card">
      <NForm :model="typeSearchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="字典名称">
              <NInput v-model:value="typeSearchForm.name" placeholder="请输入字典名称" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="字典编码">
              <NInput v-model:value="typeSearchForm.code" placeholder="请输入字典编码" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NSpace>
              <NButton type="primary" @click="handleTypeSearch">搜索</NButton>
              <NButton @click="handleTypeReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <!-- 字典类型列表 -->
    <NCard :bordered="false" title="字典类型" class="dict-list-card">
      <template #header-extra>
        <NButton type="primary" @click="handleAddType">新增字典</NButton>
      </template>
      <NDataTable :columns="typeColumns" :data="typeData" :loading="typeLoading" :row-key="(row: Api.System.DictType) => row.id" />
      <div style="display: flex; justify-content: flex-end; margin-top: 16px">
        <NPagination
          v-model:page="typePagination.page"
          v-model:page-size="typePagination.pageSize"
          :item-count="typePagination.total"
          :prefix="({ itemCount }) => `共 ${itemCount} 条`"
          :page-sizes="[10, 20, 50, 100, 200]"
          show-size-picker
          show-quick-jumper
          @update:page="fetchTypeData"
          @update:page-size="handleTypeSearch"
        />
      </div>
    </NCard>

    <!-- 字典类型弹窗 -->
    <NModal v-model:show="showTypeModal" :title="typeModalTitle" preset="card" style="width: 500px">
      <NForm :model="typeFormData" label-placement="left" label-width="80">
        <NFormItem label="字典名称" required>
          <NInput v-model:value="typeFormData.name" placeholder="请输入字典名称" />
        </NFormItem>
        <NFormItem label="字典编码" required>
          <NInput v-model:value="typeFormData.code" placeholder="请输入字典编码" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="typeFormData.status" :options="statusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="typeFormData.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showTypeModal = false">取消</NButton>
          <NButton type="primary" @click="handleTypeSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- 字典数据抽屉 -->
    <NDrawer v-model:show="showDataDrawer" :width="700">
      <NDrawerContent :title="`字典数据 - ${currentType?.name || ''}`" closable>
        <template #header-extra>
          <NButton type="primary" size="small" @click="handleAddData">新增数据</NButton>
        </template>
        <NDataTable :columns="dataColumns" :data="dictDataList" :loading="dataLoading" :row-key="(row: Api.System.DictData) => row.id" />
      </NDrawerContent>
    </NDrawer>

    <!-- 字典数据弹窗 -->
    <NModal v-model:show="showDataModal" :title="dataModalTitle" preset="card" style="width: 500px">
      <NForm :model="dataFormData" label-placement="left" label-width="80">
        <NFormItem label="数据标签" required>
          <NInput v-model:value="dataFormData.label" placeholder="请输入数据标签" />
        </NFormItem>
        <NFormItem label="数据值" required>
          <NInput v-model:value="dataFormData.value" placeholder="请输入数据值" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="dataFormData.orderNum" :min="0" style="width: 100%" />
        </NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="dataFormData.status" :options="statusOptions" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="dataFormData.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showDataModal = false">取消</NButton>
          <NButton type="primary" @click="handleDataSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.dict-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.dict-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.dict-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.dict-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.dict-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.dict-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.dict-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.dict-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.dict-filter-card,
.dict-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.dict-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

html.dark .dict-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .dict-filter-card,
html.dark .dict-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>
