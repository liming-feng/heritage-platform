<template>
  <div class="h-container page">
    <div class="h-section-title">匠人名录</div>

    <div class="filter h-card">
      <el-input
        v-model="query.keyword"
        placeholder="搜索匠人姓名 / 技艺"
        clearable
        style="width: 240px"
        @keyup.enter="reload"
        @clear="reload"
      >
        <template #suffix><el-icon style="cursor: pointer" @click="reload"><Search /></el-icon></template>
      </el-input>
      <div class="f-item">
        <span class="f-label">门类</span>
        <a
          v-for="t in [{ v: null, n: '全部' }, ...craftTypes]"
          :key="t.v ?? 'all'"
          :class="['f-opt', { active: query.craftType === t.v }]"
          @click="pickType(t.v)"
        >{{ t.n }}</a>
      </div>
      <div class="f-item">
        <span class="f-label">排序</span>
        <a
          v-for="s in sorts"
          :key="s.value"
          :class="['f-opt', { active: query.sort === s.value }]"
          @click="pickSort(s.value)"
        >{{ s.label }}</a>
      </div>
    </div>

    <div v-if="list.length" class="grid">
      <div v-for="c in list" :key="c.id" class="man h-card" @click="$router.push(`/craftsmans/${c.id}`)">
        <img class="avatar" :src="img(c.cover)" @error="onImgErr" />
        <div class="info">
          <div class="name">
            {{ c.realName }}
            <el-tag size="small" type="warning" effect="plain">{{ c.craftType }}</el-tag>
          </div>
          <div class="inherit h-ellipsis-1" v-if="c.inheritance">师承：{{ c.inheritance }}</div>
          <div class="intro h-ellipsis-2">{{ c.intro }}</div>
          <div class="stats">
            <span><b>{{ c.goodsCount }}</b> 作品</span>
            <span><b>{{ c.followerCount }}</b> 关注</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无匠人" class="h-empty" />

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="Number(total)"
        layout="prev, pager, next"
        background
        @current-change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { http } from '@/utils/request'
import { img, onImgErr } from '@/utils/format'

const craftTypes = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)

const sorts = [
  { value: 'default', label: '综合' },
  { value: 'follower', label: '最多关注' },
  { value: 'goods', label: '作品最多' },
  { value: 'new', label: '最新入驻' }
]

const query = reactive({ pageNum: 1, pageSize: 9, keyword: '', craftType: null, sort: 'default' })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/craftsman/list', { ...query })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function pickType(v) {
  query.craftType = v
  query.pageNum = 1
  load()
}

function pickSort(v) {
  query.sort = v
  query.pageNum = 1
  load()
}

onMounted(async () => {
  load()
  // 门类选项从列表结果聚合
  http.get('/portal/craftsman/list', { pageNum: 1, pageSize: 100 })
    .then((d) => {
      const set = [...new Set((d.records || []).map((x) => x.craftType).filter(Boolean))]
      craftTypes.value = set.map((n) => ({ v: n, n }))
    })
    .catch(() => {})
})
</script>

<style scoped>
.page { padding-top: 22px; }
.filter { display: flex; align-items: center; gap: 22px; padding: 14px 18px; margin-bottom: 18px; flex-wrap: wrap; }
.f-item { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.f-label { color: var(--h-text-second); font-size: 13px; }
.f-opt { cursor: pointer; font-size: 13px; padding: 3px 8px; border-radius: 6px; }
.f-opt:hover { color: var(--h-primary); }
.f-opt.active { color: var(--h-primary); background: #f5e4e3; font-weight: 600; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; }
@media (max-width: 1000px) { .grid { grid-template-columns: repeat(2, 1fr); } }
.man { display: flex; gap: 14px; padding: 18px; cursor: pointer; }
.man:hover { transform: translateY(-3px); }
.avatar { width: 92px; height: 92px; border-radius: 12px; object-fit: cover; flex: none; background: var(--h-paper-dark); }
.info { min-width: 0; }
.name { font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.inherit { color: #8a6d2f; font-size: 12px; margin-top: 3px; }
.intro { color: var(--h-text-second); font-size: 12px; margin-top: 5px; line-height: 1.5; }
.stats { display: flex; gap: 18px; margin-top: 8px; font-size: 12px; color: var(--h-text-second); }
.stats b { color: var(--h-ink); font-size: 14px; margin-right: 2px; }
.pager { display: flex; justify-content: center; margin-top: 26px; }
</style>
