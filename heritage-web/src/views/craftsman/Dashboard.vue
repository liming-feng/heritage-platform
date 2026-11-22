<template>
  <div class="dash">
    <!-- 概览卡 -->
    <div class="cards">
      <div class="card h-card">
        <div class="c-label">本月销售额</div>
        <div class="c-val h-price">¥{{ ov.salesAmount ?? '—' }}</div>
        <div class="c-sub" :class="(ov.salesGrowthRate ?? 0) >= 0 ? 'up' : 'down'">
          环比 {{ ov.salesGrowthRate ?? 0 }}%
        </div>
      </div>
      <div class="card h-card">
        <div class="c-label">本月订单数</div>
        <div class="c-val">{{ ov.orderCount ?? '—' }}</div>
        <div class="c-sub">上月 ¥{{ ov.lastMonthAmount ?? '—' }}</div>
      </div>
      <div class="card h-card">
        <div class="c-label">本月预约人数</div>
        <div class="c-val">{{ ov.bookingPeople ?? '—' }}</div>
        <div class="c-sub">课程预约是店铺的核心转化</div>
      </div>
      <div class="card h-card">
        <div class="c-label">作品浏览总量</div>
        <div class="c-val">{{ ov.goodsViewTotal ?? '—' }}</div>
        <div class="c-sub">{{ ov.monthText }}</div>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 销售趋势 -->
      <el-col :span="14">
        <div class="h-card chart-card">
          <div class="h-section-title">近 30 天销售趋势</div>
          <div ref="trendRef" class="chart"></div>
        </div>
      </el-col>
      <!-- 待办提醒 -->
      <el-col :span="10">
        <div class="h-card chart-card">
          <div class="h-section-title">待办提醒</div>
          <div class="todos">
            <!--
              每条待办都带筛选参数跳转：点进去要直接看到「这批要处理的东西」，
              而不是落到一个未筛选的列表让用户再点一次。
              参数名与目标页的 query 一一对应（tab / stockWarn / replied）。
            -->
            <div class="todo" @click="goTodos('/craftsman/orders', { tab: '1' })">
              <span>待发货订单</span><b class="num" :class="{ alert: todo.waitDeliverOrders > 0 }">{{ todo.waitDeliverOrders }}</b>
            </div>
            <div class="todo" @click="goTodos('/craftsman/bookings', { tab: '0' })">
              <span>待审核预约</span><b class="num" :class="{ alert: todo.waitAuditBookings > 0 }">{{ todo.waitAuditBookings }}</b>
            </div>
            <div class="todo" @click="goTodos('/craftsman/orders', { tab: 'refund' })">
              <span>退款申请</span><b class="num" :class="{ alert: todo.applyingRefunds > 0 }">{{ todo.applyingRefunds }}</b>
            </div>
            <div class="todo" @click="goTodos('/craftsman/goods', { stockWarn: '1' })">
              <span>库存预警商品</span><b class="num" :class="{ alert: todo.stockWarnGoods > 0 }">{{ todo.stockWarnGoods }}</b>
            </div>
            <div class="todo" @click="goTodos('/craftsman/reviews', { replied: '1' })">
              <span>未回复评价</span><b class="num" :class="{ alert: todo.unrepliedReviews > 0 }">{{ todo.unrepliedReviews }}</b>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="row2">
      <el-col :span="12">
        <div class="h-card chart-card">
          <div class="h-section-title">商品销量 Top5</div>
          <div ref="goodsRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="h-card chart-card">
          <div class="h-section-title">课程预约排行</div>
          <div ref="bookingRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'

const router = useRouter()

/** 待办跳转：带上筛选参数，落地页直接呈现待处理的那一批 */
function goTodos(path, query) {
  router.push({ path, query })
}

const ov = ref({})
const todo = ref({})
const trendRef = ref()
const goodsRef = ref()
const bookingRef = ref()
let charts = []

function render(el, option) {
  if (!el) return
  const c = echarts.init(el)
  c.setOption(option)
  charts.push(c)
}

async function load() {
  ov.value = await http.get('/craftsman/dashboard/overview')
  todo.value = await http.get('/craftsman/dashboard/todo')

  const trend = await http.get('/craftsman/dashboard/sales-trend', { days: 30 })
  render(trendRef.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 30, bottom: 28 },
    xAxis: { type: 'category', data: (trend.points || []).map((p) => p.dateText) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: (trend.points || []).map((p) => p.amount), areaStyle: { opacity: 0.12 }, name: '销售额' }]
  })

  const goods = await http.get('/craftsman/dashboard/goods-top')
  render(goodsRef.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 10, bottom: 24 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: (goods || []).slice(0, 5).map((g) => g.goodsName).reverse() },
    series: [{ type: 'bar', data: (goods || []).slice(0, 5).map((g) => g.salesQty).reverse(), itemStyle: { color: '#c96a66', borderRadius: [0, 6, 6, 0] } }]
  })

  const rank = await http.get('/craftsman/dashboard/booking-rank')
  render(bookingRef.value, {
    tooltip: { trigger: 'axis' },
    grid: { left: 130, right: 30, top: 10, bottom: 24 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: (rank || []).slice(0, 5).map((r) => r.courseTitle).reverse() },
    series: [{ type: 'bar', data: (rank || []).slice(0, 5).map((r) => r.bookingCount).reverse(), itemStyle: { color: '#d4b572', borderRadius: [0, 6, 6, 0] } }]
  })
}

onMounted(load)
onUnmounted(() => charts.forEach((c) => c.dispose()))
</script>

<style scoped>
.cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.card { padding: 18px 20px; }
.c-label { color: var(--h-text-second); font-size: 13px; }
.c-val { font-size: 26px; font-weight: 800; margin-top: 6px; }
.c-sub { font-size: 12px; color: var(--h-text-second); margin-top: 4px; }
.c-sub.up { color: #389e0d; }
.c-sub.down { color: var(--h-primary); }
.chart-card { padding: 18px 20px; margin-bottom: 16px; }
.chart { height: 280px; }
.todos { display: flex; flex-direction: column; gap: 6px; }
.todo {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-radius: 10px; cursor: pointer;
  background: var(--h-paper);
}
.todo:hover { background: var(--h-paper-dark); }
.todo .num { font-size: 22px; font-weight: 800; }
.todo .num.alert { color: var(--h-primary); }
.row2 { margin-top: 0; }
@media (max-width: 1100px) { .cards { grid-template-columns: repeat(2, 1fr); } }
</style>
