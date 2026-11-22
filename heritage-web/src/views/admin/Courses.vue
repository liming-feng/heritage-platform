<template>
  <div class="page">
    <div class="h-page-header">
      <h2>课程管理</h2>
      <div class="head-right">
        <el-button size="small" :disabled="!selected.length" @click="batchAudit(true)">批量通过</el-button>
        <el-button size="small" :disabled="!selected.length" @click="batchAudit(false)">批量驳回</el-button>
        <el-button size="small" @click="exportCourses">导出 Excel</el-button>
      </div>
    </div>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="课程标题 / 匠人" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="reload">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="课程状态" clearable style="width: 130px" @change="reload">
        <el-option label="未开始" :value="0" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
      </el-select>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading" @selection-change="(s) => (selected = s.filter((x) => x.auditStatus === 0))">
        <el-table-column type="selection" width="42" :selectable="(row) => row.auditStatus === 0" />
        <el-table-column label="课程" min-width="220">
          <template #default="{ row }">
            <div class="g-cell">
              <img :src="img(row.cover)" @error="onImgErr" />
              <div>
                <div class="g-name">{{ row.title }}</div>
                <div class="sub">{{ row.craftsmanName }} · {{ dateTime(row.startTime) }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="90"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column label="名额" width="110">
          <template #default="{ row }">{{ row.remainQuota }}/{{ row.totalQuota }}
            <div class="sub">{{ pct(row.usedRate, '0%') }} 已约</div>
          </template>
        </el-table-column>
        <el-table-column prop="bookingCount" label="预约" width="70" />
        <el-table-column label="审核" width="110">
          <template #default="{ row }">
            <el-tag :type="AUDIT_STATUS[row.auditStatus]?.type" size="small">{{ row.auditStatusDesc }}</el-tag>
            <div class="reject" v-if="row.auditStatus === 2">{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="COURSE_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="detail(row)">详情</el-button>
            <template v-if="row.auditStatus === 0">
              <el-button text size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button text size="small" type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
            <el-button v-if="row.status === 1" text size="small" type="warning" @click="offShelf(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <DetailDialog v-model="courseDialog" :title="`课程审核 · ${courseInfo.title || ''}`" :loading="courseLoading" width="740px">
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="分类 / 匠人">
          {{ courseInfo.categoryName || '—' }} · {{ courseInfo.craftsmanName || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="上课地点">{{ courseInfo.address || '—' }}</el-descriptions-item>
        <el-descriptions-item label="开课时间">{{ dateTime(courseInfo.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ dateTime(courseInfo.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="名额">
          总 {{ courseInfo.totalQuota ?? '—' }} · 已约 {{ courseInfo.bookedQuota ?? '—' }} · 剩余
          {{ courseInfo.remainQuota ?? '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="到场率">
          {{ pct(courseInfo.checkinRate) }}
          <span class="d-sum" style="margin: 0 0 0 6px">缺席 {{ courseInfo.absentPeople ?? 0 }} 人</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="d-sec">预约状态分布</div>
      <div class="d-stats">
        <div class="d-stat"><b>{{ courseInfo.bookingWait ?? 0 }}</b><span>待审核</span></div>
        <div class="d-stat"><b>{{ courseInfo.bookingPassed ?? 0 }}</b><span>已通过</span></div>
        <div class="d-stat"><b>{{ courseInfo.bookingCheckedIn ?? 0 }}</b><span>已核销</span></div>
        <div class="d-stat"><b>{{ courseInfo.bookingFinished ?? 0 }}</b><span>已完成</span></div>
        <div class="d-stat"><b>{{ courseInfo.bookingRejected ?? 0 }}</b><span>已拒绝</span></div>
        <div class="d-stat"><b>{{ courseInfo.bookingCanceled ?? 0 }}</b><span>已取消</span></div>
      </div>

      <div class="d-sec">课程简介</div>
      <div class="d-text">{{ courseInfo.description || '—' }}</div>
    </DetailDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { img, onImgErr, dateTime, pct, COURSE_STATUS, AUDIT_STATUS } from '@/utils/format'
import DetailDialog from '@/components/DetailDialog.vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const list = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)

/* 课程审核详情弹窗 */
const courseDialog = ref(false)
const courseLoading = ref(false)
const courseInfo = ref({})
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', auditStatus: undefined, status: undefined })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/course/list', {
      ...query,
      auditStatus: query.auditStatus ?? undefined,
      status: query.status ?? undefined
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

async function detail(row) {
  courseInfo.value = {}
  courseDialog.value = true
  courseLoading.value = true
  try {
    courseInfo.value = await http.get(`/admin/course/${row.id}/audit-detail`)
  } finally {
    courseLoading.value = false
  }
}

async function audit(row, pass) {
  if (!pass) {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
    await http.post('/admin/course/audit', { ids: [row.id], pass: false, reason: value })
  } else {
    await http.post('/admin/course/audit', { ids: [row.id], pass: true })
  }
  ElMessage.success('操作成功')
  load()
}

async function batchAudit(pass) {
  if (!pass) {
    const { value } = await ElMessageBox.prompt(`驳回选中的 ${selected.value.length} 门课程，请填写原因`, '批量驳回', { inputPattern: /\S+/ })
    await http.post('/admin/course/audit', { ids: selected.value.map((x) => x.id), pass: false, reason: value })
  } else {
    await http.post('/admin/course/audit', { ids: selected.value.map((x) => x.id), pass: true })
  }
  ElMessage.success('操作成功')
  load()
}

async function offShelf(row) {
  await ElMessageBox.confirm(`下架课程「${row.title}」？已通过的预约将一并取消并通知学员`, '下架课程', { type: 'warning' })
  await http.post('/admin/course/off-shelf', { ids: [row.id], reason: '平台下架' })
  ElMessage.success('已下架')
  load()
}

async function exportCourses() {
  const res = await http.download('/admin/course/export', { ...query })
  saveBlob(res, '课程列表.xlsx')
}

onMounted(() => {
  // 工作台待办「待审核课程」带 ?auditStatus=0 跳过来
  if (route.query.auditStatus != null && route.query.auditStatus !== '') {
    query.auditStatus = Number(route.query.auditStatus)
  }
  load()
})
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.g-cell { display: flex; align-items: center; gap: 10px; }
.g-cell img { width: 54px; height: 38px; border-radius: 6px; object-fit: cover; flex: none; }
.g-name { font-size: 13px; font-weight: 600; }
.sub { color: var(--h-text-second); font-size: 12px; }
.reject { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
