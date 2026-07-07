<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NInput, NInputNumber, NModal, NPopconfirm, NSpace, NSwitch, NTag, useMessage } from 'naive-ui';
import type { DataTableColumns, FormInst } from 'naive-ui';
import { createDishSpecGroup, deleteDishSpecGroup, fetchDishSpecGroupList, updateDishSpecGroup } from '@/service/api';

defineOptions({ name: 'DishSpec' });

const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.DishSpecGroup[]>([]);

const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.DishSpecGroupCreate & { id?: number }>({
  name: '',
  sort: 0,
  status: 1,
  options: [{ name: '', sort: 0 }]
});

const rules = {
  name: { required: true, message: '请输入规格组名称', trigger: 'blur' }
};

const columns: DataTableColumns<Api.Business.DishSpecGroup> = [
  { title: '规格组', key: 'name', width: 160 },
  {
    title: '规格选项',
    key: 'options',
    width: 320,
    render: row => row.options?.length ? row.options.map(item => item.name).join(' / ') : '-'
  },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h(NSwitch, {
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
            default: () => '确定删除该规格组吗？'
          })
        ]
      })
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchDishSpecGroupList();
    if (!error && result) {
      data.value = result;
    }
  } finally {
    loading.value = false;
  }
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { name: '', sort: 0, status: 1, options: [{ name: '', sort: 0 }] };
  showModal.value = true;
}

function handleEdit(row: Api.Business.DishSpecGroup) {
  isEdit.value = true;
  formModel.value = {
    id: row.id,
    name: row.name,
    sort: row.sort,
    status: row.status,
    options: row.options?.length ? row.options.map(item => ({ id: item.id, name: item.name, sort: item.sort || 0 })) : [{ name: '', sort: 0 }]
  };
  showModal.value = true;
}

function addOption() {
  formModel.value.options.push({ name: '', sort: 0 });
}

function removeOption(index: number) {
  if (formModel.value.options.length === 1) {
    return;
  }
  formModel.value.options.splice(index, 1);
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (formModel.value.options.some(item => !item.name.trim())) {
    message.warning('规格选项名称不能为空');
    return;
  }

  if (isEdit.value && formModel.value.id) {
    const { error } = await updateDishSpecGroup(formModel.value.id, formModel.value as Api.Business.DishSpecGroupUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }

  const { error } = await createDishSpecGroup(formModel.value);
  if (!error) {
    message.success('创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: Api.Business.DishSpecGroup, enabled: boolean) {
  const { error } = await updateDishSpecGroup(row.id, {
    id: row.id,
    name: row.name,
    sort: row.sort,
    status: enabled ? 1 : 0,
    options: row.options || []
  });
  if (!error) {
    message.success(enabled ? '已启用' : '已停用');
    loadData();
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteDishSpecGroup(id);
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
  <NSpace vertical :size="16">
    <NCard :bordered="false" title="菜品规格管理">
      <template #header-extra>
        <NSpace>
          <NTag type="info">先维护规格组，再给分类绑定默认规格，菜品可覆盖</NTag>
          <NButton type="primary" @click="handleAdd">新增规格组</NButton>
        </NSpace>
      </template>
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>

    <NModal v-model:show="showModal" preset="card" :title="isEdit ? '编辑规格组' : '新增规格组'" style="width: 720px;">
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="96">
        <NFormItem label="规格组名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="例如：甜度、温度、辣度" />
        </NFormItem>
        <NFormItem label="排序">
          <NInputNumber v-model:value="formModel.sort" :min="0" />
        </NFormItem>
        <NFormItem label="状态">
          <NSwitch v-model:value="formModel.status" :checked-value="1" :unchecked-value="0" />
        </NFormItem>

        <NFormItem label="规格选项">
          <NSpace vertical style="width: 100%;">
            <div v-for="(item, index) in formModel.options" :key="`${index}-${item.id || 'new'}`" class="spec-option-row">
              <NInput v-model:value="item.name" placeholder="选项名称，例如：半糖、少冰" />
              <NInputNumber v-model:value="item.sort" :min="0" placeholder="排序" style="width: 120px;" />
              <NButton quaternary type="error" @click="removeOption(index)">删除</NButton>
            </div>
            <NButton dashed block @click="addOption">新增选项</NButton>
          </NSpace>
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
.spec-option-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px 64px;
  gap: 12px;
  align-items: center;
}
</style>
