<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { NCard, NSpin } from 'naive-ui';
import { useEcharts } from '@/hooks/common/echarts';
import type { ECOption } from '@/hooks/common/echarts';
import { CHART_COLORS } from '@/constants/chart-colors';
import { fetchDashboardOverview } from '@/service/api';
import { useThemeStore } from '@/store/modules/theme';

defineOptions({ name: 'Home' });

interface MetricCard {
  label: string;
  value: string;
  helper: string;
  deltaText?: string;
  deltaTone?: 'up' | 'down' | 'flat';
}

const loading = ref(false);
const themeStore = useThemeStore();

const overview = ref<Api.Business.DashboardOverview | null>(null);
const revenueTrend = ref<Api.Business.Revenue[]>([]);
const dishRanking = ref<Api.Business.DishRanking[]>([]);
const tableStats = ref<Api.Business.DashboardTableStats>({
  total: 0,
  free: 0,
  occupied: 0,
  settled: 0,
  cleaning: 0
});

function toSafeNumber(value: unknown) {
  const num = Number(value);
  return Number.isFinite(num) ? num : 0;
}

function getChangeRate(current: number, previous: number) {
  if (!previous) {
    return current > 0 ? 100 : 0;
  }
  return ((current - previous) / previous) * 100;
}

function getDeltaTone(change: number): 'up' | 'down' | 'flat' {
  if (change > 0.01) return 'up';
  if (change < -0.01) return 'down';
  return 'flat';
}

function formatDelta(change: number, suffix = '%') {
  if (Math.abs(change) < 0.01) {
    return `持平 0.0${suffix}`;
  }
  const sign = change > 0 ? '+' : '';
  return `${sign}${change.toFixed(1)}${suffix}`;
}

function formatCurrency(value: number) {
  return `¥${value.toFixed(2)}`;
}

const averageTicket = computed(() => {
  return toSafeNumber(overview.value?.averageTicket);
});

const occupancyRate = computed(() => {
  return toSafeNumber(overview.value?.occupancyRate);
});

const tableStatusItems = computed(() => {
  const total = tableStats.value.total || 1;
  return [
    {
      label: '占用中',
      value: tableStats.value.occupied,
      percent: (tableStats.value.occupied / total) * 100,
      tone: 'primary'
    },
    {
      label: '空闲中',
      value: tableStats.value.free,
      percent: (tableStats.value.free / total) * 100,
      tone: 'success'
    },
    {
      label: '待清洁',
      value: tableStats.value.cleaning,
      percent: (tableStats.value.cleaning / total) * 100,
      tone: 'warning'
    },
    {
      label: '已结账',
      value: tableStats.value.settled,
      percent: (tableStats.value.settled / total) * 100,
      tone: 'neutral'
    }
  ];
});

const coreMetrics = computed<MetricCard[]>(() => {
  const todayRevenue = toSafeNumber(overview.value?.todayRevenue);
  const yesterdayRevenue = toSafeNumber(overview.value?.yesterdayRevenue);
  const todayOrderCount = toSafeNumber(overview.value?.todayOrderCount);
  const yesterdayOrderCount = toSafeNumber(overview.value?.yesterdayOrderCount);
  const todayTableTurnover = toSafeNumber(overview.value?.todayTableTurnover);
  const yesterdayTableTurnover = toSafeNumber(overview.value?.yesterdayTableTurnover);

  const revenueChange = getChangeRate(todayRevenue, yesterdayRevenue);
  const orderChange = getChangeRate(todayOrderCount, yesterdayOrderCount);
  const turnoverChange = getChangeRate(todayTableTurnover, yesterdayTableTurnover);

  return [
    {
      label: '今日营业额',
      value: formatCurrency(todayRevenue),
      helper: yesterdayRevenue ? `昨日 ${formatCurrency(yesterdayRevenue)}` : '暂无昨日对比',
      deltaText: formatDelta(revenueChange),
      deltaTone: getDeltaTone(revenueChange)
    },
    {
      label: '今日订单',
      value: `${todayOrderCount}`,
      helper: yesterdayOrderCount ? `昨日 ${yesterdayOrderCount} 单` : '暂无昨日对比',
      deltaText: formatDelta(orderChange),
      deltaTone: getDeltaTone(orderChange)
    },
    {
      label: '客单价',
      value: formatCurrency(averageTicket.value),
      helper: todayOrderCount ? `由 ${todayOrderCount} 笔订单计算` : '今天还没有订单'
    },
    {
      label: '桌台占用率',
      value: `${occupancyRate.value.toFixed(1)}%`,
      helper: `${tableStats.value.occupied}/${tableStats.value.total} 张桌台在使用`
    },
    {
      label: '今日翻台率',
      value: `${(todayTableTurnover * 100).toFixed(1)}%`,
      helper: yesterdayTableTurnover ? `昨日 ${(yesterdayTableTurnover * 100).toFixed(1)}%` : '暂无昨日对比',
      deltaText: formatDelta(turnoverChange),
      deltaTone: getDeltaTone(turnoverChange)
    }
  ];
});

const alertItems = computed(() => overview.value?.alerts || []);
const sessionMetrics = computed(() => overview.value?.sessionMetrics || []);

const quickActions = [
  { title: '服务员点单', desc: '快速录入堂食订单', short: '点', to: '/service/place-order' },
  { title: '桌台看板', desc: '处理占用与清洁状态', short: '台', to: '/service/table-board' },
  { title: '结账收银', desc: '完成支付与离台流程', short: '收', to: '/service/checkout' },
  { title: '订单中心', desc: '回看订单与金额明细', short: '单', to: '/order/list' },
  { title: '营业额报表', desc: '看日周月趋势和导出', short: '报', to: '/report/revenue' }
];

const chartTextColor = computed(() => (themeStore.darkMode ? 'rgba(214, 224, 244, 0.72)' : '#6b7a90'));
const chartLineColor = computed(() => (themeStore.darkMode ? 'rgba(255,255,255,0.08)' : 'rgba(38, 74, 120, 0.12)'));

const { domRef: trendRef, updateOptions: updateTrend } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: chartTextColor.value }, data: ['营业额', '订单数'] },
  grid: { left: 56, right: 56, top: 48, bottom: 26 },
  xAxis: {
    type: 'category',
    data: [],
    axisLine: { lineStyle: { color: chartLineColor.value } },
    axisLabel: { color: chartTextColor.value }
  },
  yAxis: [
    {
      type: 'value',
      name: '营业额',
      splitLine: { lineStyle: { color: chartLineColor.value } },
      axisLabel: { color: chartTextColor.value }
    },
    {
      type: 'value',
      name: '订单数',
      splitLine: { show: false },
      axisLabel: { color: chartTextColor.value }
    }
  ],
  series: [
    { name: '营业额', type: 'bar', barWidth: 18, data: [], itemStyle: { color: CHART_COLORS.primary, borderRadius: [6, 6, 0, 0] } },
    { name: '订单数', type: 'line', yAxisIndex: 1, data: [], smooth: true, symbolSize: 7, itemStyle: { color: CHART_COLORS.info }, lineStyle: { width: 3 } }
  ]
}));

const { domRef: dishRef, updateOptions: updateDishChart } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 88, right: 26, top: 18, bottom: 20 },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: chartLineColor.value } },
    axisLabel: { color: chartTextColor.value }
  },
  yAxis: {
    type: 'category',
    data: [],
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: chartTextColor.value }
  },
  series: [
    {
      type: 'bar',
      data: [],
      barWidth: 16,
      label: { show: true, position: 'right', color: chartTextColor.value },
      itemStyle: { color: CHART_COLORS.primary, borderRadius: [0, 8, 8, 0] }
    }
  ]
}));

function updateCharts() {
  updateTrend(() => ({
    legend: { top: 0, textStyle: { color: chartTextColor.value }, data: ['营业额', '订单数'] },
    xAxis: {
      type: 'category',
      data: revenueTrend.value.map(item => item.date.slice(5)),
      axisLine: { lineStyle: { color: chartLineColor.value } },
      axisLabel: { color: chartTextColor.value }
    },
    yAxis: [
      {
        type: 'value',
        name: '营业额',
        splitLine: { lineStyle: { color: chartLineColor.value } },
        axisLabel: { color: chartTextColor.value }
      },
      {
        type: 'value',
        name: '订单数',
        splitLine: { show: false },
        axisLabel: { color: chartTextColor.value }
      }
    ],
    series: [
      {
        name: '营业额',
        type: 'bar',
        barWidth: 18,
        data: revenueTrend.value.map(item => toSafeNumber(item.totalRevenue)),
        itemStyle: { color: CHART_COLORS.primary, borderRadius: [6, 6, 0, 0] }
      },
      {
        name: '订单数',
        type: 'line',
        yAxisIndex: 1,
        data: revenueTrend.value.map(item => toSafeNumber(item.orderCount)),
        smooth: true,
        symbolSize: 7,
        itemStyle: { color: CHART_COLORS.info },
        lineStyle: { width: 3 }
      }
    ]
  }));

  updateDishChart(() => ({
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: chartLineColor.value } },
      axisLabel: { color: chartTextColor.value }
    },
    yAxis: {
      type: 'category',
      data: dishRanking.value.map(item => item.dishName),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: chartTextColor.value }
    },
    series: [
      {
        type: 'bar',
        data: dishRanking.value.map(item => toSafeNumber(item.totalQuantity)),
        barWidth: 16,
        label: { show: true, position: 'right', color: chartTextColor.value },
        itemStyle: { color: CHART_COLORS.primary, borderRadius: [0, 8, 8, 0] }
      }
    ]
  }));
}

async function loadData() {
  loading.value = true;

  try {
    const { data, error } = await fetchDashboardOverview();

    if (!error && data) {
      overview.value = data;
      revenueTrend.value = data.revenueTrend || [];
      dishRanking.value = data.dishRanking || [];
      tableStats.value = data.tableStats || {
        total: 0,
        free: 0,
        occupied: 0,
        settled: 0,
        cleaning: 0
      };
      updateCharts();
    }
  } finally {
    loading.value = false;
  }
}

watch(
  () => themeStore.darkMode,
  () => {
    updateCharts();
  }
);

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpin :show="loading">
    <div class="dashboard-page">
      <div class="dashboard-head">
        <div>
          <h2 class="dashboard-head__title">经营概览</h2>
          <p class="dashboard-head__desc">先看结果、再看趋势、最后处理异常。</p>
        </div>
        <router-link to="/report/revenue" class="dashboard-head__link">查看完整报表</router-link>
      </div>

      <section class="metric-ribbon">
        <div v-for="item in coreMetrics" :key="item.label" class="metric-ribbon__item">
          <div class="metric-ribbon__topline">
            <span class="metric-ribbon__label">{{ item.label }}</span>
            <em v-if="item.deltaText" :data-tone="item.deltaTone">{{ item.deltaText }}</em>
          </div>
          <strong class="metric-ribbon__value">{{ item.value }}</strong>
          <div class="metric-ribbon__meta">
            <span>{{ item.helper }}</span>
          </div>
        </div>
      </section>

      <section class="dashboard-grid dashboard-grid--main">
        <NCard :bordered="false" class="panel panel--trend">
          <template #header>
            <div class="panel-title-wrap">
              <span class="panel-title">近7日经营趋势</span>
              <span class="panel-subtitle">营业额和订单量放在同一张图看波动</span>
            </div>
          </template>
          <div class="session-strip">
            <div v-for="item in sessionMetrics" :key="item.label" class="session-strip__item">
              <div class="session-strip__topline">
                <strong>{{ item.label }}</strong>
                <span>{{ item.startTime }} - {{ item.endTime }}</span>
              </div>
              <div class="session-strip__main">
                <span>{{ formatCurrency(toSafeNumber(item.revenue)) }}</span>
                <em>{{ item.orderCount }} 单 / 客单 {{ formatCurrency(toSafeNumber(item.averageTicket)) }}</em>
              </div>
            </div>
          </div>
          <div ref="trendRef" class="chart chart--large" />
        </NCard>

        <NCard :bordered="false" class="panel panel--tables">
          <template #header>
            <div class="panel-title-wrap">
              <span class="panel-title">当前桌台态势</span>
              <span class="panel-subtitle">比饼图更直接，先看能不能接客</span>
            </div>
          </template>

          <div class="table-overview">
            <div class="table-overview__headline">
              <strong>{{ tableStats.total }}</strong>
              <span>当前纳入管理桌台</span>
            </div>
            <div class="table-overview__summary">
              <span>占用率 {{ occupancyRate.toFixed(1) }}%</span>
              <span>空闲 {{ tableStats.free }} 张</span>
              <span>待清洁 {{ tableStats.cleaning }} 张</span>
            </div>
          </div>

          <div class="status-list">
            <div v-for="item in tableStatusItems" :key="item.label" class="status-item">
              <div class="status-item__meta">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="status-item__bar">
                <div class="status-item__fill" :data-tone="item.tone" :style="{ width: `${item.percent}%` }" />
              </div>
              <small>{{ item.percent.toFixed(1) }}%</small>
            </div>
          </div>
        </NCard>
      </section>

      <section class="dashboard-grid dashboard-grid--sub">
        <NCard :bordered="false" class="panel">
          <template #header>
            <div class="panel-title-wrap">
              <span class="panel-title">今日菜品结构</span>
              <span class="panel-subtitle">用横向排行看爆品比用饼图更清楚</span>
            </div>
          </template>
          <div ref="dishRef" class="chart chart--medium" />
        </NCard>

        <NCard :bordered="false" class="panel">
          <template #header>
            <div class="panel-title-wrap">
              <span class="panel-title">经营提醒</span>
              <span class="panel-subtitle">首页直接提示今天该优先处理什么</span>
            </div>
          </template>

          <div class="alert-list">
            <div v-for="item in alertItems" :key="item.title" class="alert-item" :data-tone="item.tone">
              <div class="alert-item__main">
                <strong>{{ item.title }}</strong>
                <p>{{ item.detail }}</p>
              </div>
              <router-link :to="item.actionTo" class="alert-item__action">{{ item.actionLabel }}</router-link>
            </div>
          </div>
        </NCard>
      </section>

      <NCard :bordered="false" class="panel">
        <template #header>
          <div class="panel-title-wrap">
            <span class="panel-title">快捷工作台</span>
            <span class="panel-subtitle">高频操作保留轻入口，不再做厚重卡片</span>
          </div>
        </template>

        <div class="quick-actions">
          <router-link v-for="item in quickActions" :key="item.title" :to="item.to" class="quick-action">
            <span class="quick-action__badge">{{ item.short }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </router-link>
        </div>
      </NCard>
    </div>
  </NSpin>
</template>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 2px 10px;
}

.dashboard-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-head__title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #18283d;
}

.dashboard-head__desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #6c7b90;
}

.dashboard-head__link {
  color: #0f6fff;
  font-size: 13px;
  text-decoration: none;
}

.metric-ribbon {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  border: 1px solid rgba(18, 61, 121, 0.08);
  border-radius: 22px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 255, 0.98));
  box-shadow: 0 16px 36px rgba(19, 71, 135, 0.08);
}

.metric-ribbon__item {
  padding: 18px 20px;
  border-right: 1px solid rgba(18, 61, 121, 0.08);
}

.metric-ribbon__item:last-child {
  border-right: 0;
}

.metric-ribbon__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-ribbon__label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #748196;
  text-transform: uppercase;
}

.metric-ribbon__value {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  line-height: 1;
  color: #152946;
}

.metric-ribbon__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
  color: #6c7b90;
}

.metric-ribbon em {
  font-style: normal;
  font-weight: 700;
  font-size: 12px;
  white-space: nowrap;
}

.metric-ribbon em[data-tone='up'] {
  color: #1c8f65;
}

.metric-ribbon em[data-tone='down'] {
  color: #cb5b4e;
}

.metric-ribbon em[data-tone='flat'] {
  color: #76849a;
}

.dashboard-grid {
  display: grid;
  gap: 16px;
}

.dashboard-grid--main {
  grid-template-columns: minmax(0, 1.6fr) minmax(320px, 0.9fr);
}

.dashboard-grid--sub {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(246, 249, 255, 0.98));
  border: 1px solid rgba(18, 61, 121, 0.08);
  box-shadow: 0 16px 36px rgba(19, 71, 135, 0.07);
}

.panel-title-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.panel-title {
  font-size: 15px;
  font-weight: 700;
  color: #152946;
}

.panel-subtitle {
  font-size: 12px;
  color: #78879c;
}

.chart {
  width: 100%;
}

.session-strip {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 8px;
}

.session-strip__item {
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(17, 56, 112, 0.04);
  border: 1px solid rgba(18, 61, 121, 0.08);
}

.session-strip__topline,
.session-strip__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.session-strip__topline strong {
  font-size: 13px;
  color: #152946;
}

.session-strip__topline span,
.session-strip__main em {
  font-size: 12px;
  color: #6c7b90;
  font-style: normal;
}

.session-strip__main {
  margin-top: 8px;
}

.session-strip__main span {
  font-size: 20px;
  font-weight: 700;
  color: #152946;
}

.chart--large {
  height: 340px;
}

.chart--medium {
  height: 300px;
}

.table-overview {
  padding: 4px 0 8px;
}

.table-overview__headline {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.table-overview__headline strong {
  font-size: 34px;
  color: #152946;
}

.table-overview__headline span,
.table-overview__summary {
  color: #6c7b90;
}

.table-overview__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 10px;
  font-size: 13px;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 14px;
}

.status-item {
  display: grid;
  grid-template-columns: 80px 1fr 56px;
  align-items: center;
  gap: 12px;
}

.status-item__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-item__meta span,
.status-item small {
  font-size: 12px;
  color: #6c7b90;
}

.status-item__meta strong {
  font-size: 18px;
  color: #152946;
}

.status-item__bar {
  height: 10px;
  overflow: hidden;
  background: rgba(17, 56, 112, 0.08);
  border-radius: 999px;
}

.status-item__fill {
  height: 100%;
  border-radius: inherit;
}

.status-item__fill[data-tone='primary'] {
  background: linear-gradient(90deg, #4ab2ff, #0f6fff);
}

.status-item__fill[data-tone='success'] {
  background: linear-gradient(90deg, #65ce9d, #25a56f);
}

.status-item__fill[data-tone='warning'] {
  background: linear-gradient(90deg, #ffd073, #f0a11a);
}

.status-item__fill[data-tone='neutral'] {
  background: linear-gradient(90deg, #bcc7d7, #8090a6);
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid transparent;
}

.alert-item[data-tone='danger'] {
  background: #fff4f1;
  border-color: rgba(203, 91, 78, 0.16);
}

.alert-item[data-tone='warning'] {
  background: #fff8e8;
  border-color: rgba(240, 161, 26, 0.2);
}

.alert-item[data-tone='neutral'] {
  background: #f3f7ff;
  border-color: rgba(15, 111, 255, 0.12);
}

.alert-item__main strong {
  display: block;
  font-size: 14px;
  line-height: 1.4;
  color: #152946;
}

.alert-item__main p {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.65;
  color: #6c7b90;
}

.alert-item__action {
  flex: none;
  color: #0f6fff;
  font-size: 12px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 18px;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(18, 61, 121, 0.08);
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.quick-action:hover {
  transform: translateY(-2px);
  border-color: rgba(15, 111, 255, 0.18);
  box-shadow: 0 14px 30px rgba(15, 111, 255, 0.1);
}

.quick-action__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(180deg, #47b0ff, #0f6fff);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.quick-action strong {
  display: block;
  font-size: 14px;
  color: #152946;
}

.quick-action p {
  margin: 5px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: #6c7b90;
}

@media (max-width: 1280px) {
  .metric-ribbon {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .metric-ribbon__item:nth-child(3) {
    border-right: 0;
  }

  .quick-actions {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dashboard-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .metric-ribbon,
  .dashboard-grid--main,
  .dashboard-grid--sub,
  .quick-actions,
  .session-strip {
    grid-template-columns: 1fr;
  }

  .metric-ribbon__item {
    border-right: 0;
    border-bottom: 1px solid rgba(18, 61, 121, 0.08);
  }

  .metric-ribbon__item:last-child {
    border-bottom: 0;
  }

  .status-item {
    grid-template-columns: 72px 1fr 50px;
  }

  .alert-item {
    flex-direction: column;
  }
}

html.dark .dashboard-head__title,
html.dark .metric-ribbon__value,
html.dark .panel-title,
html.dark .session-strip__topline strong,
html.dark .session-strip__main span,
html.dark .table-overview__headline strong,
html.dark .status-item__meta strong,
html.dark .alert-item__main strong,
html.dark .quick-action strong {
  color: rgba(240, 244, 252, 0.96);
}

html.dark .dashboard-head__desc,
html.dark .panel-subtitle,
html.dark .metric-ribbon__label,
html.dark .metric-ribbon__meta,
html.dark .session-strip__topline span,
html.dark .session-strip__main em,
html.dark .table-overview__headline span,
html.dark .table-overview__summary,
html.dark .status-item__meta span,
html.dark .status-item small,
html.dark .alert-item__main p,
html.dark .quick-action p {
  color: rgba(190, 202, 224, 0.72);
}

html.dark .metric-ribbon,
html.dark .panel,
html.dark .quick-action {
  background:
    linear-gradient(180deg, rgba(12, 17, 26, 0.98), rgba(16, 22, 34, 0.98));
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.24);
}

html.dark .session-strip__item {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .metric-ribbon__item {
  border-color: rgba(255, 255, 255, 0.06);
}

html.dark .status-item__bar {
  background: rgba(255, 255, 255, 0.08);
}

html.dark .alert-item[data-tone='danger'] {
  background: rgba(113, 38, 31, 0.34);
  border-color: rgba(203, 91, 78, 0.24);
}

html.dark .alert-item[data-tone='warning'] {
  background: rgba(104, 74, 16, 0.34);
  border-color: rgba(240, 161, 26, 0.24);
}

html.dark .alert-item[data-tone='neutral'] {
  background: rgba(20, 44, 78, 0.4);
  border-color: rgba(15, 111, 255, 0.2);
}

html.dark .quick-action:hover {
  box-shadow: 0 16px 30px rgba(0, 0, 0, 0.26);
}
</style>
