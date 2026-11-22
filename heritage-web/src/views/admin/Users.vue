<template>
  <div class="page">
    <div class="h-page-header">
      <h2>用户管理</h2>
      <el-button size="small" @click="exportUsers">导出 Excel</el-button>
    </div>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 手机号" clearable style="width: 220px" @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px" @change="reload">
        <el-option label="普通用户" :value="1" />
        <el-option label="匠人" :value="2" />
        <el-option label="管理员" :value="3" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="reload">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="u-cell">
              <el-avatar :size="32">{{ (row.nickname || row.username)?.charAt(0) }}</el-avatar>
              <div>
                <div class="u-name">{{ row.nickname || row.username }}</div>
                <div class="sub">{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column label="角色" width="100"><template #default="{ row }">{{ row.roleText }}</template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="detail(row)">详情</el-button>
            <el-button v-if="row.role !== 3" text size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <DetailDialog v-model="userDialog" :title="`用户 #${userInfo.id || ''}`" :loading="userLoading" width="580px">
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="用户名">{{ userInfo.username || '—' }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ userInfo.nickname || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userInfo.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ userInfo.roleText || '—' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">
          <el-tag size="small" :type="userInfo.status === 1 ? 'success' : 'info'">{{ userInfo.statusText || '—' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" :span="2">{{ dateTime(userInfo.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </DetailDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { dateTime } from '@/utils/format'
import DetailDialog from '@/components/DetailDialog.vue'

const list = ref([])
const total = ref(0)
const loading = ref(false)

/* 用户详情弹窗 */
const userDialog = ref(false)
const userLoading = ref(false)
const userInfo = ref({})
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', role: undefined, status: undefined })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/user/list', { ...query, role: query.role ?? undefined, status: query.status ?? undefined })
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

async function toggle(row) {
  const action = row.status === 1 ? '禁用' : '启用'
  await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示')
  await http.post('/admin/user/status', { id: row.id, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(`已${action}`)
  load()
}

async function detail(row) {
  userInfo.value = {}
  userDialog.value = true
  userLoading.value = true
  try {
    userInfo.value = await http.get(`/admin/user/${row.id}`)
  } finally {
    userLoading.value = false
  }
}

async function exportUsers() {
  const res = await http.download('/admin/user/export', { ...query })
  saveBlob(res, '用户列表.xlsx')
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.u-cell { display: flex; align-items: center; gap: 10px; }
.u-name { font-size: 13px; font-weight: 600; }
.sub { color: var(--h-text-second); font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
