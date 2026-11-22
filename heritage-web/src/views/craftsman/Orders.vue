<template>
  <div class="page">
    <div class="h-page-header">
      <h2>订单管理</h2>
      <div class="head-right">
        <el-button size="small" @click="batchDeliver" :disabled="status !== '1' || !selected.length">批量发货（{{ selected.length }}）</el-button>
        <el-button size="small" @click="exportOrders">
          {{ selected.length ? `导出所选（${selected.length}）` : '导出 Excel' }}
        </el-button>
      </div>
    </div>

    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane label="全部" name="null" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="退款处理" name="refund" />
    </el-tabs>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading" @selection-change="(s) => (selected = s)">
        <!-- 勾选列常驻：批量发货只在「待发货」页签可用，但勾选导出在任何页签都能用 -->
        <el-table-column type="selection" width="42" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div v-for="it in row.items" :key="it.id" class="oi">
              {{ it.goodsName }}<span v-if="it.specName">（{{ it.specName }}）</span> ×{{ it.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="buyerName" label="买家" width="100" />
        <el-table-column label="实付" width="100"><template #default="{ row }">¥{{ row.payAmount }}</template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="ORDER_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag>
            <div class="sub" v-if="row.refundStatus === 1">退款申请中</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="150">
          <template #default="{ row }">{{ dateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.canDeliver" text size="small" type="primary" @click="deliver(row)">发货</el-button>
            <el-button v-if="row.canAuditRefund" text size="small" type="warning" @click="auditRefund(row)">退款审核</el-button>
            <el-button text size="small" @click="remark(row)">备注</el-button>
            <el-button text size="small" @click="detail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>

    <!-- 订单详情：结构化展示，地址快照解析成可读文本，不再直接甩 JSON -->
    <el-dialog v-model="detailDialog" :title="`订单 ${detailRow.orderNo || ''}`" width="780px" top="6vh">
      <div class="od-top">
        <el-tag :type="ORDER_STATUS[detailRow.status]?.type" size="small">{{ detailRow.statusDesc }}</el-tag>
        <span class="od-meta">下单 {{ dateTime(detailRow.createTime) }}</span>
        <span class="od-meta" v-if="detailRow.payTime">支付 {{ dateTime(detailRow.payTime) }}</span>
        <span class="od-meta" v-if="detailRow.deliveryTime">发货 {{ dateTime(detailRow.deliveryTime) }}</span>
        <span class="od-meta" v-if="detailRow.finishTime">完成 {{ dateTime(detailRow.finishTime) }}</span>
      </div>

      <div class="h-section-title od-sec">商品明细</div>
      <el-table :data="detailRow.items || []" size="small">
        <el-table-column label="商品" min-width="240">
          <template #default="{ row }">
            <div class="od-item">
              <img :src="img(row.goodsImage)" @error="onImgErr" alt="" />
              <div>
                <div class="od-name">{{ row.goodsName }}</div>
                <div class="od-spec" v-if="row.specName">{{ row.specName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="90" align="right">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="70" align="center" />
        <el-table-column label="小计" width="100" align="right">
          <template #default="{ row }">¥{{ row.subtotal }}</template>
        </el-table-column>
      </el-table>

      <div class="h-section-title od-sec">收货与物流</div>
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="收货人">{{ detailRow.receiver || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailRow.receiverPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ addressText(detailRow.addressSnapshot) }}</el-descriptions-item>
        <el-descriptions-item label="物流公司">{{ detailRow.logisticsCompany || '—' }}</el-descriptions-item>
        <el-descriptions-item label="运单号">{{ detailRow.logisticsNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="买家留言" :span="2">{{ detailRow.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailRow.craftsmanRemark" label="匠人备注" :span="2">
          {{ detailRow.craftsmanRemark }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="h-section-title od-sec">金额</div>
      <div class="od-amount">
        <div><span>商品总额</span><b>¥{{ detailRow.totalAmount ?? '0.00' }}</b></div>
        <div v-if="Number(detailRow.discountAmount) > 0"><span>优惠抵扣</span><b>-¥{{ detailRow.discountAmount }}</b></div>
        <div class="od-pay"><span>实付金额</span><b>¥{{ detailRow.payAmount ?? '0.00' }}</b></div>
      </div>

      <template v-if="detailRow.refundStatus">
        <div class="h-section-title od-sec">退款</div>
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="退款状态">
            <el-tag :type="REFUND_STATUS[detailRow.refundStatus]?.type" size="small">
              {{ REFUND_STATUS[detailRow.refundStatus]?.text || '—' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailRow.refundReason" label="申请原因">{{ detailRow.refundReason }}</el-descriptions-item>
          <el-descriptions-item v-if="detailRow.refundRejectReason" label="驳回原因">{{ detailRow.refundRejectReason }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="detailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { dateTime, ORDER_STATUS, REFUND_STATUS, addressText, img, onImgErr } from '@/utils/format'

const route = useRoute()

const list = ref([])
const selected = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const detailDialog = ref(false)
const detailRow = ref({})
const status = ref('null')

async function load() {
  loading.value = true
  try {
    const refundOnly = status.value === 'refund'
    const data = await http.get('/craftsman/order/list', {
      status: ['null', 'refund'].includes(status.value) ? undefined : Number(status.value),
      refundOnly: refundOnly || undefined,
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

async function deliver(row) {
  const { value: company } = await ElMessageBox.prompt('物流公司', '发货', { inputPattern: /\S+/ })
  const { value: no } = await ElMessageBox.prompt('运单号', '发货', { inputPattern: /\S+/, inputErrorMessage: '请输入运单号' })
  await http.post('/craftsman/order/deliver', { orderId: row.id, logisticsCompany: company, logisticsNo: no })
  ElMessage.success('已发货')
  load()
}

async function batchDeliver() {
  const { value: company } = await ElMessageBox.prompt(`为选中的 ${selected.value.length} 个订单统一发货，请输入物流公司`, '批量发货', { inputPattern: /\S+/ })
  const { value: no } = await ElMessageBox.prompt('运单号', '批量发货', { inputPattern: /\S+/ })
  await http.post('/craftsman/order/deliver/batch', {
    logisticsCompany: company,
    items: selected.value.map((o) => ({ orderId: o.id, logisticsNo: no }))
  })
  ElMessage.success('批量发货成功')
  load()
}

async function auditRefund(row) {
  await ElMessageBox.confirm(
    `买家申请退款：${row.refundReason || '未填写原因'}。是否同意退款？`,
    '退款审核',
    { distinguishCancelAndClose: true, confirmButtonText: '同意退款', cancelButtonText: '拒绝' }
  )
  await http.post('/craftsman/order/refund/audit', { orderId: row.id, approve: true })
  ElMessage.success('已同意退款')
  load()
}

async function remark(row) {
  const { value } = await ElMessageBox.prompt('匠人备注（仅自己可见）', '备注', { inputValue: row.craftsmanRemark || '' })
  if (value === undefined) return
  await http.post('/craftsman/order/remark', { orderId: row.id, craftsmanRemark: value })
  ElMessage.success('已保存')
  load()
}

function detail(row) {
  detailRow.value = row || {}
  detailDialog.value = true
}

async function exportOrders() {
  // 勾选了就只导勾选的（ids 逗号分隔，后端用 IN 过滤）；没勾就按当前筛选条件导全部
  const ids = selected.value.map((r) => r.id).join(',')
  const res = await http.download('/craftsman/order/export', {
    status: ['null', 'refund'].includes(status.value) ? undefined : Number(status.value),
    refundOnly: status.value === 'refund' || undefined,
    ids: ids || undefined
  })
  saveBlob(res, ids ? '我的订单_所选.xlsx' : '我的订单.xlsx')
}

/**
 * 支持从工作台「待办提醒」带参跳转：?tab=1（待发货）/?tab=refund（退款处理）
 * 值必须是下面 el-tabs 的 name 之一，非法值忽略，回落到「全部」。
 */
const TABS = ['null', '1', '2', '3', 'refund']

onMounted(() => {
  const tab = route.query.tab
  if (tab && TABS.includes(String(tab))) status.value = String(tab)
  load()
})
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.table-card { padding: 6px 16px 16px; }
.oi { font-size: 13px; }
.sub { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
.od-top { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.od-meta { font-size: 12px; color: var(--h-text-second); }
.od-sec { margin: 16px 0 8px; }
.od-item { display: flex; align-items: center; gap: 10px; }
.od-item img { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; flex: none; border: 1px solid var(--h-border); }
.od-name { font-size: 13px; }
.od-spec { font-size: 12px; color: var(--h-text-second); }
.od-amount { display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
.od-amount > div { display: flex; gap: 16px; font-size: 13px; }
.od-amount span { color: var(--h-text-second); }
.od-pay b { color: var(--h-primary); font-size: 17px; }
</style>
