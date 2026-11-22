<template>
  <div class="page">
    <div class="h-page-header"><h2>评价管理</h2></div>

    <div class="stat-cards">
      <div class="stat h-card"><div class="s-label">总评价</div><div class="s-val">{{ stat.total ?? 0 }}</div></div>
      <div class="stat h-card"><div class="s-label">平均评分</div><div class="s-val">{{ stat.avgRating ?? '—' }}</div></div>
      <div class="stat h-card"><div class="s-label">好评率</div><div class="s-val">{{ pct(stat.goodRate) }}</div></div>
      <div class="stat h-card"><div class="s-label">未回复</div><div class="s-val" :class="{ alert: stat.unrepliedCount > 0 }">{{ stat.unrepliedCount ?? 0 }}</div></div>
    </div>

    <div class="toolbar h-card">
      <el-select v-model="query.type" placeholder="评价类型" clearable style="width: 140px" @change="reload">
        <el-option label="商品评价" :value="1" />
        <el-option label="课程评价" :value="2" />
      </el-select>
      <el-select v-model="query.rating" placeholder="评分" clearable style="width: 120px" @change="reload">
        <el-option v-for="n in [5, 4, 3, 2, 1]" :key="n" :label="`${n} 星`" :value="n" />
      </el-select>
      <el-checkbox v-model="query.replied" label="只看未回复" @change="reload" />
    </div>

    <div class="list">
      <div v-for="r in list" :key="r.id" class="review h-card">
        <img class="cover" :src="img(r.targetCover)" @error="onImgErr" />
        <div class="body">
          <div class="head">
            <el-tag size="small" effect="plain">{{ r.typeDesc }}</el-tag>
            <span class="target">{{ r.targetName }}</span>
            <el-rate :model-value="r.rating" disabled size="small" />
            <span class="time">{{ dateTime(r.createTime) }}</span>
          </div>
          <div class="content">{{ r.content }}</div>
          <div class="imgs" v-if="(r.images || []).length">
            <img v-for="(im, i) in r.images" :key="i" :src="img(im)" @error="onImgErr" />
          </div>
          <div class="reply-box" v-if="r.replied">我的回复：{{ r.reply }}</div>
          <div class="ops" v-else>
            <el-button text size="small" type="primary" @click="openReply(r)">回复学员</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!list.length && !loading" description="暂无评价" />

    <div class="pager">
      <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="prev, pager, next" background @current-change="load" />
    </div>

    <el-dialog v-model="replyDialog" title="回复评价" width="440px">
      <el-input v-model="replyText" type="textarea" :rows="3" maxlength="200" show-word-limit placeholder="感谢你的反馈…" />
      <template #footer>
        <el-button @click="replyDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReply">回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr, dateTime, pct } from '@/utils/format'

const route = useRoute()

const stat = ref({})
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, type: undefined, rating: undefined, replied: false })

const replyDialog = ref(false)
const replyText = ref('')
const replying = ref(null)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/craftsman/review/list', {
      type: query.type ?? undefined,
      rating: query.rating ?? undefined,
      // 注意语义相反：复选框是「只看未回复」，而后端 replied=true 表示「只看已回复」，
      // 所以勾选时要传 false（以前直接传 true，结果勾了反而只剩已回复的）
      replied: query.replied ? false : undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
    stat.value = await http.get('/craftsman/review/stat').catch(() => ({}))
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function openReply(r) {
  replying.value = r
  replyText.value = ''
  replyDialog.value = true
}

async function submitReply() {
  if (!replyText.value.trim()) return ElMessage.warning('请填写回复内容')
  await http.post('/craftsman/review/reply', { reviewId: replying.value.id, reply: replyText.value })
  ElMessage.success('回复成功')
  replyDialog.value = false
  load()
}

onMounted(() => {
  // 工作台「未回复评价」带 ?replied=1 跳过来，直接勾上「只看未回复」
  if (route.query.replied === '1') query.replied = true
  load()
})
</script>

<style scoped>
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 14px; }
.stat { padding: 16px 20px; }
.s-label { color: var(--h-text-second); font-size: 13px; }
.s-val { font-size: 24px; font-weight: 800; margin-top: 4px; }
.s-val.alert { color: var(--h-primary); }
.toolbar { display: flex; align-items: center; gap: 14px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.review { display: flex; gap: 14px; padding: 16px 18px; margin-bottom: 12px; }
.cover { width: 100px; height: 68px; object-fit: cover; border-radius: 8px; flex: none; }
.body { flex: 1; min-width: 0; }
.head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.target { font-weight: 600; }
.time { color: var(--h-text-second); font-size: 12px; margin-left: auto; }
.content { margin-top: 8px; font-size: 13px; }
.imgs { display: flex; gap: 8px; margin-top: 8px; }
.imgs img { width: 72px; height: 72px; object-fit: cover; border-radius: 6px; }
.reply-box { margin-top: 8px; background: var(--h-paper-dark); border-radius: 8px; padding: 8px 12px; font-size: 12px; color: #6b6459; }
.ops { margin-top: 6px; }
.pager { display: flex; justify-content: center; margin-top: 16px; }
</style>
