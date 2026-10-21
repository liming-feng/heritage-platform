<template>
  <div class="h-container page">
    <div class="h-page-header"><h2>确认订单</h2></div>

    <!-- 收货地址 -->
    <div class="block h-card">
      <div class="h-section-title">收货地址</div>
      <div class="addr-list">
        <div
          v-for="a in addresses"
          :key="a.id"
          :class="['addr', { active: addressId === a.id }]"
          @click="addressId = a.id"
        >
          <div class="addr-line1">
            <b>{{ a.receiver }}</b> {{ a.phone }}
            <el-tag v-if="a.isDefault === 1" size="small" type="warning" effect="plain">默认</el-tag>
          </div>
          <div class="addr-line2 h-ellipsis-2">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
        </div>
        <div class="addr new-addr" @click="addrDialog = true">
          <el-icon :size="22"><Plus /></el-icon>
          <span>新增地址</span>
        </div>
      </div>
    </div>

    <!-- 商品清单 -->
    <div class="block h-card">
      <div class="h-section-title">商品清单</div>
      <div v-for="it in preview.items || []" :key="it.id" class="row">
        <img :src="img(it.cover)" @error="onImgErr" />
        <div class="g-info">
          <div class="g-name">{{ it.goodsName }}</div>
          <div class="g-spec">{{ it.specName || '默认规格' }}</div>
        </div>
        <span>¥{{ it.price }} × {{ it.quantity }}</span>
        <b class="h-price">¥{{ it.subtotal }}</b>
      </div>
    </div>

    <!-- 优惠券 -->
    <div class="block h-card" v-if="coupons.length">
      <div class="h-section-title">
        优惠券
        <span class="best" v-if="bestDiscount > 0">已为你选最优券，省 ¥{{ bestDiscount }}</span>
      </div>
      <el-radio-group v-model="userCouponId" @change="rePreview">
        <div class="coupon-list">
          <el-radio
            v-for="c in coupons"
            :key="c.userCouponId"
            :value="c.userCouponId"
            :disabled="!c.usable"
            border
            class="coupon"
          >
            <span class="c-name">{{ c.name }}</span>
            <span class="c-rule">{{ c.ruleText }}</span>
            <span class="c-cut" v-if="c.usable">省 ¥{{ c.discountAmount }}，应付 ¥{{ c.payAmount }}</span>
            <span v-if="!c.usable" class="c-block">{{ c.blockReason }}</span>
          </el-radio>
          <el-radio :value="null" border class="coupon">不使用优惠券</el-radio>
        </div>
      </el-radio-group>
    </div>

    <!-- 备注 + 提交 -->
    <div class="block h-card submit-bar">
      <el-input v-model="remark" placeholder="给匠人留言（选填，50 字以内）" maxlength="50" show-word-limit style="max-width: 480px" />
      <div class="right">
        <div class="sum">
          共 {{ preview.totalQuantity || 0 }} 件，合计
          <span class="h-price big">¥{{ preview.payAmount ?? '—' }}</span>
          <span v-if="Number(preview.discountAmount) > 0" class="discount">
            （已优惠 ¥{{ preview.discountAmount }}）
          </span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" :disabled="!preview.canSubmit" @click="submitOrder">
          提交订单
        </el-button>
      </div>
    </div>
    <div class="warn" v-if="preview.canSubmit === false">{{ preview.message }}</div>

    <!-- 新增地址弹窗 -->
    <el-dialog v-model="addrDialog" title="新增收货地址" width="460px">
      <el-form :model="addrForm" label-width="70px">
        <el-form-item label="收货人" required><el-input v-model="addrForm.receiver" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="addrForm.phone" maxlength="11" /></el-form-item>
        <el-form-item label="省份" required><el-input v-model="addrForm.province" /></el-form-item>
        <el-form-item label="城市" required><el-input v-model="addrForm.city" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="addrForm.district" /></el-form-item>
        <el-form-item label="详细地址" required><el-input v-model="addrForm.detail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="设默认"><el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddr">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const addresses = ref([])
const addressId = ref(null)
const preview = ref({})
const coupons = ref([])
const bestDiscount = ref(0)
const userCouponId = ref(null)
const remark = ref('')
const submitting = ref(false)
const addrDialog = ref(false)
const addrForm = reactive({ receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })

/** 直接购买（商品页立即购买）或购物车结算 */
const direct = computed(() =>
  route.query.goodsId
    ? [{ goodsId: Number(route.query.goodsId), specId: route.query.specId ? Number(route.query.specId) : null, quantity: Number(route.query.quantity || 1) }]
    : null
)
const cartChecked = ref([])

async function loadAddresses() {
  addresses.value = (await http.get('/portal/address/list')) || []
  const def = addresses.value.find((a) => a.isDefault === 1) || addresses.value[0]
  if (def) addressId.value = def.id
}

/** 生成下单条目：直接购买用 query，否则取购物车勾选 */
async function buildItems() {
  if (direct.value) return direct.value
  const cart = await http.get('/portal/cart/list')
  cartChecked.value = (cart.items || []).filter((i) => i.checked === 1 && !i.invalid)
  return cartChecked.value.map((i) => ({ goodsId: i.goodsId, specId: i.specId, quantity: i.quantity }))
}

async function loadPreview() {
  const items = await buildItems()
  if (!items.length) {
    ElMessage.warning('没有可结算的商品')
    return router.replace('/cart')
  }
  preview.value = await http.post('/portal/order/preview', {
    addressId: addressId.value,
    userCouponId: userCouponId.value,
    items
  })
  // 结算页可用券：按订单金额 + 消费类型（1 商品）查询
  const av = await http.get('/portal/coupon/available', {
    amount: preview.value.totalAmount,
    scope: 1
  })
  coupons.value = av.list || []
  bestDiscount.value = Number(av.bestDiscountAmount) || 0
  // 首次进入自动选中后端给出的最优券
  if (userCouponId.value === null && av.bestUserCouponId) {
    userCouponId.value = av.bestUserCouponId
    preview.value = await http.post('/portal/order/preview', {
      addressId: addressId.value,
      userCouponId: av.bestUserCouponId,
      items
    })
  }
  if (!addressId.value && addresses.value.length) addressId.value = addresses.value[0].id
}

function rePreview() {
  loadPreview()
}

async function submitOrder() {
  if (!addressId.value) return ElMessage.warning('请选择收货地址')
  submitting.value = true
  try {
    const items = direct.value || cartChecked.value.map((i) => ({ goodsId: i.goodsId, specId: i.specId, quantity: i.quantity }))
    // 后端按匠人自动拆单，返回订单数组；跳到首单详情（多单时其余的在"我的订单"里）
    const created = await http.post('/portal/order/create', {
      addressId: addressId.value,
      remark: remark.value || undefined,
      userCouponId: userCouponId.value || undefined,
      items
    })
    const order = Array.isArray(created) ? created[0] : created
    if (!order || !order.id) throw new Error('下单返回数据异常')
    // 清购物车计数（购物车结算时后端已删勾选项）
    const c = await http.get('/portal/cart/count').catch(() => 0)
    userStore.setCartCount(Number(c) || 0)
    ElMessage.success(created.length > 1 ? `下单成功，已按匠人拆分为 ${created.length} 个订单` : '下单成功')
    router.replace(`/orders/${order.id}?pay=1`)
  } finally {
    submitting.value = false
  }
}

import { useUserStore } from '@/store/user'
const userStore = useUserStore()

async function saveAddr() {
  if (!addrForm.receiver || !addrForm.phone || !addrForm.detail) return ElMessage.warning('请填写完整收货信息')
  await http.post('/portal/address/save', { ...addrForm })
  ElMessage.success('地址已保存')
  addrDialog.value = false
  Object.assign(addrForm, { receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  loadAddresses()
}

onMounted(async () => {
  await loadAddresses()
  await loadPreview()
})
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 16px; }
.block { padding: 20px 24px; }
.addr-list { display: flex; gap: 12px; flex-wrap: wrap; }
.addr {
  width: 250px; border: 1px solid var(--h-border); border-radius: 10px;
  padding: 12px 14px; cursor: pointer; transition: all 0.15s; font-size: 13px;
}
.addr:hover { border-color: var(--h-primary-light); }
.addr.active { border-color: var(--h-primary); background: #fdf3f2; box-shadow: 0 0 0 1px var(--h-primary); }
.addr-line1 { display: flex; align-items: center; gap: 6px; margin-bottom: 4px; }
.addr-line2 { color: var(--h-text-second); }
.new-addr {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 6px; color: var(--h-text-second); min-height: 76px;
  border-style: dashed; background: var(--h-paper);
}
.coupon-list { display: flex; flex-wrap: wrap; gap: 10px; }
.coupon { height: auto !important; padding: 10px 14px; margin-right: 0 !important; }
.coupon :deep(.el-radio__label) { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
.c-name { font-weight: 600; }
.c-rule { color: var(--h-text-second); font-size: 12px; }
.c-cut { color: var(--h-primary); font-size: 12px; }
.c-block { color: #d48806; font-size: 12px; }
.row { display: grid; grid-template-columns: 70px 1fr 150px 110px; gap: 14px; align-items: center; padding: 10px 0; }
.row img { width: 70px; height: 70px; object-fit: cover; border-radius: 8px; }
.g-name { font-weight: 600; }
.g-spec { color: var(--h-text-second); font-size: 12px; margin-top: 3px; }
.submit-bar { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 14px; }
.right { display: flex; align-items: center; gap: 18px; }
.big { font-size: 24px; }
.discount { font-size: 12px; color: var(--h-text-second); }
.warn { color: #d48806; font-size: 13px; text-align: right; }
</style>
