<template>
  <div class="h-container page" v-if="order.id">
    <el-page-header class="ph" @back="$router.back()">
      <template #content>订单详情</template>
    </el-page-header>

    <!-- 状态卡 -->
    <div class="status h-card">
      <div class="s-left">
        <div class="s-main">{{ order.statusDesc }}</div>
        <div class="s-sub">{{ statusHint }}</div>
      </div>
      <div class="s-ops">
        <el-button v-if="order.canPay" type="primary" @click="pay">模拟支付</el-button>
        <el-button v-if="order.canConfirm" type="success" @click="confirm">确认收货</el-button>
        <el-button v-if="order.canCancel" @click="cancel">取消订单</el-button>
        <el-button v-if="order.canRefund" type="warning" plain @click="refund">申请退款</el-button>
      </div>
    </div>

    <!-- 收货与物流 -->
    <div class="grid2">
      <div class="h-card block">
        <div class="h-section-title">收货信息</div>
        <div class="kv"><span>收货人</span>{{ order.receiver }}</div>
        <div class="kv"><span>电话</span>{{ order.receiverPhone }}</div>
        <div class="kv"><span>地址</span>{{ addressText(order.addressSnapshot) }}</div>
        <div class="kv" v-if="order.remark"><span>买家留言</span>{{ order.remark }}</div>
      </div>
      <div class="h-card block">
        <div class="h-section-title">物流信息</div>
        <template v-if="order.logisticsNo">
          <div class="kv"><span>物流公司</span>{{ order.logisticsCompany }}</div>
          <div class="kv"><span>运单号</span>{{ order.logisticsNo }}</div>
        </template>
        <template v-else>
          <el-empty description="发货后展示物流信息" :image-size="70" />
        </template>
        <div class="kv" v-if="order.craftsmanRemark"><span>匠人备注</span>{{ order.craftsmanRemark }}</div>
      </div>
    </div>

    <!-- 商品清单 -->
    <div class="h-card block">
      <div class="h-section-title">商品清单</div>
      <div v-for="it in order.items || []" :key="it.id" class="item">
        <img :src="img(it.goodsImage)" @error="onImgErr" @click="$router.push(`/goods/${it.goodsId}`)" />
        <div class="i-info">
          <div class="i-name">{{ it.goodsName }}</div>
          <div class="i-spec">{{ it.specName || '默认规格' }}</div>
        </div>
        <span>¥{{ it.price }} × {{ it.quantity }}</span>
        <div class="right">
          <b class="h-price">¥{{ it.subtotal }}</b>
          <el-tag v-if="it.reviewed === 1" size="small" type="success">已评价</el-tag>
        </div>
      </div>
      <div class="amounts">
        <div>商品总额 <b>¥{{ order.totalAmount }}</b></div>
        <div v-if="Number(order.discountAmount) > 0">优惠券抵扣 <b>-¥{{ order.discountAmount }}</b></div>
        <div class="pay-line">实付款 <b class="h-price big">¥{{ order.payAmount }}</b></div>
      </div>
    </div>

    <!-- 时间线 -->
    <div class="h-card block">
      <div class="h-section-title">订单轨迹</div>
      <el-steps :active="stepActive" align-center finish-status="success">
        <el-step title="创建订单" :description="dateTime(order.createTime)" />
        <el-step title="付款" :description="dateTime(order.payTime)" />
        <el-step title="发货" :description="dateTime(order.deliveryTime)" />
        <el-step title="完成" :description="dateTime(order.finishTime)" />
      </el-steps>
    </div>

    <!-- 评价 -->
    <div class="h-card block" v-if="order.status === 3 && (order.items || []).some((i) => !i.reviewed)">
      <div class="h-section-title">发表评价</div>
      <el-form label-width="70px">
        <el-form-item label="商品">
          <el-select v-model="review.goodsId" placeholder="选择要评价的商品">
            <el-option
              v-for="it in order.items.filter((i) => !i.reviewed)"
              :key="it.goodsId"
              :label="it.goodsName"
              :value="it.goodsId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="review.rating" show-score />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="review.content" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="这件手工艺品做得怎么样？" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="publishReview">提交评价</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 模拟支付弹窗 -->
    <el-dialog v-model="payDialog" title="模拟支付" width="380px" align-center>
      <div class="pay-body">
        <div class="pay-amount">¥{{ order.payAmount }}</div>
        <div class="pay-tip">毕业设计演示环境，点击确认即支付成功。</div>
      </div>
      <template #footer>
        <el-button @click="payDialog = false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
  <div class="h-container page" v-else-if="!loading"><el-empty description="订单不存在" /></div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, addressText } from '@/utils/format'

const route = useRoute()
const order = ref({})
const loading = ref(true)
const payDialog = ref(false)
const payLoading = ref(false)
const review = reactive({ goodsId: null, rating: 5, content: '' })

const stepActive = computed(() => {
  const o = order.value
  if (o.finishTime) return 4
  if (o.deliveryTime) return 3
  if (o.payTime) return 2
  return 1
})

const statusHint = computed(() => {
  const map = {
    0: '请在 24 小时内完成支付，超时订单将自动取消',
    1: '匠人正在精心打包，发货后可在此查看物流',
    2: '宝贝已在路上，收货无误请点击「确认收货」',
    3: '交易完成，欢迎评价分享你的体验',
    4: '该订单已退款',
    5: '订单已取消'
  }
  return map[order.value.status] || ''
})

async function load() {
  loading.value = true
  try {
    order.value = await http.get(`/portal/order/detail/${route.params.id}`)
    const first = (order.value.items || []).find((i) => !i.reviewed)
    if (first) review.goodsId = first.goodsId
  } finally {
    loading.value = false
  }
}

function pay() {
  payDialog.value = true
}

async function doPay() {
  payLoading.value = true
  try {
    await http.post(`/portal/order/pay/${order.value.orderNo}`)
    ElMessage.success('支付成功')
    payDialog.value = false
    load()
  } finally {
    payLoading.value = false
  }
}

async function confirm() {
  await ElMessageBox.confirm('确认已收到货？', '确认收货', { type: 'info' })
  await http.post(`/portal/order/confirm/${order.value.id}`)
  ElMessage.success('已确认收货')
  load()
}

async function cancel() {
  await ElMessageBox.confirm('取消后订单将无法恢复，确定取消吗？', '取消订单', { type: 'warning' })
  await http.post(`/portal/order/cancel/${order.value.id}`)
  ElMessage.success('已取消')
  load()
}

async function refund() {
  const { value } = await ElMessageBox.prompt('请填写退款原因', '申请退款', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
  await http.post('/portal/order/refund/apply', { orderId: order.value.id, reason: value })
  ElMessage.success('已提交退款申请')
  load()
}

async function publishReview() {
  if (!review.goodsId || !review.content.trim()) return ElMessage.warning('请选择商品并填写评价内容')
  await http.post('/portal/review/publish', {
    orderId: order.value.id,
    goodsId: review.goodsId,
    rating: review.rating,
    content: review.content
  })
  ElMessage.success('评价成功')
  review.content = ''
  load()
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 16px; }
.ph { margin-bottom: 4px; }
.status { display: flex; align-items: center; padding: 20px 26px; }
.s-main { font-size: 20px; font-weight: 700; color: var(--h-primary); }
.s-sub { color: var(--h-text-second); font-size: 13px; margin-top: 4px; }
.s-ops { margin-left: auto; display: flex; gap: 10px; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.block { padding: 20px 26px; }
.kv { display: flex; gap: 14px; margin-bottom: 8px; font-size: 13px; }
.kv span { color: var(--h-text-second); flex: none; width: 62px; }
.item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px dashed var(--h-border); }
.item:last-of-type { border-bottom: none; }
.item img { width: 62px; height: 62px; object-fit: cover; border-radius: 8px; cursor: pointer; }
.i-name:hover { color: var(--h-primary); }
.i-spec { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.right { margin-left: auto; text-align: right; display: flex; flex-direction: column; gap: 4px; align-items: flex-end; }
.amounts { margin-top: 12px; text-align: right; display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: var(--h-text-second); }
.pay-line b.big { font-size: 24px; }
.pay-body { text-align: center; }
.pay-amount { font-size: 34px; font-weight: 700; color: var(--h-primary); }
.pay-tip { font-size: 12px; color: #d48806; background: #fdf6e7; border-radius: 8px; padding: 8px; margin-top: 10px; }
</style>
