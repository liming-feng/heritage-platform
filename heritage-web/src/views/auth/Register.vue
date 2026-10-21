<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="head">
        <span class="mark">遗</span>
        <h1>注册账号</h1>
        <p>注册即成为普通用户；如需入驻匠人，可在个人中心提交资质申请</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（3-20 位字母数字）" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="nickname">
          <el-input v-model="form.nickname" placeholder="昵称（选填）" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号（选填）" maxlength="11" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="密码（6-32 位）" :prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="确认密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" class="btn" :loading="loading" @click="submit">注 册</el-button>
        <div class="tips">
          已有账号？
          <router-link to="/login" class="link">去登录</router-link>
          <router-link to="/" class="link right">先逛逛</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', phone: '', password: '', confirm: '' })

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,20}$/, message: '3-20 位字母、数字或下划线', trigger: 'blur' }
  ],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, v, cb) => (v === form.password ? cb() : cb(new Error('两次输入的密码不一致'))),
      trigger: 'blur'
    }
  ]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await http.post('/portal/auth/register', {
      username: form.username,
      nickname: form.nickname || undefined,
      phone: form.phone || undefined,
      password: form.password
    })
    ElMessage.success('注册成功，已自动登录')
    // 注册接口若直接返回令牌则自动登录，否则跳登录页
    router.replace('/login')
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
</style>
