<template>
  <div class="page">
    <div class="h-page-header">
      <h2>预约规则配置</h2>
      <span class="sub">修改保存后立即热生效（写操作清缓存），无需重启后端</span>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="configName" label="规则项" min-width="180" />
        <el-table-column prop="configKey" label="配置键" width="240" />
        <el-table-column label="当前值" width="170">
          <template #default="{ row }">
            <b class="val">{{ row.configValue }}</b>
            <span class="unit">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="280" />
        <el-table-column prop="updateTime" label="最近修改" width="150"><template #default="{ row }">{{ dateTime(row.updateTime) }}</template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="edit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" :title="'修改 · ' + (editing.configName || '')" width="440px">
      <el-form label-width="80px">
        <el-form-item :label="editing.unit ? `数值（${editing.unit}）` : '数值'">
          <el-input v-model="newValue" :placeholder="editing.configValue" />
        </el-form-item>
        <el-alert :title="editing.description" type="info" :closable="false" />
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存并生效</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/utils/request'
import { dateTime } from '@/utils/format'

const list = ref([])
const loading = ref(false)
const dialog = ref(false)
const editing = reactive({})
const newValue = ref('')

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/booking-config/list')
    list.value = data.records || data || []
  } finally {
    loading.value = false
  }
}

function edit(row) {
  Object.assign(editing, row)
  newValue.value = row.configValue
  dialog.value = true
}

async function save() {
  if (!newValue.value.trim()) return ElMessage.warning('请填写数值')
  await http.post('/admin/booking-config/update', {
    configKey: editing.configKey,
    configValue: newValue.value.trim()
  })
  ElMessage.success('已保存，规则即时生效')
  dialog.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.table-card { padding: 6px 16px 16px; }
.val { font-size: 16px; color: var(--h-primary); }
.unit { color: var(--h-text-second); font-size: 12px; margin-left: 4px; }
</style>
