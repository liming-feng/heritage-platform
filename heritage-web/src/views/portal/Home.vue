<template>
  <div class="home">
    <!-- 轮播 -->
    <section class="hero h-container">
      <el-carousel height="380px" class="carousel" :interval="4500">
        <el-carousel-item v-for="b in banners" :key="b.id">
          <img class="banner" :src="img(b.image)" :alt="b.title" @error="onImgErr" @click="goBanner(b)" />
        </el-carousel-item>
      </el-carousel>
      <!-- 分类入口 -->
      <div class="cats h-card">
        <div v-for="c in categories" :key="c.id" class="cat" @click="$router.push({ path: '/goods', query: { categoryId: c.id } })">
          <span class="cat-icon">{{ c.name?.charAt(0) }}</span>
          <span class="cat-name">{{ c.name }}</span>
          <span class="cat-count">{{ c.goodsCount }} 件</span>
        </div>
      </div>
    </section>

    <!-- 热销非遗好物 -->
    <section class="h-container sec">
      <div class="h-section-title">
        热销非遗好物
        <span class="more" @click="$router.push('/goods')">更多 ›</span>
      </div>
      <div class="grid">
        <GoodsCard v-for="g in hotGoods" :key="g.id" :goods="g" />
      </div>
    </section>

    <!-- 体验课程（预约入口） -->
    <section class="h-container sec course-sec">
      <div class="h-section-title">
        非遗体验课程
        <span class="course-slogan">亲手做一件非遗，比看十遍更难忘</span>
        <span class="more" @click="$router.push('/courses')">全部课程 ›</span>
      </div>
      <div class="course-grid">
        <div v-for="c in courses" :key="c.id" class="course h-card" @click="$router.push(`/courses/${c.id}`)">
          <img :src="img(c.cover)" @error="onImgErr" />
          <div class="c-body">
            <div class="c-title h-ellipsis-1">{{ c.title }}</div>
            <div class="c-line">{{ dateTime(c.startTime) }}</div>
            <div class="c-quota">
              <span :class="c.totalQuota && c.remainQuota / c.totalQuota <= 0.2 ? 'level-danger' : 'level-ok'">
                余 <b>{{ c.remainQuota }}</b>/{{ c.totalQuota }}
              </span>
              <span class="h-price">¥{{ c.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 匠人 -->
    <section class="h-container sec">
      <div class="h-section-title">
        匠人风采
        <span class="more" @click="$router.push('/craftsmans')">全部匠人 ›</span>
      </div>
      <div class="man-grid">
        <div v-for="m in craftsmen" :key="m.id" class="man h-card" @click="$router.push(`/craftsmans/${m.id}`)">
          <img :src="img(m.cover)" @error="onImgErr" />
          <div class="m-name">{{ m.realName }}</div>
          <div class="m-type">{{ m.craftType }}</div>
        </div>
      </div>
    </section>

    <!-- 资讯 -->
    <section class="h-container sec" v-if="articles.length">
      <div class="h-section-title">
        非遗资讯
        <span class="more" @click="$router.push('/articles')">更多 ›</span>
      </div>
      <div class="art-grid">
        <div v-for="a in articles" :key="a.id" class="art h-card" @click="$router.push(`/articles/${a.id}`)">
          <img :src="img(a.cover)" @error="onImgErr" />
          <div class="a-body">
            <div class="a-title h-ellipsis-2">{{ a.title }}</div>
            <div class="a-sum h-ellipsis-2">{{ a.summary }}</div>
            <div class="a-meta">{{ dateTime(a.createTime) }} · {{ a.viewCount }} 阅读</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime } from '@/utils/format'
import GoodsCard from '@/components/GoodsCard.vue'

const router = useRouter()
const banners = ref([])
const categories = ref([])
const hotGoods = ref([])
const courses = ref([])
const craftsmen = ref([])
const articles = ref([])

function goBanner(b) {
  if (b.linkUrl) router.push(b.linkUrl)
}

onMounted(async () => {
  http.get('/portal/home/banners').then((d) => (banners.value = d || [])).catch(() => {})
  http.get('/portal/home/categories').then((d) => (categories.value = d || [])).catch(() => {})
  http.get('/portal/home/hotGoods').then((d) => (hotGoods.value = (d || []).slice(0, 8))).catch(() => {})
  http.get('/portal/home/courses').then((d) => (courses.value = (d || []).slice(0, 3))).catch(() => {})
  http.get('/portal/home/craftsmen').then((d) => (craftsmen.value = (d || []).slice(0, 5))).catch(() => {})
  http.get('/portal/article/list', { pageNum: 1, pageSize: 3 })
    .then((d) => (articles.value = d.records || []))
    .catch(() => {})
})
</script>

<style scoped>
.home { padding-bottom: 30px; }
.hero { display: flex; gap: 16px; padding-top: 20px; }
.carousel { flex: 1; min-width: 0; border-radius: 12px; overflow: hidden; }
.banner { width: 100%; height: 100%; object-fit: cover; cursor: pointer; }
.cats { width: 300px; flex: none; display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 14px; align-content: start; max-height: 380px; overflow-y: auto; }
.cat { display: flex; flex-direction: column; align-items: center; padding: 12px 4px; border-radius: 10px; cursor: pointer; transition: background 0.2s; }
.cat:hover { background: var(--h-paper); }
.cat-icon {
  width: 44px; height: 44px; border-radius: 50%;
  background: linear-gradient(135deg, #f5e4e3, #f3e8ce);
  color: var(--h-primary); font-weight: 700; font-size: 18px;
  display: flex; align-items: center; justify-content: center;
}
.cat-name { font-size: 13px; font-weight: 600; margin-top: 6px; }
.cat-count { font-size: 11px; color: var(--h-text-second); }
.sec { margin-top: 34px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.course-sec .course-slogan { margin-left: 14px; font-size: 13px; color: #8a6d2f; font-weight: 400; }
.course-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.course { display: flex; cursor: pointer; overflow: hidden; }
.course img { width: 150px; height: 100%; min-height: 104px; object-fit: cover; flex: none; }
.c-body { padding: 12px 14px; flex: 1; min-width: 0; display: flex; flex-direction: column; }
.c-title { font-weight: 600; font-size: 14px; }
.c-line { color: var(--h-text-second); font-size: 12px; margin-top: 4px; }
.c-quota { display: flex; align-items: baseline; gap: 10px; margin-top: auto; font-size: 12px; }
.c-quota b { font-size: 15px; }
.level-ok { color: #389e0d; }
.level-danger { color: var(--h-primary); }
.c-quota .h-price { margin-left: auto; font-size: 16px; }
.man-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.man { padding: 16px 12px; text-align: center; cursor: pointer; }
.man img { width: 76px; height: 76px; border-radius: 50%; object-fit: cover; margin: 0 auto; background: var(--h-paper-dark); }
.m-name { font-weight: 700; margin-top: 10px; }
.m-type { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.art-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.art { cursor: pointer; overflow: hidden; }
.art img { width: 100%; height: 150px; object-fit: cover; }
.a-body { padding: 12px 14px; }
.a-title { font-weight: 600; font-size: 14px; }
.a-sum { color: var(--h-text-second); font-size: 12px; margin-top: 5px; }
.a-meta { color: var(--h-text-second); font-size: 11px; margin-top: 8px; }
@media (max-width: 1000px) {
  .cats { display: none; }
  .grid, .art-grid, .course-grid { grid-template-columns: repeat(2, 1fr); }
  .man-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
