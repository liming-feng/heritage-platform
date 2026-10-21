<template>
  <div class="page">
    <div class="h-page-header"><h2>留言反馈</h2></div>

    <div class="stat-cards">
      <div class="stat h-card"><div class="s-label">总反馈</div><div class="s-val">{{ stat.total ?? '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">待处理</div><div class="s-val" :class="{ alert: stat.waitCount > 0 }">{{ stat.waitCount ?? '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">处理中</div><div class="s-val">{{ stat.processingCount ?? '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">已完成</div><div class="s-val">{{ stat.doneCount ?? '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">平均处理时长</div><div class="s-val">{{ stat.avgHandleHours != null ? stat.avgHandleHours + 'h' : '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">24h 完成率</div><div class="s-val">{{ stat.within24hRate != null ? Math.round(stat.within24hRate * 100) + '%' : '—' }}</div></div>
    </div>

    <div class="toolbar h-card">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="reload">
        <el-option label="待处理" :value="0" />
        <el-option label="处理中" :value="1" />
        <el-option label="已完成" :value="2" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 130px" @change="reload">
        <el-option label="功能建议" :value="1" />
        <el-option label="问题反馈" :value="2" />
        <el-option label="其他" :value="3" />
      </el-select>
      <el-button size="small" @click="exportList">导出 Excel</el-button>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="用户" width="130">
          <template #default="{ row }">{{ row.nickname || row.username }}</template>
        </el-table-column>
        <el-table-column prop="typeDesc" label="类型" width="90" />
        <el-table-column prop="content" label="内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="contact" label="联系方式" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'primary' : 'warning'" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 2" text size="small" type="primary" @click="handle(row)">处理</el-button>
            <el-button text size="small" @click="view(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { dateTime } from '@/utils/format'

const stat = ref({})
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, status: undefined, type: undefined })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/feedback/list', { ...query, status: query.status ?? undefined, type: query.type ?? undefined })
    list.value = data.records || []
    total.value = data.total || 0
    stat.value = await http.get('/admin/feedback/stat').catch(() => ({}))
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function view(row) {
  ElMessageBox.alert(
    `${row.content}<br/><br/>回复：${row.reply || '暂无'}<br/>处理时间：${dateTime(row.handleTime)}`,
    `反馈 #${row.id}`,
    { dangerouslyUseHTMLString: true }
  )
}

async function handle(row) {
  const { value: reply } = await ElMessageBox.prompt('回复内容', '处理反馈', {
    inputPlaceholder: '回复用户（可选）', inputValue: row.reply || ''
  })
  const { value: status } = await ElMessageBox.prompt('处理状态（0 待处理 / 1 处理中 / 2 已完成）', '处理反馈', {
    inputValue: String(row.status === 0 ? 1 : 2), inputPattern: /^[0-2]$/, inputErrorMessage: '请输入 0-2'
  })
  await http.post('/admin/feedback/handle', { id: row.id, reply: reply || undefined, status: Number(status) })
  ElMessage.success('已处理')
  load()
}

async function exportList() {
  const res = await http.download('/admin/feedback/export', { ...query })
  saveBlob(res, '反馈列表.xlsx')
}

onMounted(load)
</script>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin-bottom: 14px; }
.stat { padding: 14px 18px; }
.s-label { color: var(--h-text-second); font-size: 12px; }
.s-val { font-size: 22px; font-weight: 800; margin-top: 3px; }
.s-val.alert { color: var(--h-primary); }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
