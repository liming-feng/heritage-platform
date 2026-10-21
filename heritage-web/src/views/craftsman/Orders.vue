<template>
  <div class="page">
    <div class="h-page-header">
      <h2>订单管理</h2>
      <div class="head-right">
        <el-button size="small" @click="batchDeliver" :disabled="!selected.length">批量发货（{{ selected.length }}）</el-button>
        <el-button size="small" @click="exportOrders">导出 Excel</el-button>
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
        <el-table-column v-if="status === '1'" type="selection" width="42" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { dateTime, ORDER_STATUS } from '@/utils/format'

const list = ref([])
const selected = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)
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
  ElMessageBox.alert(
    `收货人：${row.receiver} ${row.receiverPhone}<br/>地址：${row.addressSnapshot || '—'}<br/>买家留言：${row.remark || '无'}<br/>物流：${row.logisticsCompany || '—'} ${row.logisticsNo || ''}`,
    `订单 ${row.orderNo}`,
    { dangerouslyUseHTMLString: true }
  )
}

async function exportOrders() {
  const res = await http.download('/craftsman/order/export', {
    status: ['null', 'refund'].includes(status.value) ? undefined : Number(status.value)
  })
  saveBlob(res, '我的订单.xlsx')
}

onMounted(load)
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.table-card { padding: 6px 16px 16px; }
.oi { font-size: 13px; }
.sub { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
