<template>
  <div class="page">
    <div class="h-page-header">
      <h2>轮播管理</h2>
      <div class="head-right">
        <el-button size="small" @click="batchSort">保存拖拽排序</el-button>
        <el-button type="primary" size="small" @click="openDialog()">新增轮播</el-button>
      </div>
    </div>

    <div class="toolbar h-card">
      <el-select v-model="query.position" placeholder="展示位置" clearable style="width: 180px" @change="reload">
        <el-option label="首页轮播" :value="1" />
        <el-option label="活动弹窗" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="reload">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="sort" label="顺序" width="70" />
        <el-table-column label="预览" width="180">
          <template #default="{ row }"><img :src="img(row.image)" style="width: 150px; height: 56px; object-fit: cover; border-radius: 6px" @error="onImgErr" /></template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="linkUrl" label="跳转链接" min-width="180" show-overflow-tooltip />
        <el-table-column prop="positionDesc" label="位置" width="100" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="删除该轮播？" @confirm="del(row)">
              <template #reference><el-button text size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑轮播' : '新增轮播'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="form.title" maxlength="30" /></el-form-item>
        <el-form-item label="图片" required>
          <ImageUpload v-model="form.image" :width="220" :height="82" tip="首页轮播建议 16:6 横图，不超过 5MB" />
        </el-form-item>
        <el-form-item label="跳转链接"><el-input v-model="form.linkUrl" placeholder="/goods/1 等" /></el-form-item>
        <el-form-item label="位置">
          <el-select v-model="form.position" style="width: 100%">
            <el-option label="首页轮播" :value="1" />
            <el-option label="活动弹窗" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr } from '@/utils/format'
import ImageUpload from '@/components/ImageUpload.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 50, position: undefined, status: undefined })
const dialog = ref(false)
const form = reactive({ id: null, title: '', image: '', linkUrl: '', position: 1, sort: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/banner/list', { ...query, position: query.position ?? undefined, status: query.status ?? undefined })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function openDialog(row) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, title: '', image: '', linkUrl: '', position: 1, sort: 0, status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.title.trim() || !form.image.trim()) return ElMessage.warning('标题与图片不能为空')
  await http.post('/admin/banner/save', { ...form })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggle(row) {
  await http.post(`/admin/banner/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
  load()
}

async function batchSort() {
  await http.post('/admin/banner/batch-sort', { items: list.value.map((r, i) => ({ id: r.id, sort: r.sort ?? i })) })
  ElMessage.success('排序已保存')
  load()
}

async function del(row) {
  await http.delete(`/admin/banner/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
</style>
