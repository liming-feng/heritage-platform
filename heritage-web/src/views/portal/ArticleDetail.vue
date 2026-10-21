<template>
  <div class="h-container page" v-if="article.id">
    <div class="main h-card">
      <h1 class="title">{{ article.title }}</h1>
      <div class="meta">
        <span>{{ article.author }}</span>
        <span v-if="article.categoryName" class="cat">{{ article.categoryName }}</span>
        <span>{{ dateTime(article.createTime) }}</span>
        <span>{{ article.viewCount }} 阅读</span>
        <span class="right">
          <a :class="['op', { done: article.liked }]" @click="toggleLike">
            <el-icon><Pointer /></el-icon> {{ article.likeCount }} 点赞
          </a>
          <a :class="['op', { done: article.favorited }]" @click="toggleFav">
            <el-icon><Star /></el-icon> {{ article.favorited ? '已收藏' : '收藏' }}
          </a>
        </span>
      </div>
      <img v-if="article.cover" class="cover" :src="img(article.cover)" @error="onImgErr" />
      <div class="rich" v-html="richText(article.content)"></div>
      <div class="tags" v-if="article.tags">
        <el-tag v-for="t in article.tags.split(',')" :key="t" size="small" effect="plain">{{ t }}</el-tag>
      </div>
    </div>

    <!-- 评论区 -->
    <div class="comments h-card">
      <div class="h-section-title">评论（{{ article.commentCount || 0 }}）</div>
      <div class="pub">
        <el-input
          v-model="commentText"
          type="textarea"
          :rows="2"
          maxlength="200"
          show-word-limit
          :placeholder="user.isLogin ? '说说你的看法…' : '登录后发表评论'"
        />
        <el-button type="primary" :disabled="!user.isLogin" @click="publish">发表评论</el-button>
      </div>
      <div v-for="c in comments" :key="c.id" class="comment">
        <el-avatar :size="34">{{ c.nickname?.charAt(0) }}</el-avatar>
        <div class="c-body">
          <div class="c-head">
            <b>{{ c.nickname }}</b>
            <span class="time">{{ dateTime(c.createTime) }}</span>
          </div>
          <div class="c-text">{{ c.content }}</div>
          <div v-for="r in c.replies || []" :key="r.id" class="reply">
            <b>{{ r.nickname }}</b>
            <span v-if="r.replyToNickname" class="to">回复 @{{ r.replyToNickname }}</span>：{{ r.content }}
          </div>
        </div>
      </div>
      <el-empty v-if="!comments.length" description="还没有评论，来抢沙发" :image-size="70" />
    </div>

    <!-- 相关阅读 -->
    <div class="h-card block" v-if="(article.related || []).length">
      <div class="h-section-title">相关阅读</div>
      <div v-for="r in article.related" :key="r.id" class="rel" @click="$router.replace(`/articles/${r.id}`)">
        <img :src="img(r.cover)" @error="onImgErr" />
        <span class="rel-title">{{ r.title }}</span>
      </div>
    </div>
  </div>
  <div class="h-container page" v-else-if="!loading"><el-empty description="资讯不存在" /></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr, dateTime, richText } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const article = ref({})
const comments = ref([])
const loading = ref(true)
const commentText = ref('')

async function load() {
  loading.value = true
  try {
    article.value = await http.get(`/portal/article/${route.params.id}`)
    comments.value = await http.get('/portal/article/comment/list', { articleId: route.params.id })
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (article.value.liked) {
    await http.post(`/portal/article/${article.value.id}/unlike`)
    article.value.liked = false
    article.value.likeCount -= 1
  } else {
    await http.post(`/portal/article/${article.value.id}/like`)
    article.value.liked = true
    article.value.likeCount += 1
  }
}

async function toggleFav() {
  if (!user.isLogin) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (article.value.favorited) {
    await http.post('/portal/favorite/remove', null, { params: { targetType: 3, targetId: article.value.id } })
    article.value.favorited = false
    ElMessage.success('已取消收藏')
  } else {
    await http.post('/portal/favorite/add', { targetType: 3, targetId: article.value.id })
    article.value.favorited = true
    ElMessage.success('已收藏')
  }
}

async function publish() {
  if (!commentText.value.trim()) return
  await http.post('/portal/article/comment/publish', {
    articleId: article.value.id,
    content: commentText.value
  })
  commentText.value = ''
  ElMessage.success('评论成功')
  comments.value = await http.get('/portal/article/comment/list', { articleId: route.params.id })
  article.value.commentCount = (article.value.commentCount || 0) + 1
}

onMounted(load)
</script>

<style scoped>
.page { padding-top: 22px; display: flex; flex-direction: column; gap: 16px; }
.main { padding: 30px 36px; }
.title { font-size: 26px; margin: 0 0 12px; }
.meta { display: flex; gap: 14px; color: var(--h-text-second); font-size: 13px; align-items: center; }
.cat { background: var(--h-paper-dark); border-radius: 4px; padding: 0 6px; }
.right { margin-left: auto; display: flex; gap: 16px; }
.op { display: inline-flex; align-items: center; gap: 3px; cursor: pointer; }
.op:hover, .op.done { color: var(--h-primary); }
.cover { width: 100%; max-height: 380px; object-fit: cover; border-radius: 10px; margin-top: 16px; }
.rich { margin-top: 18px; font-size: 15px; line-height: 1.9; }
.tags { display: flex; gap: 8px; margin-top: 20px; }
.comments, .block { padding: 22px 30px; }
.pub { display: flex; gap: 12px; align-items: flex-start; margin-bottom: 20px; }
.comment { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px dashed var(--h-border); }
.comment:last-of-type { border-bottom: none; }
.c-head { display: flex; gap: 10px; align-items: baseline; }
.time { color: var(--h-text-second); font-size: 12px; }
.c-text { margin-top: 4px; }
.reply { background: var(--h-paper-dark); border-radius: 8px; padding: 8px 12px; margin-top: 8px; font-size: 13px; }
.to { color: var(--h-text-second); }
.rel { display: flex; align-items: center; gap: 12px; padding: 10px 0; cursor: pointer; }
.rel:hover .rel-title { color: var(--h-primary); }
.rel img { width: 100px; height: 62px; object-fit: cover; border-radius: 8px; }
.rel-title { font-size: 14px; font-weight: 600; }
</style>
