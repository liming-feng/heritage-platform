<template>
  <div class="page">
    <div class="h-page-header">
      <h2>{{ form.id ? '编辑商品' : '发布新品' }}</h2>
      <el-button text @click="$router.back()">返回</el-button>
    </div>

    <div class="h-card form-card" v-loading="loading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width: 640px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit placeholder="如：蔚县手工剪纸·喜上眉梢" />
        </el-form-item>
        <el-form-item label="副标题"><el-input v-model="form.subtitle" maxlength="60" /></el-form-item>
        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="封面图" prop="cover">
          <el-input v-model="form.cover" placeholder="图片地址，如 /api/uploads/demo/goods-1.svg" @blur="preview = form.cover" />
          <img v-if="preview" :src="img(preview)" class="cover-preview" @error="onImgErr" />
        </el-form-item>
        <el-form-item label="售价（元）" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="原价（元）">
          <el-input-number v-model="form.originalPrice" :min="0" :precision="2" :step="10" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="10" />
          <span class="hint">库存低于 10 会进入库存预警</span>
        </el-form-item>
        <el-form-item label="纯手工">
          <el-switch v-model="form.isHandmade" :active-value="1" :inactive-value="0" />
          <span class="hint">标记后商品列表展示「纯手工」角标</span>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="form.description" type="textarea" :rows="2" maxlength="200" /></el-form-item>
        <el-form-item label="图文详情"><el-input v-model="form.detail" type="textarea" :rows="6" placeholder="支持 HTML 片段或纯文本" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存{{ form.id ? '修改' : '并提交审核' }}</el-button>
          <span class="hint" v-if="!form.id">新商品需通过平台审核后才能上架</span>
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
  id: null, categoryId: null, name: '', subtitle: '', cover: '',
  description: '', detail: '', price: null, originalPrice: null, stock: 0, isHandmade: 1
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  cover: [{ required: true, message: '请填写封面图地址', trigger: 'blur' }],
  price: [{ required: true, message: '请填写售价', trigger: 'change' }],
  stock: [{ required: true, message: '请填写库存', trigger: 'change' }]
}

async function load() {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const d = await http.get(`/craftsman/goods/${id}`)
    Object.keys(form).forEach((k) => (form[k] = d[k] ?? form[k]))
    preview.value = d.cover || ''
  } finally {
    loading.value = false
  }
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await http.post('/craftsman/goods/save', { ...form })
    ElMessage.success(form.id ? '修改已保存' : '已提交，等待平台审核')
    router.replace('/craftsman/goods')
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
.cover-preview { width: 160px; height: 110px; object-fit: cover; border-radius: 8px; margin-top: 8px; border: 1px solid var(--h-border); }
.hint { margin-left: 10px; color: var(--h-text-second); font-size: 12px; }
</style>
