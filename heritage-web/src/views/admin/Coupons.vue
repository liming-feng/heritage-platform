<template>
  <div class="page">
    <div class="h-page-header">
      <h2>优惠券管理</h2>
      <div class="head-right">
        <el-button size="small" @click="exportCoupons">导出券种</el-button>
        <el-button size="small" @click="exportReceive">导出领取记录</el-button>
        <el-button type="primary" size="small" @click="openDialog()">新建券种</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="reload">
      <el-tab-pane label="券种列表" name="list" />
      <el-tab-pane label="领取记录" name="receive" />
    </el-tabs>

    <!-- 券种 -->
    <template v-if="tab === 'list'">
      <div class="h-card table-card">
        <el-table :data="list" v-loading="loading">
          <el-table-column prop="name" label="券名" min-width="150" />
          <el-table-column label="面额/折扣" width="110">
            <template #default="{ row }">{{ row.type === 1 ? `¥${row.amount}` : `${row.discount} 折` }}</template>
          </el-table-column>
          <el-table-column label="门槛" width="90"><template #default="{ row }">{{ row.threshold > 0 ? `满${row.threshold}` : '无门槛' }}</template></el-table-column>
          <el-table-column prop="scopeDesc" label="适用" width="90" />
          <el-table-column label="发放/剩余" width="110">
            <template #default="{ row }">{{ row.totalCount > 0 ? `${row.receivedCount}/${row.totalCount}` : `已领 ${row.receivedCount}` }}<div class="sub" v-if="row.totalCount > 0">剩 {{ row.remainCount }}</div></template>
          </el-table-column>
          <el-table-column label="领取/使用率" width="110">
            <template #default="{ row }">
              {{ pct(row.receiveRate) }} / {{ pct(row.useRate) }}
            </template>
          </el-table-column>
          <el-table-column prop="effectiveDesc" label="有效期" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.statusDesc }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button text size="small" @click="openDialog(row)">编辑</el-button>
              <el-button text size="small" @click="stat(row)">使用统计</el-button>
              <el-button text size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="Number(total)" layout="total, prev, pager, next" background @current-change="load" />
        </div>
      </div>
    </template>

    <!-- 领取记录 -->
    <template v-else>
      <div class="h-card table-card">
        <el-table :data="receiveList" v-loading="loading">
          <el-table-column prop="name" label="券名" min-width="140" />
          <el-table-column label="用户" min-width="120"><template #default="{ row }">{{ row.userId }}</template></el-table-column>
          <el-table-column prop="statusDesc" label="使用状态" width="100" />
          <el-table-column prop="orderNo" label="订单号" width="170" />
          <el-table-column prop="createTime" label="领取时间" width="150"><template #default="{ row }">{{ dateTime(row.createTime) }}</template></el-table-column>
          <el-table-column prop="expireTime" label="过期时间" width="150"><template #default="{ row }">{{ dateTime(row.expireTime) }}</template></el-table-column>
        </el-table>
        <div class="pager">
          <el-pagination v-model:current-page="rPageNum" :page-size="pageSize" :total="Number(rTotal)" layout="total, prev, pager, next" background @current-change="loadReceive" />
        </div>
      </div>
    </template>

    <el-dialog v-model="dialog" :title="form.id ? '编辑券种' : '新建券种'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="券名" required><el-input v-model="form.name" maxlength="30" placeholder="如：新人满100减20" /></el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type === 1" label="减免金额" required>
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="折扣" required>
          <el-input-number v-model="form.discount" :min="0.01" :max="0.99" :precision="2" />
          <span class="hint">0.85 = 85 折</span>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" />
          <span class="hint">0 表示无门槛</span>
        </el-form-item>
        <el-form-item label="适用范围">
          <el-select v-model="form.scope" style="width: 100%">
            <el-option label="仅商品" :value="1" />
            <el-option label="仅课程" :value="2" />
            <el-option label="全场通用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.totalCount" :min="0" :step="50" />
          <span class="hint">0 = 不限量；已发放后只能往上调</span>
        </el-form-item>
        <el-form-item label="每人限领"><el-input-number v-model="form.perLimit" :min="1" :max="10" /></el-form-item>
        <el-form-item label="有效期" required>
          <el-date-picker v-model="range" type="datetimerange" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 使用统计：三个指标 + 按券类型的拆分表 -->
    <DetailDialog v-model="statDialog" :title="`「${statRow.name || ''}」使用统计`" width="620px">
      <div class="d-stats" style="grid-template-columns: repeat(3, minmax(0, 1fr))">
        <div class="d-stat"><b>{{ statData.receivedTotal ?? 0 }}</b><span>已领取</span></div>
        <div class="d-stat"><b>{{ statData.usedTotal ?? 0 }}</b><span>已使用</span></div>
        <div class="d-stat hl"><b>{{ pct(statData.useRate, '0%') }}</b><span>核销率</span></div>
      </div>

      <div class="d-sec">按类型拆分</div>
      <el-table :data="statData.typeStats || []" size="small">
        <el-table-column prop="typeDesc" label="类型" min-width="130" />
        <el-table-column prop="receivedCount" label="已领" width="90" align="center" />
        <el-table-column prop="usedCount" label="已用" width="90" align="center" />
        <el-table-column label="使用率" width="110" align="right">
          <template #default="{ row }">{{ pct(row.useRate, '0%') }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(statData.typeStats || []).length" :image-size="56" description="暂无领取记录" />
    </DetailDialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http, saveBlob } from '@/utils/request'
import { dateTime, pct } from '@/utils/format'
import DetailDialog from '@/components/DetailDialog.vue'

const tab = ref('list')
const list = ref([])
const receiveList = ref([])
const total = ref(0)
const rTotal = ref(0)
const pageNum = ref(1)
const rPageNum = ref(1)
const pageSize = 10
const loading = ref(false)
const dialog = ref(false)

/* 使用统计弹窗 */
const statDialog = ref(false)
const statLoading = ref(false)
const statRow = ref({})
const statData = ref({})
const range = ref([])
const form = reactive({ id: null, name: '', type: 1, amount: null, discount: null, threshold: 0, scope: 1, totalCount: 0, perLimit: 1, startTime: '', endTime: '', description: '', status: 1 })

async function load() {
  loading.value = true
  try {
    const data = await http.get('/admin/coupon/list', { pageNum: pageNum.value, pageSize })
    list.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadReceive() {
  loading.value = true
  try {
    const data = await http.get('/admin/coupon/receive-list', { pageNum: rPageNum.value, pageSize })
    receiveList.value = data.records || []
    rTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function reload() {
  tab.value === 'list' ? load() : loadReceive()
}

function openDialog(row) {
  if (row) {
    Object.assign(form, row)
    range.value = [row.startTime, row.endTime]
  } else {
    Object.assign(form, { id: null, name: '', type: 1, amount: null, discount: null, threshold: 0, scope: 1, totalCount: 0, perLimit: 1, startTime: '', endTime: '', description: '', status: 1 })
    range.value = []
  }
  dialog.value = true
}

async function save() {
  if (!form.name.trim()) return ElMessage.warning('请填写券名')
  if (!range.value || range.value.length !== 2) return ElMessage.warning('请选择有效期')
  if (form.type === 1 && !form.amount) return ElMessage.warning('请填写减免金额')
  if (form.type === 2 && !form.discount) return ElMessage.warning('请填写折扣')
  await http.post('/admin/coupon/save', {
    ...form,
    startTime: range.value[0],
    endTime: range.value[1]
  })
  ElMessage.success('已保存')
  dialog.value = false
  load()
}

async function toggle(row) {
  await http.post('/admin/coupon/status', null, { params: { id: row.id, status: row.status === 1 ? 0 : 1 } })
  ElMessage.success('操作成功')
  load()
}

async function stat(row) {
  statRow.value = row || {}
  statData.value = {}
  statDialog.value = true
  statLoading.value = true
  try {
    statData.value = await http.get('/admin/coupon/stat', { couponId: row.id })
  } finally {
    statLoading.value = false
  }
}

async function exportCoupons() {
  const res = await http.download('/admin/coupon/export')
  saveBlob(res, '券种列表.xlsx')
}

async function exportReceive() {
  const res = await http.download('/admin/coupon/receive-export')
  saveBlob(res, '领取记录.xlsx')
}

onMounted(load)
</script>

<style scoped>
.head-right { display: flex; gap: 10px; }
.table-card { padding: 6px 16px 16px; }
.sub { color: var(--h-text-second); font-size: 11px; }
.hint { margin-left: 8px; color: var(--h-text-second); font-size: 12px; }
.pager { display: flex; justify-content: flex-end; margin-top: 12px; }
</style>
