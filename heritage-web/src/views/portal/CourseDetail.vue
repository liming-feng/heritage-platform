<template>
  <div class="h-container page" v-if="course.id">
    <div class="main h-card">
      <div class="gallery">
        <img class="big" :src="img(course.cover)" @error="onImgErr" />
      </div>

      <div class="info">
        <h1 class="title">{{ course.title }}</h1>
        <p class="subtitle">{{ course.subtitle }}</p>

        <div class="price-row">
          <span class="h-price big">¥{{ course.price }}</span>
          <el-tag :type="COURSE_STATUS[course.status]?.type" effect="dark" size="small">{{ course.statusDesc }}</el-tag>
          <span class="views">浏览 {{ course.viewCount }} · {{ course.bookingCount }} 人已预约</span>
        </div>

        <!-- ★ 预约核心信息：名额 + 截止倒计时 -->
        <div class="booking-box">
          <div class="quota-row">
            <span class="k">剩余名额</span>
            <span :class="['quota-num', quotaLevel]">
              <b>{{ course.remainQuota }}</b>
              <span class="total-q">/ {{ course.totalQuota }}</span>
            </span>
            <el-progress
              class="quota-bar"
              :percentage="bookedPercent"
              :stroke-width="10"
              :color="barColor"
            />
          </div>
          <div class="quota-row" v-if="course.status !== 2 && course.remainSeconds !== null">
            <span class="k">预约截止</span>
            <span :class="['deadline', { danger: course.remainSeconds < 86400 }]">
              <el-icon><AlarmClock /></el-icon>
              <b>{{ countdown(remainSecondsNow) }}</b>
            </span>
          </div>
          <div class="quota-row" v-else-if="course.status !== 2">
            <span class="k">预约截止</span>
            <span class="closed">已过预约时间</span>
          </div>
          <div class="closed-reason" v-if="!course.canBook && course.status !== 2">
            <el-icon><WarningFilled /></el-icon>
            {{ course.closedReason || '当前不可预约' }}
          </div>
        </div>

        <div class="kvs">
          <div class="kv"><span class="k">开课时间</span>{{ dateTime(course.startTime) }}</div>
          <div class="kv"><span class="k">结束时间</span>{{ dateTime(course.endTime) }}</div>
          <div class="kv"><span class="k">课程时长</span>{{ course.durationText || (course.duration ? course.duration + ' 分钟' : '—') }}</div>
          <div class="kv"><span class="k">上课地点</span>{{ course.address || '—' }}</div>
          <div class="kv"><span class="k">难度等级</span>{{ difficultyText }}</div>
          <div class="kv" v-if="course.craftsman">
            <span class="k">授课匠人</span>
            <router-link :to="`/craftsmans/${course.craftsman.id}`" class="link">
              {{ course.craftsman.realName }}（{{ course.craftsman.craftType }}）
            </router-link>
          </div>
        </div>

        <div class="btns">
          <el-button
            type="primary"
            size="large"
            class="book-btn"
            :disabled="!course.canBook"
            @click="openBooking"
          >
            {{ course.canBook ? '立即预约' : course.closedReason || '暂不可预约' }}
          </el-button>
        </div>
        <div class="tips">预约提交后需等待匠人审核，审核结果将通过站内消息通知；开课后凭核销码到场签到。</div>
      </div>
    </div>

    <!-- 详情与评价 -->
    <div class="h-card block">
      <div class="h-section-title">课程介绍</div>
      <div class="brief">{{ course.description }}</div>
      <div class="rich" v-html="richText(course.detail)"></div>
    </div>
    <div class="h-card block">
      <div class="h-section-title">学员评价（{{ course.reviewCount || 0 }}）<span class="rate" v-if="course.avgScore">平均 {{ course.avgScore }} 分</span></div>
      <el-empty v-if="!(course.reviews || []).length" description="暂无评价" :image-size="80" />
    </div>

    <!-- 预约表单弹窗 -->
    <el-dialog v-model="bookDialog" title="预约体验课程" width="480px" :close-on-click-modal="false">
      <div class="course-brief">
        <img :src="img(course.cover)" @error="onImgErr" />
        <div>
          <div class="cb-title">{{ course.title }}</div>
          <div class="cb-line">{{ dateTime(course.startTime) }} · {{ course.address }}</div>
          <div class="cb-line">剩余名额 <b :class="quotaLevel">{{ course.remainQuota }}</b>，预约 1 人占用 1 个名额</div>
        </div>
      </div>
      <el-form ref="bookFormRef" :model="bookForm" :rules="bookRules" label-width="80px">
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="bookForm.contactName" placeholder="请输入真实姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="手机号" prop="contactPhone">
          <el-input v-model="bookForm.contactPhone" placeholder="用于开课提醒与联系" maxlength="11" />
        </el-form-item>
        <el-form-item label="报名人数" prop="quota">
          <el-input-number v-model="bookForm.quota" :min="1" :max="maxQuota" />
          <span class="quota-hint" v-if="maxQuota > 1">最多可约 {{ maxQuota }} 人（受每单名额上限限制）</span>
          <span class="quota-hint danger" v-else>当前仅剩 1 个名额</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bookForm.remark" type="textarea" :rows="2" maxlength="100" show-word-limit placeholder="有无基础、特殊需求等（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBooking">提交预约</el-button>
      </template>
    </el-dialog>
  </div>
  <div class="h-container page" v-else-if="!loading"><el-empty description="课程不存在" /></div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr, dateTime, countdown, richText, COURSE_STATUS } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const course = ref({})
const loading = ref(true)
const bookDialog = ref(false)
const submitting = ref(false)

/** 本地倒计时（从接口 remainSeconds 起每秒递减） */
const remainSecondsNow = ref(0)
let ticker = null

const bookedPercent = computed(() =>
  course.value.totalQuota ? Math.round(((course.value.totalQuota - course.value.remainQuota) / course.value.totalQuota) * 100) : 0
)
const quotaLevel = computed(() => {
  const c = course.value
  const ratio = c.totalQuota ? c.remainQuota / c.totalQuota : 0
  return ratio <= 0.2 ? 'level-danger' : ratio <= 0.5 ? 'level-warn' : 'level-ok'
})
const barColor = computed(() => (quotaLevel.value === 'level-danger' ? '#9c2c2c' : quotaLevel.value === 'level-warn' ? '#d48806' : '#389e0d'))
const difficultyText = computed(() => ['', '入门', '初级', '中级', '高级'][course.value.difficulty] || '—')
const maxQuota = computed(() => Math.max(1, Math.min(course.value.remainQuota || 1, 5)))

const bookFormRef = ref()
const bookForm = reactive({ contactName: '', contactPhone: '', quota: 1, remark: '' })
/** 校验规则与后端一致：姓名非空、手机号 11 位 */
const bookRules = {
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  quota: [{ required: true, message: '请选择人数', trigger: 'change' }]
}

async function load() {
  loading.value = true
  try {
    const detail = await http.get(`/portal/course/${route.params.id}`)
    course.value = { ...detail, ...detail }
    // detail 返回 CourseListVO 字段 + CourseDetailVO 扩展字段（组合返回）
    remainSecondsNow.value = detail.remainSeconds ?? 0
    startTicker()
    reportHistory()
  } finally {
    loading.value = false
  }
}

/** 上报浏览历史（targetType=2 课程），供用户端「我的浏览」使用；未登录不上报 */
function reportHistory() {
  if (!user.isLogin) return
  http
    .post('/portal/history/upload', { items: [{ targetType: 2, targetId: Number(route.params.id) }] })
    .catch(() => { /* 上报失败不影响浏览 */ })
}

function startTicker() {
  stopTicker()
  if (!course.value.canBook) return
  ticker = setInterval(() => {
    if (remainSecondsNow.value > 0) remainSecondsNow.value -= 1
  }, 1000)
}
function stopTicker() {
  if (ticker) clearInterval(ticker)
  ticker = null
}

function openBooking() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (course.value.remainQuota <= 0) return
  bookForm.quota = 1
  bookDialog.value = true
}

async function submitBooking() {
  await bookFormRef.value.validate()
  submitting.value = true
  try {
    await http.post('/portal/booking/submit', {
      courseId: course.value.id,
      contactName: bookForm.contactName,
      contactPhone: bookForm.contactPhone,
      quota: bookForm.quota,
      remark: bookForm.remark || undefined
    })
    bookDialog.value = false
    ElMessage.success('预约提交成功，等待匠人审核')
    router.push('/my/bookings')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
onUnmounted(stopTicker)
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 18px; }
.main { display: flex; gap: 30px; padding: 26px; }
.gallery { width: 440px; flex: none; }
.gallery .big { width: 100%; aspect-ratio: 16/10; object-fit: cover; border-radius: 10px; border: 1px solid var(--h-border); }
.info { flex: 1; min-width: 0; }
.title { font-size: 24px; margin: 0 0 6px; }
.subtitle { color: var(--h-text-second); margin: 0 0 14px; }
.price-row { display: flex; align-items: baseline; gap: 12px; margin-bottom: 16px; }
.big { font-size: 30px; }
.views { margin-left: auto; color: var(--h-text-second); font-size: 12px; }

/* 预约核心信息盒 */
.booking-box {
  background: linear-gradient(90deg, #fdf3f2, #faf7f2);
  border: 1px solid #f0dcd9; border-radius: 10px;
  padding: 16px 18px; margin-bottom: 16px;
}
.quota-row { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; }
.quota-row:last-child { margin-bottom: 0; }
.k { color: var(--h-text-second); width: 70px; flex: none; font-size: 13px; }
.quota-num b { font-size: 30px; }
.total-q { font-size: 14px; color: var(--h-text-second); }
.level-ok { color: #389e0d; }
.level-warn { color: #d48806; }
.level-danger { color: var(--h-primary); }
.quota-bar { flex: 1; }
.deadline { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.deadline b { font-variant-numeric: tabular-nums; font-size: 20px; color: #d48806; }
.deadline.danger b { color: var(--h-primary); }
.closed { color: #b9b0a5; }
.closed-reason {
  color: var(--h-primary); font-size: 13px; display: flex; align-items: center; gap: 6px;
  background: #fdf3f2; border-radius: 8px; padding: 6px 10px;
}

.kvs { margin-bottom: 16px; }
.kv { display: flex; gap: 14px; margin-bottom: 8px; font-size: 14px; }
.kv .k { color: var(--h-text-second); flex: none; width: 70px; }
.link { color: var(--h-primary); }
.btns { display: flex; gap: 12px; }
.book-btn { min-width: 260px; font-size: 16px; letter-spacing: 2px; }
.tips { color: var(--h-text-second); font-size: 12px; margin-top: 12px; }

.block { padding: 22px 28px; }
.brief { margin-bottom: 12px; }
.rate { margin-left: 12px; font-size: 14px; color: #d48806; font-weight: 400; }
.course-brief { display: flex; gap: 14px; margin-bottom: 18px; }
.course-brief img { width: 110px; height: 74px; object-fit: cover; border-radius: 8px; flex: none; }
.cb-title { font-weight: 700; margin-bottom: 4px; }
.cb-line { font-size: 13px; color: var(--h-text-second); }
.cb-line b { font-size: 16px; }
.quota-hint { margin-left: 10px; font-size: 12px; color: var(--h-text-second); }
.quota-hint.danger { color: var(--h-primary); }
</style>
