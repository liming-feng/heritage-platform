<template>
  <div class="page">
    <div class="h-page-header">
      <h2>我的课程</h2>
      <div class="head-right">
        <el-radio-group v-model="view" size="small">
          <el-radio-button value="list">列表</el-radio-button>
          <el-radio-button value="calendar">排期日历</el-radio-button>
        </el-radio-group>
        <el-button type="primary" @click="$router.push('/craftsman/courses/edit')">+ 发布课程</el-button>
      </div>
    </div>

    <!-- 列表视图 -->
    <template v-if="view === 'list'">
      <div class="toolbar h-card">
        <el-input v-model="query.keyword" placeholder="课程标题" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
        <el-select v-model="query.status" placeholder="课程状态" clearable style="width: 130px" @change="reload">
          <el-option label="未开始" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已结束" :value="2" />
        </el-select>
        <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="reload">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
      </div>

      <div class="h-card table-card">
        <el-table :data="list" v-loading="loading">
          <el-table-column label="课程" min-width="240">
            <template #default="{ row }">
              <div class="c-cell">
                <img :src="img(row.cover)" @error="onImgErr" />
                <div>
                  <div class="c-name">{{ row.title }}</div>
                  <div class="c-sub">{{ row.address }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="开课时间" width="150"><template #default="{ row }">{{ dateTime(row.startTime) }}</template></el-table-column>
          <el-table-column label="名额" width="130">
            <template #default="{ row }">
              <span :class="row.totalQuota && row.remainQuota / row.totalQuota <= 0.2 ? 'level-danger' : ''">
                {{ row.remainQuota }}/{{ row.totalQuota }}
              </span>
              <!-- usedRate 后端给的是 0–100，直接补 %；没有该字段时用本地比例兜底 -->
              <div class="used">{{ row.usedRate != null ? pct(row.usedRate) : Math.round((row.bookedQuota / (row.totalQuota || 1)) * 100) + '%' }} 已约</div>
            </template>
          </el-table-column>
          <el-table-column prop="bookingCount" label="预约" width="70" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="COURSE_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag></template>
          </el-table-column>
          <el-table-column label="审核" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.auditStatusDesc" :type="AUDIT_STATUS[row.auditStatus]?.type" size="small">
                {{ row.auditStatusDesc }}
              </el-tag>
              <span v-else style="color: var(--h-text-second)">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" fixed="right">
            <template #default="{ row }">
              <el-button text size="small" @click="$router.push(`/craftsman/courses/edit/${row.id}`)">编辑</el-button>
              <el-button v-if="row.status === 1" text size="small" type="success" @click="finish(row)">结束课程</el-button>
              <el-button text size="small" @click="$router.push({ path: '/craftsman/bookings', query: { courseId: row.id } })">预约名单</el-button>
              <el-popconfirm title="删除后不可恢复，确定？" @confirm="del(row)">
                <template #reference><el-button text size="small" type="danger">删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
        </div>
      </div>
    </template>

    <!-- 排期日历 -->
    <template v-else>
      <div class="h-card cal-card">
        <div class="cal-head">
          <span class="cal-title">{{ monthText }}</span>
          <div>
            <el-button size="small" @click="shiftMonth(-1)">‹ 上月</el-button>
            <el-button size="small" @click="shiftMonth(1)">下月 ›</el-button>
          </div>
        </div>
        <div class="cal-grid">
          <div class="cal-wk" v-for="w in ['一','二','三','四','五','六','日']" :key="w">{{ w }}</div>
          <div v-for="cell in calendar" :key="cell.key" :class="['cal-cell', { other: cell.other }]">
            <div class="d">{{ cell.day }}</div>
            <div v-for="c in cell.courses" :key="c.id" class="evt" @click="$router.push(`/courses/${c.id}`)">
              {{ c.title.length > 8 ? c.title.slice(0, 8) + '…' : c.title }}
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, pct, COURSE_STATUS, AUDIT_STATUS } from '@/utils/format'

const view = ref('list')
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined, auditStatus: undefined })

// 日历
const cur = reactive({ y: new Date().getFullYear(), m: new Date().getMonth() + 1 })
const calendar = ref([])
const monthText = computed(() => `${cur.y} 年 ${cur.m} 月`)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/craftsman/course/list', {
      ...query,
      status: query.status ?? undefined,
      auditStatus: query.auditStatus ?? undefined
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

async function loadCalendar() {
  const data = await http.get('/craftsman/course/calendar', { year: cur.y, month: cur.m })
  const byDate = {}
  for (const d of data || []) {
    byDate[d.date] = d.courses || []
  }
  const first = new Date(cur.y, cur.m - 1, 1)
  const days = new Date(cur.y, cur.m, 0).getDate()
  const lead = (first.getDay() + 6) % 7
  const cells = []
  const prevDays = new Date(cur.y, cur.m - 1, 0).getDate()
  for (let i = lead; i > 0; i--) {
    cells.push({ key: 'p' + i, day: prevDays - i + 1, other: true, courses: [] })
  }
  for (let d = 1; d <= days; d++) {
    const key = `${cur.y}-${String(cur.m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({ key, day: d, other: false, courses: byDate[key] || [] })
  }
  while (cells.length % 7 !== 0) cells.push({ key: 'n' + cells.length, day: cells.length % 7, other: true, courses: [] })
  calendar.value = cells
}

function shiftMonth(delta) {
  let m = cur.m + delta, y = cur.y
  if (m < 1) { m = 12; y-- }
  if (m > 12) { m = 1; y++ }
  cur.y = y
  cur.m = m
}

watch(view, (v) => v === 'calendar' && loadCalendar())
watch(() => [cur.y, cur.m], () => view.value === 'calendar' && loadCalendar())

async function finish(row) {
  await ElMessageBox.confirm(`确定结束课程「${row.title}」吗？结束后未核销的预约将按爽约处理`, '结束课程', { type: 'warning' })
  await http.post(`/craftsman/course/${row.id}/finish`)
  ElMessage.success('课程已结束')
  load()
}

async function del(row) {
  await http.delete(`/craftsman/course/${row.id}`)
  ElMessage.success('已删除')
  load()
}

import { ElMessageBox } from 'element-plus'

onMounted(load)
</script>

<style scoped>
.head-right { display: flex; gap: 12px; align-items: center; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.c-cell { display: flex; align-items: center; gap: 10px; }
.c-cell img { width: 60px; height: 40px; object-fit: cover; border-radius: 6px; flex: none; }
.c-name { font-size: 13px; font-weight: 600; }
.c-sub { color: var(--h-text-second); font-size: 12px; }
.used { color: var(--h-text-second); font-size: 11px; }
.level-danger { color: var(--h-primary); font-weight: 700; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.cal-card { padding: 18px 20px; }
.cal-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.cal-title { font-weight: 700; font-size: 16px; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }
.cal-wk { text-align: center; color: var(--h-text-second); font-size: 12px; padding: 4px 0; }
.cal-cell { min-height: 86px; background: var(--h-paper); border-radius: 8px; padding: 6px; }
.cal-cell.other { opacity: 0.4; }
.cal-cell .d { font-size: 12px; color: var(--h-text-second); }
.evt {
  margin-top: 3px; background: #f5e4e3; color: var(--h-primary-dark);
  font-size: 11px; border-radius: 5px; padding: 2px 6px; cursor: pointer;
  overflow: hidden; white-space: nowrap; text-overflow: ellipsis;
}
.evt:hover { background: #f0d6d3; }
</style>
