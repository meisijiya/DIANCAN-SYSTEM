<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import { NButton, NCard, NDataTable, NForm, NFormItem, NInput, NInputNumber, NModal, NPopconfirm, NSelect, NSpace, NTag, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { createMemberLevel, fetchCouponTemplatePage, fetchMemberLevelList, updateMemberLevel, updateMemberLevelStatus } from '@/service/api';

defineOptions({ name: 'MarketingMemberLevel' });

type LevelRow = Record<string, any>;

const message = useMessage();
const loading = ref(false);
const showModal = ref(false);
const isEdit = ref(false);
const data = ref<LevelRow[]>([]);
const couponTemplateOptions = ref<{ label: string; value: number }[]>([]);

const formModel = ref({
  id: undefined as number | undefined,
  levelCode: '',
  levelName: '',
  sort: 1,
  growthThreshold: 0,
  pointsRate: 1,
  discountRate: 1,
  benefitConfig: '',
  upgradeCouponTemplateId: null as number | null,
  exclusiveCouponTemplateId: null as number | null,
  remark: ''
});

const columns: DataTableColumns<LevelRow> = [
  { title: '等级编码', key: 'levelCode', width: 150 },
  { title: '等级名称', key: 'levelName', width: 140 },
  { title: '排序', key: 'sort', width: 80 },
  { title: '成长门槛', key: 'growthThreshold', width: 110 },
  { title: '积分倍率', key: 'pointsRate', width: 100 },
  { title: '折扣倍率', key: 'discountRate', width: 100 },
  { title: '升级礼包券', key: 'upgradeCouponTemplateId', width: 110, render: row => row.upgradeCouponTemplateId || '-' },
  { title: '等级专属券', key: 'exclusiveCouponTemplateId', width: 110, render: row => row.exclusiveCouponTemplateId || '-' },
  { title: '状态', key: 'status', width: 90, render: row => h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '启用' : '停用') }) },
  { title: '备注', key: 'remark', minWidth: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(NPopconfirm, { onPositiveClick: () => handleToggleStatus(row) }, { trigger: () => h(NButton, { size: 'small', type: row.status === 1 ? 'warning' : 'success' }, { default: () => (row.status === 1 ? '停用' : '启用') }), default: () => `确定${row.status === 1 ? '停用' : '启用'}该等级吗？` })
        ]
      });
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchMemberLevelList();
    if (!error && result) data.value = result || [];
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  formModel.value = { id: undefined, levelCode: '', levelName: '', sort: 1, growthThreshold: 0, pointsRate: 1, discountRate: 1, benefitConfig: '', upgradeCouponTemplateId: null, exclusiveCouponTemplateId: null, remark: '' };
}

function handleAdd() {
  isEdit.value = false;
  resetForm();
  showModal.value = true;
}

function handleEdit(row: LevelRow) {
  isEdit.value = true;
  formModel.value = { id: row.id, levelCode: row.levelCode, levelName: row.levelName, sort: row.sort, growthThreshold: row.growthThreshold, pointsRate: row.pointsRate, discountRate: row.discountRate, benefitConfig: row.benefitConfig || '', upgradeCouponTemplateId: row.upgradeCouponTemplateId || null, exclusiveCouponTemplateId: row.exclusiveCouponTemplateId || null, remark: row.remark || '' };
  showModal.value = true;
}

async function handleSubmit() {
  if (!formModel.value.levelName || (!isEdit.value && !formModel.value.levelCode)) {
    message.warning('请填写完整等级信息');
    return;
  }
  if (isEdit.value && formModel.value.id) {
    const { error } = await updateMemberLevel(formModel.value.id, { levelName: formModel.value.levelName, sort: formModel.value.sort, growthThreshold: formModel.value.growthThreshold, pointsRate: formModel.value.pointsRate, discountRate: formModel.value.discountRate, benefitConfig: formModel.value.benefitConfig, upgradeCouponTemplateId: formModel.value.upgradeCouponTemplateId, exclusiveCouponTemplateId: formModel.value.exclusiveCouponTemplateId, remark: formModel.value.remark, status: data.value.find(item => item.id === formModel.value.id)?.status ?? 1 });
    if (!error) {
      message.success('会员等级更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }
  const { error } = await createMemberLevel({ levelCode: formModel.value.levelCode, levelName: formModel.value.levelName, sort: formModel.value.sort, growthThreshold: formModel.value.growthThreshold, pointsRate: formModel.value.pointsRate, discountRate: formModel.value.discountRate, benefitConfig: formModel.value.benefitConfig, upgradeCouponTemplateId: formModel.value.upgradeCouponTemplateId, exclusiveCouponTemplateId: formModel.value.exclusiveCouponTemplateId, remark: formModel.value.remark });
  if (!error) {
    message.success('会员等级创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: LevelRow) {
  const { error } = await updateMemberLevelStatus(row.id, row.status === 1 ? 0 : 1);
  if (!error) {
    message.success('状态更新成功');
    loadData();
  }
}

async function loadCouponTemplates() {
  const { data: result, error } = await fetchCouponTemplatePage({ pageNum: 1, pageSize: 200 });
  if (!error && result?.records) {
    couponTemplateOptions.value = result.records.map((item: Record<string, any>) => ({
      label: item.name,
      value: item.id
    }));
  }
}

onMounted(() => {
  loadCouponTemplates();
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="level-hero">
      <div class="level-hero__eyebrow">LEVEL DESIGN</div>
      <div class="level-hero__head">
        <div>
          <h2 class="level-hero__title">把会员等级做成一套可运营的梯度体系</h2>
          <p class="level-hero__desc">这里适合配置成长门槛、积分倍率和折扣规则，方便后续做差异化权益和促活策略。</p>
        </div>
        <div class="level-hero__stats">
          <div class="level-hero__stat"><span>等级数量</span><strong>{{ data.length }}</strong></div>
          <div class="level-hero__stat"><span>启用等级</span><strong>{{ data.filter(item => item.status === 1).length }}</strong></div>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="会员等级配置">
      <template #header-extra><NButton type="primary" @click="handleAdd">新增等级</NButton></template>
      <NDataTable :columns="columns" :data="data" :loading="loading" :pagination="false" />
    </NCard>

    <NModal v-model:show="showModal" preset="card" :title="isEdit ? '编辑会员等级' : '新增会员等级'" style="width: 680px">
      <NForm :model="formModel" label-placement="left" label-width="100">
        <NFormItem label="等级编码"><NInput v-model:value="formModel.levelCode" :disabled="isEdit" placeholder="例如 NORMAL / GOLD" /></NFormItem>
        <NFormItem label="等级名称"><NInput v-model:value="formModel.levelName" placeholder="请输入等级名称" /></NFormItem>
        <NFormItem label="排序"><NInputNumber v-model:value="formModel.sort" :min="0" style="width: 100%" /></NFormItem>
        <NFormItem label="成长门槛"><NInputNumber v-model:value="formModel.growthThreshold" :min="0" style="width: 100%" /></NFormItem>
        <NFormItem label="积分倍率"><NInputNumber v-model:value="formModel.pointsRate" :min="0.01" :precision="2" style="width: 100%" /></NFormItem>
        <NFormItem label="折扣倍率"><NInputNumber v-model:value="formModel.discountRate" :min="0.01" :precision="2" style="width: 100%" /></NFormItem>
        <NFormItem label="升级礼包券"><NSelect v-model:value="formModel.upgradeCouponTemplateId" clearable :options="couponTemplateOptions" placeholder="可选：会员升级后自动发放" /></NFormItem>
        <NFormItem label="等级专属券"><NSelect v-model:value="formModel.exclusiveCouponTemplateId" clearable :options="couponTemplateOptions" placeholder="可选：当前等级专属券" /></NFormItem>
        <NFormItem label="权益配置"><NInput v-model:value="formModel.benefitConfig" type="textarea" placeholder="可存JSON字符串，后续用于扩展权益" /></NFormItem>
        <NFormItem label="备注"><NInput v-model:value="formModel.remark" placeholder="请输入备注" /></NFormItem>
      </NForm>
      <template #footer><NSpace justify="end"><NButton @click="showModal = false">取消</NButton><NButton type="primary" @click="handleSubmit">保存</NButton></NSpace></template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.level-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.level-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.level-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.level-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.level-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.level-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(140px, 1fr));
  gap: 12px;
}

.level-hero__stat {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.level-hero__stat span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.level-hero__stat strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}
</style>
