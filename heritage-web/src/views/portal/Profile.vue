<template>
  <div class="h-container page">
    <div class="h-page-header"><h2>个人中心</h2></div>

    <el-tabs v-model="tab">
      <el-tab-pane label="基本资料" name="profile" />
      <el-tab-pane label="收货地址" name="address" />
      <el-tab-pane label="我的收藏" name="favorite" />
      <el-tab-pane label="我的反馈" name="feedback" />
      <el-tab-pane label="修改密码" name="password" />
    </el-tabs>

    <!-- 基本资料 -->
    <div class="h-card block" v-if="tab === 'profile'">
      <el-form :model="profile" label-width="80px" style="max-width: 480px">
        <el-form-item label="头像">
          <el-avatar :size="64" :src="img(profile.avatar)">{{ user.nickname?.charAt(0) }}</el-avatar>
        </el-form-item>
        <el-form-item label="用户名"><el-input :model-value="profile.username" disabled /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="profile.nickname" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="profile.phone" maxlength="11" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profile.email" /></el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="profile.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">保密</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveProfile">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 收货地址 -->
    <div class="h-card block" v-if="tab === 'address'">
      <div class="block-head">
        <span>共 {{ addresses.length }} 个地址</span>
        <el-button type="primary" size="small" @click="openAddr()">新增地址</el-button>
      </div>
      <div class="addr-list">
        <div v-for="a in addresses" :key="a.id" :class="['addr', { default: a.isDefault === 1 }]">
          <div class="a-line1">
            <b>{{ a.receiver }}</b> {{ a.phone }}
            <el-tag v-if="a.isDefault === 1" size="small" type="warning" effect="plain">默认</el-tag>
          </div>
          <div class="a-line2">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detail }}</div>
          <div class="a-ops">
            <el-button text size="small" @click="openAddr(a)">编辑</el-button>
            <el-button v-if="a.isDefault !== 1" text size="small" @click="setDefault(a)">设为默认</el-button>
            <el-button text size="small" type="danger" @click="delAddr(a)">删除</el-button>
          </div>
        </div>
      </div>
      <el-empty v-if="!addresses.length" description="还没有收货地址" :image-size="70" />
    </div>

    <!-- 我的收藏 -->
    <div class="h-card block" v-if="tab === 'favorite'">
      <el-radio-group v-model="favType" @change="loadFav">
        <el-radio-button :value="1">非遗好物</el-radio-button>
        <el-radio-button :value="2">体验课程</el-radio-button>
        <el-radio-button :value="3">资讯</el-radio-button>
      </el-radio-group>
      <div class="fav-list">
        <div v-for="f in favs" :key="f.id" class="fav" @click="goFav(f)">
          <img :src="img(f.cover)" @error="onImgErr" />
          <div class="f-info">
            <div class="f-title h-ellipsis-1">{{ f.title }}</div>
            <div class="f-sub h-ellipsis-1">{{ f.subtitle }}</div>
            <div class="f-meta">
              <span class="h-price" v-if="f.price">¥{{ f.price }}</span>
              <el-tag v-if="f.targetAvailable === false" size="small" type="info">已下架/已结束</el-tag>
            </div>
          </div>
          <el-button text size="small" type="danger" @click.stop="unfav(f)">取消收藏</el-button>
        </div>
      </div>
      <el-empty v-if="!favs.length" description="暂无收藏" :image-size="70" />
    </div>

    <!-- 我的反馈 -->
    <div class="h-card block" v-if="tab === 'feedback'">
      <div class="block-head">
        <span>意见与建议</span>
        <el-button type="primary" size="small" @click="fbDialog = true">提交反馈</el-button>
      </div>
      <div v-for="f in feedbacks" :key="f.id" class="fb">
        <div class="fb-head">
          <el-tag size="small" effect="plain">{{ f.typeDesc }}</el-tag>
          <el-tag size="small" :type="f.status === 2 ? 'success' : 'warning'">{{ f.statusDesc }}</el-tag>
          <span class="time">{{ dateTime(f.createTime) }}</span>
        </div>
        <div class="fb-content">{{ f.content }}</div>
        <div class="fb-reply" v-if="f.reply">平台回复：{{ f.reply }}</div>
      </div>
      <el-empty v-if="!feedbacks.length" description="暂无反馈记录" :image-size="70" />
    </div>

    <!-- 修改密码 -->
    <div class="h-card block" v-if="tab === 'password'">
      <el-form :model="pwd" label-width="90px" style="max-width: 420px">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password placeholder="6-32 位" /></el-form-item>
        <el-form-item label="确认新密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="changePwd">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 地址编辑弹窗 -->
    <el-dialog v-model="addrDialog" :title="addrForm.id ? '编辑地址' : '新增地址'" width="460px">
      <el-form :model="addrForm" label-width="80px">
        <el-form-item label="收货人" required><el-input v-model="addrForm.receiver" /></el-form-item>
        <el-form-item label="手机号" required><el-input v-model="addrForm.phone" maxlength="11" /></el-form-item>
        <el-form-item label="省份" required><el-input v-model="addrForm.province" /></el-form-item>
        <el-form-item label="城市" required><el-input v-model="addrForm.city" /></el-form-item>
        <el-form-item label="区县"><el-input v-model="addrForm.district" /></el-form-item>
        <el-form-item label="详细地址" required><el-input v-model="addrForm.detail" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="addrForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addrDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAddr">保存</el-button>
      </template>
    </el-dialog>

    <!-- 反馈弹窗 -->
    <el-dialog v-model="fbDialog" title="提交反馈" width="460px">
      <el-form label-width="70px">
        <el-form-item label="类型">
          <el-select v-model="fbForm.type" style="width: 100%">
            <el-option label="功能建议" :value="1" />
            <el-option label="问题反馈" :value="2" />
            <el-option label="其他" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="fbForm.content" type="textarea" :rows="4" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="联系方式"><el-input v-model="fbForm.contact" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fbDialog = false">取消</el-button>
        <el-button type="primary" @click="submitFb">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, dateTime } from '@/utils/format'

const router = useRouter()
const user = useUserStore()

const tab = ref('profile')
const profile = reactive({ username: '', nickname: '', phone: '', email: '', gender: 0, avatar: '' })
const addresses = ref([])
const addrDialog = ref(false)
const addrForm = reactive({ id: null, receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })

const favType = ref(1)
const favs = ref([])
const feedbacks = ref([])
const fbDialog = ref(false)
const fbForm = reactive({ type: 1, content: '', contact: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function loadProfile() {
  Object.assign(profile, await http.get('/portal/user/info'))
  user.setUserInfo({ ...user.userInfo, ...profile })
}

async function saveProfile() {
  await http.put('/portal/user/info', {
    nickname: profile.nickname,
    phone: profile.phone || undefined,
    email: profile.email || undefined,
    gender: profile.gender
  })
  ElMessage.success('资料已保存')
  user.setUserInfo({ ...user.userInfo, ...profile })
}

async function loadAddr() {
  addresses.value = (await http.get('/portal/address/list')) || []
}

function openAddr(a) {
  if (a) Object.assign(addrForm, a)
  else Object.assign(addrForm, { id: null, receiver: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: 0 })
  addrDialog.value = true
}

async function saveAddr() {
  if (!addrForm.receiver || !addrForm.phone || !addrForm.detail) return ElMessage.warning('请填写完整')
  await http.post('/portal/address/save', { ...addrForm })
  ElMessage.success('已保存')
  addrDialog.value = false
  loadAddr()
}

async function setDefault(a) {
  await http.put(`/portal/address/default/${a.id}`)
  loadAddr()
}

async function delAddr(a) {
  await ElMessageBox.confirm('删除该地址？', '提示', { type: 'warning' })
  await http.delete(`/portal/address/${a.id}`)
  loadAddr()
}

async function loadFav() {
  const data = await http.get('/portal/favorite/mine', { targetType: favType.value, pageNum: 1, pageSize: 50 })
  favs.value = data.records || []
}

function goFav(f) {
  if (f.targetType === 1) router.push(`/goods/${f.targetId}`)
  else if (f.targetType === 2) router.push(`/courses/${f.targetId}`)
  else router.push(`/articles/${f.targetId}`)
}

async function unfav(f) {
  await http.post('/portal/favorite/remove', null, { params: { targetType: f.targetType, targetId: f.targetId } })
  ElMessage.success('已取消收藏')
  loadFav()
}

async function loadFb() {
  feedbacks.value = (await http.get('/portal/feedback/mine')) || []
}

async function submitFb() {
  if (!fbForm.content.trim()) return ElMessage.warning('请填写反馈内容')
  await http.post('/portal/feedback/submit', { ...fbForm })
  ElMessage.success('已提交，感谢你的反馈')
  fbDialog.value = false
  fbForm.content = ''
  loadFb()
}

async function changePwd() {
  if (!pwd.oldPassword || pwd.newPassword.length < 6) return ElMessage.warning('请填写原密码，新密码至少 6 位')
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await http.put('/portal/user/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  await user.logout()
  router.replace('/login')
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadAddr(), loadFav(), loadFb()])
})
</script>

<style scoped>
.page { padding-top: 22px; }
.block { margin-top: 4px; padding: 24px 28px; }
.block-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; color: var(--h-text-second); }
.addr-list { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.addr { border: 1px solid var(--h-border); border-radius: 10px; padding: 14px 16px; }
.addr.default { border-color: var(--h-gold); background: #fdfaf1; }
.a-line1 { display: flex; align-items: center; gap: 8px; }
.a-line2 { color: var(--h-text-second); font-size: 13px; margin-top: 4px; }
.a-ops { margin-top: 8px; display: flex; gap: 4px; }
.fav-list { display: flex; flex-direction: column; gap: 10px; margin-top: 14px; }
.fav { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--h-border); border-radius: 10px; cursor: pointer; }
.fav:hover .f-title { color: var(--h-primary); }
.fav img { width: 76px; height: 54px; object-fit: cover; border-radius: 8px; flex: none; }
.f-info { flex: 1; min-width: 0; }
.f-title { font-size: 13px; font-weight: 600; }
.f-sub { color: var(--h-text-second); font-size: 12px; margin-top: 2px; }
.f-meta { display: flex; gap: 10px; align-items: center; font-size: 12px; margin-top: 3px; }
.fb { border-bottom: 1px dashed var(--h-border); padding: 12px 0; }
.fb:last-of-type { border-bottom: none; }
.fb-head { display: flex; gap: 10px; align-items: center; }
.time { color: var(--h-text-second); font-size: 12px; margin-left: auto; }
.fb-content { margin-top: 6px; font-size: 13px; }
.fb-reply { margin-top: 6px; background: var(--h-paper-dark); border-radius: 8px; padding: 8px 12px; font-size: 12px; color: #6b6459; }
@media (max-width: 800px) { .addr-list { grid-template-columns: 1fr; } }
</style>
