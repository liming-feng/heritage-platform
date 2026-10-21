<template>
  <div class="g-card h-card" @click="$router.push(`/goods/${goods.id}`)">
    <div class="cover">
      <img :src="img(goods.cover)" :alt="goods.name" loading="lazy" @error="onImgErr" />
      <span v-if="goods.isHandmade === 1" class="hand-tag">纯手工</span>
    </div>
    <div class="body">
      <div class="name h-ellipsis-2">{{ goods.name }}</div>
      <div class="sub h-ellipsis-1">{{ goods.subtitle }}</div>
      <div class="meta">
        <span class="h-price">¥{{ goods.price }}</span>
        <span v-if="goods.originalPrice" class="h-price-origin">¥{{ goods.originalPrice }}</span>
        <span class="sales">已售 {{ goods.sales }}</span>
      </div>
      <div class="foot h-ellipsis-1">
        <span v-if="goods.craftsmanName">{{ goods.craftsmanName }}</span>
        <span v-if="goods.categoryName" class="cat">{{ goods.categoryName }}</span>
        <span v-if="goods.ratingAvg" class="rate">★ {{ goods.ratingAvg }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { img, onImgErr } from '@/utils/format'

defineProps({
  goods: { type: Object, required: true }
})
</script>

<style scoped>
.g-card { cursor: pointer; overflow: hidden; }
.g-card:hover { transform: translateY(-3px); }
.cover { position: relative; aspect-ratio: 4/3; overflow: hidden; background: var(--h-paper-dark); }
.cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s; }
.g-card:hover .cover img { transform: scale(1.05); }
.hand-tag {
  position: absolute; top: 10px; left: 10px;
  background: rgba(156, 44, 44, 0.9); color: #fff;
  font-size: 12px; padding: 2px 10px; border-radius: 12px;
}
.body { padding: 12px 14px 14px; }
.name { font-size: 15px; font-weight: 600; line-height: 1.45; min-height: 43px; }
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 3px; }
.meta { display: flex; align-items: baseline; margin-top: 8px; }
.meta .sales { margin-left: auto; color: var(--h-text-second); font-size: 12px; }
.foot { display: flex; gap: 10px; margin-top: 6px; font-size: 12px; color: var(--h-text-second); }
.cat { background: var(--h-paper-dark); border-radius: 4px; padding: 0 6px; }
.rate { color: #d48806; }
</style>
