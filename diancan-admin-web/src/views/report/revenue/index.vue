<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NCard, NSpace, NButton, NDatePicker, NRadioGroup, NRadioButton, NDataTable, NGrid, NGi, NStatistic, useMessage } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { useEcharts } from '@/hooks/common/echarts';
import type { ECOption } from '@/hooks/common/echarts';
import { fetchRevenue, exportRevenue } from '@/service/api';
import { CHART_COLORS } from '@/constants/chart-colors';

defineOptions({ name: 'ReportRevenue' });

const message = useMessage();
const loading = ref(false);
const data = ref<Api.Business.Revenue[]>([]);
const dimension = ref('day');
const dateRange = ref<[number, number] | null>(null);

const columns: DataTableColumns<Api.Business.Revenue> = [
  { title: '日期', key: 'date', width: 150 },
  { title: '营业额', key: 'totalRevenue', width: 150, render(row) { return `¥${row.totalRevenue}`; } },
  { title: '订单数', key: 'orderCount', width: 100 }
];

// 营业额折线图
const { domRef: revenueChartRef, updateOptions: updateRevenueChart } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['营业额', '订单数'] },
  grid: { left: 60, right: 40, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: '营业额(¥)', position: 'left' },
    { type: 'value', name: '订单数', position: 'right' }
  ],
  series: [
    { name: '营业额', type: 'bar', data: [], itemStyle: { color: CHART_COLORS.primary } },
    { name: '订单数', type: 'line', yAxisIndex: 1, data: [], smooth: true, itemStyle: { color: CHART_COLORS.info } }
  ]
}));

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function getDateParams() {
  if (!dateRange.value) {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 6);
    return { startDate: formatDate(start.getTime()), endDate: formatDate(end.getTime()) };
  }
  return { startDate: formatDate(dateRange.value[0]), endDate: formatDate(dateRange.value[1]) };
}

function updateChart() {
  updateRevenueChart(opts => {
    const xAxis: any = opts.xAxis || {};
    xAxis.data = data.value.map(d => d.date);
    const series = Array.isArray(opts.series) ? opts.series : [];
    const s0: any = series[0] || {};
    const s1: any = series[1] || {};
    s0.data = data.value.map(d => d.totalRevenue);
    s1.data = data.value.map(d => d.orderCount);
    return { ...opts, xAxis, series: [s0, s1] };
  });
}

async function loadData() {
  loading.value = true;
  try {
    const params = { dimension: dimension.value, ...getDateParams() };
    const { data: result, error } = await fetchRevenue(params);
    if (!error && result) {
      data.value = result;
      updateChart();
    }
  } finally { loading.value = false; }
}

async function handleExport() {
  const params = { dimension: dimension.value, ...getDateParams() };
  try {
    await exportRevenue(params);
    message.success('导出成功');
  } catch (error) {
    const msg = error instanceof Error ? error.message : '导出失败';
    message.error(msg);
  }
}

// 汇总统计
function totalRevenue() {
  return data.value.reduce((sum, d) => sum + Number(d.totalRevenue || 0), 0).toFixed(2);
}
function totalOrders() {
  return data.value.reduce((sum, d) => sum + Number(d.orderCount || 0), 0);
}
function avgRevenue() {
  if (data.value.length === 0) return '0.00';
  return (Number(totalRevenue()) / data.value.length).toFixed(2);
}

onMounted(() => { loadData(); });
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="report-hero">
      <div class="report-hero__eyebrow">REVENUE REPORT</div>
      <div class="report-hero__head">
        <div>
          <h2 class="report-hero__title">把营业额趋势、订单量和明细统计统一到一张经营总览里</h2>
          <p class="report-hero__desc">适合看短期经营波动、日周月节奏变化，以及导出复盘数据，不需要再切多个报表入口。</p>
        </div>
        <div class="report-hero__badge">
          <span>统计周期记录</span>
          <strong>{{ data.length }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="营业额统计">
      <template #header-extra>
        <NSpace>
          <NRadioGroup v-model:value="dimension" @update:value="loadData">
            <NRadioButton value="day">按日</NRadioButton>
            <NRadioButton value="week">按周</NRadioButton>
            <NRadioButton value="month">按月</NRadioButton>
          </NRadioGroup>
          <NDatePicker v-model:value="dateRange" type="daterange" clearable @update:value="loadData" />
          <NButton type="primary" @click="handleExport">导出Excel</NButton>
        </NSpace>
      </template>

      <!-- 汇总卡片 -->
      <NGrid :cols="3" :x-gap="16" style="margin-bottom: 16px;">
        <NGi>
          <NCard :bordered="false" class="revenue-metric-card revenue-metric-card--primary">
            <NStatistic label="总营业额">
              <template #prefix>¥</template>
              {{ totalRevenue() }}
            </NStatistic>
          </NCard>
        </NGi>
        <NGi>
          <NCard :bordered="false" class="revenue-metric-card revenue-metric-card--info">
            <NStatistic label="总订单数" :value="totalOrders()" />
          </NCard>
        </NGi>
        <NGi>
          <NCard :bordered="false" class="revenue-metric-card revenue-metric-card--neutral">
            <NStatistic label="日均营业额">
              <template #prefix>¥</template>
              {{ avgRevenue() }}
            </NStatistic>
          </NCard>
        </NGi>
      </NGrid>

      <!-- 图表 -->
      <div ref="revenueChartRef" style="height: 380px; width: 100%;" />
    </NCard>

    <!-- 明细表格 -->
    <NCard :bordered="false" title="明细数据">
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.report-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.report-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.report-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.report-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.report-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.report-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.report-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.report-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}

.revenue-metric-card {
  border-top: 4px solid rgba(var(--admin-accent-rgb), 0.85);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 247, 255, 0.98)) !important;
}

.revenue-metric-card--primary {
  border-top-color: var(--admin-accent-strong);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 32%),
    linear-gradient(180deg, rgba(244, 249, 255, 0.98), rgba(232, 241, 255, 0.98)) !important;
}

.revenue-metric-card--info {
  border-top-color: color-mix(in srgb, var(--admin-accent-strong) 72%, #46b4ff);
}

.revenue-metric-card--neutral {
  border-top-color: color-mix(in srgb, var(--admin-accent-strong) 48%, #44556f);
}

.revenue-metric-card :deep(.n-statistic__label) {
  color: rgba(21, 44, 76, 0.66);
}

.revenue-metric-card :deep(.n-statistic-value),
.revenue-metric-card :deep(.n-statistic-value__content),
.revenue-metric-card :deep(.n-statistic-value__prefix),
.revenue-metric-card :deep(.n-statistic-value__suffix) {
  color: #123055;
}

html.dark .revenue-metric-card {
  border-top-color: rgba(var(--admin-accent-rgb), 0.72);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 28%),
    linear-gradient(180deg, rgba(9, 13, 21, 0.98), rgba(14, 18, 27, 0.98)) !important;
  box-shadow:
    0 20px 34px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .revenue-metric-card :deep(.n-statistic__label) {
  color: rgba(183, 198, 228, 0.72);
}

html.dark .revenue-metric-card :deep(.n-statistic-value),
html.dark .revenue-metric-card :deep(.n-statistic-value__content),
html.dark .revenue-metric-card :deep(.n-statistic-value__prefix),
html.dark .revenue-metric-card :deep(.n-statistic-value__suffix) {
  color: rgba(241, 246, 255, 0.96);
}
</style>
