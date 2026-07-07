<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { NCard, NSpace, NDatePicker, NInputNumber, NButton, NDataTable, NGrid, NGi } from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { useEcharts } from '@/hooks/common/echarts';
import type { ECOption } from '@/hooks/common/echarts';
import { fetchDishRanking } from '@/service/api';
import { CHART_COLORS } from '@/constants/chart-colors';

defineOptions({ name: 'ReportDishRanking' });

const loading = ref(false);
const data = ref<Api.Business.DishRanking[]>([]);
const dateRange = ref<[number, number] | null>(null);
const limit = ref(20);

const columns: DataTableColumns<Api.Business.DishRanking> = [
  { title: '排名', key: 'rank', width: 60, render(_row, index) { return index + 1; } },
  { title: '菜品名称', key: 'dishName', width: 200 },
  { title: '销量', key: 'totalQuantity', width: 100 },
  { title: '销售额', key: 'totalAmount', width: 120, render(row) { return `¥${row.totalAmount}`; } }
];

// 销量横向柱状图
const { domRef: quantityChartRef, updateOptions: updateQuantityChart } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 120, right: 40, top: 20, bottom: 30 },
  xAxis: { type: 'value', name: '销量' },
  yAxis: { type: 'category', data: [], inverse: true },
  color: CHART_COLORS.palette,
  series: [{ type: 'bar', data: [], barMaxWidth: 24, itemStyle: { color: CHART_COLORS.primary } }]
}));

// 销售额饼图
const { domRef: amountChartRef, updateOptions: updateAmountChart } = useEcharts<ECOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
  legend: { type: 'scroll', bottom: 0 },
  color: CHART_COLORS.palette,
  series: [{
    type: 'pie', radius: ['35%', '65%'], avoidLabelOverlap: true,
    label: { formatter: '{b}: {d}%' }, data: []
  }]
}));

function formatDate(ts: number) {
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function getDateParams() {
  if (!dateRange.value) {
    const end = new Date();
    const start = new Date();
    start.setDate(start.getDate() - 29);
    return { startDate: formatDate(start.getTime()), endDate: formatDate(end.getTime()) };
  }
  return { startDate: formatDate(dateRange.value[0]), endDate: formatDate(dateRange.value[1]) };
}

function updateCharts() {
  const top10 = data.value.slice(0, 10);

  updateQuantityChart(opts => {
    const yAxis: any = opts.yAxis || {};
    yAxis.data = top10.map(d => d.dishName).reverse();
    const series = Array.isArray(opts.series) ? opts.series : [];
    const s0: any = series[0] || {};
    s0.data = top10.map(d => d.totalQuantity).reverse();
    return { ...opts, yAxis, series: [s0] };
  });

  updateAmountChart(opts => {
    const series = Array.isArray(opts.series) ? opts.series : [];
    const s0: any = series[0] || {};
    s0.data = top10.map(d => ({ name: d.dishName, value: d.totalAmount }));
    return { ...opts, series: [s0] };
  });
}

async function loadData() {
  loading.value = true;
  try {
    const params = { ...getDateParams(), limit: limit.value };
    const { data: result, error } = await fetchDishRanking(params);
    if (!error && result) {
      data.value = result;
      updateCharts();
    }
  } finally { loading.value = false; }
}

onMounted(() => { loadData(); });
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="ranking-hero">
      <div class="ranking-hero__eyebrow">DISH PERFORMANCE</div>
      <div class="ranking-hero__head">
        <div>
          <h2 class="ranking-hero__title">把销量 Top10 和销售额占比做成一张菜品表现面板</h2>
          <p class="ranking-hero__desc">适合快速定位畅销菜、潜力菜和结构失衡的品类，为活动和菜单调整提供直接依据。</p>
        </div>
        <div class="ranking-hero__badge">
          <span>当前排行数</span>
          <strong>{{ data.length }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" title="菜品销售排行">
      <template #header-extra>
        <NSpace>
          <NDatePicker v-model:value="dateRange" type="daterange" clearable @update:value="loadData" />
          <NInputNumber v-model:value="limit" :min="5" :max="100" style="width: 100px" />
          <NButton type="primary" @click="loadData">查询</NButton>
        </NSpace>
      </template>

      <!-- 图表区域 -->
      <NGrid :cols="2" :x-gap="16">
        <NGi>
          <NCard :bordered="false" title="销量 Top10" size="small">
            <div ref="quantityChartRef" style="height: 380px; width: 100%;" />
          </NCard>
        </NGi>
        <NGi>
          <NCard :bordered="false" title="销售额占比" size="small">
            <div ref="amountChartRef" style="height: 380px; width: 100%;" />
          </NCard>
        </NGi>
      </NGrid>
    </NCard>

    <!-- 明细表格 -->
    <NCard :bordered="false" title="排行明细">
      <NDataTable :columns="columns" :data="data" :loading="loading" />
    </NCard>
  </NSpace>
</template>

<style scoped>
.ranking-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.ranking-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.ranking-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.ranking-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.ranking-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.ranking-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.ranking-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.ranking-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}
</style>
