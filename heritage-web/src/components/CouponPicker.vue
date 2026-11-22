<template>
  <el-drawer
    :model-value="modelValue"
    title="选择优惠券"
    size="440px"
    direction="rtl"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="cp-head">
      <div class="cp-head-main">
        可用 <b>{{ usableList.length }}</b> 张
        <span v-if="maxDiscount > 0"> · 最高可省 <b>¥{{ maxDiscount }}</b></span>
      </div>
      <span class="cp-head-tip">券按抵扣金额从高到低排序</span>
    </div>

    <el-tabs v-model="tab" class="cp-tabs">
      <el-tab-pane :label="`可用 (${usableList.length})`" name="usable" />
      <el-tab-pane :label="`不可用 (${unusableList.length})`" name="unusable" />
    </el-tabs>

    <div v-if="shown.length" class="cp-list">
      <div
        v-for="c in shown"
        :key="c.userCouponId"
        class="cp-card"
        :class="{ off: !c.usable, on: c.userCouponId === temp }"
        @click="c.usable && (temp = c.userCouponId)"
      >
        <div class="cp-side">
          <div class="cp-num">{{ c.usable ? money(c.discountAmount) : '—' }}</div>
          <div class="cp-side-txt">{{ c.typeDesc || (c.type === 2 ? '折扣券' : '满减券') }}</div>
        </div>
        <div class="cp-main">
          <div class="cp-name">
            <span class="cp-name-txt">{{ c.name }}</span>
            <em v-if="isBest(c)" class="cp-badge best">最省</em>
            <em v-if="expiringSoon(c)" class="cp-badge soon">即将过期</em>
          </div>
          <div class="cp-rule">{{ c.ruleText }}</div>
          <div class="cp-exp">有效期至 {{ date(c.expireTime) }}</div>
          <div v-if="c.usable" class="cp-pay">本单省 ¥{{ c.discountAmount }}，应付 ¥{{ c.payAmount }}</div>
          <div v-else class="cp-block">{{ c.blockReason || '不可用于本单' }}</div>
        </div>
        <div class="cp-check">
          <el-icon v-if="c.userCouponId === temp" :size="20"><CircleCheckFilled /></el-icon>
          <span v-else class="cp-dot" />
        </div>
      </div>
    </div>

    <el-empty v-else :image-size="70" :description="tab === 'usable' ? '本单暂无可用优惠券' : '没有不可用的券'">
      <el-button v-if="tab === 'usable'" type="primary" plain size="small" @click="goCenter">去领券中心</el-button>
    </el-empty>

    <template #footer>
      <div class="cp-foot">
        <el-button link :type="temp === null ? 'primary' : 'default'" @click="temp = null">
          不使用优惠券
        </el-button>
        <div class="cp-foot-right">
          <span v-if="picked" class="cp-foot-cut">已选：省 ¥{{ picked.discountAmount }}</span>
          <el-button type="primary" @click="confirm">确定</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { money, date } from '@/utils/format'

/**
 * 结算页「选择优惠券」抽屉
 *
 * 改造前是结算页里一排横向 el-radio 小卡片：一屏放不下、看不到有效期、
 * 不可用的券混在一起还占位置，也分不清"这张为什么不能用"。
 * 这里换成抽屉：可用/不可用分页签、卡片式展示规则与到手价、底部固定确认。
 *
 * 选择结果不直接改父组件状态，先存 temp，点「确定」才 emit，
 * 这样用户可以随便点着比较，取消时不会把原选择弄丢。
 */
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  coupons: { type: Array, default: () => [] },
  selectedId: { type: [Number, String], default: null }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const router = useRouter()
const tab = ref('usable')
const temp = ref(props.selectedId)

// 每次打开都回到当前已选的那张，并默认停在「可用」页签
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      temp.value = props.selectedId ?? null
      tab.value = 'usable'
    }
  }
)

const usableList = computed(() => props.coupons.filter((c) => c.usable))
const unusableList = computed(() => props.coupons.filter((c) => !c.usable))
const shown = computed(() => (tab.value === 'usable' ? usableList.value : unusableList.value))
const maxDiscount = computed(() =>
  usableList.value.reduce((m, c) => Math.max(m, Number(c.discountAmount) || 0), 0)
)
const picked = computed(() => usableList.value.find((c) => c.userCouponId === temp.value) || null)

/**
 * 抵扣最多的那张券 —— 国内电商选券页都会标一个「推荐/最省」，
 * 因为用户真正的问题不是「有哪些券」，而是「选哪张划算」。
 */
const bestId = computed(() => {
  let best = null
  let max = 0
  usableList.value.forEach((c) => {
    const d = Number(c.discountAmount) || 0
    if (d > max) {
      max = d
      best = c.userCouponId
    }
  })
  return best
})

function isBest(c) {
  return c.usable && bestId.value != null && c.userCouponId === bestId.value
}

/** 3 天内到期：临期券最容易白白浪费，提前提醒比事后惋惜有用 */
function expiringSoon(c) {
  if (!c.expireTime) return false
  const end = new Date(String(c.expireTime).replace(/-/g, '/')).getTime()
  if (Number.isNaN(end)) return false
  const days = (end - Date.now()) / 86400000
  return days >= 0 && days <= 3
}

function confirm() {
  emit('confirm', temp.value)
  emit('update:modelValue', false)
}

function goCenter() {
  emit('update:modelValue', false)
  router.push('/coupons')
}
</script>

<style scoped>
.cp-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding-bottom: 8px;
}
.cp-head-main { font-size: 13px; color: var(--h-ink); }
.cp-head-main b { color: var(--h-primary); font-size: 16px; }
.cp-head-tip { font-size: 12px; color: var(--h-text-second); }
.cp-tabs { margin-top: -6px; }

.cp-list { display: flex; flex-direction: column; gap: 10px; padding-bottom: 10px; }
.cp-card {
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--h-border);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s, box-shadow 0.18s;
}
.cp-card:hover { border-color: var(--h-primary-light); }
.cp-card.on {
  border-color: var(--h-primary);
  box-shadow: 0 0 0 1px var(--h-primary);
  background: #fdf3f2;
}
.cp-card.off {
  cursor: not-allowed;
  background: #faf8f5;
  opacity: 0.72;
}

.cp-side {
  width: 82px;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border-right: 1px dashed var(--h-border);
}
.cp-num { font-size: 20px; font-weight: 700; color: var(--h-primary); line-height: 1; }
.cp-card.off .cp-num { color: var(--h-text-second); }
.cp-side-txt { font-size: 11px; color: var(--h-text-second); }

.cp-main { flex: 1; min-width: 0; }
.cp-name { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600; color: var(--h-ink); }
.cp-name-txt { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cp-badge {
  flex: none;
  font-style: normal;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.6;
  padding: 0 4px;
  border-radius: 3px;
}
.cp-badge.best { color: #fff; background: var(--h-primary); }
.cp-badge.soon { color: #b98900; background: #fdf3d8; }
.cp-rule { font-size: 12px; color: var(--h-text-second); margin-top: 3px; }
.cp-exp { font-size: 11px; color: var(--h-text-second); margin-top: 3px; }
.cp-pay { font-size: 12px; color: var(--h-primary); margin-top: 5px; }
.cp-block { font-size: 12px; color: #d48806; margin-top: 5px; }

.cp-check { width: 24px; display: flex; align-items: center; justify-content: center; color: var(--h-primary); }
.cp-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid var(--h-border);
  background: #fff;
}

.cp-foot { display: flex; align-items: center; justify-content: space-between; }
.cp-foot-right { display: flex; align-items: center; gap: 12px; }
.cp-foot-cut { font-size: 12px; color: var(--h-primary); }
</style>
