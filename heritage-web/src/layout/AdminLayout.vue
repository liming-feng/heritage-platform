<template>
  <el-container class="admin-shell">
    <!-- 侧边栏 -->
    <el-aside width="210px" class="aside">
      <div class="brand">
        <span class="brand-mark">遗</span>
        <div>
          <div class="brand-title">非遗管理后台</div>
          <div class="brand-sub">{{ user.nickname || '管理员' }}</div>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#26221e"
        text-color="#c8c0b4"
        active-text-color="#f5d9a8"
      >
        <el-menu-item index="/admin/dashboard"><el-icon><DataBoard /></el-icon>数据总览大屏</el-menu-item>
        <div class="menu-group">用户管理</div>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/craftsmans"><el-icon><Avatar /></el-icon>匠人资质审核</el-menu-item>
        <div class="menu-group">内容审核</div>
        <el-menu-item index="/admin/goods"><el-icon><Goods /></el-icon>商品管理</el-menu-item>
        <el-menu-item index="/admin/courses"><el-icon><Collection /></el-icon>课程管理</el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><Menu /></el-icon>分类管理</el-menu-item>
        <div class="menu-group">运营管理</div>
        <el-menu-item index="/admin/articles"><el-icon><Document /></el-icon>资讯管理</el-menu-item>
        <el-menu-item index="/admin/banners"><el-icon><Picture /></el-icon>轮播管理</el-menu-item>
        <el-menu-item index="/admin/coupons"><el-icon><Ticket /></el-icon>优惠券管理</el-menu-item>
        <el-menu-item index="/admin/feedbacks"><el-icon><ChatDotRound /></el-icon>留言反馈</el-menu-item>
        <div class="menu-group">预约监管（题眼）</div>
        <el-menu-item index="/admin/bookings"><el-icon><Calendar /></el-icon>预约监管</el-menu-item>
        <el-menu-item index="/admin/booking-config"><el-icon><Setting /></el-icon>预约规则配置</el-menu-item>
        <div class="menu-group">系统</div>
        <el-menu-item index="/admin/logs"><el-icon><List /></el-icon>操作日志</el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header" height="56px">
        <div class="crumb">{{ $route.meta?.title || '' }}</div>
        <div class="right">
          <router-link to="/" class="back-portal"><el-icon><House /></el-icon>回前台</router-link>
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28">{{ user.nickname?.charAt(0) || '管' }}</el-avatar>
              <span>{{ user.nickname || '管理员' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="__logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()

function onCommand(cmd) {
  if (cmd === '__logout') {
    user.logout().then(() => router.replace('/login'))
  }
}
</script>

<style scoped>
.admin-shell { height: 100%; }
.aside { background: #26221e; overflow-y: auto; overflow-x: hidden; }
.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 16px 12px; color: #fff;
  border-bottom: 1px solid #3a342d;
}
.brand-mark {
  width: 34px; height: 34px; border-radius: 8px; flex: none;
  background: linear-gradient(135deg, var(--h-primary), var(--h-primary-dark));
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff;
}
.brand-title { font-size: 15px; font-weight: 600; color: #f0e9de; }
.brand-sub { font-size: 12px; color: #8a857c; }
.menu { border-right: none; }
.menu-group {
  padding: 12px 20px 4px; font-size: 12px; color: #7d766a; letter-spacing: 1px;
}
.menu :deep(.el-menu-item) { height: 42px; }
.menu :deep(.el-menu-item.is-active) { background: #3a322a !important; }
.header {
  background: #fff; border-bottom: 1px solid var(--h-border);
  display: flex; align-items: center; justify-content: space-between;
}
.crumb { font-size: 16px; font-weight: 600; }
.right { display: flex; align-items: center; gap: 18px; }
.back-portal { display: flex; align-items: center; gap: 4px; font-size: 13px; color: var(--h-text-second); }
.back-portal:hover { color: var(--h-primary); }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.main { background: var(--h-paper); padding: 18px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
