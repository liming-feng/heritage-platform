<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>消息中心</h2>
      <el-button text type="primary" :disabled="!unread" @click="readAll">全部已读</el-button>
    </div>

    <el-tabs v-model="type" @tab-change="reload">
      <el-tab-pane v-for="t in typeTabs" :key="String(t.value)" :label="t.label" :name="String(t.value)" />
    </el-tabs>

    <div v-for="m in list" :key="m.id" :class="['msg h-card', { unread: m.isRead === 0 }]" @click="read(m)">
      <span class="dot" v-if="m.isRead === 0"></span>
      <el-tag size="small" :type="typeColor(m.type)" effect="plain">{{ m.typeDesc }}</el-tag>
      <div class="body">
        <div class="title">{{ m.title }}</div>
        <div class="content">{{ m.content }}</div>
      </div>
      <span class="time">{{ dateTime(m.createTime) }}</span>
    </div>

    <el-empty v-if="!list.length && !loading" description="暂无消息" class="h-empty" />

    <div class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="Number(total)"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { dateTime } from '@/utils/format'

const user = useUserStore()

const typeTabs = [
  { value: null, label: '全部' },
  { value: 1, label: '预约通知' },
  { value: 2, label: '订单通知' },
  { value: 3, label: '系统通知' }
]

const type = ref('null')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

const unread = computed(() => list.value.some((m) => m.isRead === 0))

function typeColor(t) {
  return { 1: 'warning', 2: 'success', 3: 'info' }[t] || 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/message/list', {
      type: type.value === 'null' ? undefined : Number(type.value),
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

async function read(m) {
  if (m.isRead === 1) return
  await http.post(`/portal/message/read/${m.id}`)
  m.isRead = 1
  user.setUnreadCount(Math.max(0, (user.unreadCount || 0) - 1))
}

async function readAll() {
  await http.post('/portal/message/read-all')
  ElMessage.success('已全部标记为已读')
  load()
  user.setUnreadCount(0)
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.msg {
  display: flex; align-items: flex-start; gap: 12px;
  padding: 16px 18px; margin-bottom: 10px; cursor: pointer; position: relative;
}
.msg.unread { background: #fffdf5; border-color: #f0e2b6; }
.dot { position: absolute; left: 6px; top: 50%; transform: translateY(-50%); width: 6px; height: 6px; border-radius: 50%; background: var(--h-primary); }
.body { flex: 1; min-width: 0; }
.title { font-weight: 600; }
.content { color: var(--h-text-second); font-size: 13px; margin-top: 3px; }
.time { color: var(--h-text-second); font-size: 12px; flex: none; }
.pager { display: flex; justify-content: center; margin-top: 18px; }
</style>
