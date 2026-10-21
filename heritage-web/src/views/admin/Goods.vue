<template>
  <div class="page">
    <div class="h-page-header">
      <h2>商品管理</h2>
      <div class="head-right">
        <el-button size="small" :disabled="!selected.length" @click="batchAudit(true)">批量通过</el-button>
        <el-button size="small" :disabled="!selected.length" @click="batchAudit(false)">批量驳回</el-button>
        <el-button size="small" @click="exportGoods">导出 Excel</el-button>
      </div>
    </div>

    <div class="toolbar h-card">
      <el-input v-model="query.keyword" placeholder="商品名称 / 匠人" clearable style="width: 200px" @keyup.enter="reload" @clear="reload" />
      <el-select v-model="query.auditStatus" placeholder="审核状态" clearable style="width: 130px" @change="reload">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-select v-model="query.status" placeholder="上架状态" clearable style="width: 130px" @change="reload">
        <el-option label="已上架" :value="1" />
        <el-option label="已下架" :value="0" />
        <el-option label="待审核" :value="2" />
      </el-select>
      <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 140px" @change="reload">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </div>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading" @selection-change="(s) => (selected = s.filter((x) => x.auditStatus === 0))">
        <el-table-column type="selection" width="42" :selectable="(row) => row.auditStatus === 0" />
        <el-table-column label="商品" min-width="230">
          <template #default="{ row }">
            <div class="g-cell">
              <img :src="img(row.cover)" @error="onImgErr" />
              <div>
                <div class="g-name">{{ row.name }}</div>
                <div class="sub">{{ row.craftsmanName }} · {{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="审核" width="110">
          <template #default="{ row }">
            <el-tag :type="AUDIT_STATUS[row.auditStatus]?.type" size="small">{{ row.auditStatusDesc }}</el-tag>
            <div class="reject" v-if="row.auditStatus === 2">{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column label="上架" width="90">
          <template #default="{ row }"><el-tag :type="GOODS_STATUS[row.status]?.type" size="small">{{ row.statusDesc }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="detail(row)">详情</el-button>
            <template v-if="row.auditStatus === 0">
              <el-button text size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button text size="small" type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
            <el-button v-if="row.status === 1" text size="small" type="warning" @click="offShelf(row)">强制下架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.pageNum" :page-size="query.pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { img, onImgErr, GOODS_STATUS, AUDIT_STATUS } from '@/utils/format'

const list = ref([])
const selected = ref([])
const categories = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', auditStatus: undefined, status: undefined, categoryId: undefined })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/goods/list', {
      ...query,
      auditStatus: query.auditStatus ?? undefined,
      status: query.status ?? undefined,
      categoryId: query.categoryId ?? undefined
    })
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

async function detail(row) {
  const d = await http.get(`/admin/goods/${row.id}/audit-detail`)
  const imgs = (d.images || []).map((u) => `<img src="${img(u)}" style="width:90px;height:90px;object-fit:cover;border-radius:6px;margin:3px" />`).join('')
  const specs = (d.specs || []).map((s) => `${s.specName} ¥${s.price}（库存${s.stock}）`).join('；') || '无规格'
  ElMessageBox.alert(
    `${imgs || ''}<br/>分类：${d.categoryName} · 匠人：${d.craftsmanName}<br/>` +
    `价格：¥${d.price}（原价 ¥${d.originalPrice}）<br/>规格：${specs}<br/>` +
    `简介：${d.description || '—'}<br/>详情：${(d.detail || '—').slice(0, 200)}`,
    `商品审核 · ${d.name}`,
    { dangerouslyUseHTMLString: true, customStyle: { maxWidth: '620px' } }
  )
}

async function audit(row, pass) {
  if (!pass) {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
    await http.post('/admin/goods/audit', { id: row.id, pass: false, reason: value })
  } else {
    await http.post('/admin/goods/audit', { id: row.id, pass: true })
  }
  ElMessage.success('操作成功')
  load()
}

async function batchAudit(pass) {
  if (!pass) {
    const { value } = await ElMessageBox.prompt(`驳回选中的 ${selected.value.length} 个商品，请填写原因`, '批量驳回', { inputPattern: /\S+/ })
    await http.post('/admin/goods/audit/batch', { ids: selected.value.map((x) => x.id), pass: false, reason: value })
  } else {
    await http.post('/admin/goods/audit/batch', { ids: selected.value.map((x) => x.id), pass: true })
  }
  ElMessage.success('操作成功')
  load()
}

async function offShelf(row) {
  await ElMessageBox.confirm(`强制下架「${row.name}」？匠人会收到通知`, '强制下架', { type: 'warning' })
  await http.post('/admin/goods/off-shelf', { ids: [row.id], reason: '平台强制下架' })
  ElMessage.success('已下架')
  load()
}

async function exportGoods() {
  const res = await http.download('/admin/goods/export', { ...query })
  saveBlob(res, '商品列表.xlsx')
}

onMounted(() => {
  load()
  http.get('/admin/category/list', { pageNum: 1, pageSize: 100 }).then((d) => (categories.value = d.records || [])).catch(() => {})
})
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.toolbar { display: flex; align-items: center; gap: 12px; padding: 14px 16px; margin-bottom: 14px; flex-wrap: wrap; }
.table-card { padding: 6px 16px 16px; }
.g-cell { display: flex; align-items: center; gap: 10px; }
.g-cell img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; flex: none; }
.g-name { font-size: 13px; font-weight: 600; }
.sub { color: var(--h-text-second); font-size: 12px; }
.reject { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
