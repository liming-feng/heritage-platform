<template>
  <div class="page">
    <div class="h-page-header">
      <h2>我的商品</h2>
      <el-button type="primary" @click="$router.push('/craftsman/goods/edit')">+ 发布新品</el-button>
    </div>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="商品名称" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.status" placeholder="上架状态" clearable style="width: 130px" @change="reload">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
        <el-option label="待审核" :value="2" />
      </el-select>
      <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="reload">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-checkbox v-model="stockWarn" label="只看库存预警" @change="reload" />
      <el-button text type="warning" @click="showStockWarn">库存预警榜</el-button>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="goods-cell">
              <img :src="img(row.cover)" @error="onImgErr" />
              <div>
                <div class="g-name">{{ row.name }}</div>
                <div class="g-sub">{{ row.subtitle }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="stock" label="库存" width="90">
          <template #default="{ row }">
            <span :class="{ 'stock-warn': row.stockWarn }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="GOODS_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column label="审核" width="100">
          <template #default="{ row }">
            <el-tag :type="AUDIT_STATUS[row.auditStatus]?.type" size="small">{{ row.auditStatusDesc }}</el-tag>
            <div class="reject" v-if="row.auditStatus === 2">{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push(`/craftsman/goods/edit/${row.id}`)">编辑</el-button>
            <el-button text size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="shelf(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button text size="small" @click="showViews(row)">浏览量</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="Number(total)"
          layout="total, prev, pager, next"
          background
          @current-change="load"
        />
      </div>
    </div>

    <!-- 浏览量趋势 -->
    <el-dialog
      v-model="viewDialog"
      :title="`「${viewRow.name}」浏览量`"
      width="580px"
      @opened="renderViewChart"
      @closed="disposeViewChart"
    >
      <div class="view-head">
        <div class="view-total">{{ viewTrend.total ?? 0 }}</div>
        <div>
          <div class="view-label">累计浏览</div>
          <div class="view-sub">近 {{ VIEW_DAYS }} 天有 {{ trendSum }} 人看过</div>
        </div>
      </div>
      <div v-if="trendSum > 0" ref="viewChart" style="height: 260px"></div>
      <el-empty v-else :image-size="64" :description="`近 ${VIEW_DAYS} 天还没有浏览记录`" />
      <div class="view-tip">
        按天统计「当天有多少人看过」，同一人一天只计一次；累计浏览含未登录访问，因此两者数值不会相等。
      </div>
    </el-dialog>

    <!-- 库存预警榜 -->
    <el-dialog v-model="warnDialog" title="库存预警榜" width="640px" top="8vh">
      <div class="warn-head">
        <div class="wh-left">
          <b class="wh-num">{{ warnList.length }}</b>
          <span class="wh-label">件在售商品低于预警线</span>
        </div>
        <span class="wh-tip">预警线：库存 ≤ {{ STOCK_LINE }} 件 · 按库存从少到多排序</span>
      </div>

      <el-empty v-if="!warnList.length" :image-size="70" description="库存都在预警线以上，暂无需要补货的商品" />

      <div v-else class="rank-list">
        <div v-for="(r, i) in warnList" :key="r.id" class="rank-item" :class="r.warnLevel">
          <div class="rk-no">{{ i + 1 }}</div>
          <img class="rk-cover" :src="img(r.cover)" alt="" @error="onImgErr" />
          <div class="rk-main">
            <div class="rk-name">{{ r.name }}</div>
            <div class="rk-sub">{{ r.categoryName || '未分类' }} · 累计售出 {{ r.sales || 0 }}</div>
          </div>
          <div class="rk-stock">
            <div class="rk-num">{{ r.stock }}<span>件</span></div>
            <div class="rk-bar"><i :style="{ width: barWidth(r.stock) }"></i></div>
          </div>
          <el-tag class="rk-tag" size="small" :type="r.warnLevel === 'out' ? 'danger' : 'warning'">
            {{ r.warnLevel === 'out' ? '已售罄' : '库存告急' }}
          </el-tag>
          <el-button text size="small" type="primary" @click="$router.push(`/craftsman/goods/edit/${r.id}`)">补货</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { http } from '@/utils/request'
import { img, onImgErr, GOODS_STATUS, AUDIT_STATUS } from '@/utils/format'

const route = useRoute()

const list = ref([])
const total = ref(0)
const loading = ref(false)
const stockWarn = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined, auditStatus: undefined })

const viewDialog = ref(false)
const viewRow = ref({})
const viewChart = ref()
const VIEW_DAYS = 7
const viewTrend = ref({ total: 0, points: [] })
const trendSum = computed(() =>
  (viewTrend.value.points || []).reduce((s, p) => s + (p.count || 0), 0)
)
let viewChartInstance = null
const warnDialog = ref(false)
const warnList = ref([])

/** 与后端 Constants.STOCK_WARN_THRESHOLD 保持一致；只用于画刻度条与说明文案 */
const STOCK_LINE = 5

/** 库存占预警线的比例，用于条形的宽度（0 件也给一小截，"见底"要看得出来） */
function barWidth(stock) {
  const pct = ((Number(stock) || 0) / STOCK_LINE) * 100
  return Math.max(6, Math.min(100, pct)) + '%'
}

async function load() {
  loading.value = true
  try {
    const data = await http.get('/craftsman/goods/list', {
      ...query,
      status: query.status ?? undefined,
      auditStatus: query.auditStatus ?? undefined,
      // 「只看库存预警」此前只改了本地 bool，没进请求参数，等于点了没反应
      stockWarn: stockWarn.value || undefined
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

async function shelf(row) {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '下架' : '上架'}「${row.name}」吗？`, '提示')
  await http.post(`/craftsman/goods/${row.id}/shelf`, null, { params: { onShelf: row.status !== 1 } })
  ElMessage.success('操作成功')
  load()
}

async function showViews(row) {
  viewRow.value = row
  // 先用列表里已有的累计数垫底，接口回来再覆盖：弹窗一打开就有内容，不会白屏
  viewTrend.value = { total: row.viewCount ?? 0, points: [] }
  viewDialog.value = true
  try {
    const data = await http.get(`/craftsman/goods/${row.id}/view-trend`, { days: VIEW_DAYS })
    if (data) viewTrend.value = data
  } catch (e) {
    // 错误提示由 axios 拦截器统一弹出，这里只保证弹窗不崩
  }
  await nextTick()
  renderViewChart()
}

/**
 * 画图。
 * 不能直接在 showViews 里 init：el-dialog 有入场动画，容器此刻宽度是 0，
 * echarts 会按 0×0 画布初始化，之后就一直是白的（这正是本页此前的故障表现）。
 * 所以挂在 @opened 上画，并在数据晚到的情况下补一次重绘。
 */
function renderViewChart() {
  const el = viewChart.value
  if (!el || trendSum.value <= 0) return
  if (!viewChartInstance || viewChartInstance.getDom() !== el) {
    viewChartInstance?.dispose()
    viewChartInstance = echarts.init(el)
  }
  const points = viewTrend.value.points || []
  viewChartInstance.setOption({
    grid: { left: 8, right: 16, top: 18, bottom: 4, containLabel: true },
    tooltip: { trigger: 'axis', formatter: (p) => `${p[0].axisValue}<br/>浏览 ${p[0].value} 人` },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: points.map((p) => p.dateText || p.date),
      axisLine: { lineStyle: { color: '#e6ded2' } },
      axisLabel: { color: '#7a7268', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#f2ece2' } },
      axisLabel: { color: '#7a7268', fontSize: 11 }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbolSize: 6,
      data: points.map((p) => p.count || 0),
      itemStyle: { color: '#9c2c2c' },
      lineStyle: { width: 2 },
      areaStyle: { color: 'rgba(156, 44, 44, 0.12)' }
    }]
  }, true)
  viewChartInstance.resize()
}

function disposeViewChart() {
  viewChartInstance?.dispose()
  viewChartInstance = null
}

async function showStockWarn() {
  warnList.value = await http.get('/craftsman/goods/stock-warn')
  warnDialog.value = true
}

onMounted(() => {
  // 工作台「库存预警商品」带 ?stockWarn=1 跳过来
  if (route.query.stockWarn === '1') stockWarn.value = true
  load()
})
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.goods-cell { display: flex; align-items: center; gap: 10px; }
.goods-cell img { width: 54px; height: 54px; object-fit: cover; border-radius: 8px; flex: none; }
.g-name { font-size: 13px; font-weight: 600; }
.g-sub { color: var(--h-text-second); font-size: 12px; }
.stock-warn { color: var(--h-primary); font-weight: 700; }
.reject { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }

/* ---- 库存预警榜 ---- */
.warn-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 0 4px 12px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--h-border);
}
.wh-num { font-size: 26px; line-height: 1; color: var(--h-primary); margin-right: 6px; }
.wh-label { font-size: 13px; color: var(--h-ink); }
.wh-tip { font-size: 12px; color: var(--h-text-second); }
.rank-list { display: flex; flex-direction: column; gap: 8px; }
.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--h-border);
  border-radius: var(--h-radius);
  background: var(--h-paper);
}
.rank-item.low { background: #fffaf0; border-color: #f3e1bd; }
.rank-item.out { background: #fdf1f0; border-color: #f2cfcc; }
.rk-no {
  width: 22px;
  height: 22px;
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--h-text-second);
}
.rank-item:nth-child(1) .rk-no { background: #c0392b; }
.rank-item:nth-child(2) .rk-no { background: #cf6a3a; }
.rank-item:nth-child(3) .rk-no { background: #b98900; }
.rk-cover { width: 42px; height: 42px; border-radius: 8px; object-fit: cover; flex: none; border: 1px solid var(--h-border); }
.rk-main { flex: 1; min-width: 0; }
.rk-name { font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rk-sub { font-size: 12px; color: var(--h-text-second); margin-top: 2px; }
.rk-stock { width: 96px; flex: none; text-align: right; }
.rk-num { font-size: 18px; font-weight: 700; line-height: 1; color: var(--h-ink); }
.rk-num span { font-size: 12px; font-weight: 400; color: var(--h-text-second); margin-left: 2px; }
.rank-item.low .rk-num { color: #b98900; }
.rank-item.out .rk-num { color: #c0392b; }
.rk-bar { height: 4px; border-radius: 2px; background: #ece3d6; margin-top: 6px; overflow: hidden; }
.rk-bar i { display: block; height: 100%; border-radius: 2px; background: #d4a017; }
.rank-item.out .rk-bar i { background: #c0392b; }
.rk-tag { flex: none; }
.view-head { display: flex; align-items: center; gap: 14px; padding: 2px 4px 12px; border-bottom: 1px solid var(--h-border); margin-bottom: 8px; }
.view-total { font-size: 30px; font-weight: 700; line-height: 1; color: var(--h-primary); }
.view-label { font-size: 13px; color: var(--h-ink); }
.view-sub { font-size: 12px; color: var(--h-text-second); margin-top: 4px; }
.view-tip { font-size: 12px; color: var(--h-text-second); line-height: 1.6; padding: 6px 4px 0; }
</style>
