<template>
  <div class="h-container page">
    <div class="h-section-title">非遗资讯</div>

    <div class="filter">
      <el-radio-group v-model="query.categoryId" @change="reload">
        <el-radio-button :value="null">全部</el-radio-button>
        <el-radio-button v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="query.keyword"
        placeholder="搜索资讯"
        clearable
        style="width: 220px"
        @keyup.enter="reload"
        @clear="reload"
      />
    </div>

    <div class="list">
      <div v-for="a in list" :key="a.id" class="item h-card" @click="$router.push(`/articles/${a.id}`)">
        <img :src="img(a.cover)" @error="onImgErr" />
        <div class="info">
          <div class="title">
            <el-tag v-if="a.isTop === 1" size="small" type="danger" effect="dark">置顶</el-tag>
            {{ a.title }}
          </div>
          <div class="summary h-ellipsis-2">{{ a.summary }}</div>
          <div class="meta">
            <span>{{ a.author }}</span>
            <span v-if="a.categoryName" class="cat">{{ a.categoryName }}</span>
            <span>{{ dateTime(a.createTime) }}</span>
            <span class="m-right">{{ a.viewCount }} 阅读 · {{ a.likeCount }} 点赞 · {{ a.commentCount }} 评论</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length && !loading" description="暂无资讯" class="h-empty" />

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
import { img, onImgErr, dateTime } from '@/utils/format'

const categories = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, categoryId: null, keyword: '' })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/portal/article/list', { ...query })
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

onMounted(() => {
  load()
  http.get('/portal/category/list').catch(() => {})
})
</script>

<style scoped>
.page { padding-top: 22px; }
.filter { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
.item { display: flex; gap: 18px; padding: 16px; margin-bottom: 14px; cursor: pointer; }
.item:hover { transform: translateY(-2px); }
.item img { width: 200px; height: 120px; object-fit: cover; border-radius: 10px; flex: none; }
.info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.title { font-size: 17px; font-weight: 700; display: flex; align-items: center; gap: 8px; }
.item:hover .title { color: var(--h-primary); }
.summary { color: var(--h-text-second); font-size: 13px; margin-top: 6px; }
.meta { display: flex; gap: 14px; margin-top: auto; color: var(--h-text-second); font-size: 12px; align-items: center; }
.cat { background: var(--h-paper-dark); border-radius: 4px; padding: 0 6px; }
.m-right { margin-left: auto; }
.pager { display: flex; justify-content: center; margin-top: 24px; }
</style>
