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

    <DetailDialog v-model="cmDialog" :title="`匠人资质 · ${cmInfo.realName || ''}`" :loading="cmLoading" width="760px">
      <el-descriptions :column="2" size="small" border>
        <el-descriptions-item label="姓名">{{ cmInfo.realName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="技艺门类">{{ cmInfo.craftType || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ cmInfo.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ cmInfo.idCardNo || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="师承" :span="2">{{ cmInfo.inheritance || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="d-sec">个人简介</div>
      <div class="d-text">{{ cmInfo.intro || '—' }}</div>

      <div class="d-sec">从艺履历</div>
      <div class="d-text">{{ cmInfo.resume || '—' }}</div>

      <div class="d-sec">证书材料<span class="d-sec-extra">共 {{ (cmInfo.certImages || []).length }} 张</span></div>
      <div v-if="(cmInfo.certImages || []).length" class="d-imgs">
        <img v-for="(u, i) in cmInfo.certImages" :key="i" :src="img(u)" alt="" @error="onImgErr" />
      </div>
      <div v-else class="d-sum">未上传证书图片</div>

      <div class="d-sec">已发布作品</div>
      <div v-if="(cmInfo.goodsPreview || []).length" class="d-chips">
        <el-tag v-for="(p, i) in cmInfo.goodsPreview" :key="i" size="small" effect="plain" type="info">
          {{ p.name }}<span v-if="p.statusText"> · {{ p.statusText }}</span>
        </el-tag>
      </div>
      <div v-else class="d-sum">暂无</div>

      <div class="d-sec">已发布课程</div>
      <div v-if="(cmInfo.coursePreview || []).length" class="d-chips">
        <el-tag v-for="(p, i) in cmInfo.coursePreview" :key="i" size="small" effect="plain" type="info">
          {{ p.name }}<span v-if="p.statusText"> · {{ p.statusText }}</span>
        </el-tag>
      </div>
      <div v-else class="d-sum">暂无</div>
    </DetailDialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { img, onImgErr, dateTime, AUDIT_STATUS } from '@/utils/format'
import DetailDialog from '@/components/DetailDialog.vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const auditStatus = ref('null')
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const loading = ref(false)

/* 资质材料弹窗 */
const cmDialog = ref(false)
const cmLoading = ref(false)
const cmInfo = ref({})

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
  cmInfo.value = {}
  cmDialog.value = true
  cmLoading.value = true
  try {
    cmInfo.value = await http.get(`/admin/craftsman/${row.id}/audit-detail`)
  } finally {
    cmLoading.value = false
  }
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

onMounted(() => {
  // 工作台待办「待审核匠人」带 ?auditStatus=0 跳过来（本页筛选是字符串枚举）
  const q = route.query.auditStatus
  if (q != null && q !== '' && ['0', '1', '2'].includes(String(q))) {
    auditStatus.value = String(q)
  }
  load()
})
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
