<template>
  <div class="page">
    <div class="h-page-header">
      <h2>预约监管</h2>
      <span class="sub">全平台每一条预约都可见可管，强制取消与用户端共用同一套名额归还逻辑</span>
    </div>

    <!-- 预约统计 5 项 -->
    <div class="stat-grid">
      <div class="h-card chart-card">
        <div class="ch-title">① 时段分布</div>
        <div ref="s1" class="chart-sm"></div>
      </div>
      <div class="h-card chart-card">
        <div class="ch-title">② 爽约统计</div>
        <div ref="s2" class="chart-sm"></div>
      </div>
      <div class="h-card chart-card">
        <div class="ch-title">③ 转化漏斗</div>
        <div ref="s3" class="chart-sm"></div>
      </div>
      <div class="h-card chart-card">
        <div class="ch-title">④ 课程热度 Top5</div>
        <div ref="s4" class="chart-sm"></div>
      </div>
      <div class="h-card chart-card">
        <div class="ch-title">⑤ 30 天预约趋势</div>
        <div ref="s5" class="chart-sm"></div>
      </div>
    </div>

    <!-- 列表 -->
    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane v-for="t in tabs" :key="String(t.value)" :label="t.label" :name="String(t.value)" />
    </el-tabs>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="预约号 / 课程 / 联系人 / 匠人" clearable style="width: 240px" @keyup.enter="reload" @clear="reload" />
      <el-checkbox v-model="query.onlyTimeout" label="只看超时未审" @change="reload" />
      <el-button size="small" @click="exportList">导出 Excel</el-button>
      <span class="spacer"></span>
      <span class="hint-btn">
        定时任务手动触发：
        <el-button size="small" text type="warning" @click="runTask('/admin/booking/task/auto-reject-timeout')">超时自动拒</el-button>
        <el-button size="small" text type="warning" @click="runTask('/admin/booking/task/auto-finish-courses')">课程推进</el-button>
        <el-button size="small" text type="warning" @click="runTask('/admin/booking/task/course-reminders')">开课提醒</el-button>
      </span>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="bookingNo" label="预约号" width="165" />
        <el-table-column label="课程" min-width="170">
          <template #default="{ row }">
            <div class="ellipsis">{{ row.courseTitle }}</div>
            <div class="sub">{{ dateTime(row.courseStartTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="craftsmanName" label="匠人" width="100" />
        <el-table-column label="学员" width="130">
          <template #default="{ row }">{{ row.contactName }}<div class="sub">{{ row.contactPhone }}</div></template>
        </el-table-column>
        <el-table-column prop="quota" label="人数" width="60" />
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="BOOKING_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag>
            <div class="sub" v-if="row.timeout">超时</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="openDetail(row)">详情/轨迹</el-button>
            <el-popconfirm v-if="[0, 1].includes(row.status)" title="强制取消将立即归还名额并通知用户，确定？" width="220" @confirm="forceCancel(row)">
              <template #reference><el-button text size="small" type="danger">强制取消</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <!-- 详情 + 流转轨迹 -->
    <el-drawer v-model="detailDrawer" size="460px" :title="'预约详情 · ' + (detail.booking?.bookingNo || '')">
      <template v-if="detail.booking">
        <div class="d-row"><span>课程</span><b>{{ detail.booking.courseTitle }}</b></div>
        <div class="d-row"><span>学员</span><b>{{ detail.booking.contactName }} {{ detail.booking.contactPhone }}（{{ detail.booking.quota }} 人）</b></div>
        <div class="d-row"><span>匠人</span><b>{{ detail.booking.craftsmanName }}</b></div>
        <div class="d-row"><span>状态</span><el-tag :type="BOOKING_STATUS[detail.booking.status]?.type" size="small">{{ detail.booking.statusDesc }}</el-tag></div>
        <div class="d-row" v-if="detail.booking.cancelReason"><span>取消原因</span><b class="danger">{{ detail.booking.cancelReason }}（{{ detail.booking.cancelByDesc }}）</b></div>
        <div class="d-row" v-if="detail.booking.rejectReason"><span>拒绝原因</span><b class="danger">{{ detail.booking.rejectReason }}</b></div>
        <div class="d-row"><span>核销码</span><b>{{ detail.booking.checkinCode || '—' }}</b></div>
        <el-divider>状态流转轨迹（{{ detail.statusLogs?.length || 0 }} 条）</el-divider>
        <el-timeline>
          <el-timeline-item
            v-for="log in detail.statusLogs || []"
            :key="log.id"
            :timestamp="dateTime(log.createTime)"
            :type="log.toStatus === 2 || log.toStatus === 3 ? 'danger' : 'success'"
          >
            <b>{{ log.fromStatusDesc }} → {{ log.toStatusDesc }}</b>
            <div class="log-op">{{ log.operatorRoleDesc }} {{ log.operatorName }} {{ log.remark || '' }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { http, saveBlob } from '@/utils/request'
import { dateTime, BOOKING_STATUS } from '@/utils/format'

const tabs = [
  { value: null, label: '全部' },
  { value: 0, label: '待审核' },
  { value: 1, label: '已通过' },
  { value: 4, label: '已核销' },
  { value: 5, label: '已完成' },
  { value: 2, label: '已拒绝' },
  { value: 3, label: '已取消' }
]

const list = ref([])
const total = ref(0)
const loading = ref(false)
const status = ref('null')
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', onlyTimeout: false })

const detailDrawer = ref(false)
const detail = ref({})
const s1 = ref(); const s2 = ref(); const s3 = ref(); const s4 = ref(); const s5 = ref()
const charts = []

function render(el, option) {
  if (!el) return
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
}

async function loadStats() {
  const slot = await http.get('/admin/booking/stat/time-slot')
  render(s1.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 10, top: 14, bottom: 22 },
    xAxis: { type: 'category', data: (slot.slots || []).map((s) => s.slotName) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: (slot.slots || []).map((s) => s.bookingCount), itemStyle: { color: '#9c2c2c', borderRadius: [4, 4, 0, 0] } }]
  })

  const absent = await http.get('/admin/booking/stat/absent')
  render(s2.value, {
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    series: [{
      type: 'pie', radius: ['30%', '58%'], center: ['50%', '52%'],
      data: [
        { name: '已到场核销', value: absent.attend },
        { name: '爽约', value: absent.absent }
      ],
      label: { fontSize: 11 },
      color: ['#6b7c5e', '#9c2c2c']
    }]
  })

  const funnel = await http.get('/admin/booking/stat/funnel')
  render(s3.value, {
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    series: [{
      type: 'funnel', left: '6%', width: '88%', top: 10, bottom: 10, minSize: '26%',
      label: { position: 'inside', fontSize: 11, color: '#fff', formatter: '{b} {c}' },
      labelLine: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: (funnel.stages || []).map((s) => ({ name: s.name, value: Number(s.value) }))
    }]
  })

  const heat = await http.get('/admin/booking/stat/course-heat')
  const items = (heat.list || heat.items || []).slice(0, 5)
  render(s4.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 40, top: 10, bottom: 6, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: items.map((i) => i.courseTitle).reverse(), axisLabel: { fontSize: 11, width: 80, overflow: 'truncate' } },
    series: [{ type: 'bar', data: items.map((i) => Number(i.bookingCount)).reverse(), barMaxWidth: 16, label: { show: true, position: 'right', fontSize: 10, color: '#8a6d2f' }, itemStyle: { color: '#c8a45c', borderRadius: [0, 4, 4, 0] } }]
  })

  const trend = await http.get('/admin/booking/stat/trend', { days: 30 })
  render(s5.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 12, top: 14, bottom: 22 },
    xAxis: { type: 'category', data: (trend.points || []).map((p) => p.dateText), axisLabel: { fontSize: 10, interval: 5, hideOverlap: true } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: (trend.points || []).map((p) => Number(p.count)), areaStyle: { opacity: 0.14 }, itemStyle: { color: '#9c2c2c' } }]
  })
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/booking/list', {
      status: status.value === 'null' ? undefined : Number(status.value),
      keyword: query.keyword || undefined,
      onlyTimeout: query.onlyTimeout || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
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

async function openDetail(row) {
  detail.value = await http.get(`/admin/booking/${row.id}`)
  detailDrawer.value = true
}

async function forceCancel(row) {
  const { value } = await ElMessageBox.prompt('取消原因（用户会收到站内通知）', '强制取消预约', {
    inputValue: '场地临时安排调整，敬请谅解', inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
  })
  await http.post('/admin/booking/cancel', { bookingId: row.id, reason: value })
  ElMessage.success('已强制取消，名额已归还并通知用户')
  load()
}

async function runTask(path) {
  const n = await http.post(path)
  ElMessage.success(`任务执行完成，处理 ${n ?? 0} 条`)
  load()
}

async function exportList() {
  const res = await http.download('/admin/booking/export', {
    status: status.value === 'null' ? undefined : Number(status.value)
  })
  saveBlob(res, '全平台预约.xlsx')
}

import { ElMessageBox } from 'element-plus'

onMounted(async () => {
  load()
  await nextTick()
  loadStats()
})
</script>

<style scoped>
.sub { color: var(--h-text-second); font-size: 12px; }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.stat-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 14px; }
.chart-card { padding: 10px 12px; }
.ch-title { font-size: 12px; font-weight: 600; margin-bottom: 2px; color: var(--h-text-second); }
.chart-sm { height: 170px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 12px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.toolbar .spacer { flex: 1; }
.hint-btn { font-size: 12px; color: var(--h-text-second); }
.table-card { padding: 6px 16px 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.d-row { display: flex; gap: 14px; margin-bottom: 10px; font-size: 13px; align-items: center; }
.d-row span { color: var(--h-text-second); width: 64px; flex: none; }
.danger { color: var(--h-primary); }
.log-op { color: var(--h-text-second); font-size: 12px; }
@media (max-width: 1300px) { .stat-grid { grid-template-columns: repeat(3, 1fr); } }
</style>
