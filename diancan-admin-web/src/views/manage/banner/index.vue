<script setup lang="ts">
import { h, onMounted, ref } from 'vue';
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NImage,
  NInput,
  NInputNumber,
  NModal,
  NPopconfirm,
  NSelect,
  NSpace,
  NTag,
  NUpload,
  useMessage
} from 'naive-ui';
import type { DataTableColumns, SelectOption, UploadCustomRequestOptions } from 'naive-ui';
import { createBanner, fetchBannerPage, updateBanner, updateBannerStatus, uploadBannerImage } from '@/service/api';

defineOptions({
  name: 'ManageBanner'
});

const message = useMessage();
const loading = ref(false);
const showModal = ref(false);
const isEdit = ref(false);
const total = ref(0);
const data = ref<Api.Banner.HomeBanner[]>([]);
const uploadLoading = ref(false);
const previewImageUrl = ref('');

const searchForm = ref<Api.Banner.HomeBannerQuery>({
  title: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const formModel = ref<Api.Banner.HomeBannerSubmit & { id?: number }>({
  title: '',
  subtitle: '',
  imageUrl: '',
  actionType: 0,
  targetPath: '',
  sort: 0,
  status: 1
});

const statusOptions: SelectOption[] = [
  { label: '全部', value: undefined },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];

const actionOptions: SelectOption[] = [
  { label: '不跳转', value: 0 },
  { label: '页面跳转', value: 1 },
  { label: '切换Tab', value: 2 }
];

const columns: DataTableColumns<Api.Banner.HomeBanner> = [
  { title: '标题', key: 'title', width: 170 },
  { title: '副标题', key: 'subtitle', width: 220 },
  {
    title: '图片',
    key: 'imageUrl',
    width: 110,
    render(row) {
      return h(NImage, {
        src: row.imageUrl,
        width: 64,
        height: 40,
        objectFit: 'cover',
        style: 'border-radius: 10px; border: 1px solid #e5e7eb;'
      });
    }
  },
  {
    title: '动作',
    key: 'actionType',
    width: 120,
    render(row) {
      return ['无动作', '页面跳转', '切换Tab'][row.actionType] || '无动作';
    }
  },
  { title: '跳转路径', key: 'targetPath', width: 220 },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'warning' }, { default: () => (row.status === 1 ? '启用' : '停用') });
    }
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    render(row) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
          h(
            NPopconfirm,
            { onPositiveClick: () => handleToggleStatus(row) },
            {
              trigger: () =>
                h(NButton, { size: 'small', type: row.status === 1 ? 'warning' : 'success' }, { default: () => (row.status === 1 ? '停用' : '启用') }),
              default: () => `确定${row.status === 1 ? '停用' : '启用'}该轮播图吗？`
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
    const { data: result, error } = await fetchBannerPage(searchForm.value);
    if (!error && result) {
      data.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  formModel.value = {
    title: '',
    subtitle: '',
    imageUrl: '',
    actionType: 0,
    targetPath: '',
    sort: 0,
    status: 1
  };
  previewImageUrl.value = '';
}

function handleAdd() {
  isEdit.value = false;
  resetForm();
  showModal.value = true;
}

function handleEdit(row: Api.Banner.HomeBanner) {
  isEdit.value = true;
  formModel.value = {
    id: row.id,
    title: row.title,
    subtitle: row.subtitle || '',
    imageUrl: row.imageUrl,
    actionType: row.actionType,
    targetPath: row.targetPath || '',
    sort: row.sort,
    status: row.status
  };
  previewImageUrl.value = row.imageUrl;
  showModal.value = true;
}

async function handleSubmit() {
  const payload = { ...formModel.value };
  if (payload.actionType === 0) {
    payload.targetPath = '';
  }

  if (isEdit.value && formModel.value.id) {
    const { error } = await updateBanner(formModel.value.id, payload);
    if (!error) {
      message.success('轮播图更新成功');
      showModal.value = false;
      loadData();
    }
    return;
  }

  const { error } = await createBanner(payload);
  if (!error) {
    message.success('轮播图创建成功');
    showModal.value = false;
    loadData();
  }
}

async function handleToggleStatus(row: Api.Banner.HomeBanner) {
  const { error } = await updateBannerStatus(row.id, row.status === 1 ? 0 : 1);
  if (!error) {
    message.success('状态更新成功');
    loadData();
  }
}

function handleSearch() {
  searchForm.value.pageNum = 1;
  loadData();
}

function handleReset() {
  searchForm.value = {
    title: '',
    status: undefined,
    pageNum: 1,
    pageSize: 10
  };
  loadData();
}

function handleImageInputChange(value: string) {
  previewImageUrl.value = value || '';
}

async function handleUploadImage(options: UploadCustomRequestOptions) {
  const rawFile = options.file.file;
  if (!rawFile) {
    options.onError();
    return;
  }
  uploadLoading.value = true;
  try {
    const { data: result, error } = await uploadBannerImage(rawFile as File);
    if (!error && result) {
      formModel.value.imageUrl = result.objectName;
      previewImageUrl.value = result.url;
      message.success('图片上传成功');
      options.onFinish();
      return;
    }
    options.onError();
  } finally {
    uploadLoading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<template>
  <NSpace vertical :size="12">
    <NCard :bordered="false" class="banner-hero">
      <div class="banner-hero__eyebrow">BANNER STUDIO</div>
      <div class="banner-hero__head">
        <div>
          <h2 class="banner-hero__title">把首页轮播图做成一块可运营的内容陈列位</h2>
          <p class="banner-hero__desc">适合管理主推活动、跳转入口和展示顺序，让首页内容发布和状态控制更直观。</p>
        </div>
        <div class="banner-hero__badge">
          <span>当前轮播数</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </NCard>

    <NCard :bordered="false" class="banner-filter-card">
      <NForm :model="searchForm" label-placement="left" label-width="80">
        <NGrid :cols="24" :x-gap="18">
          <NGi :span="8">
            <NFormItem label="标题">
              <NInput v-model:value="searchForm.title" placeholder="请输入标题关键词" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="4">
            <NFormItem label="状态">
              <NSelect v-model:value="searchForm.status" :options="statusOptions" clearable />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NSpace justify="end" class="search-actions">
              <NButton type="primary" @click="handleSearch">搜索</NButton>
              <NButton @click="handleReset">重置</NButton>
            </NSpace>
          </NGi>
        </NGrid>
      </NForm>
    </NCard>

    <NCard :bordered="false" title="首页轮播图" class="banner-list-card">
      <template #header-extra>
        <NButton type="primary" @click="handleAdd">新增轮播图</NButton>
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
          prefix: ({ itemCount }) => `共 ${itemCount} 条`,
          pageSizes: [10, 20, 50, 100, 200],
          onChange: (page: number) => { searchForm.pageNum = page; loadData(); },
          onUpdatePageSize: (pageSize: number) => { searchForm.pageSize = pageSize; searchForm.pageNum = 1; loadData(); }
        }"
      />
    </NCard>

    <NModal v-model:show="showModal" preset="card" :title="isEdit ? '编辑轮播图' : '新增轮播图'" style="width: 680px">
      <NForm :model="formModel" label-placement="left" label-width="100">
        <NFormItem label="主标题">
          <NInput v-model:value="formModel.title" placeholder="例如：本周主推 / 新客到店礼" />
        </NFormItem>
        <NFormItem label="副标题">
          <NInput v-model:value="formModel.subtitle" placeholder="例如：堂食人气菜、限时赠饮、下午茶时段" />
        </NFormItem>
        <NFormItem label="图片">
          <NSpace vertical :size="8" style="width: 100%;">
            <NUpload :show-file-list="false" :custom-request="handleUploadImage">
              <NButton :loading="uploadLoading">上传轮播图图片</NButton>
            </NUpload>
            <NInput v-model:value="formModel.imageUrl" placeholder="可手动输入对象键或图片URL" @update:value="handleImageInputChange" />
            <NImage
              v-if="previewImageUrl"
              :src="previewImageUrl"
              width="220"
              object-fit="cover"
              style="border-radius: 14px; border: 1px solid #e5e7eb;"
            />
          </NSpace>
        </NFormItem>
        <NGrid :cols="24" :x-gap="16">
          <NGi :span="8">
            <NFormItem label="动作">
              <NSelect v-model:value="formModel.actionType" :options="actionOptions" />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NFormItem label="排序">
              <NInputNumber v-model:value="formModel.sort" :min="0" style="width: 100%;" />
            </NFormItem>
          </NGi>
          <NGi :span="8">
            <NFormItem label="状态">
              <NSelect v-model:value="formModel.status" :options="statusOptions.filter(item => item.value !== undefined)" />
            </NFormItem>
          </NGi>
        </NGrid>
        <NFormItem label="跳转路径">
          <NInput v-model:value="formModel.targetPath" placeholder="/pages/coupon/index 或 /pages/menu/index" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">取消</NButton>
          <NButton type="primary" @click="handleSubmit">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </NSpace>
</template>

<style scoped>
.banner-hero {
  overflow: hidden;
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.2), transparent 24%),
    linear-gradient(135deg, rgba(252, 254, 255, 0.98), rgba(228, 239, 255, 0.98)) !important;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.14);
  box-shadow:
    0 26px 48px rgba(var(--admin-accent-rgb), 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.banner-hero__eyebrow {
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.banner-hero__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.banner-hero__title {
  margin: 0;
  font-size: 28px;
  color: color-mix(in srgb, var(--admin-accent-strong) 58%, #1b2d45);
}

.banner-hero__desc {
  max-width: 760px;
  margin: 10px 0 0;
  line-height: 1.75;
  color: color-mix(in srgb, var(--admin-accent-strong) 28%, #44556f);
}

.banner-hero__badge {
  min-width: 170px;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--admin-accent-rgb), 0.12);
}

.banner-hero__badge span {
  display: block;
  font-size: 12px;
  color: rgba(var(--admin-accent-rgb), 0.74);
}

.banner-hero__badge strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--admin-accent-strong);
}

.banner-filter-card,
.banner-list-card {
  overflow: hidden;
  border: 1px solid rgba(var(--admin-accent-rgb), 0.1);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.06), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)) !important;
  box-shadow:
    0 20px 36px rgba(var(--admin-accent-rgb), 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.banner-list-card :deep(.n-card-header) {
  padding-bottom: 10px;
}

.search-actions {
  width: 100%;
}

html.dark .banner-hero {
  background:
    radial-gradient(circle at top right, rgba(var(--admin-accent-rgb), 0.16), transparent 24%),
    linear-gradient(135deg, rgba(4, 6, 10, 0.99), rgba(10, 13, 19, 0.99)) !important;
  border-color: rgba(255, 255, 255, 0.06);
  box-shadow:
    0 30px 56px rgba(0, 0, 0, 0.42),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html.dark .banner-filter-card,
html.dark .banner-list-card {
  border-color: rgba(255, 255, 255, 0.06);
  background:
    radial-gradient(circle at top left, rgba(var(--admin-accent-rgb), 0.08), transparent 22%),
    linear-gradient(180deg, rgba(5, 7, 11, 0.98), rgba(10, 13, 19, 0.98)) !important;
  box-shadow:
    0 24px 42px rgba(0, 0, 0, 0.3),
    inset 0 1px 0 rgba(255, 255, 255, 0.04);
}
</style>

