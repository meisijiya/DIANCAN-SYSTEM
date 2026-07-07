<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { NButton, NCard, NEmpty, NSpin, NSpace, NTag } from 'naive-ui';
import { fetchMemberOverview } from '@/service/api';

defineOptions({ name: 'MarketingMemberStat' });

type OverviewData = Record<string, any>;

const loading = ref(false);
const overview = ref<OverviewData | null>(null);

const metricCards = computed(() => {
  const current = overview.value || {};
  return [
    { label: '会员总量', value: current.totalMembers || 0, tone: 'primary', note: '当前会员档案总数' },
    { label: '活跃会员', value: current.activeMembers || 0, tone: 'success', note: '状态正常的会员数量' },
    { label: '冻结会员', value: current.frozenMembers || 0, tone: 'warning', note: '需要关注的异常会员' },
    { label: '近7天新增', value: current.recentNewMembers || 0, tone: 'info', note: '最近7天新入会人数' }
  ];
});

const assetCards = computed(() => {
  const current = overview.value || {};
  return [
    { label: '总积分余额', value: current.totalPointsBalance || 0, suffix: '分' },
    { label: '总成长值', value: current.totalGrowthValue || 0, suffix: '' },
    { label: '累计消费', value: current.totalAmountConsumed || 0, prefix: '¥' }
  ];
});

const maxLevelCount = computed(() => {
  const list = overview.value?.levelDistribution || [];
  if (!list.length) return 1;
  return Math.max(...list.map((item: Record<string, any>) => Number(item.memberCount || 0)), 1);
});

const maxTrendCount = computed(() => {
  const list = overview.value?.recentTrend || [];
  if (!list.length) return 1;
  return Math.max(...list.map((item: Record<string, any>) => Number(item.newMemberCount || 0)), 1);
});

async function loadData() {
  loading.value = true;
  try {
    const { data, error } = await fetchMemberOverview();
    if (!error && data) {
      overview.value = data;
    }
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpin :show="loading">
    <NSpace vertical :size="12">
      <NCard :bordered="false" class="stat-hero">
        <div class="stat-hero__eyebrow">MEMBER INSIGHT</div>
        <div class="stat-hero__head">
          <div>
            <h2 class="stat-hero__title">把会员规模、等级结构和新增趋势压缩到一个运营面板</h2>
            <p class="stat-hero__desc">适合运营日常巡检会员资产，看活跃量、看新增走势，也能快速判断当前等级结构是否健康。</p>
          </div>
          <NButton type="primary" @click="loadData">刷新数据</NButton>
        </div>
      </NCard>

      <div class="metric-grid">
        <NCard v-for="item in metricCards" :key="item.label" :bordered="false" class="metric-card" :data-tone="item.tone">
          <div class="metric-card__label">{{ item.label }}</div>
          <div class="metric-card__value">{{ item.value }}</div>
          <div class="metric-card__note">{{ item.note }}</div>
        </NCard>
      </div>

      <div class="asset-grid">
        <NCard v-for="item in assetCards" :key="item.label" :bordered="false" class="asset-card">
          <div class="asset-card__label">{{ item.label }}</div>
          <div class="asset-card__value">
            <span v-if="item.prefix">{{ item.prefix }}</span>{{ item.value }}<span v-if="item.suffix">{{ item.suffix }}</span>
          </div>
        </NCard>
      </div>

      <div class="panel-grid">
        <NCard :bordered="false" title="等级分布" class="panel-card">
          <template v-if="overview?.levelDistribution?.length">
            <div v-for="item in overview?.levelDistribution" :key="item.levelId" class="bar-row">
              <div class="bar-row__meta">
                <span class="bar-row__name">{{ item.levelName }}</span>
                <NTag size="small" type="info">{{ item.memberCount }} 人</NTag>
              </div>
              <div class="bar-row__track">
                <div class="bar-row__fill" :style="{ width: `${(Number(item.memberCount || 0) / maxLevelCount) * 100}%` }"></div>
              </div>
            </div>
          </template>
          <NEmpty v-else description="暂无等级分布数据" />
        </NCard>

        <NCard :bordered="false" title="近7天新增趋势" class="panel-card">
          <template v-if="overview?.recentTrend?.length">
            <div class="trend-list">
              <div v-for="item in overview?.recentTrend" :key="item.date" class="trend-item">
                <div class="trend-item__date">{{ item.date }}</div>
                <div class="trend-item__bar">
                  <div class="trend-item__fill" :style="{ height: `${Math.max((Number(item.newMemberCount || 0) / maxTrendCount) * 100, 8)}%` }"></div>
                </div>
                <div class="trend-item__value">{{ item.newMemberCount }}</div>
              </div>
            </div>
          </template>
          <NEmpty v-else description="暂无新增趋势数据" />
        </NCard>
      </div>
    </NSpace>
  </NSpin>
</template>

<style scoped>
.stat-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.stat-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.stat-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.stat-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.stat-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.metric-grid,
.asset-grid,
.panel-grid {
  display: grid;
  gap: 16px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.asset-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.panel-grid {
  grid-template-columns: 1.2fr 1fr;
}

.metric-card,
.asset-card,
.panel-card {
  box-shadow: 0 16px 38px rgba(16, 33, 61, 0.06);
}

.metric-card__label,
.asset-card__label {
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.metric-card__value,
.asset-card__value {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 700;
  color: #123055;
}

.metric-card__note {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(21, 44, 76, 0.56);
}

.metric-card[data-tone='primary'] .metric-card__value {
  color: #0f6fff;
}

.metric-card[data-tone='success'] .metric-card__value {
  color: #2f8f6b;
}

.metric-card[data-tone='warning'] .metric-card__value {
  color: #c96a3d;
}

.metric-card[data-tone='info'] .metric-card__value {
  color: #1b5dbf;
}

.bar-row + .bar-row {
  margin-top: 16px;
}

.bar-row__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.bar-row__name {
  font-weight: 700;
  color: #123055;
}

.bar-row__track {
  height: 14px;
  border-radius: 999px;
  background: #edf3fb;
  overflow: hidden;
}

.bar-row__fill {
  height: 100%;
  min-width: 8px;
  border-radius: 999px;
  background: linear-gradient(90deg, #0f6fff 0%, #6da9ff 100%);
}

.trend-list {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
  align-items: end;
  min-height: 280px;
}

.trend-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.trend-item__date {
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.trend-item__bar {
  display: flex;
  align-items: end;
  justify-content: center;
  width: 100%;
  height: 180px;
  padding: 0 8px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(238, 244, 255, 0.9), rgba(247, 250, 255, 0.9));
}

.trend-item__fill {
  width: 100%;
  min-height: 8%;
  border-radius: 14px 14px 6px 6px;
  background: linear-gradient(180deg, #2f8f6b 0%, #85cfb0 100%);
}

.trend-item__value {
  font-size: 13px;
  font-weight: 700;
  color: #123055;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .asset-grid,
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
