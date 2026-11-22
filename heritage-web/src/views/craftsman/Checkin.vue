<template>
  <div class="page">
    <div class="h-page-header">
      <h2>核销签到台</h2>
      <span class="sub">学员出示核销码，扫码或输码完成签到（开课前 30 分钟 ~ 课程结束可核销）</span>
    </div>

    <div class="main-grid">
      <!-- 扫码区 -->
      <div class="h-card scan-card">
        <div class="scan-box" :class="{ scanning }">
          <el-icon :size="56" color="#9c2c2c"><Crop /></el-icon>
          <div class="scan-tip">将学员的核销二维码对准扫码框</div>
          <div class="scan-sub">（演示环境无摄像头硬件，可使用下方手动输码）</div>
        </div>
        <el-input
          v-model="code"
          size="large"
          placeholder="或输入明文短码，回车核销"
          class="code-input"
          @keyup.enter="doCheckin"
          clearable
        >
          <template #append>
            <el-button type="primary" :loading="checking" @click="doCheckin">核 销</el-button>
          </template>
        </el-input>
        <el-divider>待核销（课程未结束）</el-divider>
        <div class="today-list">
          <div v-for="b in todayList" :key="b.id" class="today-item">
            <div>
              <b>{{ b.contactName }}</b>（{{ b.quota }}人）
              <div class="sub">{{ b.courseTitle }} · {{ dateTime(b.courseStartTime) }}</div>
            </div>
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="!canCheckinNow(b)"
              @click="code = b.checkinCode || ''; $nextTick(doCheckin)"
            >
              {{ canCheckinNow(b) ? '直接核销' : '未到核销时间' }}
            </el-button>
          </div>
          <el-empty v-if="!todayList.length" :image-size="60">
            <template #description>
              <div class="empty-title">当前没有可核销的预约</div>
              <div class="empty-reason">
                核销窗口是<b>开课前 30 分钟 ~ 课程结束</b>。如果你名下的预约都对应已结束的课程，
                这里就会是空的 —— 它们已经过了核销期，不能再用核销码补签。
              </div>
              <div class="empty-reason">
                想在演示时有数据：把某门课的结束时间改到将来，再让学员预约并通过即可。
              </div>
            </template>
          </el-empty>
        </div>
      </div>

      <!-- 核销结果 -->
      <div class="h-card result-card">
        <template v-if="result">
          <el-result icon="success" title="核销成功" :sub-title="result.statusDesc">
            <template #extra>
              <div class="result-info">
                <div class="r-row"><span>预约号</span><b>{{ result.bookingNo }}</b></div>
                <div class="r-row"><span>课程</span><b>{{ result.courseTitle }}</b></div>
                <div class="r-row"><span>学员</span><b>{{ result.contactName }} {{ result.contactPhone }}</b></div>
                <div class="r-row"><span>人数</span><b>{{ result.quota }} 人</b></div>
                <div class="r-row"><span>上课地点</span><b>{{ result.courseAddress }}</b></div>
                <div class="r-row"><span>开课时间</span><b>{{ dateTime(result.courseStartTime) }}</b></div>
                <div class="r-row"><span>核销方式</span><b>{{ result.checkinWay === 1 ? '扫码核销' : result.checkinWay === 2 ? '输码核销' : '后台核销' }}</b></div>
                <div class="r-row"><span>核销时间</span><b>{{ dateTime(result.checkinTime) }}</b></div>
              </div>
              <el-button type="primary" plain @click="result = null">继续核销下一位</el-button>
            </template>
          </el-result>
        </template>
        <el-empty v-else description="核销结果将展示在这里" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { dateTime } from '@/utils/format'

const code = ref('')
const checking = ref(false)
const scanning = ref(false)
const result = ref(null)
const todayList = ref([])

async function doCheckin() {
  const v = code.value.trim()
  if (!v) return ElMessage.warning('请先扫码或输入核销短码')
  checking.value = true
  try {
    result.value = await http.post('/craftsman/booking/checkin', { checkinCode: v })
    ElMessage.success(`已为 ${result.value.contactName} 完成核销`)
    code.value = ''
    loadToday()
  } finally {
    checking.value = false
  }
}

async function loadToday() {
  // 只取「课程尚未结束」的已通过预约（checkinable）：
  // 课程一结束就再也核销不了，列出来只会让人点了白点一次
  const data = await http.get('/craftsman/booking/list', {
    status: 1,
    checkinable: true,
    pageNum: 1,
    pageSize: 8
  })
  todayList.value = data.records || []
}

/** 与后端 Constants.CHECKIN_ADVANCE_MINUTES 一致：开课前 30 分钟才可核销 */
const CHECKIN_ADVANCE_MINUTES = 30

/** 现在是否已进入核销窗口（课程结束时间由后端过滤，这里只判开课前 30 分钟） */
function canCheckinNow(row) {
  if (!row.courseStartTime) return true
  const start = new Date(String(row.courseStartTime).replace(/-/g, '/')).getTime()
  if (Number.isNaN(start)) return true
  return Date.now() >= start - CHECKIN_ADVANCE_MINUTES * 60 * 1000
}

onMounted(loadToday)
</script>

<style scoped>
.main-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 1100px) { .main-grid { grid-template-columns: 1fr; } }
.scan-card, .result-card { padding: 22px 26px; }
.scan-box {
  border: 2px dashed #e0b4b2; border-radius: 14px;
  padding: 34px 0; text-align: center; background: #fdf7f6;
  margin-bottom: 16px;
}
.scan-box.scanning { border-color: var(--h-primary); animation: pulse 1.2s infinite; }
@keyframes pulse { 50% { background: #fbeeed; } }
.scan-tip { font-weight: 600; margin-top: 10px; }
.scan-sub { color: var(--h-text-second); font-size: 12px; margin-top: 4px; }
.empty-title { font-size: 14px; color: var(--h-text); font-weight: 600; margin-bottom: 6px; }
.empty-reason { font-size: 12px; color: var(--h-text-second); line-height: 1.7; max-width: 380px; margin: 0 auto 4px; }
.today-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 10px; background: var(--h-paper); margin-bottom: 8px;
}
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.result-info { text-align: left; margin-bottom: 16px; }
.r-row { display: flex; gap: 14px; margin-bottom: 8px; font-size: 13px; }
.r-row span { color: var(--h-text-second); width: 66px; flex: none; }
</style>
