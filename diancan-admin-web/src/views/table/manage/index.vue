<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref } from 'vue';
import {
  NButton, NCard, NDataTable, NForm, NFormItem, NImage, NInput, NInputNumber, NModal,
  NPopconfirm, NSelect, NSpace, NTag, useMessage
} from 'naive-ui';
import type { DataTableColumns, FormInst } from 'naive-ui';
import {
  createTable,
  fetchTableAreaList,
  deleteTable,
  downloadQrCodeTaskFile,
  downloadTableQrCode,
  fetchQrCodeTask,
  fetchTableList,
  submitDownloadAllTableQrCodesTask,
  submitGenerateAllTableQrCodesTask,
  updateTable
} from '@/service/api';

defineOptions({ name: 'TableManage' });

const message = useMessage();
const loading = ref(false);
const generating = ref(false);
const packaging = ref(false);
const data = ref<Api.Business.DiningTable[]>([]);
const areaList = ref<Api.Business.TableArea[]>([]);
const filterKeyword = ref('');
const filterStatus = ref<string>('all');
const filterArea = ref<string>('all');
const qrTask = ref<Api.Business.QrCodeTask | null>(null);
let qrTaskTimer: number | null = null;
let qrTaskDismissTimer: number | null = null;

const showModal = ref(false);
const isEdit = ref(false);
const formRef = ref<FormInst | null>(null);
const formModel = ref<Api.Business.TableCreate & { id?: number }>({ code: '', name: '', capacity: 4, areaId: null });
const qrPreviewVisible = ref(false);
const qrPreviewTitle = ref('');
const qrPreviewUrl = ref('');

const rules = {
  code: { required: true, message: '请输入桌台编号', trigger: 'blur' },
  name: { required: true, message: '请输入桌台名称', trigger: 'blur' }
};

const statusMap: Record<number, { label: string; type: 'success' | 'error' | 'warning' | 'info'; accent: string; tone: string }> = {
  0: { label: '空闲', type: 'success', accent: 'var(--table-status-free-accent)', tone: 'success' },
  1: { label: '占用', type: 'error', accent: 'var(--table-status-busy-accent)', tone: 'error' },
  2: { label: '已结账', type: 'info', accent: 'var(--table-status-paid-accent)', tone: 'info' },
  3: { label: '待清洁', type: 'warning', accent: 'var(--table-status-cleaning-accent)', tone: 'warning' }
};

const statusFilterOptions = [
  { label: '全部状态', value: 'all' },
  { label: '空闲', value: '0' },
  { label: '占用', value: '1' },
  { label: '已结账', value: '2' },
  { label: '待清洁', value: '3' }
];

const areaFilterOptions = computed(() => {
  const orderedAreas: string[] = [];
  areaList.value.forEach(item => orderedAreas.push(item.name));
  const areaSet = new Set(orderedAreas);
  data.value.forEach(item => areaSet.add(item.areaName || '未分区'));
  const areas = [...areaSet];
  return [
    { label: '全部区域', value: 'all' },
    ...areas.map(area => ({ label: area, value: area }))
  ];
});

const areaSelectOptions = computed(() =>
  areaList.value.map(item => ({
    label: item.status === 1 ? item.name : `${item.name}（已停用）`,
    value: item.id,
    disabled: item.status !== 1
  }))
);

const currentAreaStatus = computed(() => {
  if (formModel.value.areaId == null) return null;
  return areaList.value.find(item => item.id === formModel.value.areaId) || null;
});

const tableOverview = computed(() => ({
  total: data.value.length,
  free: data.value.filter(item => item.status === 0).length,
  occupied: data.value.filter(item => item.status === 1).length,
  cleaning: data.value.filter(item => item.status === 3).length,
  qrReady: data.value.filter(item => !!item.qrCodeUrl).length,
  areas: areaFilterOptions.value.length - 1
}));

const occupancyRate = computed(() => {
  if (!tableOverview.value.total) return 0;
  return Math.round((tableOverview.value.occupied / tableOverview.value.total) * 100);
});

const taskProgress = computed(() => {
  if (!qrTask.value || !qrTask.value.total) return 0;
  return Math.min(100, Math.max(0, Math.round(((qrTask.value.completed || 0) / qrTask.value.total) * 100)));
});

const taskThemeClass = computed(() => {
  if (!qrTask.value) return '';
  if (qrTask.value.status === 'FAILED') return 'is-failed';
  if (qrTask.value.status === 'SUCCESS') return 'is-success';
  return qrTask.value.taskType === 'DOWNLOAD_ALL' ? 'is-packaging' : 'is-generating';
});

const filteredData = computed(() => {
  const keyword = filterKeyword.value.trim().toLowerCase();
  return data.value.filter(item => {
    const matchKeyword =
      !keyword ||
      item.code.toLowerCase().includes(keyword) ||
      item.name.toLowerCase().includes(keyword);

    const matchStatus = filterStatus.value === 'all' || String(item.status) === filterStatus.value;
    const areaName = item.areaName || '未分区';
    const matchArea = filterArea.value === 'all' || areaName === filterArea.value;

    return matchKeyword && matchStatus && matchArea;
  });
});

const statusSummaryChips = computed(() => [
  { key: 'all', label: '全部', count: filteredData.value.length, tone: 'default' },
  { key: '0', label: '空闲', count: filteredData.value.filter(item => item.status === 0).length, tone: 'success' },
  { key: '1', label: '占用', count: filteredData.value.filter(item => item.status === 1).length, tone: 'error' },
  { key: '2', label: '已结账', count: filteredData.value.filter(item => item.status === 2).length, tone: 'info' },
  { key: '3', label: '待清洁', count: filteredData.value.filter(item => item.status === 3).length, tone: 'warning' }
]);

const columns: DataTableColumns<Api.Business.DiningTable> = [
  { title: '桌台编号', key: 'code', width: 100 },
  { title: '桌台名称', key: 'name', width: 120 },
  { title: '座位数', key: 'capacity', width: 80 },
  { title: '区域', key: 'areaName', width: 100 },
  {
    title: '状态', key: 'status', width: 100,
    render(row) {
      const s = statusMap[row.status] || { label: '未知', type: 'info' as const, accent: 'var(--table-status-paid-accent)', tone: 'info' };
      return h(
        NTag,
        {
          bordered: false,
          style: {
            color: '#fff',
            background: s.accent
          }
        },
        { default: () => s.label }
      );
    }
  },
  {
    title: '二维码', key: 'qrCodeUrl', width: 100,
    render(row) {
      return row.qrCodeUrl
        ? h(
            NButton,
            {
              size: 'small',
              type: 'info',
              secondary: true,
              onClick: () => handlePreviewQrCode(row)
            },
            { default: () => '查看' }
          )
        : '-';
    }
  },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作', key: 'actions', width: 260,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(
            NButton,
            {
              size: 'small',
              type: 'info',
              onClick: () => handleDownloadQrCode(row)
            },
            { default: () => '下载二维码' }
          ),
          h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除该桌台吗？'
          })
        ]
      });
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const [{ data: tableResult, error: tableError }, { data: areaResult, error: areaError }] = await Promise.all([
      fetchTableList(),
      fetchTableAreaList()
    ]);
    if (!tableError && tableResult) data.value = tableResult;
    if (!areaError && areaResult) areaList.value = areaResult;
  } finally { loading.value = false; }
}

function handleAdd() {
  isEdit.value = false;
  formModel.value = { code: '', name: '', capacity: 4, areaId: null };
  showModal.value = true;
}

function handleEdit(row: Api.Business.DiningTable) {
  isEdit.value = true;
  formModel.value = { id: row.id, code: row.code, name: row.name, capacity: row.capacity, areaId: row.areaId ?? null };
  showModal.value = true;
}

async function handleSubmit() {
  await formRef.value?.validate();
  if (isEdit.value && formModel.value.id) {
    const { error } = await updateTable({ ...formModel.value, id: formModel.value.id } as Api.Business.TableUpdate);
    if (!error) { message.success('更新成功'); showModal.value = false; loadData(); }
  } else {
    const { error } = await createTable(formModel.value);
    if (!error) { message.success('创建成功'); showModal.value = false; loadData(); }
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteTable(id);
  if (!error) { message.success('删除成功'); loadData(); }
}

async function handleDownloadQrCode(row: Api.Business.DiningTable) {
  try {
    await downloadTableQrCode(row.id, `${row.code}-${row.name}-qrcode.png`);
    message.success('二维码下载已开始');
  } catch (error) {
    const msg = error instanceof Error ? error.message : '二维码下载失败';
    message.error(msg);
  }
}

function handlePreviewQrCode(row: Api.Business.DiningTable) {
  if (!row.qrCodeUrl) return;
  qrPreviewTitle.value = `${row.code}-${row.name}`;
  qrPreviewUrl.value = row.qrCodeUrl;
  qrPreviewVisible.value = true;
}

async function handleGenerateAllQrCodes() {
  if (generating.value) return;

  generating.value = true;
  try {
    const { data: task, error } = await submitGenerateAllTableQrCodesTask();
    if (!error && task) {
      message.success('批量生成任务已提交');
      startPollingTask(task.taskId, false);
    }
  } finally {
    generating.value = false;
  }
}

async function handleDownloadAllQrCodes() {
  if (packaging.value) return;

  packaging.value = true;
  try {
    const { data: task, error } = await submitDownloadAllTableQrCodesTask();
    if (!error && task) {
      message.success('打包下载任务已提交');
      startPollingTask(task.taskId, true);
    }
  } finally {
    packaging.value = false;
  }
}

function clearTaskTimer() {
  if (qrTaskTimer !== null) {
    window.clearTimeout(qrTaskTimer);
    qrTaskTimer = null;
  }
}

function clearTaskDismissTimer() {
  if (qrTaskDismissTimer !== null) {
    window.clearTimeout(qrTaskDismissTimer);
    qrTaskDismissTimer = null;
  }
}

async function pollTask(taskId: string, autoDownload: boolean) {
  const { data: task, error } = await fetchQrCodeTask(taskId);
  if (error || !task) {
    clearTaskTimer();
    return;
  }

  qrTask.value = task;

  if (task.status === 'SUCCESS') {
    clearTaskTimer();
    clearTaskDismissTimer();
    qrTaskDismissTimer = window.setTimeout(() => {
      qrTask.value = null;
      qrTaskDismissTimer = null;
    }, 2500);

    if (task.taskType === 'GENERATE_ALL') {
      message.success(task.message || '二维码批量生成完成');
      await loadData();
      return;
    }

    if (autoDownload && task.downloadable) {
      try {
        await downloadQrCodeTaskFile(task.taskId, task.fileName || undefined);
        message.success('二维码压缩包下载已开始');
      } catch (err) {
        const msg = err instanceof Error ? err.message : '压缩包下载失败';
        message.error(msg);
      }
    }
    return;
  }

  if (task.status === 'FAILED') {
    clearTaskTimer();
    clearTaskDismissTimer();
    message.error(task.message || '二维码任务执行失败');
    return;
  }

  qrTaskTimer = window.setTimeout(() => {
    pollTask(taskId, autoDownload);
  }, 2000);
}

function startPollingTask(taskId: string, autoDownload: boolean) {
  clearTaskTimer();
  clearTaskDismissTimer();
  qrTask.value = {
    taskId,
    taskType: autoDownload ? 'DOWNLOAD_ALL' : 'GENERATE_ALL',
    status: 'PENDING',
    message: autoDownload ? '二维码打包任务排队中' : '二维码生成任务排队中',
    total: 0,
    completed: 0,
    downloadable: false
  };
  void pollTask(taskId, autoDownload);
}

onMounted(() => { loadData(); });
onUnmounted(() => {
  clearTaskTimer();
  clearTaskDismissTimer();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="table-manage-hero">
      <div class="table-manage-hero__eyebrow">桌台档案</div>
      <div class="table-manage-hero__head">
        <div>
          <h2 class="table-manage-hero__title">把桌台档案、二维码和区域筛选统一到一张桌台注册中心里</h2>
          <p class="table-manage-hero__desc">适合维护桌台编号、查看状态和批量生成二维码，减少桌台基础数据管理的零散操作。</p>
        </div>
        <div class="table-manage-hero__badge">
          <span>当前在台率</span>
          <strong>{{ occupancyRate }}%</strong>
          <small>{{ tableOverview.occupied }} / {{ tableOverview.total || 0 }} 张桌台正在接待</small>
        </div>
      </div>

      <div class="table-manage-hero__stats">
        <div class="table-manage-hero__stat">
          <span>当前桌台数</span>
          <strong>{{ filteredData.length }}</strong>
        </div>
        <div class="table-manage-hero__stat" data-tone="success">
          <span>空闲</span>
          <strong>{{ tableOverview.free }}</strong>
        </div>
        <div class="table-manage-hero__stat" data-tone="error">
          <span>占用</span>
          <strong>{{ tableOverview.occupied }}</strong>
        </div>
        <div class="table-manage-hero__stat" data-tone="warning">
          <span>待清洁</span>
          <strong>{{ tableOverview.cleaning }}</strong>
        </div>
        <div class="table-manage-hero__stat" data-tone="info">
          <span>二维码已就绪</span>
          <strong>{{ tableOverview.qrReady }}</strong>
        </div>
        <div class="table-manage-hero__stat">
          <span>区域数量</span>
          <strong>{{ tableOverview.areas }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="桌台管理" class="table-manage-card">
      <template #header-extra>
        <NSpace :size="8" class="table-manage-actions">
          <NInput v-model:value="filterKeyword" class="table-filter-input" placeholder="筛选桌台编号/名称" clearable />
          <NSelect v-model:value="filterStatus" class="table-filter-status" :options="statusFilterOptions" />
          <NSelect v-model:value="filterArea" class="table-filter-area" :options="areaFilterOptions" />
          <NButton type="success" :loading="generating" @click="handleGenerateAllQrCodes">生成所有桌台点餐码</NButton>
          <NButton type="info" :loading="packaging" @click="handleDownloadAllQrCodes">下载全部桌贴码（按区域压缩）</NButton>
          <NButton type="primary" @click="handleAdd">新增桌台</NButton>
        </NSpace>
      </template>
      <div class="table-manage-toolbar">
        <div class="table-manage-toolbar__summary">
          <span>当前筛选</span>
          <strong>{{ filterArea === 'all' ? '全部区域' : filterArea }}</strong>
          <em>{{ filterStatus === 'all' ? '全部状态' : statusFilterOptions.find(item => item.value === filterStatus)?.label }}</em>
        </div>
        <div class="table-manage-toolbar__summary">
          <span>展示结果</span>
          <strong>{{ filteredData.length }}</strong>
          <em>支持按桌台编号、名称、状态和区域联筛</em>
        </div>
      </div>
      <div class="table-manage-status-strip">
        <button
          v-for="chip in statusSummaryChips"
          :key="chip.key"
          type="button"
          class="table-manage-status-chip"
          :data-tone="chip.tone"
          :class="{ 'table-manage-status-chip--active': filterStatus === chip.key || (chip.key === 'all' && filterStatus === 'all') }"
          @click="filterStatus = chip.key"
        >
          <span>{{ chip.label }}</span>
          <strong>{{ chip.count }}</strong>
        </button>
      </div>
      <div v-if="qrTask" class="task-panel" :class="taskThemeClass">
        <div class="task-panel__meta">
          <div class="task-panel__ratio">{{ qrTask.completed || 0 }}/{{ qrTask.total || 0 }}</div>
          <div class="task-panel__track">
            <div class="task-panel__bar" :style="{ width: `${taskProgress}%` }">
              <span class="task-panel__glow" />
            </div>
          </div>
          <div class="task-panel__percent">{{ taskProgress }}%</div>
        </div>
      </div>
      <NDataTable :columns="columns" :data="filteredData" :loading="loading" />
    </NCard>

    <NModal v-model:show="showModal" preset="dialog" :title="isEdit ? '编辑桌台' : '新增桌台'" positive-text="确定" negative-text="取消" @positive-click="handleSubmit">
      <NForm ref="formRef" :model="formModel" :rules="rules" label-placement="left" label-width="80">
        <NFormItem label="桌台编号" path="code">
          <NInput v-model:value="formModel.code" placeholder="请输入桌台编号" />
        </NFormItem>
        <NFormItem label="桌台名称" path="name">
          <NInput v-model:value="formModel.name" placeholder="请输入桌台名称" />
        </NFormItem>
        <NFormItem label="座位数">
          <NInputNumber v-model:value="formModel.capacity" :min="1" placeholder="座位数" style="width: 100%" />
        </NFormItem>
        <NFormItem label="所属区域">
          <NSelect
            v-model:value="formModel.areaId"
            :options="areaSelectOptions"
            placeholder="请选择区域"
            clearable
            filterable
          />
          <div v-if="currentAreaStatus?.status === 0" class="table-area-form-tip">
            当前桌台绑定的是已停用区域，如需继续使用，建议先去区域管理中启用或重新选择有效区域。
          </div>
        </NFormItem>
      </NForm>
    </NModal>

    <NModal v-model:show="qrPreviewVisible" preset="card" :title="`二维码预览 · ${qrPreviewTitle}`" style="width: 420px;" :bordered="false">
      <div class="qr-preview-wrap">
        <NImage :src="qrPreviewUrl" object-fit="contain" width="320" preview-disabled />
      </div>
    </NModal>
  </NSpace>
</template>

<style scoped>
.table-manage-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.table-manage-hero :deep(.n-card__content) {
  padding-top: 12px !important;
  padding-bottom: 12px !important;
}

.table-manage-hero__eyebrow {
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: rgba(15, 62, 124, 0.68);
}

.table-manage-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.table-manage-hero__title {
  margin: 0;
  font-size: 18px;
  line-height: 1.3;
  color: #123055;
}

.table-manage-hero__desc {
  max-width: 760px;
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(21, 44, 76, 0.72);
}

.table-manage-hero__badge {
  min-width: 148px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.table-manage-hero__badge span,
.table-manage-hero__badge small {
  display: block;
  font-size: 11px;
  color: rgba(15, 62, 124, 0.68);
}

.table-manage-hero__badge strong {
  display: block;
  margin-top: 4px;
  font-size: 20px;
  line-height: 1.1;
  color: #0f6fff;
}

.table-manage-hero__badge small {
  margin-top: 4px;
  color: rgba(21, 44, 76, 0.62);
}

.table-manage-hero__stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
  margin-top: 10px;
}

.table-manage-hero__stat {
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow:
    0 10px 18px rgba(15, 57, 119, 0.05),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.table-manage-hero__stat span,
.table-manage-hero__stat strong {
  display: block;
}

.table-manage-hero__stat span {
  font-size: 11px;
  color: rgba(21, 44, 76, 0.62);
}

.table-manage-hero__stat strong {
  margin-top: 4px;
  font-size: 18px;
  line-height: 1;
  color: #123055;
}

.table-manage-hero__stat[data-tone='success'] strong {
  color: #2f8f6b;
}

.table-manage-hero__stat[data-tone='error'] strong {
  color: #d9485f;
}

.table-manage-hero__stat[data-tone='warning'] strong {
  color: #dd8b1c;
}

.table-manage-hero__stat[data-tone='info'] strong {
  color: #14a3ff;
}

.table-manage-card {
  overflow: hidden;
}

.table-manage-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.table-manage-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.table-manage-status-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.table-manage-status-chip {
  --chip-accent: rgba(var(--admin-accent-rgb), 0.78);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.table-manage-status-chip[data-tone='success'] {
  --chip-accent: var(--table-status-free-accent);
}

.table-manage-status-chip[data-tone='error'] {
  --chip-accent: var(--table-status-busy-accent);
}

.table-manage-status-chip[data-tone='info'] {
  --chip-accent: var(--table-status-paid-accent);
}

.table-manage-status-chip[data-tone='warning'] {
  --chip-accent: var(--table-status-cleaning-accent);
}

.table-manage-status-chip span {
  font-size: 12px;
  color: color-mix(in srgb, var(--chip-accent) 46%, #52657f);
}

.table-manage-status-chip strong {
  font-size: 14px;
  color: color-mix(in srgb, var(--chip-accent) 72%, #163457);
}

.table-manage-status-chip:hover,
.table-manage-status-chip--active {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--chip-accent) 28%, rgba(var(--admin-accent-rgb), 0.12));
  box-shadow:
    0 14px 24px color-mix(in srgb, var(--chip-accent) 12%, rgba(15, 57, 119, 0.08)),
    inset 0 0 0 1px color-mix(in srgb, var(--chip-accent) 10%, rgba(255, 255, 255, 0.8));
}

.table-manage-toolbar__summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 111, 255, 0.08);
}

.table-manage-toolbar__summary span,
.table-manage-toolbar__summary em {
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
  font-style: normal;
}

.table-manage-toolbar__summary strong {
  font-size: 14px;
  color: #123055;
}

.table-filter-input {
  width: 200px;
}

:deep(.n-data-table .n-button) {
  min-width: 72px;
}

.table-filter-status {
  width: 130px;
}

.table-filter-area {
  width: 148px;
}

.table-area-form-tip {
  margin-top: 8px;
  font-size: 12px;
  line-height: 1.6;
  color: rgba(184, 96, 16, 0.92);
}

.task-panel {
  --task-bg: #ffffff;
  --task-line: linear-gradient(90deg, #22c55e 0%, #06b6d4 48%, #60a5fa 100%);
  --task-shadow: rgba(15, 23, 42, 0.08);
  position: relative;
  margin: 0 0 12px 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 8px 12px;
  width: 100%;
  background: var(--task-bg);
  box-shadow: 0 6px 16px -12px var(--task-shadow);
}

.task-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 0.95));
  pointer-events: none;
}

.task-panel__meta {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.task-panel__percent {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  flex-shrink: 0;
}

.task-panel__track {
  position: relative;
  flex: 1;
  height: 4px;
  overflow: hidden;
  border-radius: 999px;
  background: #e2e8f0;
}

.task-panel__bar {
  position: relative;
  height: 100%;
  min-width: 6px;
  border-radius: inherit;
  background: var(--task-line);
  transition: width 0.45s ease;
  box-shadow: 0 0 24px -4px var(--task-shadow);
}

.task-panel__ratio {
  color: #64748b;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  line-height: 1;
  flex-shrink: 0;
}

.task-panel__glow {
  position: absolute;
  top: -1px;
  right: -20px;
  width: 26px;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(255, 255, 255, 0.82) 50%, transparent 100%);
  transform: skewX(-20deg);
  animation: task-scan 1.4s linear infinite;
}

.task-panel.is-packaging {
  --task-line: linear-gradient(90deg, #f59e0b 0%, #f97316 45%, #fb7185 100%);
}

.task-panel.is-success {
  --task-line: linear-gradient(90deg, #4ade80 0%, #34d399 50%, #22c55e 100%);
}

.task-panel.is-failed {
  --task-line: linear-gradient(90deg, #fb7185 0%, #ef4444 50%, #b91c1c 100%);
}

.qr-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px 0 4px;
}

@keyframes task-scan {
  from {
    transform: translateX(-18px) skewX(-20deg);
    opacity: 0.24;
  }

  50% {
    opacity: 0.92;
  }

  to {
    transform: translateX(18px) skewX(-20deg);
    opacity: 0.24;
  }
}

@media (max-width: 768px) {
  .table-manage-hero__head,
  .table-manage-toolbar {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-manage-status-chip {
    width: calc(50% - 4px);
    justify-content: space-between;
  }

  .table-manage-hero__badge {
    min-width: 0;
    width: 100%;
  }

  .table-manage-hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-manage-actions {
    justify-content: flex-start;
  }

  .task-panel {
    width: 100%;
    padding: 8px 12px;
  }

  .task-panel__percent {
    font-size: 12px;
  }
}

html.dark .table-manage-hero__badge small,
html.dark .table-manage-hero__stat span,
html.dark .table-manage-toolbar__summary span,
html.dark .table-manage-toolbar__summary em {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .table-manage-hero__stat,
html.dark .table-manage-toolbar__summary {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 18px 30px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .table-manage-hero__stat strong,
html.dark .table-manage-toolbar__summary strong {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .table-manage-status-chip {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .table-manage-status-chip span {
  color: color-mix(in srgb, var(--chip-accent) 52%, rgba(214, 225, 247, 0.84));
}

html.dark .table-manage-status-chip strong {
  color: color-mix(in srgb, var(--chip-accent) 66%, rgba(241, 246, 255, 0.96));
}

html.dark .table-manage-status-chip:hover,
html.dark .table-manage-status-chip--active {
  box-shadow:
    0 18px 30px rgba(0, 0, 0, 0.24),
    inset 0 0 0 1px color-mix(in srgb, var(--chip-accent) 18%, rgba(255, 255, 255, 0.08));
}

html.dark .table-area-form-tip {
  color: #ffcf8b;
}
</style>
