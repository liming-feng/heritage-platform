<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>优惠券</h2>
      <span class="sub">领券中心 · 我的券包</span>
    </div>

    <el-tabs v-model="tab" @tab-change="reload">
      <el-tab-pane label="领券中心" name="center" />
      <el-tab-pane :label="`未使用(${counts[0] || 0})`" name="0" />
      <el-tab-pane :label="`已使用(${counts[1] || 0})`" name="1" />
      <el-tab-pane :label="`已过期(${counts[2] || 0})`" name="2" />
    </el-tabs>

    <!-- 领券中心 -->
    <template v-if="tab === 'center'">
      <div class="coupon-grid">
        <div v-for="c in center" :key="c.id" :class="['coupon h-card', c.type === 1 ? 'cut' : 'disc']">
          <div class="c-left">
            <template v-if="c.type === 1">
              <span class="unit">¥</span><b class="val">{{ c.amount }}</b>
              <span class="cond">满 {{ c.threshold }} 可用</span>
            </template>
            <template v-else>
              <b class="val">{{ c.discount }}</b><span class="unit">折</span>
              <span class="cond">{{ c.threshold > 0 ? `满 ${c.threshold} 可用` : '无门槛' }}</span>
            </template>
          </div>
          <div class="c-right">
            <div class="c-name">{{ c.name }}</div>
            <div class="c-rule">{{ c.ruleText }}</div>
            <div class="c-scope">{{ c.scopeDesc }}</div>
            <div class="c-time">{{ date(c.startTime) }} ~ {{ date(c.endTime) }}</div>
            <div class="c-remain" v-if="c.totalCount > 0">仅剩 {{ c.remainCount }} 张</div>
            <el-button
              type="primary"
              size="small"
              :disabled="!c.canReceive"
              @click="receive(c)"
            >{{ c.receivedByMe >= c.perLimit && c.canReceive === false ? '已达领取上限' : c.canReceive ? '立即领取' : (c.blockReason || '不可领取') }}</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!center.length && !loading" description="暂无可领的券" />
    </template>

    <!-- 我的券 -->
    <template v-else>
      <div class="coupon-grid">
        <div v-for="c in mine" :key="c.id" :class="['coupon h-card', c.status === 0 ? (c.type === 1 ? 'cut' : 'disc') : 'used']">
          <div class="c-left">
            <template v-if="c.type === 1">
              <span class="unit">¥</span><b class="val">{{ c.amount }}</b>
              <span class="cond">满 {{ c.threshold }} 可用</span>
            </template>
            <template v-else>
              <b class="val">{{ c.discount }}</b><span class="unit">折</span>
            </template>
          </div>
          <div class="c-right">
            <div class="c-name">{{ c.name }}</div>
            <div class="c-rule">{{ c.ruleText }}</div>
            <div class="c-time">有效期至 {{ date(c.expireTime) }}</div>
            <el-tag v-if="c.status === 1" type="info" size="small">已用于订单 {{ c.orderNo }}</el-tag>
            <el-tag v-else-if="c.status === 2" type="info" size="small">已过期</el-tag>
          </div>
        </div>
      </div>
      <el-empty v-if="!mine.length && !loading" description="暂无优惠券" />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { date } from '@/utils/format'

const tab = ref('center')
const center = ref([])
const mine = ref([])
const counts = ref({})
const loading = ref(false)

async function loadCenter() {
  center.value = (await http.get('/portal/coupon/center')) || []
}

async function loadMine() {
  const status = tab.value === 'center' ? undefined : Number(tab.value)
  const data = await http.get('/portal/coupon/mine', { status, pageNum: 1, pageSize: 50 })
  mine.value = data.records || []
}

async function loadCounts() {
  counts.value = (await http.get('/portal/coupon/mine/count')) || {}
}

async function receive(c) {
  await http.post(`/portal/coupon/receive/${c.id}`)
  ElMessage.success('领取成功')
  loadCenter()
  loadCounts()
}

function reload() {
  tab.value === 'center' ? loadCenter() : loadMine()
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadCenter(), loadCounts()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page { padding-top: 22px; }
.coupon-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-top: 6px; }
@media (max-width: 900px) { .coupon-grid { grid-template-columns: 1fr; } }
.coupon { display: flex; overflow: hidden; min-height: 130px; }
.coupon.used { opacity: 0.6; filter: grayscale(0.4); }
.c-left {
  width: 180px; flex: none; color: #fff; position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.coupon.cut .c-left { background: linear-gradient(135deg, #c96a66, #9c2c2c); }
.coupon.disc .c-left { background: linear-gradient(135deg, #d4b572, #a8843c); }
.coupon.used .c-left { background: #b9b0a5; }
.val { font-size: 34px; font-weight: 800; }
.unit { font-size: 14px; }
.cond { font-size: 12px; opacity: 0.9; margin-top: 2px; }
.c-right { flex: 1; padding: 14px 16px; display: flex; flex-direction: column; gap: 3px; }
.c-name { font-weight: 700; font-size: 15px; }
.c-rule { color: var(--h-text-second); font-size: 12px; }
.c-scope { color: #8a6d2f; font-size: 12px; }
.c-time { color: var(--h-text-second); font-size: 11px; }
.c-remain { color: var(--h-primary); font-size: 12px; }
.c-right .el-button { margin-top: 6px; align-self: flex-start; }
</style>
