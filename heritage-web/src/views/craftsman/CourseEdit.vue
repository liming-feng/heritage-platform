<template>
  <div class="page">
    <div class="h-page-header">
      <h2>{{ form.id ? '编辑课程' : '发布课程' }}</h2>
      <el-button text @click="$router.back()">返回</el-button>
    </div>

    <div class="h-card form-card" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" style="max-width: 660px">
        <el-form-item label="课程标题" prop="title">
          <el-input v-model="form.title" maxlength="50" show-word-limit placeholder="如：蔚县剪纸初体验——剪一只窗花" />
        </el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subtitle" maxlength="60" /></el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="form.categoryId" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图" prop="cover">
          <el-input v-model="form.cover" placeholder="图片地址" @blur="preview = form.cover" />
          <img v-if="preview" :src="img(preview)" class="cover-preview" @error="onImgErr" />
        </el-form-item>
        <el-form-item label="开课时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开课时间" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" />
        </el-form-item>
        <el-form-item label="预约截止" prop="bookingDeadline">
          <el-date-picker v-model="form.bookingDeadline" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" placeholder="此后不可再预约" />
        </el-form-item>
        <el-form-item label="取消截止" prop="cancelDeadline">
          <el-date-picker v-model="form.cancelDeadline" type="datetime" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" placeholder="此后用户不可取消" />
        </el-form-item>
        <el-form-item label="总名额" prop="totalQuota">
          <el-input-number v-model="form.totalQuota" :min="1" :max="200" />
          <span class="hint">每个预约按人数占用名额，超卖由后端三层防护兜底</span>
        </el-form-item>
        <el-form-item label="时长（分钟）"><el-input-number v-model="form.duration" :min="30" :step="30" /></el-form-item>
        <el-form-item label="上课地点" prop="address"><el-input v-model="form.address" maxlength="100" /></el-form-item>
        <el-form-item label="课程简介"><el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" /></el-form-item>
        <el-form-item label="课程详情"><el-input v-model="form.detail" type="textarea" :rows="6" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存{{ form.id ? '修改' : '并提交审核' }}</el-button>
          <span class="hint" v-if="!form.id">课程需通过平台审核后才会上架展示</span>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const preview = ref('')
const categories = ref([])

const form = reactive({
  id: null, categoryId: null, title: '', subtitle: '', cover: '',
  description: '', detail: '', address: '',
  startTime: '', endTime: '', bookingDeadline: '', cancelDeadline: '',
  totalQuota: 10, duration: 90
})

const rules = {
  title: [{ required: true, message: '请输入课程标题', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  cover: [{ required: true, message: '请填写封面图地址', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开课时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  totalQuota: [{ required: true, message: '请填写总名额', trigger: 'change' }],
  address: [{ required: true, message: '请填写上课地点', trigger: 'blur' }]
}

async function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    // 匠人端课程编辑：复用列表接口拿本条数据（列表 VO 含编辑所需的全部字段）
    const data = await http.get('/craftsman/course/list', { pageNum: 1, pageSize: 100 })
    const row = (data.records || []).find((c) => String(c.id) === String(id))
    if (!row) return ElMessage.warning('课程不存在或无权访问')
    Object.keys(form).forEach((k) => (form[k] = row[k] ?? form[k]))
    preview.value = row.cover || ''
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await http.post('/craftsman/course/save', { ...form })
    ElMessage.success(form.id ? '修改已保存' : '已提交，等待平台审核')
    router.replace('/craftsman/courses')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  http.get('/portal/category/list').then((d) => (categories.value = d || [])).catch(() => {})
  load()
})
</script>

<style scoped>
.form-card { padding: 26px 30px; }
.cover-preview { width: 160px; height: 100px; object-fit: cover; border-radius: 8px; margin-top: 8px; border: 1px solid var(--h-border); }
.hint { margin-left: 10px; color: var(--h-text-second); font-size: 12px; }
</style>
