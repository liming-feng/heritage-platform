<template>
  <div class="page">
    <div class="h-page-header">
      <h2>操作日志</h2>
      <el-button size="small" @click="exportLogs">导出 Excel</el-button>
    </div>

    <div class="toolbar h-card">
      <el-select v-model="query.operation" placeholder="操作类型" clearable style="width: 220px" @change="reload">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="操作人 / 方法" clearable style="width: 180px" @keyup.enter="reload" @clear="reload" />
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="操作人" width="130">
          <template #default="{ row }">{{ row.operator }}<div class="sub">{{ row.roleText }}</div></template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" min-width="180" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="120" />
        <el-table-column prop="costText" label="耗时" width="80" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">{{ row.successText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { http, saveBlob } from '@/utils/request'
import { dateTime } from '@/utils/format'

const list = ref([])
const types = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 15, operation: undefined, keyword: '' })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/log/list', { ...query, operation: query.operation ?? undefined })
    list.value = data.records || []
    total.value = data.total || 0
    types.value = (await http.get('/admin/log/types')) || []
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

async function exportLogs() {
  const res = await http.download('/admin/log/export', { ...query })
  saveBlob(res, '操作日志.xlsx')
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.sub { color: var(--h-text-second); font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
