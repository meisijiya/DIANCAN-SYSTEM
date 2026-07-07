<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import {
  NButton, NCard, NDataTable, NForm, NFormItem, NGrid, NGi, NImage, NInput, NInputNumber, NModal, NPopconfirm,
  NSelect, NSpace, NTag, NUpload, useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst, SelectOption, UploadCustomRequestOptions } from 'naive-ui';
import {
  createDish,
  fetchDishCategoryList,
  fetchDishList,
  fetchDishSpecGroupList,
  updateDish,
  updateDishSoldOut,
  updateDishStatus,
  uploadDishImage
} from '@/service/api';

defineOptions({ name: 'DishList' });

const message = useMessage();
function toIdKey(value: string | number | null | undefined) {
  if (value === null || value === undefined) return '';
  return String(value);
}

const loading = ref(false);
const data = ref<Api.Business.Dish[]>([]);
const total = ref(0);
const categoryOptions = ref<SelectOption[]>([]);
const categoryMap = ref<Record<string, Api.Business.DishCategory>>({});
const specGroupMap = ref<Record<string, Api.Business.DishSpecGroup>>({});

const searchForm = ref<Api.Business.DishQuery & { pageNum: number; pageSize: number }>({
  pageNum: 1,
  pageSize: 10,
  categoryId: undefined,
  name: '',
  status: undefined
});

const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.DishCreate & { id?: Api.Business.IdType }>({
  categoryId: 0,
  name: '',
  price: 0,
  spiceLevel: 0,
  stock: -1,
  specItems: []
});
const uploadLoading = ref(false);
const previewImageUrl = ref('');
const extraSpecGroupId = ref<Api.Business.IdType | null>(null);

const rules = {
  name: { required: true, message: '请输入菜品名称', trigger: 'blur' },
  price: { required: true, type: 'number' as const, message: '请输入价格', trigger: 'blur' }
};

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '上架', value: 1 },
  { label: '下架', value: 0 }
];

const availableExtraSpecGroupOptions = computed(() => {
  const selectedIds = new Set((formModel.value.specItems || []).map(item => toIdKey(item.specGroupId)));
  return Object.values(specGroupMap.value)
    .filter(item => item.status === 1 && !selectedIds.has(toIdKey(item.id)))
    .map(item => ({ label: item.name, value: toIdKey(item.id) }));
});

const columns: DataTableColumns<Api.Business.Dish> = [
  { title: '菜品名称', key: 'name', width: 150 },
  {
    title: '图片',
    key: 'image',
    width: 96,
    render(row) {
      if (!row.image) return '-';
      return h(NImage, {
        src: row.image,
        width: 44,
        height: 44,
        objectFit: 'cover',
        style: 'border-radius: 6px; border: 1px solid #e5e7eb;'
      });
    }
  },
  { title: '分类', key: 'categoryName', width: 100, render: row => row.categoryName || '未分类' },
  { title: '价格', key: 'price', width: 88, render: row => `¥${row.price}` },
  {
    title: '规格',
    key: 'specItems',
    width: 260,
    render: row => formatDishSpec(row.specItems)
  },
  { title: '库存', key: 'stock', width: 80, render: row => row.stock === -1 ? '不限' : String(row.stock) },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row => h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => row.status === 1 ? '上架' : '下架' })
  },
  {
    title: '售罄',
    key: 'soldOut',
    width: 80,
    render: row => row.soldOut === 1 ? h(NTag, { type: 'error' }, { default: () => '已售罄' }) : '-'
  },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作',
    key: 'actions',
    width: 320,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(NPopconfirm, { onPositiveClick: () => handleToggleSoldOut(row) }, {
            trigger: () => h(
              NButton,
              { size: 'small', type: row.soldOut === 1 ? 'success' : 'error', secondary: true },
              { default: () => row.soldOut === 1 ? '取消估清' : '估清' }
            ),
            default: () => row.soldOut === 1 ? '确定取消估清吗？' : '确定将该菜品估清吗？'
          }),
          h(NPopconfirm, { onPositiveClick: () => handleToggleStatus(row) }, {
            trigger: () => h(
              NButton,
              { size: 'small', type: row.status === 1 ? 'warning' : 'success' },
              { default: () => row.status === 1 ? '下架' : '上架' }
            ),
            default: () => `确定${row.status === 1 ? '下架' : '上架'}该菜品吗？`
          })
        ]
      })
  }
];

async function loadCategories() {
  const { data: result, error } = await fetchDishCategoryList();
  if (!error && result) {
    categoryMap.value = result.reduce<Record<string, Api.Business.DishCategory>>((acc, item) => {
      acc[toIdKey(item.id)] = item;
      return acc;
    }, {});
    categoryOptions.value = [
      { label: '未分类', value: 0 },
      ...result.map(item => ({ label: item.name, value: toIdKey(item.id) }))
    ];
  }
}

async function loadSpecGroups() {
  const { data: result, error } = await fetchDishSpecGroupList();
  if (!error && result) {
    specGroupMap.value = result.reduce<Record<string, Api.Business.DishSpecGroup>>((acc, item) => {
      acc[toIdKey(item.id)] = item;
      return acc;
    }, {});
  }
}

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchDishList(searchForm.value);
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
  searchForm.value = { pageNum: 1, pageSize: 10, categoryId: undefined, name: '', status: undefined };
  loadData();
}

function createSpecItemsFromCategory(categoryId: Api.Business.IdType) {
  const category = categoryMap.value[toIdKey(categoryId)];
  if (!category?.specGroupIds?.length) {
    return [];
  }
  return category.specGroupIds
    .map(specGroupId => buildDishSpecItem(specGroupId))
    .filter(Boolean) as Api.Business.DishSpecItem[];
}

function buildDishSpecItem(specGroupId: Api.Business.IdType) {
  const group = specGroupMap.value[toIdKey(specGroupId)];
  if (!group) {
    return null;
  }
  return {
    specGroupId: group.id,
    specGroupName: group.name,
    optionIds: group.options.map(item => toIdKey(item.id)).filter(Boolean),
    optionNames: group.options.map(item => item.name)
  };
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { categoryId: 0, name: '', price: 0, spiceLevel: 0, stock: -1, specItems: [] };
  previewImageUrl.value = '';
  extraSpecGroupId.value = null;
  showModal.value = true;
}

function handleEdit(row: Api.Business.Dish) {
  isEdit.value = true;
  formModel.value = {
    id: row.id,
    categoryId: row.categoryId ?? 0,
    name: row.name,
    price: row.price,
    image: row.image || undefined,
    thumbnail: row.thumbnail || undefined,
    spiceLevel: row.spiceLevel,
    ingredients: row.ingredients || undefined,
    description: row.description || undefined,
    stock: row.stock,
    preparationTime: row.preparationTime || undefined,
    specItems: (row.specItems || []).map(item => ({
      specGroupId: item.specGroupId,
      specGroupName: item.specGroupName,
      optionIds: [...item.optionIds],
      optionNames: [...item.optionNames]
    }))
  };
  previewImageUrl.value = row.image || '';
  extraSpecGroupId.value = null;
  showModal.value = true;
}

function handleCategoryChange(categoryId: Api.Business.IdType) {
  formModel.value.categoryId = categoryId || 0;
  formModel.value.specItems = createSpecItemsFromCategory(formModel.value.categoryId);
}

function addExtraSpecGroup() {
  if (!extraSpecGroupId.value) {
    return;
  }
  const nextItem = buildDishSpecItem(extraSpecGroupId.value);
  if (!nextItem) {
    return;
  }
  formModel.value.specItems = [...(formModel.value.specItems || []), nextItem];
  extraSpecGroupId.value = null;
}

function removeSpecItem(index: number) {
  formModel.value.specItems?.splice(index, 1);
}

function updateSpecItemOptions(index: number, optionIds: Api.Business.IdType[]) {
  const specItem = formModel.value.specItems?.[index];
  if (!specItem) {
    return;
  }
  specItem.optionIds = optionIds;
  specItem.optionNames = optionIds
    .map(optionId => specGroupMap.value[toIdKey(specItem.specGroupId)]?.options.find(item => toIdKey(item.id) === toIdKey(optionId))?.name || '')
    .filter(Boolean);
}

function getSpecOptionSelectOptions(specGroupId: Api.Business.IdType) {
  return (specGroupMap.value[toIdKey(specGroupId)]?.options || []).map(item => ({
    label: item.name,
    value: toIdKey(item.id)
  }));
}

async function handleSubmit() {
  await formRef.value?.validate();
  const payload: Api.Business.DishCreate & { id?: Api.Business.IdType } = {
    ...formModel.value,
    categoryId: formModel.value.categoryId || 0,
    spiceLevel: 0,
    specItems: (formModel.value.specItems || []).filter(item => item.optionIds?.length)
  };
  if (isEdit.value && formModel.value.id) {
    const { error } = await updateDish(formModel.value.id, payload as Api.Business.DishUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }

  const { error } = await createDish(payload);
  if (!error) {
    message.success('创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: Api.Business.Dish) {
  const newStatus = row.status === 1 ? 0 : 1;
  const { error } = await updateDishStatus(row.id, newStatus);
  if (!error) {
    message.success('操作成功');
    loadData();
  }
}

async function handleToggleSoldOut(row: Api.Business.Dish) {
  const targetSoldOut = row.soldOut === 1 ? 0 : 1;
  const { error } = await updateDishSoldOut(row.id, targetSoldOut as 0 | 1);
  if (!error) {
    message.success(targetSoldOut === 1 ? '已设为估清' : '已取消估清');
    loadData();
  }
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

function handleImageInputChange(value: string) {
  const val = (value || '').trim();
  previewImageUrl.value = /^https?:\/\//i.test(val) ? val : '';
}

async function handleUploadDishImage(options: UploadCustomRequestOptions) {
  const rawFile = options.file.file;
  if (!rawFile) {
    options.onError();
    return;
  }
  if (!rawFile.type.startsWith('image/')) {
    message.warning('仅支持上传图片文件');
    options.onError();
    return;
  }
  if (rawFile.size > 5 * 1024 * 1024) {
    message.warning('图片大小不能超过 5MB');
    options.onError();
    return;
  }

  uploadLoading.value = true;
  try {
    const { data: result, error } = await uploadDishImage(rawFile);
    if (!error && result) {
      formModel.value.image = result.objectName;
      formModel.value.thumbnail = result.objectName;
      previewImageUrl.value = result.url;
      message.success('图片上传成功');
      options.onFinish();
      return;
    }
    options.onError();
  } finally {
    uploadLoading.value = false;
  }
}

function formatDishSpec(specItems?: Api.Business.DishSpecItem[]) {
  if (!specItems?.length) {
    return '-';
  }
  return specItems.map(item => `${item.specGroupName}：${item.optionNames.join('/')}`).join('；');
}

onMounted(async () => {
  await Promise.all([loadCategories(), loadSpecGroups()]);
  loadData();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="菜品名称">
              <NInput v-model:value="searchForm.name" placeholder="请输入菜品名称" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="5">
            <NFormItem label="分类">
              <NSelect v-model:value="searchForm.categoryId" :options="categoryOptions" placeholder="全部分类" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="5">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.status" :options="statusOptions" placeholder="全部" clearable />
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

    <NCard :bordered="false" title="菜品列表">
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">新增菜品</NButton>
      </template>
      <NDataTable
        remote
        :columns="columns"
        :data="data"
        :loading="loading"
        :scroll-x="1520"
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

    <NModal v-model:show="showModal" preset="card" :title="isEdit ? '编辑菜品' : '新增菜品'" style="width: 720px;">
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="100">
        <NFormItem label="所属分类">
          <NSelect v-model:value="formModel.categoryId" :options="categoryOptions" placeholder="未分类也可直接保存" @update:value="handleCategoryChange" />
        </NFormItem>
        <NFormItem label="菜品名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="请输入菜品名称" />
        </NFormItem>
        <NFormItem label="价格" path="price">
          <NInputNumber v-model:value="formModel.price" :min="0" :precision="2" style="width: 100%" />
        </NFormItem>
        <NFormItem label="库存">
          <NInputNumber v-model:value="formModel.stock" :min="-1" placeholder="-1表示不限库存" style="width: 100%" />
        </NFormItem>
        <NFormItem label="制作时间(分)">
          <NInputNumber v-model:value="formModel.preparationTime" :min="1" placeholder="预设制作时限" style="width: 100%" />
        </NFormItem>
        <NFormItem label="图片URL">
          <NSpace vertical :size="8" style="width: 100%;">
            <NUpload :show-file-list="false" :custom-request="handleUploadDishImage">
              <NButton :loading="uploadLoading">上传图片</NButton>
            </NUpload>
            <NInput v-model:value="formModel.image" placeholder="可手动输入对象键或图片URL" @update:value="handleImageInputChange" />
            <NImage v-if="previewImageUrl" :src="previewImageUrl" width="140" object-fit="cover" style="border-radius: 8px; border: 1px solid #e5e7eb;" />
          </NSpace>
        </NFormItem>
        <NFormItem label="菜品规格">
          <NSpace vertical style="width: 100%;">
            <div class="spec-builder">
              <NSelect v-model:value="extraSpecGroupId" :options="availableExtraSpecGroupOptions" placeholder="给当前菜品额外加一个规格组" clearable />
              <NButton :disabled="!extraSpecGroupId" @click="addExtraSpecGroup">添加规格组</NButton>
            </div>
            <div v-if="formModel.specItems?.length" class="dish-spec-list">
              <div v-for="(item, index) in formModel.specItems" :key="`${item.specGroupId}-${index}`" class="dish-spec-row">
                <div class="dish-spec-row__head">
                  <strong>{{ item.specGroupName }}</strong>
                  <NButton quaternary type="error" @click="removeSpecItem(index)">移除</NButton>
                </div>
                <NSelect
                  :value="item.optionIds"
                  multiple
                  :options="getSpecOptionSelectOptions(item.specGroupId)"
                  placeholder="请选择该菜品支持的规格值"
                  @update:value="value => updateSpecItemOptions(index, value as Api.Business.IdType[])"
                />
              </div>
            </div>
            <NTag v-else type="default">当前未配置规格，菜品将按普通单品展示</NTag>
          </NSpace>
        </NFormItem>
        <NFormItem label="简介">
          <NInput v-model:value="formModel.description" type="textarea" placeholder="请输入菜品简介" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.search-actions {
  width: 100%;
}

.spec-builder {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
  width: 100%;
}

.dish-spec-list {
  display: grid;
  gap: 12px;
}

.dish-spec-row {
  padding: 14px;
  border: 1px solid #e5edf8;
  border-radius: 14px;
  background: #f8fbff;
}

.dish-spec-row__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
</style>
