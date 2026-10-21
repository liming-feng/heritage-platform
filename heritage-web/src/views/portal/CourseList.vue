<template>
  <div class="h-container page">
    <div class="page-head">
      <div class="h-section-title">非遗体验课程</div>
      <el-checkbox v-model="onlyBookable" label="只看可预约" @change="reload" />
    </div>

    <div class="filter h-card">
      <div class="f-item">
        <span class="f-label">分类</span>
        <a
          v-for="c in [{ id: null, name: '全部' }, ...categories]"
          :key="c.id ?? 'all'"
          :class="['f-opt', { active: query.categoryId === c.id }]"
          @click="pickCategory(c.id)"
        >{{ c.name }}</a>
      </div>
      <div class="f-item">
        <span class="f-label">排序</span>
        <a
          v-for="s in sorts"
          :key="s.value"
          :class="['f-opt', { active: query.sort === s.value }]"
          @click="pickSort(s.value)"
        >{{ s.label }}</a>
      </div>
      <span class="total">共 {{ total }} 门</span>
    </div>

    <div v-if="list.length" class="grid">
      <div v-for="c in list" :key="c.id" class="course h-card" @click="$router.push(`/courses/${c.id}`)">
        <div class="cover">
          <img :src="img(c.cover)" :alt="c.title" loading="lazy" @error="onImgErr" />
          <el-tag class="status-tag" :type="COURSE_STATUS[c.status]?.type" size="small" effect="dark">
            {{ c.statusDesc }}
          </el-tag>
        </div>
        <div class="body">
          <div class="title h-ellipsis-2">{{ c.title }}</div>
          <div class="sub h-ellipsis-1">{{ c.subtitle }}</div>

          <!-- 名额与截止 -->
          <div class="quota-line">
            <template v-if="c.status !== 2">
              <span :class="['quota', quotaLevel(c)]">
                剩余 <b>{{ c.remainQuota }}</b>/{{ c.totalQuota }}
              </span>
              <span v-if="c.canBook && c.remainSeconds !== null" class="deadline">
                距截止 <b>{{ countdown(c.remainSeconds) }}</b>
              </span>
              <span v-else-if="!c.canBook" class="closed">{{ c.closedReason }}</span>
            </template>
            <span v-else class="closed">课程已结束</span>
          </div>

          <div class="meta">
            <span class="h-price">¥{{ c.price }}</span>
            <span class="booked">{{ c.bookingCount }} 人已预约</span>
          </div>
          <div class="foot h-ellipsis-1">
            <span>{{ dateTime(c.startTime) }} 开课</span>
            <span class="addr h-ellipsis-1">{{ c.address }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无课程" class="h-empty" />

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="Number(total)"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, countdown, COURSE_STATUS } from '@/utils/format'

const categories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const onlyBookable = ref(false)

const sorts = [
  { value: 'default', label: '综合' },
  { value: 'hot', label: '最热' },
  { value: 'price_asc', label: '价格↑' },
  { value: 'price_desc', label: '价格↓' },
  { value: 'new', label: '最新' }
]

const query = reactive({ pageNum: 1, pageSize: 9, categoryId: null, keyword: '', sort: 'default', onlyBookable: undefined })

/** 名额紧张程度：<=20% 红、<=50% 橙、其余绿 */
function quotaLevel(c) {
  const ratio = c.totalQuota ? c.remainQuota / c.totalQuota : 0
  if (ratio <= 0.2) return 'level-danger'
  if (ratio <= 0.5) return 'level-warn'
  return 'level-ok'
}

/** 本地跳动的倒计时：remainSeconds 每秒 -1，展示用 format.countdown */
const timers = new Map()
function startTickers() {
  stopTickers()
  list.value.forEach((c) => {
    if (c.remainSeconds === null || c.remainSeconds === undefined) return
    timers.set(
      c.id,
      setInterval(() => {
        if (c.remainSeconds > 0) c.remainSeconds -= 1
      }, 1000)
    )
  })
}
function stopTickers() {
  timers.forEach((t) => clearInterval(t))
  timers.clear()
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/course/list', { ...query })
    list.value = data.records || []
    total.value = data.total || 0
    startTickers()
  } finally {
    loading.value = false
  }
}

function reload() {
  query.onlyBookable = onlyBookable.value ? 1 : undefined
  query.pageNum = 1
  load()
}

function pickCategory(id) {
  query.categoryId = id
  query.pageNum = 1
  load()
}

function pickSort(v) {
  query.sort = v
  query.pageNum = 1
  load()
}

onMounted(async () => {
  http.get('/portal/category/list').then((d) => (categories.value = d || [])).catch(() => {})
  await load()
})
onUnmounted(stopTickers)
</script>

<style scoped>
.page { padding-top: 22px; }
.page-head { display: flex; align-items: center; justify-content: space-between; }
.filter { display: flex; align-items: center; gap: 22px; padding: 12px 18px; margin: 14px 0 18px; flex-wrap: wrap; }
.f-item { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.f-label { color: var(--h-text-second); font-size: 13px; }
.f-opt { cursor: pointer; font-size: 13px; padding: 3px 8px; border-radius: 6px; }
.f-opt:hover { color: var(--h-primary); }
.f-opt.active { color: var(--h-primary); background: #f5e4e3; font-weight: 600; }
.total { margin-left: auto; color: var(--h-text-second); font-size: 13px; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
@media (max-width: 1000px) { .grid { grid-template-columns: repeat(2, 1fr); } }
.course { cursor: pointer; overflow: hidden; }
.course:hover { transform: translateY(-3px); }
.cover { position: relative; aspect-ratio: 16/9; overflow: hidden; background: var(--h-paper-dark); }
.cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.course:hover .cover img { transform: scale(1.04); }
.status-tag { position: absolute; top: 10px; left: 10px; }
.body { padding: 14px 16px 16px; }
.title { font-size: 16px; font-weight: 600; line-height: 1.45; min-height: 46px; }
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 3px; }
.quota-line {
  display: flex; align-items: center; gap: 10px; margin-top: 10px;
  font-size: 13px; min-height: 22px;
}
.quota b { font-size: 16px; }
.level-ok { color: #389e0d; }
.level-warn { color: #d48806; }
.level-danger { color: var(--h-primary); }
.deadline { color: var(--h-text-second); }
.deadline b { color: #d48806; font-variant-numeric: tabular-nums; }
.closed { color: #b9b0a5; }
.meta { display: flex; align-items: baseline; margin-top: 8px; }
.booked { margin-left: auto; color: var(--h-text-second); font-size: 12px; }
.foot { display: flex; gap: 10px; margin-top: 6px; font-size: 12px; color: var(--h-text-second); }
.foot .addr { flex: 1; min-width: 0; }
.pager { display: flex; justify-content: center; margin-top: 26px; }
</style>
