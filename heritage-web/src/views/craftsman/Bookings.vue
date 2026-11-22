<template>
  <div class="page">
    <div class="h-page-header">
      <h2>预约管理</h2>
      <span class="sub">审核预约 → 学员凭核销码到场 → 你在「核销签到台」完成核销</span>
    </div>

    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane v-for="t in tabs" :key="String(t.value)" :label="t.count !== undefined ? `${t.label}(${t.count})` : t.label" :name="String(t.value)" />
    </el-tabs>

    <div class="toolbar h-card">
      <el-select v-model="query.courseId" placeholder="按课程筛选" clearable filterable style="width: 240px" @change="reload">
        <el-option v-for="c in myCourses" :key="c.id" :label="c.title" :value="c.id" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="联系人 / 预约号" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
      <el-checkbox v-model="query.onlyTimeout" label="只看超时未审" @change="reload" />
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="bookingNo" label="预约号" width="170" />
        <el-table-column label="课程" min-width="180">
          <template #default="{ row }">
            <div class="ellipsis">{{ row.courseTitle }}</div>
            <div class="sub">{{ dateTime(row.courseStartTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="学员" width="150">
          <template #default="{ row }">
            <div>{{ row.contactName }} {{ row.contactPhone }}</div>
            <div class="sub">{{ row.buyerName }} · {{ row.quota }} 人</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="BOOKING_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag>
            <div class="sub" v-if="row.timeout">已超时</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="150">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.canAudit">
              <el-button text size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button text size="small" type="danger" @click="audit(row, false)">拒绝</el-button>
            </template>
            <el-button v-if="row.canCheckin" text size="small" type="primary" @click="checkin(row)">核销</el-button>
            <el-button v-if="[0, 1].includes(row.status)" text size="small" @click="cancel(row)">取消</el-button>
            <el-button text size="small" @click="roster(row)">名单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <!-- 预约名单：结构化表格，替代原来「1. 张三 138… 1人 [已通过]」的纯文本 -->
    <DetailDialog
      v-model="rosterDialog"
      :title="`「${rosterRow.courseTitle || ''}」预约名单`"
      :loading="rosterLoading"
      width="720px"
    >
      <div class="d-sum">
        共 <b>{{ rosterSummary.total }}</b> 条预约，已通过 <b>{{ rosterSummary.passed }}</b> 条、合计
        <b>{{ rosterSummary.people }}</b> 个名额
        <span v-if="rosterRow.courseStartTime"> · 开课 {{ dateTime(rosterRow.courseStartTime) }}</span>
      </div>

      <el-table :data="rosterList" size="small" max-height="420">
        <el-table-column type="index" label="#" width="46" />
        <el-table-column prop="contactName" label="学员" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="名额" width="70" align="center">
          <template #default="{ row }">{{ row.quota }} 人</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="BOOKING_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="checkinCode" label="核销码" width="110">
          <template #default="{ row }"><span v-if="row.checkinCode" class="code">{{ row.checkinCode }}</span><span v-else>—</span></template>
        </el-table-column>
        <el-table-column label="签到时间" width="130">
          <template #default="{ row }">{{ row.checkinTime ? dateTime(row.checkinTime) : '—' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!rosterLoading && !rosterList.length" :image-size="60" description="该课程还没有预约记录" />
    </DetailDialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { dateTime, BOOKING_STATUS } from '@/utils/format'
import DetailDialog from '@/components/DetailDialog.vue'

const route = useRoute()

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
const myCourses = ref([])
const total = ref(0)
const loading = ref(false)

/* 预约名单弹窗 */
const rosterDialog = ref(false)
const rosterLoading = ref(false)
const rosterRow = ref({})
const rosterList = ref([])
const status = ref('null')
const query = reactive({ pageNum: 1, pageSize: 10, courseId: undefined, keyword: '', onlyTimeout: false })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/craftsman/booking/list', {
      status: status.value === 'null' ? undefined : Number(status.value),
      courseId: query.courseId || undefined,
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

async function loadCounts() {
  const c = await http.get('/craftsman/booking/count')
  // count 返回各状态数量，形状为 {status: n}
  tabs.forEach((t) => (t.count = c ? c[t.value] : undefined))
}

function reload() {
  query.pageNum = 1
  load()
  loadCounts()
}

async function audit(row, approve) {
  if (!approve) {
    const { value } = await ElMessageBox.prompt('请填写拒绝原因（学员会看到）', '拒绝预约', {
      inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
    })
    await http.post('/craftsman/booking/audit', { bookingId: row.id, approve: false, rejectReason: value })
  } else {
    await ElMessageBox.confirm(`通过 ${row.contactName} 的预约（${row.quota} 人）？通过后占用 ${row.quota} 个名额`, '通过预约')
    await http.post('/craftsman/booking/audit', { bookingId: row.id, approve: true })
  }
  ElMessage.success('操作成功')
  reload()
}

async function checkin(row) {
  await ElMessageBox.confirm(`确认 ${row.contactName} 已到场？核销后不可撤销`, '核销确认')
  await http.post('/craftsman/booking/checkin', { bookingId: row.id })
  ElMessage.success('已核销')
  reload()
}

async function cancel(row) {
  const { value } = await ElMessageBox.prompt('请填写取消原因（学员会收到通知）', '取消预约', {
    inputPattern: /\S+/, inputErrorMessage: '原因不能为空'
  })
  await http.post('/craftsman/booking/cancel', { bookingId: row.id, reason: value })
  ElMessage.success('已取消，名额已归还')
  reload()
}

async function roster(row) {
  rosterRow.value = row
  rosterList.value = []
  rosterDialog.value = true
  rosterLoading.value = true
  try {
    rosterList.value = (await http.get('/craftsman/booking/roster', { courseId: row.courseId })) || []
  } finally {
    rosterLoading.value = false
  }
}

/** 名单里各状态的人数，给弹窗顶部做一句摘要 */
const rosterSummary = computed(() => {
  const all = rosterList.value
  const passed = all.filter((x) => [1, 4, 5].includes(x.status)).length
  const people = all.filter((x) => [1, 4, 5].includes(x.status)).reduce((s, x) => s + (x.quota || 0), 0)
  return { total: all.length, passed, people }
})

onMounted(async () => {
  if (route.query.courseId) query.courseId = Number(route.query.courseId)
  // 工作台「待审核预约」带 ?tab=0 跳过来；值必须是 tabs 里的 value，非法值忽略
  const tab = route.query.tab
  if (tab && tabs.some((t) => String(t.value) === String(tab))) status.value = String(tab)
  http.get('/craftsman/course/list', { pageNum: 1, pageSize: 100 }).then((d) => (myCourses.value = d.records || [])).catch(() => {})
  reload()
})
</script>

<style scoped>
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.ellipsis { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toolbar { display: flex; align-items: center; gap: 14px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.code { font-family: var(--font-mono, monospace); letter-spacing: 0.5px; color: var(--h-primary); }
</style>
