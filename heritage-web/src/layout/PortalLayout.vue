<template>
  <div class="portal">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="h-container topbar-inner">
        <router-link to="/" class="logo">
          <span class="logo-mark">遗</span>
          <span class="logo-text">非遗手工艺平台</span>
        </router-link>

        <nav class="nav">
          <router-link to="/" :class="{ active: $route.path === '/' }">首页</router-link>
          <router-link to="/goods" :class="{ active: $route.path.startsWith('/goods') || $route.path.startsWith('/search') }">非遗商城</router-link>
          <router-link to="/courses" :class="{ active: $route.path.startsWith('/courses') }">体验课程</router-link>
          <router-link to="/craftsmans" :class="{ active: $route.path.startsWith('/craftsmans') }">匠人名录</router-link>
          <router-link to="/articles" :class="{ active: $route.path.startsWith('/articles') }">非遗资讯</router-link>
        </nav>

        <div class="actions">
          <el-input
            v-model="keyword"
            placeholder="搜索非遗好物"
            class="search"
            clearable
            @keyup.enter="goSearch"
          >
            <template #suffix>
              <el-icon style="cursor: pointer" @click="goSearch"><Search /></el-icon>
            </template>
          </el-input>

          <template v-if="user.isLogin">
            <el-badge :value="user.cartCount" :hidden="!user.cartCount" class="badge">
              <router-link to="/cart" class="icon-link" title="购物车">
                <el-icon :size="20"><ShoppingCart /></el-icon>
              </router-link>
            </el-badge>
            <el-badge :value="user.unreadCount" :hidden="!user.unreadCount" class="badge">
              <router-link to="/messages" class="icon-link" title="消息中心">
                <el-icon :size="20"><Bell /></el-icon>
              </router-link>
            </el-badge>
            <el-dropdown trigger="click" @command="onCommand">
              <span class="user-chip">
                <el-avatar :size="30" :src="img(user.userInfo?.avatar)">{{ user.nickname?.charAt(0) || '客' }}</el-avatar>
                <span class="uname">{{ user.nickname || '我的' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="/my/bookings">我的预约</el-dropdown-item>
                  <el-dropdown-item command="/orders">我的订单</el-dropdown-item>
                  <el-dropdown-item command="/coupons">我的优惠券</el-dropdown-item>
                  <el-dropdown-item command="/profile">个人中心</el-dropdown-item>
                  <el-dropdown-item divided command="__logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <!-- 匠人/管理员工作台入口 -->
            <router-link v-if="user.role === 2" to="/craftsman/dashboard" class="workbench">匠人工作台</router-link>
            <router-link v-if="user.role === 3" to="/admin/dashboard" class="workbench">管理后台</router-link>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <!-- 内容 -->
    <main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="h-container">
        <div class="foot-line">非遗手工艺品展示与预约体验平台 · 让指尖上的记忆被更多人看见</div>
        <div class="foot-sub">毕业设计作品 · SpringBoot + Vue · 图片与数据均为演示用途</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '@/utils/request'
import { useUserStore } from '@/store/user'
import { img, onImgErr } from '@/utils/format'

const router = useRouter()
const user = useUserStore()
const keyword = ref('')

function goSearch() {
  if (keyword.value.trim()) router.push({ path: '/search', query: { q: keyword.value.trim() } })
}

function onCommand(cmd) {
  if (cmd === '__logout') {
    user.logout().then(() => router.replace('/'))
    return
  }
  router.push(cmd)
}

/** 登录状态下刷新购物车 / 未读消息角标 */
async function refreshBadges() {
  if (!user.isLogin) return
  try {
    const [cart, unread] = await Promise.all([
      http.get('/portal/cart/count'),
      http.get('/portal/message/unread-count')
    ])
    user.setCartCount(typeof cart === 'number' ? cart : Number(cart) || 0)
    user.setUnreadCount(typeof unread === 'number' ? unread : Number(unread) || 0)
  } catch {
    /* 未登录或接口失败不阻塞页面 */
  }
}

onMounted(refreshBadges)
defineExpose({ refreshBadges })
</script>

<style scoped>
.portal { min-height: 100%; display: flex; flex-direction: column; }
.topbar {
  background: rgba(250, 247, 242, 0.96);
  border-bottom: 1px solid var(--h-border);
  position: sticky; top: 0; z-index: 100;
  backdrop-filter: blur(8px);
}
.topbar-inner { display: flex; align-items: center; gap: 28px; height: 64px; }
.logo { display: flex; align-items: center; gap: 10px; }
.logo-mark {
  width: 36px; height: 36px; border-radius: 8px;
  background: linear-gradient(135deg, var(--h-primary), var(--h-primary-dark));
  color: #fff; font-size: 20px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.logo-text { font-size: 18px; font-weight: 700; color: var(--h-ink); letter-spacing: 1px; }
.nav { display: flex; gap: 6px; }
.nav a {
  padding: 6px 14px; border-radius: 8px; font-size: 15px; color: var(--h-ink);
  transition: all 0.2s;
}
.nav a:hover { color: var(--h-primary); background: var(--h-paper-dark); }
.nav a.active { color: var(--h-primary); font-weight: 600; background: #f5e4e3; }
.actions { margin-left: auto; display: flex; align-items: center; gap: 14px; }
.search { width: 200px; }
.icon-link { color: var(--h-ink); display: flex; }
.icon-link:hover { color: var(--h-primary); }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.uname { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.workbench {
  padding: 5px 12px; border-radius: 8px; font-size: 13px;
  border: 1px solid var(--h-gold); color: #8a6d2f; background: #faf3e3;
}
.workbench:hover { background: #f3e8ce; }
.main { flex: 1; padding-bottom: 40px; }
.footer { background: var(--h-ink); color: #cfc8bd; padding: 26px 0; margin-top: auto; }
.foot-line { text-align: center; font-size: 14px; }
.foot-sub { text-align: center; font-size: 12px; color: #8a857c; margin-top: 8px; }
@media (max-width: 900px) {
  .nav, .search { display: none; }
}
</style>
