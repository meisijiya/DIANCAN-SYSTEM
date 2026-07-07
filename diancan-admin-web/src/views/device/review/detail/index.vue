<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NGi,
  NGrid,
  NImage,
  NRate,
  NSkeleton,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui';
import { useRoute, useRouter } from 'vue-router';
import { fetchOrderDetail, fetchReviewDetail } from '@/service/api';

defineOptions({ name: 'DeviceReviewDetail' });

const props = defineProps<{
  orderId?: string;
}>();
const emit = defineEmits<{
  back: [];
}>();

const route = useRoute();
const router = useRouter();
const message = useMessage();

const detailLoading = ref(false);
const detail = ref<Api.Business.ReviewDetail | null>(null);
const detailOrder = ref<Api.Business.OrderDetail | null>(null);

const detailItems = computed(() => {
  const review = detail.value;
  if (!review) return [];
  const orderItemMap = new Map((detailOrder.value?.items || []).map(item => [Number(item.id), item]));
  return review.itemReviews.map(item => {
    const orderItem = orderItemMap.get(Number(item.orderItemId));
    return {
      id: item.id,
      orderItemId: item.orderItemId,
      rating: item.rating,
      dishName: orderItem?.dishName || `订单项#${item.orderItemId}`,
      dishImage: orderItem?.dishImage || '',
      quantity: orderItem?.quantity || 1,
      amount: orderItem?.amount || 0
    };
  });
});

const ratingText = computed(() => {
  const score = detail.value?.overallRating || 0;
  if (score >= 4.5) return '非常满意';
  if (score >= 3.5) return '整体满意';
  if (score >= 2.5) return '体验一般';
  if (score > 0) return '需要改进';
  return '未评分';
});

const averageItemRating = computed(() => {
  if (!detailItems.value.length) return 0;
  const sum = detailItems.value.reduce((acc, cur) => acc + Number(cur.rating || 0), 0);
  return Number((sum / detailItems.value.length).toFixed(1));
});

const topRatedCount = computed(() => detailItems.value.filter(item => Number(item.rating) >= 4).length);
const reviewedDishCount = computed(() => detailItems.value.length);
const reviewedAmount = computed(() =>
  detailItems.value.reduce((sum, item) => sum + Number(item.amount || 0), 0).toFixed(2)
);

function getScoreType(score: number) {
  if (score >= 4) return 'success';
  if (score >= 3) return 'warning';
  return 'error';
}

function formatAmount(amount: number) {
  return Number(amount || 0).toFixed(2);
}

function getDishImage(item: { dishImage?: string }) {
  return item.dishImage || '';
}

async function loadData() {
  const orderId = String(props.orderId || route.query.orderId || '');
  if (!orderId) {
    message.warning('缺少订单ID，无法加载评价详情');
    return;
  }

  detailLoading.value = true;
  detail.value = null;
  detailOrder.value = null;
  try {
    const [{ data: reviewResult, error: reviewError }, { data: orderResult, error: orderError }] = await Promise.all([
      fetchReviewDetail(orderId),
      fetchOrderDetail(orderId)
    ]);

    if (reviewError || !reviewResult) {
      message.error('评价详情加载失败');
      return;
    }

    if (orderError || !orderResult) {
      message.warning('订单菜品信息加载失败，已展示评分主体信息');
    }

    detail.value = reviewResult;
    detailOrder.value = orderResult || null;
  } finally {
    detailLoading.value = false;
  }
}

onMounted(loadData);

function handleBack() {
  emit('back');
  if (!props.orderId) {
    router.back();
  }
}
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="review-topbar">
      <NSpace justify="space-between" align="center">
        <div>
          <div class="review-topbar__eyebrow">REVIEW INSPECTION</div>
          <div class="review-topbar__title">评论详情</div>
        </div>
        <NButton @click="handleBack">返回列表</NButton>
      </NSpace>
    </NCard>

    <NCard :bordered="false" class="review-detail-card" title="评价详情">
      <NSpace vertical :size="12">
        <template v-if="detailLoading">
          <NSkeleton text :repeat="2" />
          <NSkeleton height="120px" />
          <NGrid :cols="3" :x-gap="12">
            <NGi v-for="i in 3" :key="i"><NSkeleton height="240px" /></NGi>
          </NGrid>
        </template>

        <template v-else-if="detail">
          <div class="review-detail-layout">
            <div class="review-left-panel">
              <div class="review-hero">
                <div class="review-hero__head">
                  <div>
                    <div class="review-meta">订单 {{ detailOrder?.orderNo || `#${detail.orderId}` }}</div>
                    <div class="review-score-row">
                      <NRate :value="detail.overallRating" readonly size="medium" />
                      <span class="review-score-text">{{ ratingText }}</span>
                    </div>
                    <div class="review-note">{{ detail.content || '顾客未填写文字评价' }}</div>
                  </div>
                  <div class="review-focus">
                    <span>整体评分</span>
                    <strong>{{ detail.overallRating?.toFixed(1) || '0.0' }}</strong>
                    <small>{{ reviewedDishCount }} 道菜参与评分</small>
                  </div>
                </div>
                <div class="review-badges">
                  <NTag type="success" size="small">单品均分 {{ averageItemRating }}</NTag>
                  <NTag type="info" size="small">高分菜品 {{ topRatedCount }}</NTag>
                  <NTag type="warning" size="small">评价时间 {{ detail.createTime }}</NTag>
                </div>
                <div class="review-metrics">
                  <div class="review-metrics__item">
                    <span>评分菜品</span>
                    <strong>{{ reviewedDishCount }}</strong>
                  </div>
                  <div class="review-metrics__item">
                    <span>高分菜品</span>
                    <strong>{{ topRatedCount }}</strong>
                  </div>
                  <div class="review-metrics__item">
                    <span>关联金额</span>
                    <strong>¥{{ reviewedAmount }}</strong>
                  </div>
                </div>
              </div>

              <NDescriptions bordered :column="1" label-placement="left" class="review-order-meta">
                <NDescriptionsItem label="订单ID">{{ detail.orderId }}</NDescriptionsItem>
                <NDescriptionsItem label="桌台">{{ detailOrder?.tableCode || '-' }}</NDescriptionsItem>
                <NDescriptionsItem label="区域">{{ detailOrder?.areaName || '未分区' }}</NDescriptionsItem>
                <NDescriptionsItem label="用户标识">{{ detail.customerOpenid || '-' }}</NDescriptionsItem>
              </NDescriptions>
            </div>

            <div class="review-right-panel">
              <NCard size="small" title="菜品评分">
                <NGrid v-if="detailItems.length" :cols="2" :x-gap="14" :y-gap="14">
                  <NGi v-for="item in detailItems" :key="item.id">
                    <div class="dish-review-item">
                      <NImage v-if="getDishImage(item)" class="dish-image" :src="getDishImage(item)" object-fit="cover" preview-disabled />
                      <div v-else class="dish-image dish-image-placeholder">{{ item.dishName }}</div>
                      <div class="dish-body">
                        <div class="dish-title">{{ item.dishName }}</div>
                        <div class="dish-sub">x{{ item.quantity }} · ¥{{ formatAmount(item.amount) }}</div>
                        <div class="dish-rate">
                          <NRate :value="item.rating" readonly size="small" />
                          <NTag :type="getScoreType(item.rating)" size="small">{{ item.rating }} 分</NTag>
                        </div>
                      </div>
                    </div>
                  </NGi>
                </NGrid>
                <NEmpty v-else description="暂无单品评分记录" />
              </NCard>
            </div>
          </div>
        </template>

        <NEmpty v-else description="未查询到评价详情" />
      </NSpace>
    </NCard>
  </NSpace>
</template>

<style scoped>
.review-topbar {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.16), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(232, 242, 255, 0.98)) !important;
}

.review-topbar__eyebrow {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.22em;
  color: rgba(15, 62, 124, 0.68);
}

.review-topbar__title {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: #123055;
}

.review-detail-card {
  border: 1px solid #e8ecf5;
  box-shadow: 0 12px 24px rgb(15 23 42 / 8%);
}

.review-detail-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 16px;
}

.review-left-panel {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-right-panel :deep(.n-card) {
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
  border: 1px solid #e8edf5;
}

.review-hero {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(120deg, #f8fbff 0%, #f5fff9 100%);
  border: 1px solid #e6eefc;
}

.review-hero__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.review-meta {
  font-size: 13px;
  color: #64748b;
}

.review-score-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.review-score-text {
  font-size: 14px;
  color: #0f172a;
  font-weight: 600;
}

.review-note {
  margin-top: 10px;
  color: #334155;
  line-height: 1.6;
}

.review-focus {
  min-width: 128px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(15, 111, 255, 0.1);
  box-shadow:
    0 16px 30px rgba(15, 57, 119, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.84);
}

.review-focus span,
.review-focus strong,
.review-focus small {
  display: block;
}

.review-focus span,
.review-focus small {
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.review-focus strong {
  margin-top: 8px;
  font-size: 30px;
  line-height: 1;
  color: #0f6fff;
}

.review-focus small {
  margin-top: 8px;
}

.review-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.review-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.review-metrics__item {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.68);
  border: 1px solid rgba(15, 111, 255, 0.08);
}

.review-metrics__item span,
.review-metrics__item strong {
  display: block;
}

.review-metrics__item span {
  font-size: 12px;
  color: rgba(21, 44, 76, 0.62);
}

.review-metrics__item strong {
  margin-top: 8px;
  font-size: 22px;
  line-height: 1.1;
  color: #123055;
}

.review-order-meta {
  overflow: hidden;
  border-radius: 16px;
}

.dish-review-item {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  background-color: #fff;
  height: 100%;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.dish-review-item:hover {
  transform: translateY(-4px);
  border-color: #cfe0ff;
  box-shadow: 0 10px 20px rgb(30 64 175 / 12%);
}

.dish-image {
  width: 100%;
  height: 140px;
}

.dish-body {
  padding: 10px 12px 12px;
}

.dish-image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
  background: linear-gradient(120deg, #f8fafc 0%, #f1f5f9 100%);
}

.dish-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.dish-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.dish-rate {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 1200px) {
  .review-detail-layout {
    grid-template-columns: 1fr;
  }

  .review-left-panel {
    position: static;
  }

  .review-hero {
    gap: 12px;
  }

  .review-hero__head {
    flex-direction: column;
  }

  .review-focus {
    min-width: 0;
    width: 100%;
  }

  .review-metrics {
    grid-template-columns: 1fr;
  }

  .review-badges {
    align-items: flex-start;
  }

  .review-right-panel :deep(.n-grid) {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}

html.dark .review-topbar {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(8, 12, 20, 0.98), rgba(14, 19, 30, 0.98)) !important;
}

html.dark .review-topbar__eyebrow,
html.dark .review-focus span,
html.dark .review-focus small,
html.dark .review-metrics__item span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .review-topbar__title,
html.dark .review-focus strong,
html.dark .review-metrics__item strong,
html.dark .dish-title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .review-focus,
html.dark .review-metrics__item,
html.dark .dish-review-item {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow:
    0 18px 30px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .dish-sub,
html.dark .dish-image-placeholder {
  color: rgba(173, 188, 216, 0.72);
}

html.dark .dish-image-placeholder {
  background: linear-gradient(120deg, rgba(12, 17, 28, 0.96), rgba(8, 12, 20, 0.96));
}
</style>
