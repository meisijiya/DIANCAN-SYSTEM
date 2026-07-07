<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NImage, NInput, NInputNumber, NModal, NPopconfirm, NSelect, NSpace, NSwitch, NTag, NUpload, useMessage } from 'naive-ui';
import type { DataTableColumns, FormInst, SelectOption, UploadCustomRequestOptions } from 'naive-ui';
import { createDishCategory, fetchDishCategoryList, fetchDishSpecGroupList, updateDishCategory, deleteDishCategory, uploadDishImage } from '@/service/api';

defineOptions({ name: 'DishCategory' });

const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.DishCategory[]>([]);
const specGroupOptions = ref<SelectOption[]>([]);
const uploadLoading = ref(false);
const previewImageUrl = ref('');

const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.DishCategoryCreate & { id?: number }>({ name: '', sort: 0, status: 1, image: '', specGroupIds: [] });

const rules = {
  name: { required: true, message: '请输入分类名称', trigger: 'blur' }
};

function uniqueSpecGroupIds(ids: Api.Business.IdType[] = []) {
  const seen = new Set<string>();
  return ids.filter(item => {
    const key = String(item);
    if (!key || seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}

const columns: DataTableColumns<Api.Business.DishCategory> = [
  { title: '分类名称', key: 'name', width: 180 },
  {
    title: '分类图片',
    key: 'image',
    width: 100,
    render: row => {
      if (!row.image) return '-';
      return h(NImage, {
        src: row.image,
        width: 40,
        height: 40,
        objectFit: 'cover',
        style: 'border-radius: 10px; border: 1px solid #e5e7eb;'
      });
    }
  },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '默认规格',
    key: 'specGroupNames',
    width: 240,
    render: row => row.specGroupNames?.length ? row.specGroupNames.join(' / ') : '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row =>
      h(NSwitch, {
        value: row.status === 1,
        onUpdateValue: (value: boolean) => handleToggleStatus(row, value)
      })
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除该分类吗？'
          })
        ]
      })
  }
];

async function loadSpecGroups() {
  const { data: result, error } = await fetchDishSpecGroupList();
  if (!error && result) {
    specGroupOptions.value = result
      .filter(item => item.status === 1)
      .map(item => ({ label: item.name, value: item.id }));
  }
}

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchDishCategoryList();
    if (!error && result) {
      data.value = result;
    }
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { name: '', sort: 0, status: 1, image: '', specGroupIds: [] };
  previewImageUrl.value = '';
  showModal.value = true;
}

function handleEdit(row: Api.Business.DishCategory) {
  isEdit.value = true;
  formModel.value = {
    id: row.id,
    name: row.name,
    sort: row.sort,
    status: row.status,
    image: row.image || '',
    specGroupIds: [...(row.specGroupIds || [])]
  };
  previewImageUrl.value = row.image || '';
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  formModel.value.specGroupIds = uniqueSpecGroupIds(formModel.value.specGroupIds);
  if (isEdit.value && formModel.value.id) {
    const { error } = await updateDishCategory(formModel.value.id, formModel.value as Api.Business.DishCategoryUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }

  const { error } = await createDishCategory(formModel.value);
  if (!error) {
    message.success('创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: Api.Business.DishCategory, enabled: boolean) {
  const { error } = await updateDishCategory(row.id, {
    id: row.id,
    name: row.name,
    sort: row.sort,
    status: enabled ? 1 : 0,
    image: row.image || '',
    specGroupIds: row.specGroupIds || []
  });
  if (!error) {
    message.success(enabled ? '已启用' : '已停用');
    loadData();
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteDishCategory(id);
  if (!error) {
    message.success('删除成功');
    loadData();
  }
}

function handleImageInputChange(value: string) {
  const val = (value || '').trim();
  previewImageUrl.value = val;
}

async function handleUploadCategoryImage(options: UploadCustomRequestOptions) {
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
      previewImageUrl.value = result.url || result.objectName;
      message.success('分类图片上传成功');
      options.onFinish();
      return;
    }
    options.onError();
  } finally {
    uploadLoading.value = false;
  }
}

onMounted(() => {
  loadSpecGroups();
  loadData();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" title="菜品分类管理">
      <template #header-extra>
        <NSpace>
          <NTag type="info">分类只配置默认规格，具体菜品可再覆盖</NTag>
          <NButton type="primary" @click="handleAdd">新增分类</NButton>
        </NSpace>
      </template>
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>

    <NModal v-model:show="showModal" preset="dialog" :title="isEdit ? '编辑分类' : '新增分类'" positive-text="确定" negative-text="取消" @positive-click="handleSubmit">
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="96">
        <NFormItem label="分类名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="请输入分类名称" />
        </NFormItem>
        <NFormItem label="排序" path="sort">
          <NInputNumber v-model:value="formModel.sort" :min="0" placeholder="排序序号" />
        </NFormItem>
        <NFormItem label="分类图片" path="image">
          <NSpace vertical :size="8" style="width: 100%;">
            <NUpload :show-file-list="false" :custom-request="handleUploadCategoryImage">
              <NButton :loading="uploadLoading">上传图片</NButton>
            </NUpload>
            <NInput v-model:value="formModel.image" placeholder="可手动输入对象键或图片URL" @update:value="handleImageInputChange" />
            <NImage v-if="previewImageUrl" :src="previewImageUrl" width="96" height="96" object-fit="cover" style="border-radius: 12px; border: 1px solid #e5e7eb;" />
          </NSpace>
        </NFormItem>
        <NFormItem label="默认规格组" path="specGroupIds">
          <NSelect v-model:value="formModel.specGroupIds" multiple :options="specGroupOptions" placeholder="选择该分类默认带出的规格组" />
        </NFormItem>
        <NFormItem label="状态" path="status">
          <NSwitch v-model:value="formModel.status" :checked-value="1" :unchecked-value="0" />
        </NFormItem>
      </NForm>
    </NModal>
  </NSpace>
</template>
