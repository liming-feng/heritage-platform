<template>
  <div class="dash">
    <!-- 总览指标 -->
    <div class="cards">
      <div class="card h-card" v-for="c in cards" :key="c.label">
        <div class="c-label">{{ c.label }}</div>
        <div class="c-val" :class="c.cls">{{ c.value }}</div>
      </div>
    </div>

    <!-- 审核待办：每一条都能点进去，落地页自动带上对应筛选 -->
    <div class="todo-bar h-card">
      <span class="t-label">待办：</span>
      <button
        v-for="t in todoItems"
        :key="t.label"
        type="button"
        class="t-chip"
        :class="[t.tone, { zero: !t.count }]"
        :title="t.count ? `查看${t.label}` : `暂无${t.label}`"
        @click="t.count && goTodos(t)"
      >
        <span>{{ t.label }}</span>
        <b>{{ t.count }}</b>
        <i v-if="t.count">›</i>
      </button>
    </div>

    <el-row :gutter="14">
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">非遗作品数量（按分类）</div><div ref="c1" class="chart"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">商品销量（按分类·件）</div><div ref="c2" class="chart"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">商品分类销售额占比</div><div ref="c3" class="chart"></div></div>
      </el-col>
    </el-row>
    <el-row :gutter="14">
      <el-col :span="12">
        <div class="h-card chart-card"><div class="ch-title">平台交易金额趋势（30 天）</div><div ref="c4" class="chart"></div></div>
      </el-col>
      <el-col :span="6">
        <div class="h-card chart-card"><div class="ch-title">订单状态统计</div><div ref="c5" class="chart"></div></div>
      </el-col>
      <el-col :span="6">
        <div class="h-card chart-card"><div class="ch-title">用户访问与收藏</div><div ref="c6" class="chart"></div></div>
      </el-col>
    </el-row>
    <el-row :gutter="14">
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">课程热度（占用名额）</div><div ref="c7" class="chart"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">优惠券领取与使用</div><div ref="c8" class="chart"></div></div>
      </el-col>
      <el-col :span="8">
        <div class="h-card chart-card"><div class="ch-title">匠人销售排行 Top5</div><div ref="c9" class="chart"></div></div>
      </el-col>
    </el-row>
    <el-row :gutter="14">
      <el-col :span="12">
        <div class="h-card chart-card"><div class="ch-title">用户增长趋势（30 天）</div><div ref="c10" class="chart"></div></div>
      </el-col>
      <el-col :span="12">
        <div class="h-card chart-card"><div class="ch-title">月度活跃用户（近 6 月）</div><div ref="c11" class="chart"></div></div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { http } from '@/utils/request'

const router = useRouter()

/**
 * 待办条的四项。
 * 每一项都对应「列表页 + 一个筛选参数」，参数名与目标页的 query 一一对应：
 *   auditStatus=0（待审核） / status=0（反馈待处理）
 *
 * 这里用**路由 name** 而不是手写 path：真实路径是 /admin/craftsmans（复数带 s），
 * 手写成 /admin/craftsmen 就会 404 —— name 由 router 保证唯一，改路径也不会断。
 *
 * 数字为 0 时保持置灰不可点，避免点进去看到一个空列表还以为没生效。
 */
const todoItems = computed(() => [
  { label: '待审核商品', count: Number(ov.value.waitGoods || 0), tone: 'warn', name: 'AdminGoods', query: { auditStatus: 0 } },
  { label: '待审核课程', count: Number(ov.value.waitCourse || 0), tone: 'warn', name: 'AdminCourses', query: { auditStatus: 0 } },
  { label: '待审核匠人', count: Number(ov.value.waitCraftsman || 0), tone: 'warn', name: 'AdminCraftsmans', query: { auditStatus: 0 } },
  { label: '未处理反馈', count: Number(ov.value.waitFeedback || 0), tone: 'info', name: 'AdminFeedbacks', query: { status: 0 } }
])

function goTodos(item) {
  router.push({ name: item.name, query: item.query })
}

const ov = ref({})
const charts = []
const c1 = ref(); const c2 = ref(); const c3 = ref(); const c4 = ref(); const c5 = ref()
const c6 = ref(); const c7 = ref(); const c8 = ref(); const c9 = ref(); const c10 = ref(); const c11 = ref()

const cards = computed(() => [
  { label: '注册用户', value: ov.value.userTotal ?? '—' },
  { label: '入驻匠人', value: ov.value.craftsmanTotal ?? '—' },
  { label: '在售作品', value: ov.value.goodsOnSale ?? '—' },
  { label: '开放课程', value: ov.value.courseOpen ?? '—' },
  { label: '累计订单', value: ov.value.orderTotal ?? '—', cls: 'h-price' },
  { label: '交易金额', value: ov.value.payAmount != null ? '¥' + ov.value.payAmount : '—', cls: 'h-price' },
  { label: '预约总量', value: ov.value.bookingTotal ?? '—', cls: 'h-price' }
])

function render(el, option) {
  if (!el) return
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
}

const PIE_COLORS = ['#9c2c2c', '#c96a66', '#c8a45c', '#8a6d2f', '#6b7c5e', '#5e7c8a', '#a8843c', '#b05a4a']

function pieOption(items) {
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    series: [{
      type: 'pie', radius: ['34%', '62%'], center: ['50%', '52%'],
      data: (items || []).map((i) => ({ name: i.name, value: i.value })),
      label: { fontSize: 11, formatter: '{b}' },
      color: PIE_COLORS
    }]
  }
}

function barOption(names, values, color, unit = '') {
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v) => v + unit },
    grid: { left: 8, right: 46, top: 10, bottom: 6, containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 11, width: 90, overflow: 'truncate' } },
    series: [{ type: 'bar', data: values, itemStyle: { color, borderRadius: [0, 5, 5, 0] } }]
  }
}

async function load() {
  ov.value = await http.get('/admin/stat/overview')

  const pie1 = await http.get('/admin/stat/goods-count-pie')
  render(c1.value, pieOption(pie1.items))

  const pie2 = await http.get('/admin/stat/sales-quantity-pie')
  render(c2.value, pieOption(pie2.items))

  const pie3 = await http.get('/admin/stat/category-sales-share')
  render(c3.value, pieOption(pie3.items))

  const trend = await http.get('/admin/stat/sales-trend', { days: 30 })
  render(c4.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 60, right: 20, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: (trend.points || []).map((p) => p.label), axisLabel: { fontSize: 10, interval: 4, hideOverlap: true } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: (trend.points || []).map((p) => p.value), areaStyle: { opacity: 0.14 }, itemStyle: { color: '#9c2c2c' } }]
  })

  const order = await http.get('/admin/stat/order-status')
  render(c5.value, {
    tooltip: { trigger: 'item', formatter: '{b}: {c}' },
    legend: { bottom: 0, left: 'center', itemWidth: 9, itemHeight: 9, textStyle: { fontSize: 10 } },
    series: [{ type: 'pie', radius: '50%', center: ['50%', '46%'], data: (order.items || []).map((i) => ({ name: i.name, value: i.value })), color: PIE_COLORS, label: { show: false }, labelLine: { show: false } }]
  })

  const visit = await http.get('/admin/stat/visit-favorite')
  render(c6.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 12, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: ['商品浏览', '课程浏览', '资讯浏览', '总浏览', '收藏'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [visit.goodsView, visit.courseView, visit.articleView, visit.totalView, visit.favoriteTotal], itemStyle: { color: '#c8a45c', borderRadius: [5, 5, 0, 0] } }]
  })

  const heat = await http.get('/admin/stat/course-heat')
  const hItems = (heat.items || []).slice(0, 6)
  render(c7.value, barOption(hItems.map((i) => i.title).reverse(), hItems.map((i) => i.value ?? i.quota).reverse(), '#9c2c2c'))

  const coupon = await http.get('/admin/stat/coupon-use')
  render(c8.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 12, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: ['发放总量', '已领取', '已使用'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [coupon.receivedTotal, coupon.receivedTotal, coupon.usedTotal], itemStyle: { color: '#6b7c5e', borderRadius: [5, 5, 0, 0] } }]
  })

  const rank = await http.get('/admin/stat/craftsman-rank')
  const rItems = (rank.items || []).slice(0, 5)
  render(c9.value, barOption(rItems.map((i) => i.name).reverse(), rItems.map((i) => i.amount).reverse(), '#a8843c', ' 元'))

  const growth = await http.get('/admin/stat/user-growth', { days: 30 })
  render(c10.value, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['每日新增', '累计'], top: 0 },
    grid: { left: 44, right: 16, top: 30, bottom: 26 },
    xAxis: { type: 'category', data: (growth.points || []).map((p) => p.label), axisLabel: { fontSize: 10, interval: 4, hideOverlap: true } },
    yAxis: { type: 'value' },
    series: [
      { name: '每日新增', type: 'bar', data: (growth.points || []).map((p) => p.value), itemStyle: { color: '#c96a66' } },
      { name: '累计', type: 'line', smooth: true, data: (growth.points || []).map((p) => p.total ?? p.value), itemStyle: { color: '#8a6d2f' } }
    ]
  })

  const active = await http.get('/admin/stat/monthly-active', { months: 6 })
  render(c11.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 16, top: 20, bottom: 26 },
    xAxis: { type: 'category', data: (active.points || []).map((p) => p.label) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: (active.points || []).map((p) => p.value), itemStyle: { color: '#5e7c8a', borderRadius: [5, 5, 0, 0] } }]
  })
}

onMounted(async () => {
  await nextTick()
  load()
})
onUnmounted(() => charts.forEach((c) => c.dispose()))
</script>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(7, 1fr); gap: 12px; margin-bottom: 12px; }
.card { padding: 14px 16px; text-align: center; }
.c-label { color: var(--h-text-second); font-size: 12px; }
.c-val { font-size: 22px; font-weight: 800; margin-top: 4px; }
.todo-bar { display: flex; align-items: center; gap: 12px; padding: 10px 18px; margin-bottom: 12px; flex-wrap: wrap; }
.t-label { font-size: 13px; color: var(--h-text-second); }
.t-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border: 1px solid var(--h-border);
  border-radius: 8px;
  background: var(--h-paper);
  font-size: 13px;
  font-family: inherit;
  color: var(--h-ink);
  cursor: pointer;
  transition: border-color 0.18s, box-shadow 0.18s, transform 0.18s;
}
.t-chip b { font-size: 15px; font-weight: 700; }
.t-chip i { font-style: normal; color: var(--h-text-second); }
.t-chip.warn b { color: #b98900; }
.t-chip.info b { color: var(--h-text-second); }
.t-chip:hover {
  border-color: var(--h-primary);
  color: var(--h-primary);
  transform: translateY(-1px);
  box-shadow: var(--h-shadow);
}
.t-chip:hover b,
.t-chip:hover i { color: var(--h-primary); }
/* 数字为 0：置灰且不可点，避免点进去空空如也还以为是没反应 */
.t-chip.zero { cursor: default; opacity: 0.55; background: transparent; }
.t-chip.zero:hover { transform: none; box-shadow: none; border-color: var(--h-border); color: var(--h-ink); }
.t-chip.zero:hover b { color: #b98900; }
.t-chip.zero.info:hover b { color: var(--h-text-second); }
.chart-card { padding: 14px 16px 8px; margin-bottom: 14px; }
.ch-title { font-size: 14px; font-weight: 600; margin-bottom: 6px; }
.chart { height: 240px; }
.el-row { margin-bottom: 0; }
.el-col { margin-bottom: 0; }
@media (max-width: 1200px) { .cards { grid-template-columns: repeat(4, 1fr); } }
</style>
