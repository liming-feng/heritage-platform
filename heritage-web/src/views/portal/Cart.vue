<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>购物车</h2>
      <span class="sub">共 {{ data.totalCount || 0 }} 件商品</span>
    </div>

    <div class="h-card" v-if="items.length">
      <div class="head">
        <el-checkbox v-model="allChecked" @change="checkAll">全选</el-checkbox>
        <span class="col-goods">商品信息</span>
        <span class="col-price">单价</span>
        <span class="col-qty">数量</span>
        <span class="col-sub">小计</span>
        <span class="col-op">操作</span>
      </div>

      <div v-for="it in items" :key="it.id" :class="['row', { invalid: it.invalid }]">
        <el-checkbox
          :model-value="!!it.checked"
          :disabled="!!it.invalid"
          @change="(v) => checkOne(it, v)"
        />
        <div class="col-goods goods">
          <img :src="img(it.cover)" @error="onImgErr" @click="$router.push(`/goods/${it.goodsId}`)" />
          <div>
            <div class="g-name h-ellipsis-2" @click="$router.push(`/goods/${it.goodsId}`)">{{ it.goodsName }}</div>
            <div class="g-spec">{{ it.specName || '默认规格' }}</div>
            <el-tag v-if="it.invalid" type="danger" size="small">已失效</el-tag>
          </div>
        </div>
        <span class="col-price">¥{{ it.price }}</span>
        <div class="col-qty">
          <el-input-number
            v-model="it.quantity"
            :min="1"
            :max="it.stock || 999"
            size="small"
            :disabled="!!it.invalid"
            @change="(v) => changeQty(it, v)"
          />
        </div>
        <span class="col-sub h-price">¥{{ it.subtotal }}</span>
        <span class="col-op">
          <el-button text type="danger" size="small" @click="remove(it)">删除</el-button>
        </span>
      </div>

      <div class="bar">
        <el-button text type="danger" size="small" @click="clearCart">清空购物车</el-button>
        <div class="bar-right">
          <span>已选 <b class="h-price">{{ data.checkedQuantity || 0 }}</b> 件</span>
          <span class="amount">合计：<b class="h-price big">{{ data.checkedAmount || '0.00' }}</b></span>
          <el-button type="primary" size="large" :disabled="!(data.checkedCount > 0)" @click="goCheckout">
            去结算（{{ data.checkedCount || 0 }}）
          </el-button>
        </div>
      </div>
    </div>

    <el-empty v-else description="购物车还是空的" class="h-empty">
      <el-button type="primary" @click="$router.push('/goods')">去逛逛</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr } from '@/utils/format'

const router = useRouter()
const user = useUserStore()

const data = ref({})
const items = computed(() => data.value.items || [])
const allChecked = computed({
  get: () => items.value.length > 0 && items.value.every((i) => i.checked === 1),
  set: () => {}
})

async function load() {
  data.value = await http.get('/portal/cart/list')
}

async function checkOne(it, v) {
  await http.put('/portal/cart/check', { ids: [it.id], checked: v ? 1 : 0 })
  load()
}

async function checkAll(v) {
  await http.put('/portal/cart/check', {
    ids: items.value.filter((i) => !i.invalid).map((i) => i.id),
    checked: v ? 1 : 0
  })
  load()
}

async function changeQty(it, v) {
  await http.put('/portal/cart/update', { id: it.id, quantity: v })
  refreshCount()
}

async function remove(it) {
  await http.delete(`/portal/cart/${it.id}`)
  ElMessage.success('已删除')
  load()
  refreshCount()
}

async function clearCart() {
  await ElMessageBox.confirm('确定清空购物车吗？', '提示', { type: 'warning' })
  await http.delete('/portal/cart/clear')
  load()
  refreshCount()
}

async function refreshCount() {
  const c = await http.get('/portal/cart/count')
  user.setCartCount(Number(c) || 0)
}

function goCheckout() {
  router.push('/checkout')
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.h-card { padding: 0 20px; }
.head, .row {
  display: grid;
  grid-template-columns: 40px 1fr 110px 150px 120px 70px;
  align-items: center; gap: 10px; padding: 14px 0;
}
.head { border-bottom: 1px solid var(--h-border); color: var(--h-text-second); font-size: 13px; }
.row { border-bottom: 1px dashed var(--h-border); }
.row:last-of-type { border-bottom: none; }
.row.invalid { opacity: 0.55; }
.goods { display: flex; gap: 12px; align-items: center; min-width: 0; }
.goods img { width: 76px; height: 76px; object-fit: cover; border-radius: 8px; cursor: pointer; flex: none; }
.g-name { cursor: pointer; line-height: 1.4; }
.g-name:hover { color: var(--h-primary); }
.g-spec { color: var(--h-text-second); font-size: 12px; margin: 2px 0; }
.col-sub { font-weight: 600; }
.bar {
  display: flex; align-items: center; margin-top: 10px;
  border-top: 1px solid var(--h-border); padding: 14px 0;
}
.bar-right { margin-left: auto; display: flex; align-items: center; gap: 18px; }
.big { font-size: 22px; }
</style>
