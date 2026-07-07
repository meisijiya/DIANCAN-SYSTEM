<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NCheckbox,
  NCheckboxGroup,
  NDataTable,
  NDatePicker,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NTabPane,
  NTabs,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption } from 'naive-ui';
import {
  createCouponTemplate,
  fetchCouponGrantTaskDetailPage,
  fetchCouponGrantTaskPage,
  fetchCouponTemplatePage,
  fetchUserCouponPage,
  fetchUserList,
  grantCoupon,
  updateCouponTemplate,
  updateCouponTemplateStatus
} from '@/service/api';

defineOptions({
  name: 'ManageCoupon'
});

const message = useMessage();
const activeTab = ref('template');

const templateLoading = ref(false);
const templateSearch = ref<Api.Coupon.CouponTemplateQuery>({
  name: '',
  status: undefined,
  type: undefined,
  pageNum: 1,
  pageSize: 10
});
const templateData = ref<Api.Coupon.CouponTemplate[]>([]);
const templateTotal = ref(0);

const couponLoading = ref(false);
const couponSearch = ref<Api.Coupon.UserCouponQuery>({
  keyword: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
});
const couponData = ref<Api.Coupon.UserCoupon[]>([]);
const couponTotal = ref(0);
const taskLoading = ref(false);
const taskSearch = ref<Api.Coupon.CouponGrantTaskQuery>({
  templateName: '',
  taskStatus: undefined,
  pageNum: 1,
  pageSize: 10
});
const taskData = ref<Api.Coupon.CouponGrantTask[]>([]);
const taskTotal = ref(0);
const taskDetailLoading = ref(false);
const taskDetailDrawerVisible = ref(false);
const currentTaskDetail = ref<Api.Coupon.CouponGrantTask | null>(null);
const taskDetailSearch = ref<Api.Coupon.CouponGrantTaskDetailQuery>({
  taskId: '',
  grantStatus: undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 10
});
const taskDetailData = ref<Api.Coupon.CouponGrantTaskDetail[]>([]);
const taskDetailTotal = ref(0);

const userOptions = ref<SelectOption[]>([]);
const showTemplateModal = ref(false);
const editingTemplateId = ref<string | null>(null);
const fixedDateRange = ref<[number, number] | null>(null);
const selectedWeekdays = ref<number[]>([]);
const templateForm = ref<Api.Coupon.CouponTemplateSubmit>({
  name: '',
  type: 1,
  thresholdAmount: 0,
  discountAmount: 0,
  discountRate: null,
  totalQuantity: 0,
  perUserLimit: 1,
  validityType: 2,
  validFrom: null,
  validTo: null,
  validDays: 7,
  status: 1,
  description: '',
  availableWeekdays: null
});

const showGrantModal = ref(false);
const grantSubmitting = ref(false);
const grantForm = ref<Api.Coupon.CouponGrantSubmit>({
  templateId: '',
  grantMode: 1,
  userIds: [],
  remark: ''
});
const currentGrantTemplate = ref<Api.Coupon.CouponTemplate | null>(null);

const templateTypeOptions = [
  { label: '满减券', value: 1 },
  { label: '折扣券', value: 2 }
];

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const couponStatusOptions = [
  { label: '未使用', value: 0 },
  { label: '已使用', value: 1 },
  { label: '已过期', value: 2 },
  { label: '已锁定', value: 3 }
];

const taskStatusOptions = [
  { label: '待处理', value: 0 },
  { label: '分发中', value: 1 },
  { label: '处理中', value: 2 },
  { label: '已完成', value: 3 },
  { label: '部分成功', value: 4 },
  { label: '已失败', value: 5 }
];

const taskUserStatusOptions = [
  { label: '待处理', value: 0 },
  { label: '发放成功', value: 1 },
  { label: '发放失败', value: 2 },
  { label: '已跳过', value: 3 }
];

const weekdayOptions = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
  { label: '周六', value: 6 },
  { label: '周日', value: 7 }
];

const isFixedValidity = computed(() => templateForm.value.validityType === 1);
const isDiscountCoupon = computed(() => templateForm.value.type === 2);

const templateColumns: DataTableColumns<Api.Coupon.CouponTemplate> = [
  { title: '券名', key: 'name', width: 180 },
  {
    title: '类型',
    key: 'type',
    width: 90,
    render: row => h(NTag, { type: row.type === 1 ? 'warning' : 'success' }, { default: () => (row.type === 1 ? '满减券' : '折扣券') })
  },
  {
    title: '优惠规则',
    key: 'rule',
    width: 180,
    render: row => (row.type === 1 ? `满 ${formatMoney(row.thresholdAmount)} 减 ${formatMoney(row.discountAmount)}` : `${formatRate(row.discountRate)} 折`)
  },
  {
    title: '有效期',
    key: 'validity',
    width: 220,
    render: row => (row.validityType === 1 ? `${row.validFrom || '-'} ~ ${row.validTo || '-'}` : `领券后 ${row.validDays || 0} 天有效`)
  },
  {
    title: '可用星期',
    key: 'availableWeekdays',
    width: 180,
    render: row => formatAvailableWeekdays(row.availableWeekdays)
  },
  {
    title: '发放统计',
    key: 'quantity',
    width: 120,
    render: row => `${row.issuedQuantity}/${row.totalQuantity === 0 ? '不限' : row.totalQuantity}`
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: row => h(NTag, { type: row.status === 1 ? 'success' : 'default' }, { default: () => (row.status === 1 ? '启用' : '停用') })
  },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 260,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => openEditModal(row) }, { default: () => '编辑' }),
          h(NButton, { size: 'small', type: 'info', onClick: () => openGrantModal(row) }, { default: () => '发券' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleToggleTemplateStatus(row) },
            {
              trigger: () =>
                h(
                  NButton,
                  { size: 'small', type: row.status === 1 ? 'warning' : 'success' },
                  { default: () => (row.status === 1 ? '停用' : '启用') }
                ),
              default: () => `确定${row.status === 1 ? '停用' : '启用'}该模板吗？`
            }
          )
        ]
      })
  }
];

const userCouponColumns: DataTableColumns<Api.Coupon.UserCoupon> = [
  { title: '券名', key: 'couponName', width: 180 },
  { title: '用户', key: 'username', width: 120 },
  { title: '手机号', key: 'phone', width: 140 },
  {
    title: '面额/折扣',
    key: 'value',
    width: 120,
    render: row => (row.couponType === 1 ? `减 ${formatMoney(row.discountAmount)}` : `${formatRate(row.discountRate)} 折`)
  },
  {
    title: '来源',
    key: 'sourceType',
    width: 100,
    render: row => (row.sourceType === 2 ? '全员发放' : '指定发放')
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => {
      const tagMap: Record<number, { label: string; type: 'success' | 'warning' | 'error' | 'default' }> = {
        0: { label: '未使用', type: 'success' },
        1: { label: '已使用', type: 'default' },
        2: { label: '已过期', type: 'warning' },
        3: { label: '已锁定', type: 'error' }
      };
      const current = tagMap[row.status] || tagMap[0];
      return h(NTag, { type: current.type }, { default: () => current.label });
    }
  },
  { title: '领取时间', key: 'receivedTime', width: 180 },
  { title: '有效期至', key: 'validTo', width: 180 }
];

const taskColumns: DataTableColumns<Api.Coupon.CouponGrantTask> = [
  { title: '任务ID', key: 'id', width: 200 },
  { title: '模板名称', key: 'templateName', width: 160 },
  {
    title: '发放方式',
    key: 'grantMode',
    width: 100,
    render: row => (row.grantMode === 2 ? '全部用户' : '指定用户')
  },
  {
    title: '任务状态',
    key: 'taskStatus',
    width: 110,
    render: row => {
      const statusMap: Record<number, { label: string; type: 'default' | 'success' | 'warning' | 'error' | 'info' }> = {
        0: { label: '待处理', type: 'warning' },
        1: { label: '分发中', type: 'info' },
        2: { label: '处理中', type: 'info' },
        3: { label: '已完成', type: 'success' },
        4: { label: '部分成功', type: 'warning' },
        5: { label: '已失败', type: 'error' }
      };
      const current = statusMap[row.taskStatus] || statusMap[0];
      return h(NTag, { type: current.type }, { default: () => current.label });
    }
  },
  {
    title: '处理进度',
    key: 'progress',
    width: 180,
    render: row => `${row.finishedBatchCount || 0}/${row.totalBatchCount || 0} 批，成功 ${row.successCount} / 失败 ${row.failCount}`
  },
  { title: '开始时间', key: 'startedTime', width: 180 },
  { title: '完成时间', key: 'finishedTime', width: 180 },
  { title: '最后错误', key: 'lastError', width: 220, ellipsis: { tooltip: true } },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    fixed: 'right',
    render: row =>
      h(
        NButton,
        { size: 'small', type: 'primary', ghost: true, onClick: () => openTaskDetailDrawer(row) },
        { default: () => '查看明细' }
      )
  }
];

const taskDetailColumns: DataTableColumns<Api.Coupon.CouponGrantTaskDetail> = [
  { title: '用户ID', key: 'userId', width: 140 },
  { title: '用户名', key: 'username', width: 140 },
  { title: '手机号', key: 'phone', width: 140 },
  {
    title: '发放状态',
    key: 'grantStatus',
    width: 100,
    render: row => {
      const statusMap: Record<number, { label: string; type: 'default' | 'success' | 'warning' | 'error' }> = {
        0: { label: '待处理', type: 'warning' },
        1: { label: '发放成功', type: 'success' },
        2: { label: '发放失败', type: 'error' },
        3: { label: '已跳过', type: 'default' }
      };
      const current = statusMap[row.grantStatus] || statusMap[0];
      return h(NTag, { type: current.type, size: 'small' }, { default: () => current.label });
    }
  },
  { title: '失败原因', key: 'failReason', width: 220, ellipsis: { tooltip: true } },
  { title: '处理时间', key: 'finishedTime', width: 180 },
  { title: '入队时间', key: 'createTime', width: 180 }
];

function formatMoney(value?: number | null) {
  return Number(value || 0).toFixed(2);
}

function formatRate(value?: number | null) {
  return Number(value || 0).toFixed(2);
}

function formatAvailableWeekdays(value?: string | null) {
  if (!value) {
    return '全周可用';
  }
  const labels = value
    .split(',')
    .map(item => Number(item))
    .map(item => weekdayOptions.find(option => option.value === item)?.label)
    .filter(Boolean);
  return labels.length ? labels.join('、') : '全周可用';
}

function resetTemplateForm() {
  editingTemplateId.value = null;
  fixedDateRange.value = null;
  selectedWeekdays.value = [];
  templateForm.value = {
    name: '',
    type: 1,
    thresholdAmount: 0,
    discountAmount: 0,
    discountRate: null,
    totalQuantity: 0,
    perUserLimit: 1,
    validityType: 2,
    validFrom: null,
    validTo: null,
    validDays: 7,
    status: 1,
    description: '',
    availableWeekdays: null
  };
}

function openCreateModal() {
  resetTemplateForm();
  showTemplateModal.value = true;
}

function openEditModal(row: Api.Coupon.CouponTemplate) {
  editingTemplateId.value = row.id;
  selectedWeekdays.value = row.availableWeekdays ? row.availableWeekdays.split(',').map(item => Number(item)) : [];
  templateForm.value = {
    name: row.name,
    type: row.type,
    thresholdAmount: row.thresholdAmount,
    discountAmount: row.discountAmount,
    discountRate: row.discountRate,
    totalQuantity: row.totalQuantity,
    perUserLimit: row.perUserLimit,
    validityType: row.validityType,
    validFrom: row.validFrom,
    validTo: row.validTo,
    validDays: row.validDays,
    status: row.status,
    description: row.description || '',
    availableWeekdays: row.availableWeekdays
  };
  fixedDateRange.value = row.validFrom && row.validTo ? [new Date(row.validFrom).getTime(), new Date(row.validTo).getTime()] : null;
  showTemplateModal.value = true;
}

function openGrantModal(row: Api.Coupon.CouponTemplate) {
  currentGrantTemplate.value = row;
  grantForm.value = {
    templateId: row.id,
    grantMode: 1,
    userIds: [],
    remark: ''
  };
  showGrantModal.value = true;
}

function syncTemplateValidity() {
  if (templateForm.value.validityType === 1 && fixedDateRange.value) {
    templateForm.value.validFrom = new Date(fixedDateRange.value[0]).toISOString().slice(0, 19).replace('T', ' ');
    templateForm.value.validTo = new Date(fixedDateRange.value[1]).toISOString().slice(0, 19).replace('T', ' ');
    templateForm.value.validDays = null;
    return;
  }
  templateForm.value.validFrom = null;
  templateForm.value.validTo = null;
}

function syncTemplateWeekdays() {
  if (!selectedWeekdays.value.length || selectedWeekdays.value.length === 7) {
    templateForm.value.availableWeekdays = null;
    return;
  }

  templateForm.value.availableWeekdays = [...selectedWeekdays.value]
    .sort((a, b) => a - b)
    .join(',');
}

async function loadTemplateData() {
  templateLoading.value = true;
  try {
    const { data, error } = await fetchCouponTemplatePage(templateSearch.value);
    if (!error && data) {
      templateData.value = data.list || [];
      templateTotal.value = data.total || 0;
    }
  } finally {
    templateLoading.value = false;
  }
}

async function loadUserCouponData() {
  couponLoading.value = true;
  try {
    const { data, error } = await fetchUserCouponPage(couponSearch.value);
    if (!error && data) {
      couponData.value = data.list || [];
      couponTotal.value = data.total || 0;
    }
  } finally {
    couponLoading.value = false;
  }
}

async function loadTaskData() {
  taskLoading.value = true;
  try {
    const { data, error } = await fetchCouponGrantTaskPage(taskSearch.value);
    if (!error && data) {
      taskData.value = data.list || [];
      taskTotal.value = data.total || 0;
    }
  } finally {
    taskLoading.value = false;
  }
}

async function loadTaskDetailData() {
  if (!taskDetailSearch.value.taskId) {
    taskDetailData.value = [];
    taskDetailTotal.value = 0;
    return;
  }

  taskDetailLoading.value = true;
  try {
    const { data, error } = await fetchCouponGrantTaskDetailPage(taskDetailSearch.value);
    if (!error && data) {
      taskDetailData.value = data.list || [];
      taskDetailTotal.value = data.total || 0;
    }
  } finally {
    taskDetailLoading.value = false;
  }
}

async function loadUsers() {
  const { data, error } = await fetchUserList({ pageNum: 1, pageSize: 2000 });
  if (!error && data) {
    userOptions.value = (data.list || []).map(item => ({
      label: `${item.username}${item.phone ? ` / ${item.phone}` : ''}`,
      value: String(item.id)
    }));
  }
}

async function handleSubmitTemplate() {
  syncTemplateValidity();
  syncTemplateWeekdays();
  const payload = { ...templateForm.value };

  if (payload.type === 1) {
    payload.discountRate = null;
  } else {
    payload.discountAmount = 0;
  }
  if (payload.validityType === 2) {
    payload.validFrom = null;
    payload.validTo = null;
  }

  let error;
  if (editingTemplateId.value) {
    ({ error } = await updateCouponTemplate(editingTemplateId.value, { ...payload, id: editingTemplateId.value }));
  } else {
    ({ error } = await createCouponTemplate(payload));
  }

  if (!error) {
    message.success(editingTemplateId.value ? '模板更新成功' : '模板创建成功');
    showTemplateModal.value = false;
    loadTemplateData();
  }
}

async function handleToggleTemplateStatus(row: Api.Coupon.CouponTemplate) {
  const { error } = await updateCouponTemplateStatus(row.id, row.status === 1 ? 0 : 1);
  if (!error) {
    message.success('状态更新成功');
    loadTemplateData();
  }
}

async function handleGrantCoupon() {
  if (grantForm.value.grantMode === 1 && (!grantForm.value.userIds || grantForm.value.userIds.length === 0)) {
    message.warning('请选择至少一个用户');
    return;
  }
  if (grantSubmitting.value) {
    return;
  }

  grantSubmitting.value = true;
  try {
    const { data, error } = await grantCoupon(grantForm.value);
    if (!error && data) {
      message.success(`发券任务已入队，任务ID：${data.id}`);
      showGrantModal.value = false;
      activeTab.value = 'task';
      loadTaskData();
    }
  } finally {
    grantSubmitting.value = false;
  }
}

function handleTemplateSearch() {
  templateSearch.value.pageNum = 1;
  loadTemplateData();
}

function handleUserCouponSearch() {
  couponSearch.value.pageNum = 1;
  loadUserCouponData();
}

function handleTaskSearch() {
  taskSearch.value.pageNum = 1;
  loadTaskData();
}

function openTaskDetailDrawer(row: Api.Coupon.CouponGrantTask) {
  currentTaskDetail.value = row;
  taskDetailSearch.value = {
    taskId: row.id,
    grantStatus: undefined,
    keyword: '',
    pageNum: 1,
    pageSize: 10
  };
  taskDetailDrawerVisible.value = true;
  loadTaskDetailData();
}

function handleTaskDetailSearch() {
  taskDetailSearch.value.pageNum = 1;
  loadTaskDetailData();
}

onMounted(() => {
  loadTemplateData();
  loadUserCouponData();
  loadTaskData();
  loadUsers();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="coupon-hero">
      <div class="coupon-hero__eyebrow">优惠券运营</div>
      <div class="coupon-hero__head">
        <div>
          <h2 class="coupon-hero__title">把模板、用户券和发券任务放进同一张优惠券运营面板里</h2>
          <p class="coupon-hero__desc">适合做发券、跟踪任务进度和查看用户领券状态，减少营销配置与执行之间的割裂感。</p>
        </div>
        <div class="coupon-hero__badge">
          <span>当前模板数</span>
          <strong>{{ templateTotal }}</strong>
        </div>
      </div>
    </NCard>

    <NTabs v-model:value="activeTab" type="line">
      <NTabPane name="template" tab="优惠券模板">
        <NSpace vertical :size="12">
          <NCard :bordered="false" class="coupon-filter-card">
            <NForm :model="templateSearch" label-placement="left" label-width="80">
              <NGrid :cols="24" :x-gap="18">
                <NGi :span="6">
                  <NFormItem label="券名称">
                    <NInput v-model:value="templateSearch.name" placeholder="请输入券名称" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="4">
                  <NFormItem label="状态">
                    <NSelect v-model:value="templateSearch.status" :options="statusOptions" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="4">
                  <NFormItem label="类型">
                    <NSelect v-model:value="templateSearch.type" :options="templateTypeOptions" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="10">
                  <NSpace justify="end" class="search-actions">
                    <NButton type="primary" @click="handleTemplateSearch">搜索</NButton>
                    <NButton @click="loadTemplateData">刷新</NButton>
                    <NButton type="success" @click="openCreateModal">新建模板</NButton>
                  </NSpace>
                </NGi>
              </NGrid>
            </NForm>
          </NCard>

          <NCard :bordered="false" title="模板列表" class="coupon-list-card">
            <NDataTable
              remote
              :columns="templateColumns"
              :data="templateData"
              :loading="templateLoading"
              :pagination="{
                page: templateSearch.pageNum,
                pageSize: templateSearch.pageSize,
                itemCount: templateTotal,
                showSizePicker: true,
                prefix: ({ itemCount }) => `共 ${itemCount} 条`,
                pageSizes: [10, 20, 50, 100, 200],
                onChange: (page: number) => { templateSearch.pageNum = page; loadTemplateData(); },
                onUpdatePageSize: (pageSize: number) => { templateSearch.pageSize = pageSize; templateSearch.pageNum = 1; loadTemplateData(); }
              }"
            />
          </NCard>
        </NSpace>
      </NTabPane>

      <NTabPane name="coupon" tab="用户优惠券">
        <NSpace vertical :size="12">
          <NCard :bordered="false" class="coupon-filter-card">
            <NForm :model="couponSearch" label-placement="left" label-width="80">
              <NGrid :cols="24" :x-gap="18">
                <NGi :span="8">
                  <NFormItem label="关键词">
                    <NInput v-model:value="couponSearch.keyword" placeholder="券名 / 用户名 / 手机号" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="4">
                  <NFormItem label="状态">
                    <NSelect v-model:value="couponSearch.status" :options="couponStatusOptions" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="8">
                  <NSpace justify="end" class="search-actions">
                    <NButton type="primary" @click="handleUserCouponSearch">搜索</NButton>
                    <NButton @click="loadUserCouponData">刷新</NButton>
                  </NSpace>
                </NGi>
              </NGrid>
            </NForm>
          </NCard>

          <NCard :bordered="false" title="用户优惠券列表" class="coupon-list-card">
            <NDataTable
              remote
              :columns="userCouponColumns"
              :data="couponData"
              :loading="couponLoading"
              :pagination="{
                page: couponSearch.pageNum,
                pageSize: couponSearch.pageSize,
                itemCount: couponTotal,
                showSizePicker: true,
                prefix: ({ itemCount }) => `共 ${itemCount} 条`,
                pageSizes: [10, 20, 50, 100, 200],
                onChange: (page: number) => { couponSearch.pageNum = page; loadUserCouponData(); },
                onUpdatePageSize: (pageSize: number) => { couponSearch.pageSize = pageSize; couponSearch.pageNum = 1; loadUserCouponData(); }
              }"
            />
          </NCard>
        </NSpace>
      </NTabPane>

      <NTabPane name="task" tab="发券任务">
        <NSpace vertical :size="12">
          <NCard :bordered="false" class="coupon-filter-card">
            <NForm :model="taskSearch" label-placement="left" label-width="80">
              <NGrid :cols="24" :x-gap="18">
                <NGi :span="8">
                  <NFormItem label="模板名称">
                    <NInput v-model:value="taskSearch.templateName" placeholder="请输入模板名称" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="4">
                  <NFormItem label="状态">
                    <NSelect v-model:value="taskSearch.taskStatus" :options="taskStatusOptions" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="8">
                  <NSpace justify="end" class="search-actions">
                    <NButton type="primary" @click="handleTaskSearch">搜索</NButton>
                    <NButton @click="loadTaskData">刷新</NButton>
                  </NSpace>
                </NGi>
              </NGrid>
            </NForm>
          </NCard>

          <NCard :bordered="false" title="发券任务列表" class="coupon-list-card">
            <NDataTable
              remote
              :columns="taskColumns"
              :data="taskData"
              :loading="taskLoading"
              :scroll-x="1450"
              :pagination="{
                page: taskSearch.pageNum,
                pageSize: taskSearch.pageSize,
                itemCount: taskTotal,
                showSizePicker: true,
                prefix: ({ itemCount }) => `共 ${itemCount} 条`,
                pageSizes: [10, 20, 50, 100, 200],
                onChange: (page: number) => { taskSearch.pageNum = page; loadTaskData(); },
                onUpdatePageSize: (pageSize: number) => { taskSearch.pageSize = pageSize; taskSearch.pageNum = 1; loadTaskData(); }
              }"
            />
          </NCard>
        </NSpace>
      </NTabPane>
    </NTabs>

    <NModal v-model:show="showTemplateModal" preset="card" :title="editingTemplateId ? '编辑优惠券模板' : '新建优惠券模板'" style="width: 760px">
      <NForm :model="templateForm" label-placement="left" label-width="100">
        <NGrid :cols="24" :x-gap="16">
          <NGi :span="12">
            <NFormItem label="券名称">
              <NInput v-model:value="templateForm.name" placeholder="请输入优惠券名称" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="类型">
              <NSelect v-model:value="templateForm.type" :options="templateTypeOptions" />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="状态">
              <NSelect v-model:value="templateForm.status" :options="statusOptions" />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NFormItem label="门槛金额">
              <NInputNumber v-model:value="templateForm.thresholdAmount" :min="0" :precision="2" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="8" v-if="!isDiscountCoupon">
            <NFormItem label="优惠金额">
              <NInputNumber v-model:value="templateForm.discountAmount" :min="0" :precision="2" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="8" v-else>
            <NFormItem label="折扣比例">
              <NInputNumber v-model:value="templateForm.discountRate" :min="0.01" :max="0.99" :precision="2" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NFormItem label="发放总量">
              <NInputNumber v-model:value="templateForm.totalQuantity" :min="0" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NFormItem label="每人限领">
              <NInputNumber v-model:value="templateForm.perUserLimit" :min="0" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="16">
            <NFormItem label="有效期类型">
              <NRadioGroup v-model:value="templateForm.validityType">
                <NSpace>
                  <NRadio :value="1">固定时间</NRadio>
                  <NRadio :value="2">领券后N天</NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </NGi>
          <NGi :span="16" v-if="isFixedValidity">
            <NFormItem label="固定有效期">
              <NDatePicker v-model:value="fixedDateRange" type="datetimerange" clearable style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="8" v-else>
            <NFormItem label="有效天数">
              <NInputNumber v-model:value="templateForm.validDays" :min="1" style="width: 100%" />
            </NFormItem>
          </NGi>
          <NGi :span="24">
            <NFormItem label="可用星期">
              <NCheckboxGroup v-model:value="selectedWeekdays">
                <NSpace>
                  <NCheckbox v-for="item in weekdayOptions" :key="item.value" :value="item.value" :label="item.label" />
                </NSpace>
              </NCheckboxGroup>
            </NFormItem>
          </NGi>
          <NGi :span="24">
            <NFormItem label="说明">
              <NInput v-model:value="templateForm.description" type="textarea" placeholder="请输入使用说明" />
            </NFormItem>
          </NGi>
        </NGrid>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showTemplateModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmitTemplate">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="showGrantModal" preset="card" title="发放优惠券" style="width: 620px">
      <NForm :model="grantForm" label-placement="left" label-width="100">
        <NFormItem label="当前模板">
          <NInput :value="currentGrantTemplate?.name || ''" disabled />
        </NFormItem>
        <NFormItem label="发放方式">
          <NRadioGroup v-model:value="grantForm.grantMode">
            <NSpace>
              <NRadio :value="1">指定用户</NRadio>
              <NRadio :value="2">全部用户</NRadio>
            </NSpace>
          </NRadioGroup>
        </NFormItem>
        <NFormItem label="选择用户" v-if="grantForm.grantMode === 1">
          <NSelect v-model:value="grantForm.userIds" multiple filterable clearable :options="userOptions" placeholder="请选择要发券的用户" />
        </NFormItem>
        <NFormItem label="发放备注">
          <NInput v-model:value="grantForm.remark" type="textarea" placeholder="例如：补偿券、新用户关怀券" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton :disabled="grantSubmitting" @click="showGrantModal = false">取消</NButton>
          <NButton type="primary" :loading="grantSubmitting" :disabled="grantSubmitting" @click="handleGrantCoupon">确认发券</NButton>
        </NSpace>
      </template>
    </NModal>

    <NDrawer v-model:show="taskDetailDrawerVisible" :width="920" placement="right">
      <NDrawerContent :title="`任务明细 · ${currentTaskDetail?.templateName || ''}`" closable>
        <NSpace vertical :size="12">
          <NCard v-if="currentTaskDetail" :bordered="false" size="small" class="task-detail-summary">
            <div class="task-detail-summary__head">
              <div>
                <strong>任务ID：{{ currentTaskDetail.id }}</strong>
                <p>
                  {{ currentTaskDetail.grantMode === 2 ? '全部用户发放' : '指定用户发放' }}
                  · 目标 {{ currentTaskDetail.targetCount }} 人
                </p>
              </div>
              <NTag :type="currentTaskDetail.failCount > 0 ? 'warning' : 'success'">
                成功 {{ currentTaskDetail.successCount }} / 失败 {{ currentTaskDetail.failCount }}
              </NTag>
            </div>
            <div class="task-detail-summary__meta">
              <span>批次进度：{{ currentTaskDetail.finishedBatchCount || 0 }}/{{ currentTaskDetail.totalBatchCount || 0 }}</span>
              <span>开始时间：{{ currentTaskDetail.startedTime || '-' }}</span>
              <span>完成时间：{{ currentTaskDetail.finishedTime || '-' }}</span>
            </div>
            <div v-if="currentTaskDetail.lastError" class="task-detail-summary__error">
              最近错误：{{ currentTaskDetail.lastError }}
            </div>
          </NCard>

          <NCard :bordered="false" title="用户处理明细" class="coupon-detail-card">
            <NForm :model="taskDetailSearch" label-placement="left" label-width="80">
              <NGrid :cols="24" :x-gap="18">
                <NGi :span="10">
                  <NFormItem label="关键词">
                    <NInput v-model:value="taskDetailSearch.keyword" placeholder="用户名 / 手机号" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="6">
                  <NFormItem label="状态">
                    <NSelect v-model:value="taskDetailSearch.grantStatus" :options="taskUserStatusOptions" clearable />
                  </NFormItem>
                </NGi>
                <NGi :span="8">
                  <NSpace justify="end" class="search-actions">
                    <NButton type="primary" @click="handleTaskDetailSearch">搜索</NButton>
                    <NButton @click="loadTaskDetailData">刷新</NButton>
                  </NSpace>
                </NGi>
              </NGrid>
            </NForm>

            <NDataTable
              remote
              :columns="taskDetailColumns"
              :data="taskDetailData"
              :loading="taskDetailLoading"
              :scroll-x="1020"
              :pagination="{
                page: taskDetailSearch.pageNum,
                pageSize: taskDetailSearch.pageSize,
                itemCount: taskDetailTotal,
                showSizePicker: true,
                prefix: ({ itemCount }) => `共 ${itemCount} 条`,
                pageSizes: [10, 20, 50, 100, 200],
                onChange: (page: number) => { taskDetailSearch.pageNum = page; loadTaskDetailData(); },
                onUpdatePageSize: (pageSize: number) => { taskDetailSearch.pageSize = pageSize; taskDetailSearch.pageNum = 1; loadTaskDetailData(); }
              }"
            />
          </NCard>
        </NSpace>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped>
.search-actions {
  width: 100%;
}

.coupon-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.coupon-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.coupon-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.coupon-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.coupon-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.coupon-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.coupon-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.coupon-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.coupon-filter-card,
.coupon-list-card,
.coupon-detail-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.coupon-list-card :deep(.n-card-header),
.coupon-detail-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

.task-detail-summary {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.1), transparent 26%),
    linear-gradient(135deg, rgba(247, 250, 255, 0.98), rgba(235, 243, 255, 0.98)) !important;
  box-shadow:
    0 18px 34px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.task-detail-summary__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.task-detail-summary__head strong {
  display: block;
  font-size: 16px;
  color: #123055;
}

.task-detail-summary__head p {
  margin: 8px 0 0;
  color: rgba(18, 48, 85, 0.72);
}

.task-detail-summary__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-top: 14px;
  color: #4b5f7b;
}

.task-detail-summary__error {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(255, 237, 213, 0.72);
  color: #9a3412;
  line-height: 1.7;
}

html.dark .coupon-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .coupon-filter-card,
html.dark .coupon-list-card,
html.dark .coupon-detail-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .task-detail-summary {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 26%),
    linear-gradient(135deg, rgba(5, 7, 11, 0.98), rgba(11, 15, 22, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .task-detail-summary__head strong {
  color: rgba(243, 247, 255, 0.96);
}

html.dark .task-detail-summary__head p,
html.dark .task-detail-summary__meta {
  color: rgba(209, 220, 241, 0.72);
}

html.dark .task-detail-summary__error {
  background: rgba(127, 29, 29, 0.28);
  color: #fecaca;
}
</style>

