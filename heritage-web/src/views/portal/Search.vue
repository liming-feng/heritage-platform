<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>搜索结果</h2>
      <span class="sub">关键词「{{ q }}」共 {{ total }} 件</span>
    </div>

    <div class="grid" v-if="list.length">
      <GoodsCard v-for="g in list" :key="g.id" :goods="g" />
    </div>
    <el-empty v-else-if="!loading" description="没有找到相关商品，换个关键词试试" class="h-empty" />

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
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { http } from '@/utils/request'
import GoodsCard from '@/components/GoodsCard.vue'

const route = useRoute()
const router = useRouter()

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12
const loading = ref(false)
const q = ref('')

async function load() {
  q.value = String(route.query.q || '')
  if (!q.value) return
  loading.value = true
  try {
    const data = await http.get('/portal/goods/search', { keyword: q.value, pageNum: pageNum.value, pageSize })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

watch(() => route.query.q, () => {
  pageNum.value = 1
  load()
})

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; }
@media (max-width: 1100px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px) { .grid { grid-template-columns: repeat(2, 1fr); } }
.pager { display: flex; justify-content: center; margin-top: 26px; }
</style>
