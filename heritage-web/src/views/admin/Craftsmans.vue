<template>
  <div class="page">
    <div class="h-page-header">
      <h2>匠人资质审核</h2>
      <el-button size="small" @click="exportCrafts">导出 Excel</el-button>
    </div>

    <el-tabs v-model="auditStatus" @tab-change="reload">
      <el-tab-pane label="全部" name="null" />
      <el-tab-pane label="待审核" name="0" />
      <el-tab-pane label="已通过" name="1" />
      <el-tab-pane label="已驳回" name="2" />
    </el-tabs>

    <div class="h-card table-card">
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="匠人" min-width="200">
          <template #default="{ row }">
            <div class="m-cell">
              <img :src="img(row.cover)" @error="onImgErr" />
              <div>
                <div class="m-name">{{ row.realName }}</div>
                <div class="sub">{{ row.craftType }} · {{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="goodsCount" label="作品" width="70" />
        <el-table-column prop="courseCount" label="课程" width="70" />
        <el-table-column prop="followerCount" label="粉丝" width="70" />
        <el-table-column label="资质状态" width="100">
          <template #default="{ row }">
            <el-tag :type="AUDIT_STATUS[row.auditStatus]?.type" size="small">{{ row.auditStatusText }}</el-tag>
            <div class="reject" v-if="row.auditStatus === 2">{{ row.rejectReason }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="detail(row)">资质材料</el-button>
            <template v-if="row.auditStatus === 0">
              <el-button text size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button text size="small" type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { img, onImgErr, dateTime, AUDIT_STATUS } from '@/utils/format'

const auditStatus = ref('null')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/craftsman/list', {
      auditStatus: auditStatus.value === 'null' ? undefined : Number(auditStatus.value),
      pageNum: pageNum.value,
      pageSize
    })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  pageNum.value = 1
  load()
}

async function detail(row) {
  const d = await http.get(`/admin/craftsman/${row.id}/audit-detail`)
  const certs = (d.certImages || []).map((u) => `<img src="${img(u)}" style="width:120px;height:90px;object-fit:cover;border-radius:6px;margin:4px" />`).join('')
  const preview = (list) => (list || []).map((p) => `${p.name}（${p.statusText}）`).join('、') || '无'
  ElMessageBox.alert(
    `姓名：${d.realName}（${d.craftType}）<br/>身份证号：${d.idCardNo || '未填写'}<br/>联系电话：${d.phone}<br/>` +
    `师承：${d.inheritance || '—'}<br/>简介：${d.intro || '—'}<br/>履历：${d.resume || '—'}<br/>` +
    `证书图片：${certs || '未上传'}<br/>` +
    `已发布作品：${preview(d.goodsPreview)}<br/>已发布课程：${preview(d.coursePreview)}`,
    `匠人资质 · ${d.realName}`,
    { dangerouslyUseHTMLString: true, customStyle: { maxWidth: '620px' } }
  )
}

async function audit(row, pass) {
  if (!pass) {
    const { value } = await ElMessageBox.prompt('请填写驳回原因', '驳回申请', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空' })
    await http.post('/admin/craftsman/audit', { id: row.id, pass: false, reason: value })
  } else {
    await ElMessageBox.confirm(`通过「${row.realName}」的匠人资质审核？通过后其作品与课程将进入正常审核通道`, '通过审核')
    await http.post('/admin/craftsman/audit', { id: row.id, pass: true })
  }
  ElMessage.success('操作成功')
  load()
}

async function exportCrafts() {
  const res = await http.download('/admin/craftsman/export', { auditStatus: auditStatus.value === 'null' ? undefined : Number(auditStatus.value) })
  saveBlob(res, '匠人列表.xlsx')
}

onMounted(load)
</script>

<style scoped>
.table-card { padding: 6px 16px 16px; }
.m-cell { display: flex; align-items: center; gap: 10px; }
.m-cell img { width: 46px; height: 46px; border-radius: 8px; object-fit: cover; flex: none; }
.m-name { font-size: 13px; font-weight: 600; }
.sub { color: var(--h-text-second); font-size: 12px; }
.reject { color: #d48806; font-size: 11px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
