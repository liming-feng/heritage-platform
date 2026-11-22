<template>
  <div class="h-container page" v-if="goods.id">
    <div class="main h-card">
      <!-- 左：图集 -->
      <div class="gallery">
        <img class="big" :src="img(currentImage)" @error="onImgErr" />
        <div class="thumbs" v-if="images.length > 1">
          <img
            v-for="(im, i) in images"
            :key="i"
            :src="img(im)"
            :class="{ active: im === currentImage }"
            @mouseenter="currentImage = im"
            @error="onImgErr"
          />
        </div>
      </div>

      <!-- 右：信息 -->
      <div class="info">
        <h1 class="name">{{ goods.name }}</h1>
        <p class="subtitle">{{ goods.subtitle }}</p>

        <div class="price-bar">
          <span class="h-price big-price">¥{{ goods.price }}</span>
          <span v-if="goods.originalPrice" class="h-price-origin">¥{{ goods.originalPrice }}</span>
          <span class="views">浏览 {{ goods.viewCount }} · 已售 {{ goods.sales }}</span>
        </div>

        <div class="kv"><span class="k">分类</span>{{ goods.category?.name || '—' }}</div>
        <div class="kv" v-if="goods.craftsman">
          <span class="k">匠人</span>
          <router-link :to="`/craftsmans/${goods.craftsman.id}`" class="link">
            {{ goods.craftsman.realName }}（{{ goods.craftsman.craftType }}）
          </router-link>
        </div>

        <!-- 规格选择 -->
        <div class="kv" v-if="goods.specs?.length">
          <span class="k">规格</span>
          <div class="specs">
            <div
              v-for="s in goods.specs"
              :key="s.id"
              :class="['spec', { active: specId === s.id, disabled: s.stock <= 0 }]"
              @click="s.stock > 0 && (specId = s.id)"
            >
              {{ s.specName }} <span class="spec-price">¥{{ s.price }}</span>
              <span class="spec-stock" v-if="s.stock <= 5">仅剩 {{ s.stock }}</span>
            </div>
          </div>
        </div>

        <div class="kv">
          <span class="k">数量</span>
          <el-input-number v-model="quantity" :min="1" :max="maxBuy" />
          <span class="stock">库存 {{ specId ? currentSpec.stock : goods.stock }}</span>
        </div>

        <div class="btns">
          <el-button type="primary" size="large" :disabled="!canBuy" @click="addCart">
            <el-icon><ShoppingCart /></el-icon>&nbsp;加入购物车
          </el-button>
          <el-button size="large" :disabled="!canBuy" @click="buyNow">立即购买</el-button>
          <el-button size="large" circle :type="followed ? 'warning' : 'default'" @click="toggleFavorite">
            <el-icon><Star /></el-icon>
          </el-button>
        </div>
        <div class="hint" v-if="goods.stock <= 0">该商品已售罄</div>
      </div>
    </div>

    <!-- 描述与详情 -->
    <div class="desc h-card">
      <div class="h-section-title">作品介绍</div>
      <div class="brief">{{ goods.description }}</div>
      <div class="rich" v-html="richText(goods.detail)"></div>
      <div class="imgs" v-if="images.length">
        <img v-for="(im, i) in images" :key="i" :src="img(im)" @error="onImgErr" />
      </div>
    </div>

    <!-- 评价 -->
    <div class="reviews h-card">
      <div class="h-section-title">
        用户评价（{{ goods.reviewCount || 0 }}）
        <span class="rate" v-if="goods.ratingAvg">平均 {{ goods.ratingAvg }} 分</span>
      </div>
      <div v-for="r in goods.reviews || []" :key="r.id" class="review">
        <div class="review-head">
          <el-rate :model-value="r.rating" disabled size="small" />
          <span class="nick">{{ r.nickname }}</span>
          <span class="time">{{ dateTime(r.createTime) }}</span>
          <span class="likes"><el-icon><Pointer /></el-icon>{{ r.likeCount }}</span>
        </div>
        <div class="content">{{ r.content }}</div>
        <div class="review-imgs" v-if="r.images?.length">
          <img v-for="(im, i) in r.images" :key="i" :src="img(im)" @error="onImgErr" />
        </div>
        <div class="reply" v-if="r.reply">匠人回复：{{ r.reply }}</div>
      </div>
      <el-empty v-if="!(goods.reviews || []).length" description="暂无评价" :image-size="80" />
    </div>
  </div>

  <div class="h-container page" v-else-if="!loading">
    <el-empty description="商品不存在或已下架" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr, dateTime, richText } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const goods = ref({})
const loading = ref(true)
const currentImage = ref('')
const specId = ref(null)
const quantity = ref(1)
const followed = ref(false)

const images = computed(() => {
  const arr = goods.value.images || []
  return arr.length ? arr : goods.value.cover ? [goods.value.cover] : []
})
const currentSpec = computed(() => (goods.value.specs || []).find((s) => s.id === specId.value) || {})
const maxBuy = computed(() => Number(specId.value ? currentSpec.value.stock : goods.value.stock) || 0)
const canBuy = computed(() => maxBuy.value > 0)

watch(images, (v) => (currentImage.value = v[0] || ''), { immediate: true })
watch(specId, () => (quantity.value = 1))

async function load() {
  loading.value = true
  try {
    goods.value = await http.get(`/portal/goods/${route.params.id}`)
    const firstInStock = (goods.value.specs || []).find((s) => s.stock > 0)
    if (firstInStock) specId.value = firstInStock.id
    await refreshFav()
    reportHistory()
  } finally {
    loading.value = false
  }
}

/**
 * 上报浏览历史（browse_history）
 *
 * 这一行不是可选装饰：用户端「我的浏览」列表、以及匠人端「浏览量趋势」的数据源
 * 都是 browse_history。此前前端从未调用该接口，导致该表只有初始化时的演示数据，
 * 匠人的趋势图永远是一条 0。
 *
 * 只对登录用户上报（接口要求登录）；接口内部按 (user,type,target) 去重，
 * 重复浏览只刷新时间，不会撑大表。
 */
function reportHistory() {
  if (!user.isLogin) return
  http
    .post('/portal/history/upload', { items: [{ targetType: 1, targetId: Number(route.params.id) }] })
    .catch(() => { /* 上报失败不影响浏览 */ })
}

async function refreshFav() {
  if (!user.isLogin) return
  try {
    followed.value = await http.get('/portal/favorite/status', {
      targetType: 1,
      targetId: route.params.id
    })
  } catch { /* 未登录忽略 */ }
}

async function toggleFavorite() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (followed.value) {
    await http.post('/portal/favorite/remove', null, { params: { targetType: 1, targetId: route.params.id } })
    followed.value = false
    ElMessage.success('已取消收藏')
  } else {
    await http.post('/portal/favorite/add', { targetType: 1, targetId: Number(route.params.id) })
    followed.value = true
    ElMessage.success('已收藏')
  }
}

async function addCart() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  await http.post('/portal/cart/add', {
    goodsId: goods.value.id,
    specId: specId.value,
    quantity: quantity.value
  })
  const count = await http.get('/portal/cart/count')
  user.setCartCount(Number(count) || 0)
  ElMessage.success('已加入购物车')
}

async function buyNow() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  router.push({
    path: '/checkout',
    query: { goodsId: goods.value.id, specId: specId.value || '', quantity: quantity.value }
  })
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 18px; }
.main { display: flex; gap: 30px; padding: 26px; }
.gallery { width: 420px; flex: none; }
.gallery .big { width: 100%; aspect-ratio: 1; object-fit: cover; border-radius: 10px; border: 1px solid var(--h-border); }
.thumbs { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }
.thumbs img { width: 62px; height: 62px; object-fit: cover; border-radius: 6px; cursor: pointer; border: 2px solid transparent; }
.thumbs img.active { border-color: var(--h-primary); }
.info { flex: 1; min-width: 0; }
.name { font-size: 22px; margin: 0 0 6px; line-height: 1.4; }
.subtitle { color: var(--h-text-second); margin: 0 0 14px; }
.price-bar {
  background: linear-gradient(90deg, #fdf3f2, #faf7f2);
  padding: 14px 16px; border-radius: 10px; margin-bottom: 16px;
  display: flex; align-items: baseline; gap: 10px;
}
.big-price { font-size: 28px; }
.views { margin-left: auto; color: var(--h-text-second); font-size: 12px; }
.kv { display: flex; align-items: flex-start; gap: 14px; margin-bottom: 14px; font-size: 14px; }
.k { color: var(--h-text-second); flex: none; width: 40px; }
.link { color: var(--h-primary); }
.specs { display: flex; flex-wrap: wrap; gap: 8px; }
.spec {
  border: 1px solid var(--h-border); border-radius: 8px; padding: 6px 14px;
  cursor: pointer; transition: all 0.15s;
}
.spec:hover { border-color: var(--h-primary-light); }
.spec.active { border-color: var(--h-primary); color: var(--h-primary); background: #fdf3f2; }
.spec.disabled { color: #bbb; cursor: not-allowed; text-decoration: line-through; }
.spec-price { font-size: 12px; color: var(--h-primary); margin-left: 4px; }
.spec-stock { font-size: 11px; color: #d48806; margin-left: 6px; }
.stock { color: var(--h-text-second); font-size: 12px; margin-left: 10px; }
.btns { display: flex; gap: 12px; margin-top: 20px; }
.btns .el-button { min-width: 140px; }
.hint { color: #d48806; font-size: 13px; margin-top: 10px; }
.desc, .reviews { padding: 24px 28px; }
.brief { color: var(--h-ink); margin-bottom: 14px; }
.imgs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 14px; }
.imgs img { width: 100%; border-radius: 8px; }
.rate { margin-left: 12px; font-size: 14px; color: #d48806; font-weight: 400; }
.review { border-bottom: 1px dashed var(--h-border); padding: 14px 0; }
.review:last-of-type { border-bottom: none; }
.review-head { display: flex; align-items: center; gap: 12px; }
.nick { font-weight: 600; }
.time, .likes { color: var(--h-text-second); font-size: 12px; }
.likes { margin-left: auto; display: flex; align-items: center; gap: 3px; }
.content { margin-top: 6px; }
.review-imgs { display: flex; gap: 8px; margin-top: 8px; }
.review-imgs img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.reply {
  margin-top: 8px; background: var(--h-paper-dark); border-radius: 8px;
  padding: 8px 12px; font-size: 13px; color: #6b6459;
}
</style>
