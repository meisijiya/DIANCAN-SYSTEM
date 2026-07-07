<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NCard, NSpace, NDatePicker, NButton, NDataTable, NGrid, NGi, NStatistic } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { useEcharts } from '@/hooks/common/echarts';
import type { ECOption } from '@/hooks/common/echarts';
import { fetchTableTurnover } from '@/service/api';
import { CHART_COLORS } from '@/constants/chart-colors';

defineOptions({ name: 'ReportTableTurnover' });

const loading = ref(false);
const data = ref<Api.Business.TableTurnover[]>([]);
const dateRange = ref<[number, number] | null>(null);

const columns: DataTableColumns<Api.Business.TableTurnover> = [
  { title: '日期', key: 'date', width: 150 },
  { title: '订单数', key: 'totalOrders', width: 100 },
  { title: '桌台总数', key: 'totalTables', width: 100 },
  { title: '翻台率', key: 'turnoverRate', width: 100, render(row) { return `${row.turnoverRate}次`; } }
];

// 翻台率趋势图
const { domRef: turnoverChartRef, updateOptions: updateTurnoverChart } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['翻台率', '订单数'] },
  grid: { left: 60, right: 50, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: [] },
  yAxis: [
    { type: 'value', name: '翻台率(次)', position: 'left' },
    { type: 'value', name: '订单数', position: 'right' }
  ],
  series: [
    { name: '翻台率', type: 'line', data: [], smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: CHART_COLORS.warning } },
    { name: '订单数', type: 'bar', yAxisIndex: 1, data: [], barMaxWidth: 24, itemStyle: { color: CHART_COLORS.info, opacity: 0.7 } }
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
  updateTurnoverChart(opts => {
    const xAxis: any = opts.xAxis || {};
    xAxis.data = data.value.map(d => d.date);
    const series = Array.isArray(opts.series) ? opts.series : [];
    const s0: any = series[0] || {};
    const s1: any = series[1] || {};
    s0.data = data.value.map(d => d.turnoverRate);
    s1.data = data.value.map(d => d.totalOrders);
    return { ...opts, xAxis, series: [s0, s1] };
  });
}

// 汇总
function avgTurnover() {
  if (data.value.length === 0) return '0.00';
  const sum = data.value.reduce((s, d) => s + Number(d.turnoverRate || 0), 0);
  return (sum / data.value.length).toFixed(2);
}
function totalOrders() {
  return data.value.reduce((s, d) => s + Number(d.totalOrders || 0), 0);
}

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchTableTurnover(getDateParams());
    if (!error && result) {
      data.value = result;
      updateChart();
    }
  } finally { loading.value = false; }
}

onMounted(() => { loadData(); });
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" title="翻台率统计">
      <template #header-extra>
        <NSpace>
          <NDatePicker v-model:value="dateRange" type="daterange" clearable @update:value="loadData" />
          <NButton type="primary" @click="loadData">查询</NButton>
        </NSpace>
      </template>

      <!-- 汇总卡片 -->
      <NGrid :cols="2" :x-gap="16" style="margin-bottom: 16px;">
        <NGi>
          <NCard :bordered="false" class="turnover-metric-card turnover-metric-card--warning">
            <NStatistic label="平均翻台率">
              <template #suffix>次/天</template>
              {{ avgTurnover() }}
            </NStatistic>
          </NCard>
        </NGi>
        <NGi>
          <NCard :bordered="false" class="turnover-metric-card turnover-metric-card--primary">
            <NStatistic label="总订单数" :value="totalOrders()" />
          </NCard>
        </NGi>
      </NGrid>

      <!-- 图表 -->
      <div ref="turnoverChartRef" style="height: 380px; width: 100%;" />
    </NCard>

    <!-- 明细表格 -->
    <NCard :bordered="false" title="明细数据">
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.turnover-metric-card {
  border-left: 4px solid rgba(var(--admin-accent-rgb), 0.84);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(243, 248, 255, 0.98)) !important;
}

.turnover-metric-card--warning {
  border-left-color: #e2a335;
  background:
    radial-gradient(circle at top right, rgba(226, 163, 53, 0.12), transparent 30%),
    linear-gradient(180deg, rgba(255, 252, 244, 0.98), rgba(255, 247, 228, 0.98)) !important;
}

.turnover-metric-card--primary {
  border-left-color: var(--admin-accent-strong);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 30%),
    linear-gradient(180deg, rgba(244, 249, 255, 0.98), rgba(231, 241, 255, 0.98)) !important;
}

.turnover-metric-card :deep(.n-statistic__label) {
  color: rgba(21, 44, 76, 0.66);
}

.turnover-metric-card :deep(.n-statistic-value),
.turnover-metric-card :deep(.n-statistic-value__content),
.turnover-metric-card :deep(.n-statistic-value__prefix),
.turnover-metric-card :deep(.n-statistic-value__suffix) {
  color: #123055;
}

html.dark .turnover-metric-card {
  border-left-color: rgba(var(--admin-accent-rgb), 0.72);
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.12), transparent 28%),
    linear-gradient(180deg, rgba(9, 13, 21, 0.98), rgba(14, 18, 27, 0.98)) !important;
  box-shadow:
    0 20px 34px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .turnover-metric-card--warning {
  border-left-color: #d5a04c;
  background:
    radial-gradient(circle at top right, rgba(213, 160, 76, 0.16), transparent 28%),
    linear-gradient(180deg, rgba(22, 17, 10, 0.98), rgba(14, 12, 8, 0.98)) !important;
}

html.dark .turnover-metric-card :deep(.n-statistic__label) {
  color: rgba(183, 198, 228, 0.72);
}

html.dark .turnover-metric-card :deep(.n-statistic-value),
html.dark .turnover-metric-card :deep(.n-statistic-value__content),
html.dark .turnover-metric-card :deep(.n-statistic-value__prefix),
html.dark .turnover-metric-card :deep(.n-statistic-value__suffix) {
  color: rgba(241, 246, 255, 0.96);
}
</style>
