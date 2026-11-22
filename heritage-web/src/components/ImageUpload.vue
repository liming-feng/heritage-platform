<template>
  <div class="image-upload">
    <el-upload
      class="iu-uploader"
      :show-file-list="false"
      :http-request="doUpload"
      :before-upload="beforeUpload"
      :disabled="disabled || uploading"
      accept="image/jpeg,image/png,image/webp,image/gif"
    >
      <div v-loading="uploading" class="iu-box" :style="{ width: width + 'px', height: height + 'px' }">
        <template v-if="modelValue">
          <img :src="img(modelValue)" class="iu-img" alt="封面" @error="onImgErr" />
          <div class="iu-mask">
            <el-icon><Refresh /></el-icon>
            <span>更换</span>
          </div>
        </template>
        <template v-else>
          <el-icon class="iu-plus"><Plus /></el-icon>
          <span class="iu-text">点击上传</span>
        </template>
      </div>
    </el-upload>

    <div class="iu-side">
      <div class="iu-actions">
        <el-button v-if="modelValue && !disabled" link type="danger" size="small" @click="clear">删除</el-button>
        <el-button v-if="!disabled" link size="small" @click="manual = !manual">
          {{ manual ? '收起地址' : '手动填写地址' }}
        </el-button>
      </div>
      <div class="iu-tip">{{ tip || `支持 jpg / png / webp / gif，单个不超过 ${maxMb}MB` }}</div>
      <el-input
        v-if="manual"
        v-model="draft"
        size="small"
        placeholder="粘贴图片地址，如 /api/uploads/demo/goods-1.svg"
        @change="applyDraft"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { img, onImgErr } from '@/utils/format'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 上传接口，默认按当前所在端自动选择：/admin -> 管理端，/craftsman -> 匠人端，其余走用户端 */
  action: { type: String, default: '' },
  width: { type: Number, default: 160 },
  height: { type: Number, default: 110 },
  tip: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  maxMb: { type: Number, default: 5 }
})

const emit = defineEmits(['update:modelValue'])

const route = useRoute()
const uploading = ref(false)
const manual = ref(false)
const draft = ref(props.modelValue || '')

watch(
  () => props.modelValue,
  (v) => (draft.value = v || '')
)

/** 上传地址：三端各有一个入口，权限由后端拦截器按前缀隔离 */
const endpoint = computed(() => {
  if (props.action) return props.action
  const path = route.path || ''
  if (path.startsWith('/admin')) return '/admin/upload/image'
  if (path.startsWith('/craftsman')) return '/craftsman/upload/image'
  return '/portal/upload/image'
})

const ALLOWED = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

function beforeUpload(file) {
  if (!ALLOWED.includes(file.type)) {
    ElMessage.error('只支持 jpg / png / webp / gif 格式的图片')
    return false
  }
  if (file.size > props.maxMb * 1024 * 1024) {
    ElMessage.error(`图片不能超过 ${props.maxMb}MB`)
    return false
  }
  return true
}

/** 走项目统一的 axios 实例，自动带 token、统一错误提示、401 自动刷新重试 */
async function doUpload(options) {
  const fd = new FormData()
  fd.append('file', options.file)
  uploading.value = true
  try {
    const res = await http.post(endpoint.value, fd)
    const url = res?.url || ''
    if (!url) throw new Error('上传结果中缺少图片地址')
    emit('update:modelValue', url)
    draft.value = url
    ElMessage.success('上传成功')
    options.onSuccess?.(res)
  } catch (e) {
    options.onError?.(e)
  } finally {
    uploading.value = false
  }
}

function clear() {
  emit('update:modelValue', '')
  draft.value = ''
}

function applyDraft() {
  emit('update:modelValue', (draft.value || '').trim())
}
</script>

<style scoped>
.image-upload { display: flex; align-items: flex-start; gap: 14px; }
.iu-uploader :deep(.el-upload) { display: block; }
.iu-box {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed var(--h-border);
  border-radius: var(--h-radius);
  background: var(--h-paper-dark);
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}
.iu-box:hover { border-color: var(--h-primary); background: #fff; }
.iu-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.iu-plus { font-size: 24px; color: var(--h-text-second); }
.iu-text { font-size: 12px; color: var(--h-text-second); }
.iu-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #fff;
  font-size: 12px;
  background: rgba(43, 43, 43, 0.45);
  opacity: 0;
  transition: opacity 0.2s;
}
.iu-box:hover .iu-mask { opacity: 1; }
.iu-side { display: flex; flex-direction: column; gap: 6px; min-width: 0; flex: 1; }
.iu-actions { display: flex; gap: 8px; }
.iu-tip { font-size: 12px; color: var(--h-text-second); line-height: 1.5; }
</style>
