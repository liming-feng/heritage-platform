<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>我的订单</h2>
      <span class="sub">可在订单详情完成模拟支付、确认收货与评价</span>
    </div>

    <el-tabs v-model="status" @tab-change="reload">
      <el-tab-pane
        v-for="t in tabs"
        :key="String(t.value)"
        :label="t.count !== null ? `${t.label}(${t.count})` : t.label"
        :name="String(t.value)"
      />
    </el-tabs>

    <div v-for="o in list" :key="o.id" class="order h-card">
      <div class="head">
        <span class="no">订单号：{{ o.orderNo }}</span>
        <span class="time">{{ dateTime(o.createTime) }}</span>
        <el-tag :type="ORDER_STATUS[o.status]?.type" size="small">{{ o.statusDesc }}</el-tag>
      </div>
      <div class="body">
        <div class="items">
          <div v-for="it in o.items || []" :key="it.id" class="item" @click="$router.push(`/orders/${o.id}`)">
            <img :src="img(it.goodsImage)" @error="onImgErr" />
            <div class="i-info">
              <div class="i-name h-ellipsis-1">{{ it.goodsName }}</div>
              <div class="i-spec">{{ it.specName || '默认规格' }}</div>
            </div>
            <span class="i-price">¥{{ it.price }} × {{ it.quantity }}</span>
          </div>
        </div>
        <div class="side">
          <div class="pay">实付 <b class="h-price">¥{{ o.payAmount }}</b></div>
          <div class="ops">
            <el-button v-if="o.canPay" type="primary" size="small" @click="pay(o)">模拟支付</el-button>
            <el-button v-if="o.canConfirm" type="success" size="small" @click="confirm(o)">确认收货</el-button>
            <el-button v-if="o.canCancel" size="small" @click="cancel(o)">取消订单</el-button>
            <el-button v-if="o.canRefund" size="small" type="warning" plain @click="refund(o)">申请退款</el-button>
            <el-button v-if="o.status === 3 && (o.items || []).some(i => !i.reviewed)" size="small" @click="$router.push(`/orders/${o.id}`)">去评价</el-button>
            <el-button text size="small" @click="$router.push(`/orders/${o.id}`)">详情</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length && !loading" description="暂无订单" class="h-empty">
      <el-button type="primary" @click="$router.push('/goods')">去逛逛</el-button>
    </el-empty>

    <div class="pager">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="Number(total)"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>

    <!-- 模拟支付弹窗 -->
    <el-dialog v-model="payDialog" title="模拟支付" width="380px" align-center>
      <div class="pay-body">
        <div class="pay-amount">¥{{ paying.payAmount }}</div>
        <div class="pay-no">订单号 {{ paying.orderNo }}</div>
        <div class="pay-ways">
          <el-radio-group v-model="payWay">
            <el-radio value="wx" border>微信支付</el-radio>
            <el-radio value="alipay" border>支付宝</el-radio>
          </el-radio-group>
        </div>
        <div class="pay-tip">毕业设计演示环境，点击确认即支付成功，不会产生真实扣款。</div>
      </div>
      <template #footer>
        <el-button @click="payDialog = false">取消</el-button>
        <el-button type="primary" :loading="!!paying.loading" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, ORDER_STATUS } from '@/utils/format'

const tabs = [
  { value: null, label: '全部', count: null },
  { value: 0, label: '待付款', count: null },
  { value: 1, label: '待发货', count: null },
  { value: 2, label: '待收货', count: null },
  { value: 3, label: '已完成', count: null },
  { value: 4, label: '已退款', count: null },
  { value: 5, label: '已取消', count: null }
]

const status = ref('null')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 5
const loading = ref(false)

const payDialog = ref(false)
const paying = reactive({})
const payWay = ref('wx')

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/order/list', {
      status: status.value === 'null' ? undefined : Number(status.value),
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
    // Tab 角标
    const counts = await http.get('/portal/order/count').catch(() => ({}))
    tabs.forEach((t) => (t.count = counts ? counts[t.value] ?? null : null))
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

function pay(o) {
  Object.assign(paying, { id: o.id, orderNo: o.orderNo, payAmount: o.payAmount })
  payDialog.value = true
}

async function doPay() {
  paying.loading = true
  try {
    await http.post(`/portal/order/pay/${paying.orderNo}`)
    ElMessage.success('支付成功')
    payDialog.value = false
    load()
  } finally {
    paying.loading = false
  }
}

async function confirm(o) {
  await ElMessageBox.confirm('确认已收到货？', '确认收货', { type: 'info' })
  await http.post(`/portal/order/confirm/${o.id}`)
  ElMessage.success('已确认收货')
  load()
}

async function cancel(o) {
  await ElMessageBox.confirm('取消后订单将无法恢复，确定取消吗？', '取消订单', { type: 'warning' })
  await http.post(`/portal/order/cancel/${o.id}`)
  ElMessage.success('已取消')
  load()
}

async function refund(o) {
  const { value } = await ElMessageBox.prompt('请填写退款原因', '申请退款', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
  await http.post('/portal/order/refund/apply', { orderId: o.id, reason: value })
  ElMessage.success('已提交退款申请')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.order { margin-bottom: 14px; overflow: hidden; }
.head {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 18px; background: var(--h-paper); font-size: 13px;
}
.no { font-weight: 600; }
.time { color: var(--h-text-second); }
.head .el-tag { margin-left: auto; }
.body { display: flex; }
.items { flex: 1; padding: 6px 18px; min-width: 0; }
.item { display: flex; align-items: center; gap: 12px; padding: 10px 0; cursor: pointer; }
.item img { width: 62px; height: 62px; object-fit: cover; border-radius: 8px; flex: none; }
.i-name:hover { color: var(--h-primary); }
.i-spec { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.i-price { margin-left: auto; color: var(--h-text-second); font-size: 13px; flex: none; }
.side {
  width: 220px; flex: none; border-left: 1px dashed var(--h-border);
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px;
}
.pay b { font-size: 20px; }
.ops { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; padding: 0 10px; }
.pager { display: flex; justify-content: center; margin-top: 20px; }
.pay-body { text-align: center; }
.pay-amount { font-size: 34px; font-weight: 700; color: var(--h-primary); }
.pay-no { color: var(--h-text-second); font-size: 12px; margin: 4px 0 16px; }
.pay-ways { display: flex; justify-content: center; margin-bottom: 12px; }
.pay-tip { font-size: 12px; color: #d48806; background: #fdf6e7; border-radius: 8px; padding: 8px; }
</style>
