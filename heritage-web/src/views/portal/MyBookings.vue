<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>我的预约</h2>
      <span class="sub">预约提交后等待匠人审核，已通过的课程出示核销码到场签到</span>
    </div>

    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane
        v-for="t in tabs"
        :key="String(t.value)"
        :label="counts[t.value] !== undefined ? `${t.label}(${counts[t.value]})` : t.label"
        :name="String(t.value)"
      />
    </el-tabs>

    <div v-for="b in list" :key="b.id" class="booking h-card">
      <div class="left">
        <img class="cover" :src="img(b.courseCover)" @error="onImgErr" @click="goCourse(b)" />
        <div class="info">
          <div class="title" @click="goCourse(b)">{{ b.courseTitle }}</div>
          <div class="line"><el-icon><Location /></el-icon>{{ b.courseAddress || '—' }}</div>
          <div class="line"><el-icon><Timer /></el-icon>{{ dateTime(b.courseStartTime) }} 开课</div>
          <div class="line"><el-icon><User /></el-icon>{{ b.contactName }} {{ b.contactPhone }} · {{ b.quota }} 人</div>
        </div>
      </div>

      <div class="mid">
        <!-- 状态进度条 -->
        <el-steps v-if="![2, 3].includes(b.status)" :active="stepOf(b)" align-center class="steps" finish-status="success">
          <el-step v-for="(n, i) in BOOKING_STEP_NAMES" :key="i" :title="n" :description="stepDesc(b, i)" />
        </el-steps>
        <div v-else class="closed-state">
          <el-tag :type="BOOKING_STATUS[b.status]?.type" size="large" effect="plain">{{ b.statusDesc }}</el-tag>
          <div class="reason" v-if="b.status === 2 && b.rejectReason">拒绝原因：{{ b.rejectReason }}</div>
          <div class="reason" v-if="b.status === 3 && b.cancelReason">取消原因：{{ b.cancelReason }}（{{ b.cancelByDesc }}）</div>
        </div>
      </div>

      <div class="right">
        <el-tag :type="BOOKING_STATUS[b.status]?.type">{{ b.statusDesc }}</el-tag>

        <!-- ★ 核销码：已通过/已核销展示大图二维码 + 明文短码 -->
        <el-button
          v-if="b.canShowCheckinCode && b.status === 1"
          type="primary"
          size="small"
          @click="showCode(b)"
        >
          <el-icon><FullScreen /></el-icon>&nbsp;核销码
        </el-button>
        <el-tag v-else-if="b.status === 4" type="success" effect="plain">已于 {{ dateTime(b.checkinTime) }} 核销</el-tag>

        <el-button
          v-if="b.canCancel"
          size="small"
          type="danger"
          plain
          @click="cancelBooking(b)"
        >取消预约</el-button>
        <div class="cancel-tip" v-if="b.canCancel && b.courseCancelDeadline">
          {{ dateTime(b.courseCancelDeadline) }} 前可取消
        </div>
        <div class="cancel-tip" v-if="b.timeout">已超时未核销</div>
      </div>
    </div>

    <el-empty v-if="!list.length && !loading" description="还没有预约记录" class="h-empty">
      <el-button type="primary" @click="$router.push('/courses')">去看看课程</el-button>
    </el-empty>

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

    <!-- 核销码弹窗 -->
    <el-dialog v-model="codeDialog" title="到场核销码" width="360px" align-center>
      <div class="code-body" v-if="codeInfo">
        <img v-if="codeQr" :src="codeQr" class="qr" alt="核销二维码" />
        <div class="short-code">{{ codeInfo.code }}</div>
        <div class="code-course">{{ codeInfo.courseTitle }}</div>
        <div class="code-line">{{ dateTime(codeInfo.courseStartTime) }} · {{ codeInfo.courseAddress }}</div>
        <div class="code-tip">请向匠人出示此二维码或口述短码完成核销</div>
        <el-button text type="primary" @click="copyText(codeInfo.code)">复制短码</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import QRCode from 'qrcode'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, copyText, BOOKING_STATUS, BOOKING_STEP_NAMES } from '@/utils/format'

const router = useRouter()

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
const counts = ref({})
const total = ref(0)
const pageNum = ref(1)
const pageSize = 6
const loading = ref(false)
const status = ref('null')

const codeDialog = ref(false)
const codeQr = ref('')
const codeInfo = ref(null)

function stepOf(b) {
  return [0, 1, 4, 5].indexOf(b.status) + 1
}

function stepDesc(b, i) {
  if (i === 0) return dateTime(b.createTime)
  if (i === 1) return b.auditTime ? dateTime(b.auditTime) : '等待匠人审核'
  if (i === 2) return b.checkinTime ? dateTime(b.checkinTime) : '开课后凭码签到'
  return b.status === 5 ? '体验结束' : ''
}

function goCourse(b) {
  if (b.canViewCourse) router.push(`/courses/${b.courseId}`)
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/booking/mine', {
      status: status.value === 'null' ? undefined : Number(status.value),
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
    counts.value = (await http.get('/portal/booking/count')) || {}
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

async function showCode(b) {
  const code = await http.get(`/portal/booking/${b.id}/checkin-code`)
  codeInfo.value = {
    code,
    courseTitle: b.courseTitle,
    courseStartTime: b.courseStartTime,
    courseAddress: b.courseAddress
  }
  codeQr.value = await QRCode.toDataURL(String(code), { width: 240, margin: 1 })
  codeDialog.value = true
}

async function cancelBooking(b) {
  const { value } = await ElMessageBox.prompt(
    `取消后名额立即释放。${b.courseCancelDeadline ? `该课程 ${dateTime(b.courseCancelDeadline)} 后不可取消。` : ''}请填写取消原因：`,
    '取消预约',
    { inputPlaceholder: '选填，如时间冲突', type: 'warning' }
  )
  await http.post('/portal/booking/cancel', { bookingId: b.id, reason: value || undefined })
  ElMessage.success('已取消，名额已释放')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.booking { display: flex; gap: 20px; padding: 18px; margin-bottom: 14px; align-items: center; }
.left { display: flex; gap: 14px; width: 380px; flex: none; }
.cover { width: 120px; height: 82px; object-fit: cover; border-radius: 8px; cursor: pointer; flex: none; }
.info { min-width: 0; }
.title { font-weight: 700; font-size: 15px; cursor: pointer; line-height: 1.4; }
.title:hover { color: var(--h-primary); }
.line { display: flex; align-items: center; gap: 5px; color: var(--h-text-second); font-size: 12px; margin-top: 3px; }
.mid { flex: 1; min-width: 0; }
.steps :deep(.el-step__title) { font-size: 12px; }
.closed-state { text-align: center; }
.reason { color: #d48806; font-size: 12px; margin-top: 8px; }
.right {
  width: 150px; flex: none; display: flex; flex-direction: column;
  align-items: flex-end; gap: 8px;
}
.cancel-tip { color: var(--h-text-second); font-size: 11px; text-align: right; }
.pager { display: flex; justify-content: center; margin-top: 20px; }
.code-body { text-align: center; }
.qr { width: 240px; height: 240px; margin: 0 auto; }
.short-code {
  font-size: 26px; font-weight: 800; letter-spacing: 6px;
  color: var(--h-primary); margin-top: 12px; font-variant-numeric: tabular-nums;
}
.code-course { font-weight: 600; margin-top: 8px; }
.code-line { color: var(--h-text-second); font-size: 12px; margin-top: 4px; }
.code-tip { color: #d48806; font-size: 12px; margin: 10px 0; background: #fdf6e7; border-radius: 8px; padding: 6px; }
</style>
