<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    top="7vh"
    class="detail-dialog"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div v-loading="loading" class="dd-body">
      <slot />
    </div>
    <template #footer>
      <slot name="footer">
        <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      </slot>
    </template>
  </el-dialog>
</template>

<script setup>
/**
 * 详情弹窗外壳
 *
 * <p>三端原来有 7 处用 `ElMessageBox.alert` + 拼 `<br/>` 展示详情，问题是：
 * 字段没有对齐、长文本不分段、图片只能塞 <img> 更别提排版、还不能滚动。
 * 这里统一成 el-dialog + 插槽，内容用 el-descriptions / el-table 组织，
 * 页面只关心「放什么」，不再各自调样式。</p>
 *
 * <p>用法：`<DetailDialog v-model="xxDialog" :title="title" :loading="loading"> ... </DetailDialog>`</p>
 */
defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '' },
  width: { type: String, default: '680px' },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['update:modelValue'])
</script>

<style scoped>
.dd-body { min-height: 56px; }
</style>
