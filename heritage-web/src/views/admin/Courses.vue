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
            <div class="sub">{{ Math.round((row.usedRate || 0) * 100) }}% 已约</div>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { img, onImgErr, dateTime, COURSE_STATUS, AUDIT_STATUS } from '@/utils/format'

const list = ref([])
const selected = ref([])
const total = ref(0)
const loading = ref(false)
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
  const d = await http.get(`/admin/course/${row.id}/audit-detail`)
  ElMessageBox.alert(
    `${d.title}（${d.categoryName} · ${d.craftsmanName}）<br/>` +
    `开课：${dateTime(d.startTime)} ~ ${dateTime(d.endTime)}<br/>地点：${d.address}<br/>` +
    `名额：总 ${d.totalQuota}，剩余 ${d.remainQuota}，已约 ${d.bookedQuota}<br/>` +
    `预约统计：待审 ${d.bookingWait} / 通过 ${d.bookingPassed} / 核销 ${d.bookingCheckedIn} / 完成 ${d.bookingFinished} / 拒绝 ${d.bookingRejected} / 取消 ${d.bookingCanceled}<br/>` +
    `到场率：${d.checkinRate != null ? Math.round(d.checkinRate * 100) + '%' : '—'}（缺席 ${d.absentPeople} 人）<br/>` +
    `简介：${d.description || '—'}`,
    `课程审核 · ${d.title}`,
    { dangerouslyUseHTMLString: true, customStyle: { maxWidth: '560px' } }
  )
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

onMounted(load)
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
