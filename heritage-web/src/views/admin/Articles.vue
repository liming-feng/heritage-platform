<template>
  <div class="page">
    <div class="h-page-header">
      <h2>资讯管理</h2>
      <div class="head-right">
        <el-button size="small" :disabled="!selected.length" @click="batchPublish">批量上架（{{ selected.length }}）</el-button>
        <el-button size="small" @click="exportList">导出 Excel</el-button>
        <el-button type="primary" size="small" @click="openDialog()">发布资讯</el-button>
      </div>
    </div>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="标题 / 作者" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="reload">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
      </el-select>
      <el-select v-model="query.isTop" placeholder="置顶" clearable style="width: 110px" @change="reload">
        <el-option label="已置顶" :value="1" />
        <el-option label="未置顶" :value="0" />
      </el-select>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading" @selection-change="(s) => (selected = s.filter((x) => x.status === 0))">
        <el-table-column type="selection" width="42" :selectable="(row) => row.status === 0" />
        <el-table-column label="标题" min-width="260">
          <template #default="{ row }">
            <el-tag v-if="row.isTop === 1" size="small" type="danger" effect="dark">顶</el-tag>
            {{ row.title }}
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column prop="author" label="作者" width="110" />
        <el-table-column prop="viewCount" label="阅读" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="commentCount" label="评论" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" @click="toggleTop(row)">{{ row.isTop === 1 ? '取消置顶' : '置顶' }}</el-button>
            <el-button text size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="删除后不可恢复" @confirm="del(row)">
              <template #reference><el-button text size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑资讯' : '发布资讯'" width="640px" top="6vh">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题" required><el-input v-model="form.title" maxlength="60" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.categoryId" placeholder="分类 id（演示环境用数字）" style="width: 200px" /></el-form-item>
        <el-form-item label="封面"><el-input v-model="form.cover" placeholder="图片地址" /></el-form-item>
        <el-form-item label="作者"><el-input v-model="form.author" style="width: 200px" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="逗号分隔，如：剪纸,非遗" /></el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" maxlength="200" /></el-form-item>
        <el-form-item label="正文" required><el-input v-model="form.content" type="textarea" :rows="10" placeholder="支持 HTML" /></el-form-item>
        <el-form-item label="置顶"><el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">存草稿</el-radio>
          </el-radio-group>
        </el-form-item>
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
import { http, saveBlob } from '@/utils/request'
import { dateTime } from '@/utils/format'

const list = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined, isTop: undefined })
const dialog = ref(false)
const form = reactive({ id: null, title: '', categoryId: undefined, cover: '', author: '', tags: '', summary: '', content: '', isTop: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/article/list', { ...query, status: query.status ?? undefined, isTop: query.isTop ?? undefined })
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
  if (row) {
    http.get(`/admin/article/${row.id}`).then((d) => Object.assign(form, d, { id: row.id })).catch(() => {})
  } else {
    Object.assign(form, { id: null, title: '', categoryId: undefined, cover: '', author: '', tags: '', summary: '', content: '', isTop: 0, status: 1 })
  }
  dialog.value = true
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) return ElMessage.warning('标题与正文不能为空')
  await http.post('/admin/article/save', { ...form, categoryId: form.categoryId ? Number(form.categoryId) : undefined })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggleTop(row) {
  await http.post(`/admin/article/${row.id}/top`)
  load()
}

async function toggleStatus(row) {
  await http.post(`/admin/article/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
  ElMessage.success('操作成功')
  load()
}

async function batchPublish() {
  await http.post('/admin/article/batch-status', { ids: selected.value.map((x) => x.id), status: 1 })
  ElMessage.success('已批量上架')
  load()
}

async function del(row) {
  await http.delete(`/admin/article/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function exportList() {
  const res = await http.download('/admin/article/export', { ...query })
  saveBlob(res, '资讯列表.xlsx')
}

onMounted(load)
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
