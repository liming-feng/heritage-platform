<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="head">
        <span class="mark">遗</span>
        <h1>非遗手工艺平台</h1>
        <p>登录后可预约体验课程、下单购买非遗好物</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="submit">登 录</el-button>
        <div class="tips">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
          <router-link to="/" class="link right">先逛逛</router-link>
        </div>
        <el-divider />
        <div class="demo">
          <div class="demo-title">演示账号（点击填入）</div>
          <div class="demo-list">
            <el-tag v-for="a in demoAccounts" :key="a.u" type="info" effect="plain" class="demo-tag" @click="fill(a)">
              {{ a.label }}：{{ a.u }}
            </el-tag>
          </div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const demoAccounts = [
  { label: '普通用户', u: 'user01' },
  { label: '匠人', u: 'zhangjian' },
  { label: '管理员', u: 'admin' }
]

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function fill(a) {
  form.username = a.u
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await http.post('/portal/auth/login', { username: form.username, password: form.password })
    userStore.setAuth(data, data.user)
    ElMessage.success('登录成功')
    // 按角色送回默认工作台
    const redirect = route.query.redirect
    if (redirect) return router.replace(String(redirect))
    if (data.user?.role === 3) return router.replace('/admin/dashboard')
    if (data.user?.role === 2) return router.replace('/craftsman/dashboard')
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  background:
    radial-gradient(1200px 500px at 20% -10%, #f5e4e3 0%, transparent 60%),
    radial-gradient(900px 420px at 90% 110%, #f3e8ce 0%, transparent 55%),
    var(--h-paper);
  padding: 24px;
}
.auth-card {
  width: 400px; background: #fff; border-radius: 14px;
  border: 1px solid var(--h-border);
  box-shadow: var(--h-shadow-hover);
  padding: 34px 36px 26px;
}
.head { text-align: center; margin-bottom: 22px; }
.mark {
  display: inline-flex; width: 46px; height: 46px; border-radius: 12px;
  background: linear-gradient(135deg, var(--h-primary), var(--h-primary-dark));
  color: #fff; font-size: 24px; font-weight: 700;
  align-items: center; justify-content: center; margin-bottom: 10px;
}
.head h1 { font-size: 20px; margin: 0 0 4px; }
.head p { color: var(--h-text-second); font-size: 13px; margin: 0; }
.btn { width: 100%; margin-top: 4px; letter-spacing: 4px; }
.tips { margin-top: 12px; font-size: 13px; color: var(--h-text-second); }
.link { color: var(--h-primary); margin-left: 4px; }
.link.right { float: right; }
.demo-title { font-size: 12px; color: var(--h-text-second); margin-bottom: 8px; }
.demo-list { display: flex; flex-wrap: wrap; gap: 8px; }
.demo-tag { cursor: pointer; }
</style>
