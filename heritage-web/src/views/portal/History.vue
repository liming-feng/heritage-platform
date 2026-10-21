<template>
  <div class="h-container page">
    <div class="h-page-header">
      <h2>浏览历史</h2>
      <el-button text type="danger" @click="clearAll">清空历史</el-button>
    </div>

    <div v-for="g in groups" :key="g.date" class="group">
      <div class="date">{{ g.label || g.date }}</div>
      <div class="items">
        <div
          v-for="it in g.items"
          :key="it.targetType + '-' + it.targetId"
          class="item h-card"
          @click="go(it)"
        >
          <img :src="img(it.cover)" @error="onImgErr" />
          <div class="info">
            <div class="title h-ellipsis-1">{{ it.title }}</div>
            <div class="sub h-ellipsis-1">{{ it.subtitle }}</div>
            <div class="meta">
              <span class="h-price" v-if="it.price">¥{{ it.price }}</span>
              <span>{{ dateTime(it.browseTime) }}</span>
            </div>
          </div>
          <el-button text size="small" type="danger" @click.stop="remove(it)">删除</el-button>
        </div>
      </div>
    </div>

    <el-empty v-if="!groups.length" description="暂无浏览记录" class="h-empty">
      <el-button type="primary" @click="$router.push('/goods')">去逛逛</el-button>
    </el-empty>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime } from '@/utils/format'

const router = useRouter()
const groups = ref([])

async function load() {
  groups.value = (await http.get('/portal/history/list')) || []
}

function go(it) {
  // TARGET_GOODS=1 / TARGET_COURSE=2 / TARGET_ARTICLE=3
  if (it.targetType === 1) router.push(`/goods/${it.targetId}`)
  else if (it.targetType === 2) router.push(`/courses/${it.targetId}`)
  else router.push(`/articles/${it.targetId}`)
}

async function remove(it) {
  await http.delete('/portal/history/delete', { targetType: it.targetType, targetId: it.targetId })
  load()
}

async function clearAll() {
  await ElMessageBox.confirm('确定清空全部浏览历史吗？', '提示', { type: 'warning' })
  await http.delete('/portal/history/clear')
  ElMessage.success('已清空')
  load()
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; }
.group { margin-bottom: 18px; }
.date { font-weight: 700; color: var(--h-text-second); font-size: 13px; margin-bottom: 10px; }
.items { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.item { display: flex; align-items: center; gap: 12px; padding: 12px 14px; cursor: pointer; }
.item:hover .title { color: var(--h-primary); }
.item img { width: 76px; height: 56px; object-fit: cover; border-radius: 8px; flex: none; }
.info { flex: 1; min-width: 0; }
.title { font-size: 13px; font-weight: 600; }
.sub { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.meta { display: flex; gap: 10px; font-size: 12px; color: var(--h-text-second); margin-top: 3px; align-items: baseline; }
@media (max-width: 900px) { .items { grid-template-columns: 1fr; } }
</style>
