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
  NSwitch,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import {
  deleteMemberExchange,
  fetchCouponTemplatePage,
  fetchMemberBenefitConfig,
  fetchMemberExchangeList,
  saveMemberBenefitConfig,
  saveMemberExchange
} from '@/service/api';

defineOptions({ name: 'MarketingMemberBenefit' });

const message = useMessage();
const saving = ref(false);
const exchangeLoading = ref(false);
const showExchangeModal = ref(false);
const exchangeOptions = ref<{ label: string; value: number }[]>([]);
const exchangeRows = ref<Record<string, any>[]>([]);

const configForm = ref({
  pointsDeductionRule: {
    enabled: false,
    pointsPerStep: 100,
    amountPerStep: 1,
    maxDeductionRatio: 0.2,
    maxPointsPerOrder: 2000
  },
  pointsExpireRule: {
    enabled: false,
    expireDays: 365
  },
  birthdayBenefitRule: {
    enabled: false,
    couponTemplateId: null as number | null
  }
});

const exchangeForm = ref({
  id: undefined as number | undefined,
  templateId: null as number | null,
  pointsCost: 1000,
  perUserLimit: 1,
  sort: 1,
  status: 1,
  remark: ''
});

const exchangeColumns: DataTableColumns<Record<string, any>> = [
  { title: '优惠券模板', key: 'templateName', minWidth: 180 },
  { title: '所需积分', key: 'pointsCost', width: 120 },
  { title: '每人上限', key: 'perUserLimit', width: 110, render: row => (row.perUserLimit === 0 ? '不限' : `${row.perUserLimit} 次`) },
  { title: '排序', key: 'sort', width: 90 },
  { title: '状态', key: 'status', width: 90, render: row => h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '启用' : '停用') }) },
  { title: '备注', key: 'remark', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEditExchange(row) }, { default: () => '编辑' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDeleteExchange(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => '确定删除该兑换项吗？'
            }
          )
        ]
      })
  }
];

const enabledSummary = computed(() => [
  { label: '积分抵现', active: configForm.value.pointsDeductionRule.enabled },
  { label: '积分过期', active: configForm.value.pointsExpireRule.enabled },
  { label: '生日权益', active: configForm.value.birthdayBenefitRule.enabled }
]);

async function loadBenefitConfig() {
  const { data, error } = await fetchMemberBenefitConfig();
  if (!error && data) {
    configForm.value = {
      pointsDeductionRule: {
        enabled: Boolean(data.pointsDeductionRule?.enabled),
        pointsPerStep: Number(data.pointsDeductionRule?.pointsPerStep || 100),
        amountPerStep: Number(data.pointsDeductionRule?.amountPerStep || 1),
        maxDeductionRatio: Number(data.pointsDeductionRule?.maxDeductionRatio || 0.2),
        maxPointsPerOrder: Number(data.pointsDeductionRule?.maxPointsPerOrder || 2000)
      },
      pointsExpireRule: {
        enabled: Boolean(data.pointsExpireRule?.enabled),
        expireDays: Number(data.pointsExpireRule?.expireDays || 365)
      },
      birthdayBenefitRule: {
        enabled: Boolean(data.birthdayBenefitRule?.enabled),
        couponTemplateId: data.birthdayBenefitRule?.couponTemplateId ?? null
      }
    };
  }
}

async function loadExchangeList() {
  exchangeLoading.value = true;
  try {
    const { data, error } = await fetchMemberExchangeList();
    if (!error && data) {
      exchangeRows.value = data || [];
    }
  } finally {
    exchangeLoading.value = false;
  }
}

async function loadTemplateOptions() {
  const { data, error } = await fetchCouponTemplatePage({ pageNum: 1, pageSize: 200 });
  if (!error && data?.records) {
    exchangeOptions.value = data.records.map((item: Record<string, any>) => ({
      label: item.name,
      value: item.id
    }));
  }
}

async function handleSaveConfig() {
  saving.value = true;
  try {
    const { error } = await saveMemberBenefitConfig({
      pointsDeductionRule: {
        ...configForm.value.pointsDeductionRule,
        amountPerStep: Number(configForm.value.pointsDeductionRule.amountPerStep || 0)
      },
      pointsExpireRule: configForm.value.pointsExpireRule,
      birthdayBenefitRule: configForm.value.birthdayBenefitRule
    });
    if (!error) {
      message.success('会员权益配置已保存');
      loadBenefitConfig();
    }
  } finally {
    saving.value = false;
  }
}

function resetExchangeForm() {
  exchangeForm.value = {
    id: undefined,
    templateId: null,
    pointsCost: 1000,
    perUserLimit: 1,
    sort: 1,
    status: 1,
    remark: ''
  };
}

function handleAddExchange() {
  resetExchangeForm();
  showExchangeModal.value = true;
}

function handleEditExchange(row: Record<string, any>) {
  exchangeForm.value = {
    id: row.id,
    templateId: row.templateId,
    pointsCost: row.pointsCost,
    perUserLimit: row.perUserLimit,
    sort: row.sort,
    status: row.status,
    remark: row.remark || ''
  };
  showExchangeModal.value = true;
}

async function handleSaveExchange() {
  if (!exchangeForm.value.templateId) {
    message.warning('请选择优惠券模板');
    return;
  }
  const { error } = await saveMemberExchange(exchangeForm.value);
  if (!error) {
    message.success('兑换项已保存');
    showExchangeModal.value = false;
    loadExchangeList();
  }
}

async function handleDeleteExchange(id: number) {
  const { error } = await deleteMemberExchange(id);
  if (!error) {
    message.success('兑换项已删除');
    loadExchangeList();
  }
}

onMounted(() => {
  loadTemplateOptions();
  loadBenefitConfig();
  loadExchangeList();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="benefit-hero">
      <div class="benefit-hero__eyebrow">MEMBER BENEFITS</div>
      <div class="benefit-hero__head">
        <div>
          <h2 class="benefit-hero__title">把会员二期和三期权益收口到一张运营工作台</h2>
          <p class="benefit-hero__desc">积分抵现、积分过期、生日券、积分兑换券都从这里配置，等级礼包和等级专属券在会员等级页绑定模板即可。</p>
        </div>
        <div class="benefit-hero__tags">
          <span v-for="item in enabledSummary" :key="item.label" class="hero-tag" :class="{ active: item.active }">
            {{ item.label }}
          </span>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="基础权益规则">
      <NForm :model="configForm" label-placement="left" label-width="120">
        <div class="config-grid">
          <div class="config-block">
            <div class="config-block__title">积分抵现</div>
            <NFormItem label="启用规则"><NSwitch v-model:value="configForm.pointsDeductionRule.enabled" /></NFormItem>
            <NFormItem label="每步积分"><NInputNumber v-model:value="configForm.pointsDeductionRule.pointsPerStep" :min="1" style="width: 100%" /></NFormItem>
            <NFormItem label="每步金额"><NInputNumber v-model:value="configForm.pointsDeductionRule.amountPerStep" :min="0.01" :precision="2" style="width: 100%" /></NFormItem>
            <NFormItem label="最高抵扣比例"><NInputNumber v-model:value="configForm.pointsDeductionRule.maxDeductionRatio" :min="0" :max="1" :precision="2" style="width: 100%" /></NFormItem>
            <NFormItem label="每单最多积分"><NInputNumber v-model:value="configForm.pointsDeductionRule.maxPointsPerOrder" :min="0" style="width: 100%" /></NFormItem>
          </div>

          <div class="config-block">
            <div class="config-block__title">积分过期</div>
            <NFormItem label="启用规则"><NSwitch v-model:value="configForm.pointsExpireRule.enabled" /></NFormItem>
            <NFormItem label="有效天数"><NInputNumber v-model:value="configForm.pointsExpireRule.expireDays" :min="1" style="width: 100%" /></NFormItem>
          </div>

          <div class="config-block">
            <div class="config-block__title">生日权益</div>
            <NFormItem label="启用规则"><NSwitch v-model:value="configForm.birthdayBenefitRule.enabled" /></NFormItem>
            <NFormItem label="生日券模板">
              <NSelect v-model:value="configForm.birthdayBenefitRule.couponTemplateId" clearable :options="exchangeOptions" placeholder="生日当天自动发券" />
            </NFormItem>
          </div>
        </div>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton type="primary" :loading="saving" @click="handleSaveConfig">保存配置</NButton>
        </NSpace>
      </template>
    </NCard>

    <NCard :bordered="false" title="积分兑换优惠券">
      <template #header-extra>
        <NButton type="primary" @click="handleAddExchange">新增兑换项</NButton>
      </template>
      <NDataTable :columns="exchangeColumns" :data="exchangeRows" :loading="exchangeLoading" :pagination="false" />
    </NCard>

    <NModal v-model:show="showExchangeModal" preset="card" title="积分兑换项" style="width: 640px">
      <NForm :model="exchangeForm" label-placement="left" label-width="110">
        <NFormItem label="优惠券模板"><NSelect v-model:value="exchangeForm.templateId" :options="exchangeOptions" placeholder="请选择优惠券模板" /></NFormItem>
        <NFormItem label="所需积分"><NInputNumber v-model:value="exchangeForm.pointsCost" :min="1" style="width: 100%" /></NFormItem>
        <NFormItem label="每人上限"><NInputNumber v-model:value="exchangeForm.perUserLimit" :min="0" style="width: 100%" /></NFormItem>
        <NFormItem label="排序"><NInputNumber v-model:value="exchangeForm.sort" :min="0" style="width: 100%" /></NFormItem>
        <NFormItem label="状态">
          <NSelect v-model:value="exchangeForm.status" :options="[{ label: '启用', value: 1 }, { label: '停用', value: 0 }]" />
        </NFormItem>
        <NFormItem label="备注"><NInput v-model:value="exchangeForm.remark" placeholder="可填写使用说明" /></NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showExchangeModal = false">取消</NButton>
          <NButton type="primary" @click="handleSaveExchange">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.benefit-hero {
  background:
    radial-gradient(circle at top right, rgba(36, 130, 94, 0.16), transparent 28%),
    linear-gradient(135deg, rgba(251, 254, 252, 0.98), rgba(231, 247, 240, 0.98)) !important;
}

.benefit-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(24, 84, 61, 0.68);
}

.benefit-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.benefit-hero__title {
  margin: 0;
  font-size: 28px;
  color: #173d2f;
}

.benefit-hero__desc {
  max-width: 780px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(23, 61, 47, 0.72);
}

.benefit-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.hero-tag {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: rgba(23, 61, 47, 0.56);
  border: 1px solid rgba(36, 130, 94, 0.08);
}

.hero-tag.active {
  color: #24825e;
  border-color: rgba(36, 130, 94, 0.22);
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.config-block {
  padding: 18px;
  border-radius: 18px;
  background: rgba(245, 249, 247, 0.96);
  border: 1px solid rgba(36, 130, 94, 0.08);
}

.config-block__title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #173d2f;
}

@media (max-width: 1280px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}
</style>
