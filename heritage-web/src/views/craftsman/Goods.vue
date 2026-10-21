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
    <el-dialog v-model="viewDialog" :title="`「${viewRow.name}」浏览量趋势`" width="560px">
      <div ref="viewChart" style="height: 300px" v-if="viewDialog"></div>
    </el-dialog>

    <!-- 库存预警榜 -->
    <el-dialog v-model="warnDialog" title="库存预警商品" width="560px">
      <el-table :data="warnList" size="small">
        <el-table-column prop="name" label="商品" min-width="180" />
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column prop="warnText" label="预警" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.warnLevel === 'out' ? 'danger' : 'warning'">{{ row.warnText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push(`/craftsman/goods/edit/${row.id}`)">补货</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { http } from '@/utils/request'
import { img, onImgErr, GOODS_STATUS, AUDIT_STATUS } from '@/utils/format'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const stockWarn = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', status: undefined, auditStatus: undefined })

const viewDialog = ref(false)
const viewRow = ref({})
const viewChart = ref()
const warnDialog = ref(false)
const warnList = ref([])

async function load() {
  loading.value = true
  try {
    const data = await http.get('/craftsman/goods/list', {
      ...query,
      status: query.status ?? undefined,
      auditStatus: query.auditStatus ?? undefined
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
  viewDialog.value = true
  await nextTick()
  const points = await http.get(`/craftsman/goods/${row.id}/views`)
  echarts.init(viewChart.value).setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (points || []).map((p) => p.dateText || p.date) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: (points || []).map((p) => p.count), areaStyle: { opacity: 0.15 } }]
  })
}

async function showStockWarn() {
  warnList.value = await http.get('/craftsman/goods/stock-warn')
  warnDialog.value = true
}

onMounted(load)
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
</style>
