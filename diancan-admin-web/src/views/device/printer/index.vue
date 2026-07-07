<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import {
  NButton, NCard, NSpace, NDataTable, NModal, NForm, NFormItem,
  NInput, NSelect, NTag, NPopconfirm, useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst, SelectOption } from 'naive-ui';
import {
  fetchPrinterList,
  createPrinter,
  updatePrinter,
  deletePrinter,
  fetchDishCategoryList,
  updateCategoryMapping
} from '@/service/api';

defineOptions({ name: 'DevicePrinter' });

const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.Printer[]>([]);
const categoryOptions = ref<SelectOption[]>([]);
let lastVoiceAt = 0;
let lastOfflineCount = 0;
const VOICE_PREF_KEY = 'admin_voice_enabled';
const voiceEnabled = ref(true);

function playVoice(text: string, cooldownMs = 1200) {
  if (!voiceEnabled.value) return;
  if (!('speechSynthesis' in window)) return;
  const now = Date.now();
  if (now - lastVoiceAt < cooldownMs) return;
  lastVoiceAt = now;
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = 'zh-CN';
  utter.rate = 1;
  window.speechSynthesis.speak(utter);
}

function initVoicePreference() {
  try {
    const saved = window.localStorage.getItem(VOICE_PREF_KEY);
    if (saved === '0') voiceEnabled.value = false;
    if (saved === '1') voiceEnabled.value = true;
  } catch {
    // ignore
  }
}

function toggleVoice() {
  voiceEnabled.value = !voiceEnabled.value;
  try {
    window.localStorage.setItem(VOICE_PREF_KEY, voiceEnabled.value ? '1' : '0');
  } catch {
    // ignore
  }
  message.success(voiceEnabled.value ? '语音播报已开启' : '语音播报已关闭');
}

// 新增/编辑弹窗
const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.PrinterCreate & { id?: number }>({ name: '', sn: '', type: 0, location: '' });

// 分类映射弹窗
const showMappingModal = ref(false);
const mappingPrinter = ref<Api.Business.Printer | null>(null);
const selectedCategoryIds = ref<number[]>([]);

const rules = {
  name: { required: true, message: '请输入打印机名称', trigger: 'blur' },
  sn: { required: true, message: '请输入序列号', trigger: 'blur' },
  type: { required: true, type: 'number' as const, message: '请选择类型', trigger: 'change' }
};

const typeOptions: SelectOption[] = [
  { label: '前台', value: 0 }, { label: '后厨', value: 1 }
];

const columns: DataTableColumns<Api.Business.Printer> = [
  { title: '名称', key: 'name', width: 120 },
  { title: '序列号', key: 'sn', width: 150 },
  {
    title: '类型', key: 'type', width: 80,
    render(row) { return row.type === 0 ? '前台' : '后厨'; }
  },
  {
    title: '状态', key: 'status', width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error' },
        { default: () => row.status === 1 ? '在线' : '离线' });
    }
  },
  { title: '位置', key: 'location', width: 120, render(row) { return row.location || '-'; } },
  {
    title: '关联分类', key: 'categoryIds', width: 150,
    render(row) {
      if (!row.categoryIds || row.categoryIds.length === 0) return '-';
      return row.categoryIds.map(id => {
        const opt = categoryOptions.value.find(o => o.value === id);
        return opt ? opt.label : id;
      }).join('、');
    }
  },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作', key: 'actions', width: 280,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(NButton, { size: 'small', type: 'info', onClick: () => handleMapping(row) }, { default: () => '映射' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleToggleStatus(row) },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', type: row.status === 1 ? 'warning' : 'success' },
                  { default: () => (row.status === 1 ? '设为离线' : '设为在线') }
                ),
              default: () => `确定将该打印机${row.status === 1 ? '设为离线' : '设为在线'}吗？`
            }
          ),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => '确定删除该打印机吗？'
            }
          )
        ]
      });
    }
  }
];

async function loadCategories() {
  const { data: result, error } = await fetchDishCategoryList();
  if (!error && result) {
    categoryOptions.value = result.map(c => ({ label: c.name, value: c.id }));
  }
}

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchPrinterList();
    if (!error && result) {
      data.value = result;
      const offlineCount = result.filter(item => item.status === 0).length;
      if (offlineCount > 0 && offlineCount > lastOfflineCount) {
        playVoice(`打印机离线提醒，当前有${offlineCount}台离线`, 3000);
      }
      lastOfflineCount = offlineCount;
    }
  } finally { loading.value = false; }
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { name: '', sn: '', type: 0, location: '' };
  showModal.value = true;
}

function handleEdit(row: Api.Business.Printer) {
  isEdit.value = true;
  formModel.value = { id: row.id, name: row.name, sn: row.sn, type: row.type, location: row.location || '' };
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (isEdit.value && formModel.value.id) {
    const { error } = await updatePrinter(formModel.value.id, formModel.value as Api.Business.PrinterUpdate);
    if (!error) {
      message.success('更新成功');
      playVoice(`打印机${formModel.value.name}已更新`);
      showModal.value = false;
      loadData();
    }
  } else {
    const { error } = await createPrinter(formModel.value);
    if (!error) {
      message.success('创建成功');
      playVoice(`已新增打印机${formModel.value.name}`);
      showModal.value = false;
      loadData();
    }
  }
}

function handleMapping(row: Api.Business.Printer) {
  mappingPrinter.value = row;
  selectedCategoryIds.value = row.categoryIds ? [...row.categoryIds] : [];
  showMappingModal.value = true;
}

async function handleMappingSubmit() {
  if (!mappingPrinter.value) return;
  const mappings = selectedCategoryIds.value.map(categoryId => ({
    printerId: mappingPrinter.value!.id, categoryId
  }));
  const { error } = await updateCategoryMapping({ mappings, printerIds: [mappingPrinter.value.id] });
  if (!error) {
    message.success('映射更新成功');
    playVoice(`分类映射已更新，${mappingPrinter.value.name}`);
    showMappingModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: Api.Business.Printer) {
  const nextStatus = row.status === 1 ? 0 : 1;
  const { error } = await updatePrinter(row.id, { id: row.id, status: nextStatus });
  if (!error) {
    message.success(nextStatus === 1 ? '已设为在线' : '已设为离线');
    playVoice(nextStatus === 1 ? `${row.name}已上线` : `${row.name}已离线`);
    loadData();
  }
}

async function handleDelete(id: number) {
  const { error } = await deletePrinter(id);
  if (!error) {
    message.success('删除成功');
    playVoice('打印机已删除');
    loadData();
  }
}

onMounted(() => {
  initVoicePreference();
  loadCategories();
  loadData();
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" title="打印机管理">
      <template #header-extra>
        <NSpace :size="8" align="center">
          <NButton size="small" secondary @click="toggleVoice">
            语音: {{ voiceEnabled ? '开' : '关' }}
          </NButton>
          <NButton type="primary" @click="handleAdd">新增打印机</NButton>
        </NSpace>
      </template>
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>

    <!-- 新增/编辑弹窗 -->
    <NModal v-model:show="showModal" preset="dialog" :title="isEdit ? '编辑打印机' : '新增打印机'" positive-text="确定" negative-text="取消" @positive-click="handleSubmit">
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="80">
        <NFormItem label="名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="请输入打印机名称" />
        </NFormItem>
        <NFormItem label="序列号" path="sn">
          <NInput v-model:value="formModel.sn" placeholder="请输入序列号" />
        </NFormItem>
        <NFormItem label="类型" path="type">
          <NSelect v-model:value="formModel.type" :options="typeOptions" />
        </NFormItem>
        <NFormItem label="位置">
          <NInput v-model:value="formModel.location" placeholder="请输入位置描述" />
        </NFormItem>
      </NForm>
    </NModal>

    <!-- 分类映射弹窗 -->
    <NModal v-model:show="showMappingModal" preset="dialog" :title="`分类映射 - ${mappingPrinter?.name || ''}`" positive-text="保存" negative-text="取消" @positive-click="handleMappingSubmit">
      <NSelect v-model:value="selectedCategoryIds" :options="categoryOptions" multiple placeholder="请选择关联的菜品分类" />
    </NModal>
  </NSpace>
</template>
