<template>
  <el-container class="craftsman-shell">
    <el-aside width="200px" class="aside">
      <div class="brand">
        <span class="brand-mark">匠</span>
        <div>
          <div class="brand-title">匠人工作台</div>
          <div class="brand-sub">{{ user.nickname || '匠人' }}</div>
        </div>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#f7f3ec"
        text-color="#4c463d"
        active-text-color="#9c2c2c"
      >
        <el-menu-item index="/craftsman/dashboard"><el-icon><DataLine /></el-icon>数据看板</el-menu-item>
        <el-menu-item index="/craftsman/goods"><el-icon><Goods /></el-icon>我的商品</el-menu-item>
        <el-menu-item index="/craftsman/courses"><el-icon><Collection /></el-icon>我的课程</el-menu-item>
        <el-menu-item index="/craftsman/bookings"><el-icon><Calendar /></el-icon>预约管理</el-menu-item>
        <el-menu-item index="/craftsman/checkin"><el-icon><CircleCheck /></el-icon>核销签到台</el-menu-item>
        <el-menu-item index="/craftsman/reviews"><el-icon><ChatLineSquare /></el-icon>评价管理</el-menu-item>
        <el-menu-item index="/craftsman/orders"><el-icon><List /></el-icon>订单管理</el-menu-item>
      </el-menu>
      <div class="aside-foot">
        <router-link to="/"><el-icon><House /></el-icon>回前台</router-link>
        <a @click="logout"><el-icon><SwitchButton /></el-icon>退出</a>
      </div>
    </el-aside>

    <el-main class="main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const user = useUserStore()

function logout() {
  user.logout().then(() => router.replace('/login'))
}
</script>

<style scoped>
.craftsman-shell { height: 100%; }
.aside {
  background: #f7f3ec; border-right: 1px solid var(--h-border);
  display: flex; flex-direction: column;
}
.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 16px; border-bottom: 1px solid var(--h-border);
}
.brand-mark {
  width: 34px; height: 34px; border-radius: 8px; flex: none;
  background: linear-gradient(135deg, var(--h-gold), #a8843c);
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff;
}
.brand-title { font-size: 15px; font-weight: 700; }
.brand-sub { font-size: 12px; color: var(--h-text-second); }
.menu { border-right: none; flex: 1; background: transparent; }
.menu :deep(.el-menu-item) { height: 44px; margin: 2px 8px; border-radius: 8px; }
.menu :deep(.el-menu-item.is-active) { background: #f5e4e3; font-weight: 600; }
.aside-foot {
  padding: 12px 16px; border-top: 1px solid var(--h-border);
  display: flex; gap: 18px; font-size: 13px; color: var(--h-text-second);
}
.aside-foot a { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.aside-foot a:hover { color: var(--h-primary); }
.main { background: var(--h-paper); padding: 18px; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
