<template>
  <div class="h-container page" v-if="man.id">
    <div class="hero h-card">
      <img class="avatar" :src="img(man.cover)" @error="onImgErr" />
      <div class="info">
        <div class="name-row">
          <h1>{{ man.realName }}</h1>
          <el-tag type="warning" effect="plain">{{ man.craftType }}</el-tag>
        </div>
        <div class="inherit" v-if="man.inheritance">师承：{{ man.inheritance }}</div>
        <div class="intro">{{ man.intro }}</div>
        <div class="stats">
          <span><b>{{ man.goodsCount }}</b> 在售作品</span>
          <span><b>{{ man.courseCount }}</b> 门课程</span>
          <span><b>{{ man.followerCount }}</b> 粉丝</span>
          <span>入驻于 {{ date(man.joinTime) }}</span>
        </div>
      </div>
      <el-button
        class="follow-btn"
        :type="man.followed ? 'default' : 'primary'"
        @click="toggleFollow"
      >{{ man.followed ? '已关注' : '+ 关注' }}</el-button>
    </div>

    <div class="h-card block">
      <div class="h-section-title">从艺履历</div>
      <div class="rich" v-html="richText(man.resume)"></div>
    </div>

    <div class="h-card block">
      <div class="h-section-title">
        在售作品（{{ (man.goodsList || []).length }}）
        <span class="more" @click="$router.push({ path: '/goods', query: { craftsmanId: man.id } })">全部 ›</span>
      </div>
      <div class="goods-grid">
        <GoodsCard v-for="g in (man.goodsList || []).slice(0, 4)" :key="g.id" :goods="g" />
      </div>
      <el-empty v-if="!(man.goodsList || []).length" description="暂无在售作品" :image-size="70" />
    </div>

    <div class="h-card block">
      <div class="h-section-title">
        开设课程（{{ (man.courseList || []).length }}）
        <span class="more" @click="$router.push({ path: '/courses', query: { craftsmanId: man.id } })">全部 ›</span>
      </div>
      <div v-for="c in man.courseList || []" :key="c.id" class="course-row" @click="$router.push(`/courses/${c.id}`)">
        <img :src="img(c.cover)" @error="onImgErr" />
        <div class="c-info">
          <div class="c-title">{{ c.title }}</div>
          <div class="c-line">{{ dateTime(c.startTime) }} · {{ c.address }}</div>
          <div class="c-quota">
            剩余 <b :class="ratio(c) <= 0.2 ? 'level-danger' : ratio(c) <= 0.5 ? 'level-warn' : 'level-ok'">{{ c.remainQuota }}</b>/{{ c.totalQuota }}
            <span v-if="c.canBook && c.remainSeconds !== null" class="dl">距截止 {{ countdown(c.remainSeconds) }}</span>
            <span v-else-if="!c.canBook" class="dl closed">{{ c.closedReason }}</span>
          </div>
        </div>
        <span class="h-price">¥{{ c.price }}</span>
      </div>
      <el-empty v-if="!(man.courseList || []).length" description="暂无课程" :image-size="70" />
    </div>
  </div>
  <div class="h-container page" v-else-if="!loading"><el-empty description="匠人不存在" /></div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr, date, dateTime, countdown, richText } from '@/utils/format'
import GoodsCard from '@/components/GoodsCard.vue'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const man = ref({})
const loading = ref(true)

function ratio(c) {
  return c.totalQuota ? c.remainQuota / c.totalQuota : 0
}

async function load() {
  loading.value = true
  try {
    man.value = await http.get(`/portal/craftsman/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (man.value.followed) {
    await http.delete(`/portal/craftsman/follow/${man.value.id}`)
    man.value.followed = false
    man.value.followerCount -= 1
    ElMessage.success('已取消关注')
  } else {
    await http.post(`/portal/craftsman/follow/${man.value.id}`)
    man.value.followed = true
    man.value.followerCount += 1
    ElMessage.success('已关注')
  }
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 18px; }
.hero { display: flex; gap: 22px; padding: 26px; align-items: flex-start; }
.avatar { width: 130px; height: 130px; border-radius: 14px; object-fit: cover; flex: none; background: var(--h-paper-dark); }
.info { flex: 1; min-width: 0; }
.name-row { display: flex; align-items: center; gap: 12px; }
.name-row h1 { font-size: 24px; margin: 0; }
.inherit { color: #8a6d2f; font-size: 13px; margin-top: 4px; }
.intro { color: var(--h-text-second); margin-top: 10px; line-height: 1.7; }
.stats { display: flex; gap: 24px; margin-top: 12px; font-size: 13px; color: var(--h-text-second); }
.stats b { color: var(--h-ink); font-size: 17px; margin-right: 3px; }
.follow-btn { align-self: center; min-width: 100px; }
.block { padding: 22px 28px; }
.goods-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }
.course-row { display: flex; align-items: center; gap: 14px; padding: 12px 0; border-bottom: 1px dashed var(--h-border); cursor: pointer; }
.course-row:last-of-type { border-bottom: none; }
.course-row:hover .c-title { color: var(--h-primary); }
.course-row img { width: 110px; height: 68px; object-fit: cover; border-radius: 8px; flex: none; }
.c-info { flex: 1; min-width: 0; }
.c-title { font-weight: 600; }
.c-line { color: var(--h-text-second); font-size: 12px; margin-top: 3px; }
.c-quota { font-size: 12px; margin-top: 3px; }
.c-quota b { font-size: 14px; }
.level-ok { color: #389e0d; }
.level-warn { color: #d48806; }
.level-danger { color: var(--h-primary); }
.dl { margin-left: 12px; color: var(--h-text-second); }
.dl.closed { color: #b9b0a5; }
</style>
