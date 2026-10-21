<template>
  <div class="h-container page">
    <!-- 顶部分类导航 -->
    <div class="cats">
      <a
        v-for="c in [{ id: null, name: '全部' }, ...categories]"
        :key="c.id ?? 'all'"
        :class="['cat', { active: query.categoryId === c.id }]"
        @click="pickCategory(c.id)"
      >{{ c.name }}</a>
    </div>

    <!-- 筛选条 -->
    <div class="filter h-card">
      <div class="f-item">
        <span class="f-label">排序</span>
        <a
          v-for="s in sorts"
          :key="s.value"
          :class="['f-opt', { active: query.sort === s.value }]"
          @click="pickSort(s.value)"
        >{{ s.label }}</a>
      </div>
      <div class="f-item">
        <span class="f-label">价格</span>
        <el-input v-model="minPrice" placeholder="最低" style="width: 90px" size="small" />
        <span class="dash">-</span>
        <el-input v-model="maxPrice" placeholder="最高" style="width: 90px" size="small" />
        <el-button size="small" text type="primary" @click="applyPrice">确定</el-button>
      </div>
      <el-checkbox v-model="handmade" label="仅看纯手工" @change="reload" />
      <span class="total">共 {{ total }} 件</span>
    </div>

    <!-- 商品网格 -->
    <div v-if="list.length" class="grid">
      <GoodsCard v-for="g in list" :key="g.id" :goods="g" />
    </div>
    <el-empty v-else-if="!loading" description="没有找到符合条件的商品" class="h-empty" />

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="Number(total)"
        layout="prev, pager, next, jumper"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { http } from '@/utils/request'
import GoodsCard from '@/components/GoodsCard.vue'

const route = useRoute()

const categories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)

const sorts = [
  { value: 'default', label: '综合' },
  { value: 'sales', label: '销量' },
  { value: 'price_asc', label: '价格↑' },
  { value: 'price_desc', label: '价格↓' },
  { value: 'new', label: '最新' }
]

const query = reactive({
  pageNum: 1,
  pageSize: 12,
  categoryId: route.query.categoryId ? Number(route.query.categoryId) : null,
  keyword: '',
  sort: 'default',
  isHandmade: undefined
})
const minPrice = ref('')
const maxPrice = ref('')
const handmade = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/goods/list', { ...query })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.isHandmade = handmade.value ? 1 : undefined
  query.pageNum = 1
  load()
}

function pickCategory(id) {
  query.categoryId = id
  query.pageNum = 1
  load()
}

function pickSort(v) {
  query.sort = v
  query.pageNum = 1
  load()
}

function applyPrice() {
  query.minPrice = minPrice.value || undefined
  query.maxPrice = maxPrice.value || undefined
  query.pageNum = 1
  load()
}

onMounted(() => {
  if (route.query.q) query.keyword = String(route.query.q)
  http.get('/portal/category/list').then((d) => (categories.value = d || [])).catch(() => {})
  load()
})
</script>

<style scoped>
.page { padding-top: 22px; }
.cats { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.cat {
  padding: 7px 18px; border-radius: 20px; background: #fff;
  border: 1px solid var(--h-border); cursor: pointer; font-size: 14px;
  transition: all 0.2s;
}
.cat:hover { color: var(--h-primary); border-color: var(--h-primary-light); }
.cat.active { background: var(--h-primary); color: #fff; border-color: var(--h-primary); }
.filter { display: flex; align-items: center; gap: 22px; padding: 12px 18px; margin-bottom: 18px; flex-wrap: wrap; }
.f-item { display: flex; align-items: center; gap: 8px; }
.f-label { color: var(--h-text-second); font-size: 13px; }
.f-opt { cursor: pointer; font-size: 13px; padding: 3px 8px; border-radius: 6px; }
.f-opt:hover { color: var(--h-primary); }
.f-opt.active { color: var(--h-primary); background: #f5e4e3; font-weight: 600; }
.dash { color: var(--h-text-second); }
.total { margin-left: auto; color: var(--h-text-second); font-size: 13px; }
.grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px;
}
@media (max-width: 1100px) { .grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 800px) { .grid { grid-template-columns: repeat(2, 1fr); } }
.pager { display: flex; justify-content: center; margin-top: 26px; }
</style>
