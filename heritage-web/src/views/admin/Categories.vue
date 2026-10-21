<template>
  <div class="page">
    <div class="h-page-header">
      <h2>分类管理</h2>
      <el-button type="primary" size="small" @click="openDialog()">新增分类</el-button>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="goodsCount" label="在售商品" width="90" />
        <el-table-column prop="totalGoodsCount" label="全部商品" width="90" />
        <el-table-column prop="courseCount" label="课程" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDialog(row)">编辑</el-button>
            <el-button text size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-popconfirm title="删除分类（分类下有商品时不可删）" @confirm="del(row)">
              <template #reference><el-button text size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="form.id ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required><el-input v-model="form.name" maxlength="20" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
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
import { http } from '@/utils/request'

const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const form = reactive({ id: null, name: '', description: '', sort: 0, status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/category/list', { pageNum: 1, pageSize: 100 })
    list.value = data.records || data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) Object.assign(form, row)
  else Object.assign(form, { id: null, name: '', description: '', sort: 0, status: 1 })
  dialog.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写名称')
  await http.post('/admin/category/save', { ...form })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggle(row) {
  await http.post(`/admin/category/${row.id}/status`, null, { params: { status: row.status === 1 ? 0 : 1 } })
  ElMessage.success('操作成功')
  load()
}

async function del(row) {
  await http.delete(`/admin/category/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.table-card { padding: 6px 16px 16px; }
</style>
