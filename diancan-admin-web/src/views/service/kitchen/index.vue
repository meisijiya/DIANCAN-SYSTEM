<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { NButton, NCard, NEmpty, NGrid, NGi, NInput, NSpin, NSpace, NSwitch, NTag, useMessage } from 'naive-ui';
import {
  acceptKitchenTask,
  completeKitchenTask,
  fetchKitchenTasks,
  fetchKitchenAutoAcceptEnabled,
  markKitchenDishSoldOut,
  updateKitchenAutoAcceptEnabled,
  type KitchenTask
} from '@/service/api';
import { connectWebSocket, subscribe, type WsMessage } from '@/service/websocket';

const message = useMessage();
const loading = ref(false);
const actionLoadingKey = ref<string>('');
const tasks = ref<KitchenTask[]>([]);
const rushItemIds = ref<Set<string>>(new Set());
const orderNoKeyword = ref('');
const tableCodeKeyword = ref('');
const autoAcceptEnabled = ref(false);
const autoAcceptSaving = ref(false);
const hasTaskSnapshot = ref(false);
const suppressNextTaskDiffNotify = ref(false);

const statusMap: Record<number, { label: string; type: 'warning' | 'info' | 'success' | 'default' }> = {
  0: { label: '待制作', type: 'warning' },
  1: { label: '制作中', type: 'info' },
  2: { label: '已完成', type: 'success' }
};

function parseKitchenTime(value?: string) {
  if (!value) return 0;
  const match = value.match(
    /^(\d{4})-(\d{2})-(\d{2})[T\s](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/
  );
  if (!match) {
    const timestamp = Date.parse(value);
    return Number.isNaN(timestamp) ? 0 : timestamp;
  }

  const [, year, month, day, hour, minute, second] = match;
  return new Date(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour),
    Number(minute),
    Number(second)
  ).getTime();
}

function formatKitchenTime(value?: string) {
  if (!value) return '-';
  const [datePart, timePart = ''] = value.replace('T', ' ').split(' ');
  if (!datePart) return value;
  return timePart ? `${datePart} ${timePart.slice(0, 8)}` : datePart;
}

const visibleTasks = computed(() => {
  const orderKeyword = orderNoKeyword.value.trim().toLowerCase();
  const tableKeyword = tableCodeKeyword.value.trim().toLowerCase();
  return [...tasks.value]
    .filter(task => {
      const matchOrder = !orderKeyword || (task.orderNo || '').toLowerCase().includes(orderKeyword);
      const matchTable = !tableKeyword || (task.tableCode || '').toLowerCase().includes(tableKeyword);
      return matchOrder && matchTable;
    })
    .sort((a, b) => parseKitchenTime(a.addedAt) - parseKitchenTime(b.addedAt));
});

const pendingCount = computed(() => tasks.value.filter(t => t.status === 0).length);
const cookingCount = computed(() => tasks.value.filter(t => t.status === 1).length);
const overtimeCount = computed(() => tasks.value.filter(t => t.overtime).length);
let lastVoiceAt = 0;
let pollTimer: number | null = null;

function playVoice(text: string, cooldownMs = 1200) {
  if (!('speechSynthesis' in window)) return;
  const now = Date.now();
  if (now - lastVoiceAt < cooldownMs) return;
  lastVoiceAt = now;
  const utter = new SpeechSynthesisUtterance(text);
  utter.lang = 'zh-CN';
  utter.rate = 1;
  window.speechSynthesis.speak(utter);
}

function markRush(itemId: string) {
  const next = new Set(rushItemIds.value);
  next.add(itemId);
  rushItemIds.value = next;
  window.setTimeout(() => {
    const after = new Set(rushItemIds.value);
    after.delete(itemId);
    rushItemIds.value = after;
  }, 3 * 60 * 1000);
}

function notifyNewKitchenTasks(nextTasks: KitchenTask[]) {
  if (suppressNextTaskDiffNotify.value) {
    suppressNextTaskDiffNotify.value = false;
    return;
  }

  const previousIds = new Set(tasks.value.map(task => String(task.id)));
  const newTasks = nextTasks.filter(task => !previousIds.has(String(task.id)));
  if (!newTasks.length || !hasTaskSnapshot.value) return;

  const latestTask = [...newTasks].sort((a, b) => parseKitchenTime(b.addedAt) - parseKitchenTime(a.addedAt))[0];
  const tableCode = latestTask.tableCode || '未知桌号';
  const quantity = latestTask.quantity || 1;
  if (autoAcceptEnabled.value) {
    message.success('您有新的堂食订单，已为您自动接单。');
    playVoice('您有新的堂食订单，已为您自动接单。');
    return;
  }

  message.info(`您有新的堂食订单，${tableCode}，${latestTask.dishName} ${quantity} 份，请及时接单。`);
  playVoice(`您有新的堂食订单，${tableCode}，${latestTask.dishName}${quantity}份，请及时接单。`);
}

function notifyNewOrderFromEvent(order: Api.Business.Order) {
  const firstItem = order.items?.[0];
  if (!firstItem) return;

  const tableCode = order.tableCode || '未知桌号';
  const quantity = firstItem.quantity || 1;
  suppressNextTaskDiffNotify.value = true;

  if (autoAcceptEnabled.value) {
    message.success('您有新的堂食订单，已为您自动接单。');
    playVoice('您有新的堂食订单，已为您自动接单。');
    return;
  }

  message.info(`您有新的堂食订单，${tableCode}，${firstItem.dishName} ${quantity} 份，请及时接单。`);
  playVoice(`您有新的堂食订单，${tableCode}，${firstItem.dishName}${quantity}份，请及时接单。`);
}

async function loadTasks() {
  loading.value = true;
  try {
    const { data, error } = await fetchKitchenTasks();
    if (!error && data) {
      notifyNewKitchenTasks(data);
      tasks.value = data;
      hasTaskSnapshot.value = true;
    }
  } finally {
    loading.value = false;
  }
}

async function loadAutoAcceptEnabled() {
  const { data, error } = await fetchKitchenAutoAcceptEnabled();
  if (!error) {
    autoAcceptEnabled.value = !!data;
  }
}

async function handleAutoAcceptChange(value: boolean) {
  autoAcceptSaving.value = true;
  try {
    const { error } = await updateKitchenAutoAcceptEnabled(value);
    if (!error) {
      autoAcceptEnabled.value = value;
      message.success(value ? '已开启自动接单' : '已关闭自动接单');
    }
  } finally {
    autoAcceptSaving.value = false;
  }
}

async function handleAccept(task: KitchenTask) {
  actionLoadingKey.value = `accept-${task.id}`;
  try {
    const { error } = await acceptKitchenTask(task.id);
    if (!error) {
      task.status = 1;
      message.success('已接单');
      playVoice(`已接单，${task.dishName}`);
    }
  } finally {
    actionLoadingKey.value = '';
  }
}

async function handleComplete(task: KitchenTask) {
  actionLoadingKey.value = `complete-${task.id}`;
  try {
    const { error } = await completeKitchenTask(task.id);
    if (!error) {
      task.status = 2;
      message.success('已划单');
      playVoice(`已划单，${task.dishName}`);
    }
  } finally {
    actionLoadingKey.value = '';
  }
}

async function handleSoldOut(task: KitchenTask) {
  actionLoadingKey.value = `soldout-${task.id}`;
  try {
    const { error } = await markKitchenDishSoldOut(task.dishId, 1);
    if (!error) {
      message.success(`已将「${task.dishName}」标记为估清`);
    }
  } finally {
    actionLoadingKey.value = '';
  }
}

function handleKitchenEvent(msg: WsMessage<any>) {
  if (msg.eventType === 'NEW_ORDER') {
    if (msg.data?.items?.length) {
      notifyNewOrderFromEvent(msg.data as Api.Business.Order);
    }
    loadTasks();
    return;
  }
  if (msg.eventType === 'RUSH_ORDER') {
    const itemId = msg?.data?.itemId;
    if (itemId !== null && itemId !== undefined) {
      markRush(String(itemId));
      message.warning(`订单催单提醒：桌号 ${msg?.data?.tableCode || '-'}，菜品 ${msg?.data?.dishName || ''}`);
      playVoice(`催单提醒，${msg?.data?.tableCode || '未知桌号'}，${msg?.data?.dishName || '菜品'}`);
    }
    loadTasks();
  }
}

let unsubscribeKitchen: (() => void) | null = null;

onMounted(() => {
  loadTasks();
  loadAutoAcceptEnabled();
  connectWebSocket();
  unsubscribeKitchen = subscribe('/topic/kitchen', handleKitchenEvent);
  pollTimer = window.setInterval(() => {
    loadTasks();
  }, 8000);
});

onUnmounted(() => {
  unsubscribeKitchen?.();
  if (pollTimer) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
});
</script>

<template>
  <NSpin :show="loading">
    <NSpace vertical :size="12">
      <NCard :bordered="false" class="kitchen-hero">
        <div class="kitchen-hero__eyebrow">KITCHEN FLOW</div>
        <div class="kitchen-hero__head">
          <div>
            <h2 class="kitchen-hero__title">把后厨任务、接单节奏和催单提醒放在一条制作流水线上</h2>
            <p class="kitchen-hero__desc">适合后厨在忙时快速判断积压情况、自动接单状态和异常催单，不用额外切换任务视角。</p>
          </div>
          <div class="kitchen-hero__stats">
            <div class="kitchen-hero__stat"><span>待制作</span><strong>{{ pendingCount }}</strong></div>
            <div class="kitchen-hero__stat"><span>制作中</span><strong>{{ cookingCount }}</strong></div>
            <div class="kitchen-hero__stat"><span>超时</span><strong>{{ overtimeCount }}</strong></div>
          </div>
        </div>
      </NCard>

      <NCard :bordered="false">
        <NSpace align="center" justify="space-between" :wrap="true">
          <NSpace :size="20">
            <div>待制作：{{ pendingCount }}</div>
            <div>制作中：{{ cookingCount }}</div>
            <div>超时：<span style="color: #d03050;">{{ overtimeCount }}</span></div>
          </NSpace>
          <NSpace align="center" :size="8">
            <NSpace align="center" :size="8" class="auto-accept-switch">
              <span style="font-size: 13px; color: #666;">自动接单</span>
              <span :class="['auto-accept-badge', autoAcceptEnabled ? 'is-on' : 'is-off']">
                {{ autoAcceptEnabled ? '已开启' : '已关闭' }}
              </span>
              <NSwitch
                :value="autoAcceptEnabled"
                :loading="autoAcceptSaving"
                @update:value="handleAutoAcceptChange"
              />
            </NSpace>
            <NInput
              v-model:value="orderNoKeyword"
              clearable
              size="small"
              placeholder="筛选订单号"
              style="width: 180px;"
            />
            <NInput
              v-model:value="tableCodeKeyword"
              clearable
              size="small"
              placeholder="筛选桌号"
              style="width: 140px;"
            />
            <NButton @click="loadTasks">刷新任务</NButton>
          </NSpace>
        </NSpace>
      </NCard>

      <NEmpty v-if="visibleTasks.length === 0" description="暂无后厨任务" />

      <NGrid v-else :cols="1" :y-gap="12">
        <NGi v-for="task in visibleTasks" :key="task.id">
          <NCard
            :bordered="true"
            :class="[
              'kitchen-task-card',
              { 'is-overtime': task.overtime, 'is-rush': rushItemIds.has(String(task.id)) }
            ]"
          >
            <NSpace vertical :size="10">
              <NSpace align="center" justify="space-between">
                <NSpace align="center">
                  <NTag :type="statusMap[task.status]?.type || 'default'">{{ statusMap[task.status]?.label || '未知' }}</NTag>
                  <NTag v-if="task.overtime" type="error">超时</NTag>
                  <NTag v-if="rushItemIds.has(String(task.id))" type="warning">催单</NTag>
                </NSpace>
                <div class="kitchen-task-card__meta">下单时间：{{ formatKitchenTime(task.addedAt) }}</div>
              </NSpace>

              <div class="kitchen-task-card__body">
                <div>
                  <div class="kitchen-task-card__title">{{ task.dishName }} × {{ task.quantity }}</div>
                  <div class="kitchen-task-card__sub">桌号：{{ task.tableCode }} ｜ 订单：{{ task.orderNo }}</div>
                  <div v-if="task.remark" class="kitchen-task-card__remark">备注：{{ task.remark }}</div>
                </div>
                <NSpace>
                  <NButton
                    v-if="task.status === 0"
                    type="primary"
                    :loading="actionLoadingKey === `accept-${task.id}`"
                    @click="handleAccept(task)"
                  >
                    接单
                  </NButton>
                  <NButton
                    v-if="task.status === 1"
                    type="success"
                    :loading="actionLoadingKey === `complete-${task.id}`"
                    @click="handleComplete(task)"
                  >
                    划单
                  </NButton>
                  <NButton
                    secondary
                    type="warning"
                    :loading="actionLoadingKey === `soldout-${task.id}`"
                    @click="handleSoldOut(task)"
                  >
                    估清
                  </NButton>
                </NSpace>
              </div>
            </NSpace>
          </NCard>
        </NGi>
      </NGrid>
    </NSpace>
  </NSpin>
</template>

<style scoped>
.auto-accept-switch {
  padding: 4px 10px;
  border: 1px solid rgba(15, 111, 255, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
}

.auto-accept-badge {
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.auto-accept-badge.is-on {
  color: #2f8f6b;
  background: #edf7f1;
  animation: auto-accept-breathe 2.4s ease-in-out infinite;
}

.auto-accept-badge.is-off {
  color: #0f6fff;
  background: #e9f2ff;
}

@keyframes auto-accept-breathe {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(34, 197, 94, 0.08);
    transform: translateY(0);
  }

  50% {
    box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.14);
    transform: translateY(-1px);
  }
}

.kitchen-hero {
  background:
    radial-gradient(circle at top right, rgba(15, 111, 255, 0.18), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(227, 239, 255, 0.98)) !important;
}

.kitchen-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(15, 62, 124, 0.68);
}

.kitchen-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.kitchen-hero__title {
  margin: 0;
  font-size: 28px;
  color: #123055;
}

.kitchen-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: rgba(21, 44, 76, 0.72);
}

.kitchen-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
}

.kitchen-hero__stat {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(15, 111, 255, 0.12);
}

.kitchen-hero__stat span {
  display: block;
  font-size: 12px;
  color: rgba(15, 62, 124, 0.68);
}

.kitchen-hero__stat strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f6fff;
}

.kitchen-task-card {
  border-color: rgba(var(--admin-accent-rgb), 0.08) !important;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.04), transparent 24%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.98)) !important;
}

.kitchen-task-card.is-rush {
  border-color: rgba(226, 163, 53, 0.28) !important;
  background:
    radial-gradient(circle at top right, rgba(226, 163, 53, 0.12), transparent 24%),
    linear-gradient(180deg, rgba(255, 251, 242, 0.98), rgba(255, 246, 225, 0.98)) !important;
}

.kitchen-task-card.is-overtime {
  border-color: rgba(220, 76, 76, 0.26) !important;
  background:
    radial-gradient(circle at top right, rgba(220, 76, 76, 0.1), transparent 24%),
    linear-gradient(180deg, rgba(255, 246, 246, 0.98), rgba(255, 238, 238, 0.98)) !important;
}

.kitchen-task-card__meta {
  font-size: 12px;
  color: #6b7280;
}

.kitchen-task-card__body {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.kitchen-task-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.kitchen-task-card__sub {
  margin-top: 4px;
  color: #667085;
}

.kitchen-task-card__remark {
  margin-top: 4px;
  color: #c24141;
}

html.dark .kitchen-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.18), transparent 24%),
    linear-gradient(135deg, rgba(8, 12, 20, 0.98), rgba(14, 19, 30, 0.98)) !important;
}

html.dark .kitchen-hero__eyebrow {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .kitchen-hero__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .kitchen-hero__desc {
  color: rgba(206, 216, 236, 0.76);
}

html.dark .kitchen-hero__stat {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
}

html.dark .kitchen-hero__stat span {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .kitchen-hero__stat strong {
  color: #dbe5ff;
}

html.dark .kitchen-task-card {
  border-color: rgba(255, 255, 255, 0.06) !important;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(8, 11, 17, 0.98), rgba(12, 16, 24, 0.98)) !important;
  box-shadow:
    0 18px 30px rgba(0, 0, 0, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .kitchen-task-card.is-rush {
  border-color: rgba(213, 160, 76, 0.28) !important;
  background:
    radial-gradient(circle at top right, rgba(213, 160, 76, 0.14), transparent 24%),
    linear-gradient(180deg, rgba(24, 18, 10, 0.98), rgba(16, 13, 9, 0.98)) !important;
}

html.dark .kitchen-task-card.is-overtime {
  border-color: rgba(220, 90, 90, 0.26) !important;
  background:
    radial-gradient(circle at top right, rgba(220, 90, 90, 0.14), transparent 24%),
    linear-gradient(180deg, rgba(28, 12, 14, 0.98), rgba(19, 10, 11, 0.98)) !important;
}

html.dark .kitchen-task-card__meta,
html.dark .kitchen-task-card__sub {
  color: rgba(183, 198, 228, 0.68);
}

html.dark .kitchen-task-card__title {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .kitchen-task-card__remark {
  color: #ffb2ae;
}
</style>
