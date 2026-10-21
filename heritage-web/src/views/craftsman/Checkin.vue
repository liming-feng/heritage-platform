<template>
  <div class="page">
    <div class="h-page-header">
      <h2>核销签到台</h2>
      <span class="sub">学员出示核销码，扫码或输码完成签到</span>
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
        <el-divider>今日待核销</el-divider>
        <div class="today-list">
          <div v-for="b in todayList" :key="b.id" class="today-item">
            <div>
              <b>{{ b.contactName }}</b>（{{ b.quota }}人）
              <div class="sub">{{ b.courseTitle }} · {{ dateTime(b.courseStartTime) }}</div>
            </div>
            <el-button size="small" type="primary" plain @click="code = b.checkinCode || ''; $nextTick(doCheckin)">直接核销</el-button>
          </div>
          <el-empty v-if="!todayList.length" description="今日暂无待核销预约" :image-size="60" />
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
  // 近期已通过待核销的预约，用于「直接核销」
  const data = await http.get('/craftsman/booking/list', { status: 1, pageNum: 1, pageSize: 8 })
  todayList.value = data.records || []
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
.today-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 12px; border-radius: 10px; background: var(--h-paper); margin-bottom: 8px;
}
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.result-info { text-align: left; margin-bottom: 16px; }
.r-row { display: flex; gap: 14px; margin-bottom: 8px; font-size: 13px; }
.r-row span { color: var(--h-text-second); width: 66px; flex: none; }
</style>
