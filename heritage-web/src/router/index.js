import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 路由表
 * meta.requiresAuth 需要登录 / meta.roles 允许的角色（1 普通用户 2 匠人 3 管理员）
 */
const routes = [
  /* ---------------- 用户端 ---------------- */
  {
    path: '/',
    component: () => import('@/layout/PortalLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/portal/Home.vue'), meta: { title: '首页' } },
      { path: 'goods', name: 'GoodsList', component: () => import('@/views/portal/GoodsList.vue'), meta: { title: '非遗商城' } },
      { path: 'goods/:id', name: 'GoodsDetail', component: () => import('@/views/portal/GoodsDetail.vue'), meta: { title: '商品详情' } },
      { path: 'search', name: 'Search', component: () => import('@/views/portal/Search.vue'), meta: { title: '搜索结果' } },
      { path: 'courses', name: 'CourseList', component: () => import('@/views/portal/CourseList.vue'), meta: { title: '体验课程' } },
      { path: 'courses/:id', name: 'CourseDetail', component: () => import('@/views/portal/CourseDetail.vue'), meta: { title: '课程详情' } },
      { path: 'craftsmans', name: 'CraftsmanList', component: () => import('@/views/portal/CraftsmanList.vue'), meta: { title: '匠人名录' } },
      { path: 'craftsmans/:id', name: 'CraftsmanDetail', component: () => import('@/views/portal/CraftsmanDetail.vue'), meta: { title: '匠人主页' } },
      { path: 'articles', name: 'ArticleList', component: () => import('@/views/portal/ArticleList.vue'), meta: { title: '非遗资讯' } },
      { path: 'articles/:id', name: 'ArticleDetail', component: () => import('@/views/portal/ArticleDetail.vue'), meta: { title: '资讯详情' } },
      { path: 'cart', name: 'Cart', component: () => import('@/views/portal/Cart.vue'), meta: { title: '购物车', requiresAuth: true } },
      { path: 'checkout', name: 'Checkout', component: () => import('@/views/portal/Checkout.vue'), meta: { title: '确认订单', requiresAuth: true } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/portal/Orders.vue'), meta: { title: '我的订单', requiresAuth: true } },
      { path: 'orders/:id', name: 'OrderDetail', component: () => import('@/views/portal/OrderDetail.vue'), meta: { title: '订单详情', requiresAuth: true } },
      { path: 'my/bookings', name: 'MyBookings', component: () => import('@/views/portal/MyBookings.vue'), meta: { title: '我的预约', requiresAuth: true } },
      { path: 'history', name: 'History', component: () => import('@/views/portal/History.vue'), meta: { title: '浏览历史', requiresAuth: true } },
      { path: 'coupons', name: 'Coupons', component: () => import('@/views/portal/Coupons.vue'), meta: { title: '优惠券', requiresAuth: true } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/portal/Messages.vue'), meta: { title: '消息中心', requiresAuth: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/portal/Profile.vue'), meta: { title: '个人中心', requiresAuth: true } }
    ]
  },

  /* ---------------- 匠人端 ---------------- */
  {
    path: '/craftsman',
    component: () => import('@/layout/CraftsmanLayout.vue'),
    meta: { requiresAuth: true, roles: [2, 3] },
    children: [
      { path: 'dashboard', name: 'CraftsmanDashboard', component: () => import('@/views/craftsman/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'goods', name: 'CraftsmanGoods', component: () => import('@/views/craftsman/Goods.vue'), meta: { title: '我的商品' } },
      { path: 'goods/edit/:id?', name: 'CraftsmanGoodsEdit', component: () => import('@/views/craftsman/GoodsEdit.vue'), meta: { title: '商品编辑', requiresAuth: true, roles: [2] } },
      { path: 'courses', name: 'CraftsmanCourses', component: () => import('@/views/craftsman/Courses.vue'), meta: { title: '我的课程' } },
      { path: 'courses/edit/:id?', name: 'CraftsmanCourseEdit', component: () => import('@/views/craftsman/CourseEdit.vue'), meta: { title: '课程编辑', requiresAuth: true, roles: [2] } },
      { path: 'bookings', name: 'CraftsmanBookings', component: () => import('@/views/craftsman/Bookings.vue'), meta: { title: '预约管理' } },
      { path: 'checkin', name: 'CraftsmanCheckin', component: () => import('@/views/craftsman/Checkin.vue'), meta: { title: '核销签到台' } },
      { path: 'reviews', name: 'CraftsmanReviews', component: () => import('@/views/craftsman/Reviews.vue'), meta: { title: '评价管理' } },
      { path: 'orders', name: 'CraftsmanOrders', component: () => import('@/views/craftsman/Orders.vue'), meta: { title: '订单管理' } }
    ]
  },

  /* ---------------- 管理端 ---------------- */
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    meta: { requiresAuth: true, roles: [3] },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据总览大屏' } },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/Users.vue'), meta: { title: '用户管理' } },
      { path: 'craftsmans', name: 'AdminCraftsmans', component: () => import('@/views/admin/Craftsmans.vue'), meta: { title: '匠人资质审核' } },
      { path: 'goods', name: 'AdminGoods', component: () => import('@/views/admin/Goods.vue'), meta: { title: '商品管理' } },
      { path: 'courses', name: 'AdminCourses', component: () => import('@/views/admin/Courses.vue'), meta: { title: '课程管理' } },
      { path: 'categories', name: 'AdminCategories', component: () => import('@/views/admin/Categories.vue'), meta: { title: '分类管理' } },
      { path: 'articles', name: 'AdminArticles', component: () => import('@/views/admin/Articles.vue'), meta: { title: '资讯管理' } },
      { path: 'banners', name: 'AdminBanners', component: () => import('@/views/admin/Banners.vue'), meta: { title: '轮播管理' } },
      { path: 'coupons', name: 'AdminCoupons', component: () => import('@/views/admin/Coupons.vue'), meta: { title: '优惠券管理' } },
      { path: 'feedbacks', name: 'AdminFeedbacks', component: () => import('@/views/admin/Feedbacks.vue'), meta: { title: '留言反馈' } },
      { path: 'bookings', name: 'AdminBookings', component: () => import('@/views/admin/Bookings.vue'), meta: { title: '预约监管' } },
      { path: 'booking-config', name: 'AdminBookingConfig', component: () => import('@/views/admin/BookingConfig.vue'), meta: { title: '预约规则配置' } },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/Logs.vue'), meta: { title: '操作日志' } }
    ]
  },

  /* ---------------- 登录 / 错误页 ---------------- */
  { path: '/login', name: 'Login', component: () => import('@/views/auth/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/auth/Register.vue'), meta: { title: '注册' } },
  { path: '/403', name: 'Forbidden', component: () => import('@/views/error/403.vue'), meta: { title: '无权限' } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('@/views/error/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

/* ------------------------------------------------------------------
 *  路由守卫：登录校验 + 角色隔离
 *  - 未登录访问需登录页 -> /login?redirect=原地址
 *  - 已登录但角色不符   -> /403（普通用户输 /admin/** 也进不来）
 * ------------------------------------------------------------------ */
router.beforeEach((to) => {
  document.title = to.meta?.title
    ? `${to.meta.title} · 非遗手工艺平台`
    : '非遗手工艺品展示与预约体验平台'

  const userStore = useUserStore()

  if (to.meta?.requiresAuth && !userStore.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (to.meta?.roles && to.meta.roles.length > 0) {
    if (!to.meta.roles.includes(userStore.role)) {
      return { path: '/403' }
    }
  }
  return true
})

export default router
