<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NSpace,
  NInput,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NPopconfirm,
  NModal,
  NSwitch,
  NTag,
  useMessage
} from 'naive-ui';
import type { DataTableColumns } from 'naive-ui';
import { fetchKitchenAutoAcceptEnabled, updateKitchenAutoAcceptEnabled } from '@/service/api/kitchen';
import { fetchConfigList, createConfig, updateConfig, deleteConfig } from '@/service/api';

defineOptions({
  name: 'ManageConfig'
});

const message = useMessage();
const loading = ref(false);
const searchForm = ref<Api.System.ConfigQuery>({
  name: '',
  configKey: '',
  pageNum: 1,
  pageSize: 10
});

const data = ref<Api.System.Config[]>([]);
const total = ref(0);
const switchLoadingMap = ref<Record<number, boolean>>({});
const KITCHEN_AUTO_ACCEPT_KEY = 'kitchen.autoAccept';

const CONFIG_CATEGORY_PRESETS = [
  { key: 'all', label: '全部配置', name: '', prefix: '' },
  { key: 'dashboard', label: '首页看板', name: '首页', prefix: 'dashboard.' },
  { key: 'kitchen', label: '后厨配置', name: '后厨', prefix: 'kitchen.' },
  { key: 'member', label: '会员配置', name: '会员', prefix: 'member.' },
  { key: 'payment', label: '支付配置', name: '支付', prefix: 'payment.' },
  { key: 'print', label: '打印配置', name: '打印', prefix: 'print.' },
  { key: 'system', label: '系统配置', name: '系统', prefix: 'sys.' }
] as const;

type ConfigCategoryKey = (typeof CONFIG_CATEGORY_PRESETS)[number]['key'];

// 编辑弹窗
const showModal = ref(false);
const modalTitle = ref('新增配置');
const formData = ref<Api.System.ConfigCreate & { id?: number }>({
  name: '',
  configKey: '',
  configValue: '',
  remark: ''
});

const isKitchenAutoAcceptConfigEditing = computed(() => formData.value.configKey === KITCHEN_AUTO_ACCEPT_KEY);

function isSwitchConfig(configKey: string) {
  return configKey === KITCHEN_AUTO_ACCEPT_KEY;
}

function parseSwitchValue(value: string | null | undefined) {
  return value === 'true' || value === '1';
}

function formatSwitchValue(enabled: boolean) {
  return enabled ? 'true' : 'false';
}

function resolveConfigCategory(configKey: string) {
  const matched = CONFIG_CATEGORY_PRESETS.find(item => item.key !== 'all' && configKey.startsWith(item.prefix));

  if (matched) {
    return {
      label: matched.label,
      type: matched.key === 'dashboard' ? 'info' : matched.key === 'kitchen' ? 'warning' : matched.key === 'member' ? 'success' : 'default'
    } as const;
  }

  return {
    label: '未分类',
    type: 'default'
  } as const;
}

const columns: DataTableColumns<Api.System.Config> = [
  { title: '配置名称', key: 'name', width: 150 },
  { title: '配置键', key: 'configKey', width: 180 },
  {
    title: '分类',
    key: 'category',
    width: 110,
    render(row) {
      const category = resolveConfigCategory(row.configKey);

      return h(
        NTag,
        { type: category.type, bordered: false },
        { default: () => category.label }
      );
    }
  },
  {
    title: '配置值',
    key: 'configValue',
    width: 200,
    render(row) {
      if (isSwitchConfig(row.configKey)) {
        return h(NSpace, { align: 'center', size: 8 }, {
          default: () => [
            h(NTag, { type: parseSwitchValue(row.configValue) ? 'success' : 'warning', bordered: false }, {
              default: () => (parseSwitchValue(row.configValue) ? '已开启' : '已关闭')
            }),
            h(NSwitch, {
              value: parseSwitchValue(row.configValue),
              loading: !!switchLoadingMap.value[row.id],
              'onUpdate:value': (value: boolean) => handleSwitchConfigChange(row, value)
            })
          ]
        });
      }

      return row.configValue || '-';
    }
  },
  { title: '备注', key: 'remark', width: 150 },
  { title: '创建时间', key: 'createTime', width: 180 },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleDelete(row.id) },
            {
              trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
              default: () => '确定删除该配置吗？'
            }
          )
        ]
      });
    }
  }
];

async function loadData() {
  loading.value = true;
  try {
    const { data: result, error } = await fetchConfigList(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
      await syncKitchenAutoAcceptConfigValue();
    }
  } finally {
    loading.value = false;
  }
}

async function syncKitchenAutoAcceptConfigValue() {
  const kitchenAutoAcceptConfig = data.value.find(item => item.configKey === KITCHEN_AUTO_ACCEPT_KEY);

  if (!kitchenAutoAcceptConfig) {
    return;
  }

  const { data: enabled, error } = await fetchKitchenAutoAcceptEnabled();

  if (!error) {
    kitchenAutoAcceptConfig.configValue = formatSwitchValue(!!enabled);
  }
}

function handleAdd() {
  modalTitle.value = '新增配置';
  formData.value = { name: '', configKey: '', configValue: '', remark: '' };
  showModal.value = true;
}

function handleEdit(row: Api.System.Config) {
  modalTitle.value = '编辑配置';
  formData.value = { ...row };
  showModal.value = true;
}

async function handleSubmit() {
  if (isKitchenAutoAcceptConfigEditing.value) {
    const enabled = parseSwitchValue(formData.value.configValue);
    const { error } = await updateKitchenAutoAcceptEnabled(enabled);

    if (!error) {
      message.success(enabled ? '已开启后厨自动接单' : '已关闭后厨自动接单');
      showModal.value = false;
      loadData();
    }

    return;
  }

  if (formData.value.id) {
    const { error } = await updateConfig(formData.value as Api.System.ConfigUpdate);
    if (!error) {
      message.success('更新成功');
      showModal.value = false;
      loadData();
    }
  } else {
    const { error } = await createConfig(formData.value);
    if (!error) {
      message.success('创建成功');
      showModal.value = false;
      loadData();
    }
  }
}

async function handleSwitchConfigChange(row: Api.System.Config, value: boolean) {
  if (!isSwitchConfig(row.configKey)) {
    return;
  }

  switchLoadingMap.value = {
    ...switchLoadingMap.value,
    [row.id]: true
  };

  try {
    const { error } = await updateKitchenAutoAcceptEnabled(value);

    if (!error) {
      row.configValue = formatSwitchValue(value);
      message.success(value ? '已开启后厨自动接单' : '已关闭后厨自动接单');
    }
  } finally {
    switchLoadingMap.value = {
      ...switchLoadingMap.value,
      [row.id]: false
    };
  }
}

async function handleDelete(id: number) {
  const { error } = await deleteConfig(id);
  if (!error) {
    message.success('删除成功');
    loadData();
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = { name: '', configKey: '', pageNum: 1, pageSize: 10 };
  loadData();
}

function handlePresetFilter(type: ConfigCategoryKey) {
  searchForm.value.pageNum = 1;
  const preset = CONFIG_CATEGORY_PRESETS.find(item => item.key === type) || CONFIG_CATEGORY_PRESETS[0];
  searchForm.value.name = preset.name;
  searchForm.value.configKey = preset.prefix;

  loadData();
}

function handlePageChange(page: number) {
  searchForm.value.pageNum = page;
  loadData();
}

function handlePageSizeChange(pageSize: number) {
  searchForm.value.pageSize = pageSize;
  searchForm.value.pageNum = 1;
  loadData();
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="config-hero">
      <div class="config-hero__eyebrow">系统配置</div>
      <div class="config-hero__head">
        <div>
          <h2 class="config-hero__title">把系统配置项集中维护在一张可检索的参数工作台里</h2>
          <p class="config-hero__desc">适合统一管理关键参数、业务开关和运行时配置，减少散落在代码和数据库里的维护成本。</p>
        </div>
        <div class="config-hero__badge">
          <span>当前配置数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="config-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="6">
            <NFormItem label="配置名称">
              <NInput v-model:value="searchForm.name" placeholder="请输入配置名称" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="6">
            <NFormItem label="配置键">
              <NInput v-model:value="searchForm.configKey" placeholder="请输入配置键，如 dashboard." clearable />
            </NFormItem>
          </NGi>
          <NGi :span="12">
            <NSpace justify="space-between" align="center" class="search-actions">
              <NSpace>
                <NButton v-for="item in CONFIG_CATEGORY_PRESETS" :key="item.key" quaternary @click="handlePresetFilter(item.key)">
                  {{ item.label }}
                </NButton>
              </NSpace>
              <NSpace>
                <NButton type="primary" @click="handleSearch">搜索</NButton>
                <NButton @click="handleReset">重置</NButton>
              </NSpace>
            </NSpace>
          </NGi>
          <NGi :span="24">
            <NSpace justify="end" class="search-actions">
              <NTag type="info" :bordered="false">推荐统一前缀：dashboard.、kitchen.、member.、payment.、print.、sys.</NTag>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>
    <NCard :bordered="false" title="配置列表" class="config-list-card">
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">新增配置</NButton>
      </template>
      <NDataTable
        remote
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="{
          page: searchForm.pageNum,
          pageSize: searchForm.pageSize,
          itemCount: total,
          showSizePicker: true,
          showQuickJumper: true,
          prefix: ({ itemCount, pageCount }) => `共 ${itemCount} 条 / ${pageCount} 页`,
          pageSizes: [10, 20, 50, 100, 200],
          onChange: handlePageChange,
          onUpdatePageSize: handlePageSizeChange
        }"
      />
    </NCard>

    <!-- 编辑弹窗 -->
    <NModal v-model:show="showModal" :title="modalTitle" preset="card" style="width: 500px">
      <NForm :model="formData" label-placement="left" label-width="80">
        <NFormItem label="配置名称" required>
          <NInput v-model:value="formData.name" placeholder="请输入配置名称" />
        </NFormItem>
        <NFormItem label="配置键" required>
          <NInput v-model:value="formData.configKey" placeholder="请输入配置键" />
        </NFormItem>
        <NFormItem label="配置值">
          <NSwitch
            v-if="isKitchenAutoAcceptConfigEditing"
            :value="parseSwitchValue(formData.configValue)"
            @update:value="value => (formData.configValue = formatSwitchValue(value))"
          />
          <NInput v-else v-model:value="formData.configValue" placeholder="请输入配置值" />
        </NFormItem>
        <NFormItem label="备注">
          <NInput v-model:value="formData.remark" type="textarea" placeholder="请输入备注" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">确定</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.config-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgb(var(--primary-50-color) / 0.92), rgb(var(--primary-100-color) / 0.9)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.config-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.config-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.config-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.config-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.config-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.config-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.config-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.search-actions {
  width: 100%;
}

.config-filter-card,
.config-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.98)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.config-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

html.dark .config-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .config-hero__eyebrow,
html.dark .config-hero__badge span {
  color: rgba(206, 216, 236, 0.72);
}

html.dark .config-hero__title,
html.dark .config-hero__badge strong {
  color: rgba(241, 246, 255, 0.96);
}

html.dark .config-hero__desc {
  color: rgba(206, 216, 236, 0.78);
}

html.dark .config-hero__badge {
  background: rgba(255, 255, 255, 0.04);
  border-color: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .config-filter-card,
html.dark .config-list-card {
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.1), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  border: 1px solid rgba(255, 255, 255, 0.06);
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>

