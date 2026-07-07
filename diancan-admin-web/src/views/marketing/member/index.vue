<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NDescriptions,
  NDescriptionsItem,
  NDrawer,
  NDrawerContent,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NInput,
  NInputNumber,
  NSelect,
  NSpace,
  NTabPane,
  NTabs,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { adjustMemberPoints, fetchMemberDetail, fetchMemberPage } from '@/service/api';

defineOptions({ name: 'MarketingMember' });

type MemberRow = Record<string, any>;
type MemberRecordRow = Record<string, any>;
type MemberLevelChangeRow = Record<string, any>;
type MemberOrderContributionRow = Record<string, any>;

const message = useMessage();
const loading = ref(false);
const detailLoading = ref(false);
const showDetail = ref(false);
const total = ref(0);
const data = ref<MemberRow[]>([]);
const activeTab = ref('points');

const searchForm = ref({
  memberNo: '',
  nickname: '',
  phone: '',
  levelId: undefined as number | undefined,
  status: undefined as number | undefined,
  pageNum: 1,
  pageSize: 10
});

const levelOptions = [
  { label: '普通会员', value: 2001 },
  { label: '银卡会员', value: 2002 },
  { label: '金卡会员', value: 2003 },
  { label: '黑金会员', value: 2004 }
];

const statusOptions = [
  { label: '正常', value: 1 },
  { label: '冻结', value: 0 }
];

const detailData = ref<Record<string, any> | null>(null);
const adjustForm = ref({ changeAmount: 0, remark: '' });
const adjustLoading = ref(false);

const benefitTags = computed(() => {
  const raw = detailData.value?.currentLevelBenefitConfig;
  if (!raw) return [];

  if (Array.isArray(raw)) {
    return raw.map(item => String(item)).filter(Boolean);
  }

  if (typeof raw === 'string') {
    const text = raw.trim();
    if (!text) return [];

    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parsed.map(item => String(item)).filter(Boolean);
      }
      if (parsed && typeof parsed === 'object') {
        return Object.entries(parsed)
          .map(([key, value]) => `${key}: ${value}`)
          .filter(Boolean);
      }
    } catch (err) {
      return text
        .split(/[,\n，；;|]/)
        .map(item => item.trim())
        .filter(Boolean);
    }

    return [text];
  }

  return [String(raw)];
});

const heroMetrics = ref([
  { label: '会员总量', value: '0', tone: 'primary' },
  { label: '活跃会员', value: '0', tone: 'success' },
  { label: '积分池余额', value: '0', tone: 'warning' }
]);

const pointsColumns: DataTableColumns<MemberRecordRow> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '业务类型', key: 'bizType', width: 120 },
  {
    title: '变动积分',
    key: 'changeAmount',
    width: 110,
    render(row) {
      const positive = Number(row.changeAmount || 0) >= 0;
      return h(NTag, { type: positive ? 'success' : 'error' }, { default: () => `${positive ? '+' : ''}${row.changeAmount || 0}` });
    }
  },
  { title: '余额', key: 'balanceAfter', width: 90 },
  { title: '备注', key: 'remark', minWidth: 160 }
];

const growthColumns: DataTableColumns<MemberRecordRow> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '业务类型', key: 'bizType', width: 120 },
  {
    title: '成长值',
    key: 'changeAmount',
    width: 110,
    render(row) {
      const value = Number(row.changeAmount || 0);
      const positive = value >= 0;
      return h(NTag, { type: positive ? 'success' : 'warning' }, { default: () => `${positive ? '+' : ''}${value}` });
    }
  },
  { title: '当前成长值', key: 'growthAfter', width: 110 },
  { title: '备注', key: 'remark', minWidth: 160 }
];

const levelChangeColumns: DataTableColumns<MemberLevelChangeRow> = [
  { title: '时间', key: 'createTime', width: 180 },
  { title: '变更原因', key: 'changeReason', width: 120 },
  {
    title: '等级变化',
    key: 'levelChange',
    minWidth: 220,
    render(row) {
      const oldLevelName = row.oldLevelName || '未设置';
      const newLevelName = row.newLevelName || '-';
      return `${oldLevelName} -> ${newLevelName}`;
    }
  },
  { title: '业务类型', key: 'bizType', width: 120 },
  { title: '备注', key: 'remark', minWidth: 180 }
];

const orderContributionColumns: DataTableColumns<MemberOrderContributionRow> = [
  { title: '订单号', key: 'orderNo', width: 190 },
  { title: '订单金额', key: 'actualAmount', width: 110 },
  { title: '实付金额', key: 'paidAmount', width: 110 },
  {
    title: '奖励积分',
    key: 'pointsReward',
    width: 100,
    render(row) {
      return h(NTag, { type: 'success' }, { default: () => `+${row.pointsReward || 0}` });
    }
  },
  {
    title: '成长值',
    key: 'growthReward',
    width: 100,
    render(row) {
      return h(NTag, { type: 'info' }, { default: () => `+${row.growthReward || 0}` });
    }
  },
  { title: '下单时间', key: 'createTime', width: 180 }
];

const columns: DataTableColumns<MemberRow> = [
  { title: '会员编号', key: 'memberNo', width: 200 },
  { title: '昵称', key: 'nickname', width: 120 },
  { title: '手机号', key: 'phone', width: 140 },
  {
    title: '会员等级',
    key: 'levelName',
    width: 120,
    render(row) {
      const typeMap: Record<string, 'default' | 'info' | 'warning' | 'success'> = {
        普通会员: 'default',
        银卡会员: 'info',
        金卡会员: 'warning',
        黑金会员: 'success'
      };
      return h(NTag, { type: typeMap[row.levelName] || 'default' }, { default: () => row.levelName || '-' });
    }
  },
  { title: '成长值', key: 'growthValue', width: 100 },
  { title: '积分余额', key: 'pointsBalance', width: 100 },
  { title: '累计消费', key: 'totalAmountConsumed', width: 110 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '正常' : '冻结') });
    }
  },
  { title: '最后消费时间', key: 'lastConsumeTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render(row) {
      return h(NButton, { size: 'small', type: 'primary', onClick: () => openDetail(row) }, { default: () => '详情' });
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchMemberPage(searchForm.value);
    if (!error && result) {
      data.value = result.list || [];
      total.value = result.total || 0;
      const rows = result.list || [];
      heroMetrics.value = [
        { label: '会员总量', value: String(result.total || 0), tone: 'primary' },
        { label: '活跃会员', value: String(rows.filter((item: MemberRow) => item.status === 1).length), tone: 'success' },
        { label: '积分池余额', value: String(rows.reduce((sum: number, item: MemberRow) => sum + Number(item.pointsBalance || 0), 0)), tone: 'warning' }
      ];
    }
  } finally {
    loading.value = false;
  }
}

async function openDetail(row: MemberRow) {
  detailLoading.value = true;
  showDetail.value = true;
  activeTab.value = 'points';
  adjustForm.value = { changeAmount: 0, remark: '' };
  try {
    const { data: result, error } = await fetchMemberDetail(row.id);
    if (!error && result) {
      detailData.value = result;
    }
  } finally {
    detailLoading.value = false;
  }
}

async function handleAdjustPoints() {
  if (!detailData.value) return;
  if (!adjustForm.value.changeAmount) {
    message.warning('调整积分不能为0');
    return;
  }
  adjustLoading.value = true;
  try {
    const { error } = await adjustMemberPoints(detailData.value.id, adjustForm.value);
    if (!error) {
      message.success('积分调整成功');
      await openDetail({ id: detailData.value.id });
      loadData();
    }
  } finally {
    adjustLoading.value = false;
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = {
    memberNo: '',
    nickname: '',
    phone: '',
    levelId: undefined,
    status: undefined,
    pageNum: 1,
    pageSize: 10
  };
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12" class="marketing-page">
    <NCard :bordered="false" class="marketing-hero">
      <div class="marketing-hero__main">
        <div>
          <div class="marketing-hero__eyebrow">VIP OPERATIONS</div>
          <h2 class="marketing-hero__title">会员资产、等级和积分在一屏内掌控</h2>
          <p class="marketing-hero__desc">适合在运营活动、人工修正和客户回访场景下快速查看会员状态，不必在多个页面来回切换。</p>
        </div>
        <div class="marketing-hero__metrics">
          <div v-for="item in heroMetrics" :key="item.label" class="marketing-hero__metric" :data-tone="item.tone">
            <div class="marketing-hero__metric-label">{{ item.label }}</div>
            <div class="marketing-hero__metric-value">{{ item.value }}</div>
          </div>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6"><NFormItem label="会员编号"><NInput v-model:value="searchForm.memberNo" placeholder="请输入会员编号" clearable /></NFormItem></NGi>
          <NGi :span="5"><NFormItem label="昵称"><NInput v-model:value="searchForm.nickname" placeholder="请输入昵称" clearable /></NFormItem></NGi>
          <NGi :span="5"><NFormItem label="手机号"><NInput v-model:value="searchForm.phone" placeholder="请输入手机号" clearable /></NFormItem></NGi>
          <NGi :span="4"><NFormItem label="等级"><NSelect v-model:value="searchForm.levelId" :options="levelOptions" clearable /></NFormItem></NGi>
          <NGi :span="4"><NFormItem label="状态"><NSelect v-model:value="searchForm.status" :options="statusOptions" clearable /></NFormItem></NGi>
        </NGrid>
        <NSpace>
          <NButton type="primary" @click="handleSearch">搜索</NButton>
          <NButton @click="handleReset">重置</NButton>
        </NSpace>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="会员列表">
      <NDataTable remote :columns="columns" :data="data" :loading="loading" :pagination="{ page: searchForm.pageNum, pageSize: searchForm.pageSize, itemCount: total, showSizePicker: true, prefix: ({ itemCount }) => `共 ${itemCount} 条`, pageSizes: [10, 20, 50, 100], onChange: (page: number) => { searchForm.pageNum = page; loadData(); }, onUpdatePageSize: (pageSize: number) => { searchForm.pageSize = pageSize; searchForm.pageNum = 1; loadData(); } }" />
    </NCard>

    <NDrawer v-model:show="showDetail" :width="960">
      <NDrawerContent title="会员详情" closable>
        <NSpace vertical :size="12">
          <NCard :bordered="false" :loading="detailLoading">
            <NDescriptions v-if="detailData" label-placement="left" bordered :column="2">
              <NDescriptionsItem label="会员编号">{{ detailData.memberNo }}</NDescriptionsItem>
              <NDescriptionsItem label="昵称">{{ detailData.nickname || '-' }}</NDescriptionsItem>
              <NDescriptionsItem label="手机号">{{ detailData.phone || '-' }}</NDescriptionsItem>
              <NDescriptionsItem label="当前等级">{{ detailData.levelName || '-' }}</NDescriptionsItem>
              <NDescriptionsItem label="当前积分">{{ detailData.pointsBalance || 0 }}</NDescriptionsItem>
              <NDescriptionsItem label="当前成长值">{{ detailData.growthValue || 0 }}</NDescriptionsItem>
              <NDescriptionsItem label="累计获得积分">{{ detailData.totalPointsEarned || 0 }}</NDescriptionsItem>
              <NDescriptionsItem label="累计消耗积分">{{ detailData.totalPointsUsed || 0 }}</NDescriptionsItem>
              <NDescriptionsItem label="积分倍率">{{ detailData.currentLevelPointsRate || 1 }}</NDescriptionsItem>
              <NDescriptionsItem label="等级折扣">{{ detailData.currentLevelDiscountRate || 1 }}</NDescriptionsItem>
              <NDescriptionsItem label="累计消费">{{ detailData.totalAmountConsumed || 0 }}</NDescriptionsItem>
              <NDescriptionsItem label="最后消费时间">{{ detailData.lastConsumeTime || '-' }}</NDescriptionsItem>
            </NDescriptions>
          </NCard>

          <NCard v-if="detailData" :bordered="false" title="当前等级权益">
            <div v-if="benefitTags.length" class="benefit-tags">
              <NTag v-for="item in benefitTags" :key="item" type="info" round>{{ item }}</NTag>
            </div>
            <div v-else class="benefit-empty">当前等级暂未配置额外权益说明</div>
          </NCard>

          <NCard :bordered="false" title="手工调整积分">
            <NForm :model="adjustForm" inline label-placement="left">
              <NFormItem label="调整积分"><NInputNumber v-model:value="adjustForm.changeAmount" style="width: 180px" /></NFormItem>
              <NFormItem label="备注"><NInput v-model:value="adjustForm.remark" placeholder="例如：活动补偿、人工修正" style="width: 320px" /></NFormItem>
              <NButton type="primary" :loading="adjustLoading" @click="handleAdjustPoints">确认调整</NButton>
            </NForm>
          </NCard>

          <NCard :bordered="false">
            <NTabs v-model:value="activeTab" type="line">
              <NTabPane name="points" tab="最近积分流水"><NDataTable :columns="pointsColumns" :data="detailData?.recentPointsRecords || []" :pagination="false" /></NTabPane>
              <NTabPane name="growth" tab="最近成长流水"><NDataTable :columns="growthColumns" :data="detailData?.recentGrowthRecords || []" :pagination="false" /></NTabPane>
              <NTabPane name="orders" tab="最近订单贡献"><NDataTable :columns="orderContributionColumns" :data="detailData?.recentOrderContributions || []" :pagination="false" /></NTabPane>
              <NTabPane name="levelChange" tab="等级变更日志"><NDataTable :columns="levelChangeColumns" :data="detailData?.recentLevelChangeLogs || []" :pagination="false" /></NTabPane>
            </NTabs>
          </NCard>
        </NSpace>
      </NDrawerContent>
    </NDrawer>
  </NSpace>
</template>

<style scoped>
.marketing-page {
  padding: 2px;
}

.marketing-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.marketing-hero__main {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 18px;
}

.marketing-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.marketing-hero__title {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
  color: #123055;
}

.marketing-hero__desc {
  max-width: 720px;
  margin: 10px 0 0;
  color: rgba(21, 44, 76, 0.72);
  line-height: 1.75;
}

.marketing-hero__metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(150px, 1fr));
  gap: 12px;
}

.marketing-hero__metric {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.1);
}

.marketing-hero__metric-label {
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.marketing-hero__metric-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #123055;
}

.marketing-hero__metric[data-tone='primary'] .marketing-hero__metric-value {
  color: #0f6fff;
}

.marketing-hero__metric[data-tone='success'] .marketing-hero__metric-value {
  color: #2f8f6b;
}

.marketing-hero__metric[data-tone='warning'] .marketing-hero__metric-value {
  color: #163a70;
}

.benefit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.benefit-empty {
  color: rgba(21, 44, 76, 0.56);
}

@media (max-width: 960px) {
  .marketing-hero__main {
    flex-direction: column;
  }

  .marketing-hero__metrics {
    grid-template-columns: 1fr;
  }
}
</style>

