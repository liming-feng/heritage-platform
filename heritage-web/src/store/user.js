import { defineStore } from 'pinia'
import axios from 'axios'

const LS_KEY = 'heritage_auth'

/**
 * 用户状态：双 Token（access + refresh）、用户信息、购物车数量、未读消息数
 */
export const useUserStore = defineStore('user', {
  state: () => {
    const saved = JSON.parse(localStorage.getItem(LS_KEY) || '{}')
    return {
      accessToken: saved.accessToken || '',
      refreshToken: saved.refreshToken || '',
      tokenHeader: 'Authorization',
      tokenPrefix: 'Bearer ',
      userInfo: saved.userInfo || null,
      cartCount: 0,
      unreadCount: 0
    }
  },

  getters: {
    isLogin: (state) => !!state.accessToken,
    role: (state) => state.userInfo?.role ?? 0,
    isAdmin: (state) => state.userInfo?.role === 3,
    isCraftsman: (state) => state.userInfo?.role === 2,
    isNormalUser: (state) => state.userInfo?.role === 1,
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || ''
  },

  actions: {
    /** 登录成功后写入 */
    setAuth(data, userInfo) {
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      if (userInfo) this.userInfo = userInfo
      this.persist()
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo
      this.persist()
    },

    persist() {
      localStorage.setItem(
        LS_KEY,
        JSON.stringify({
          accessToken: this.accessToken,
          refreshToken: this.refreshToken,
          userInfo: this.userInfo
        })
      )
    },

    setCartCount(n) {
      this.cartCount = n || 0
    },

    setUnreadCount(n) {
      this.unreadCount = n || 0
    },

    /** 用 refresh_token 换发 access_token；独立 axios 实例避免拦截器递归 */
    async doRefresh() {
      if (!this.refreshToken) {
        throw new Error('没有 refresh_token')
      }
      // 后端 @RequestBody RefreshTokenDTO，refresh_token 必须放请求体
      const res = await axios.post('/api/portal/auth/refresh', { refreshToken: this.refreshToken })
      const body = res.data
      if (!body || body.code !== 200 || !body.data?.accessToken) {
        throw new Error(body?.message || '刷新令牌失败')
      }
      this.accessToken = body.data.accessToken
      if (body.data.refreshToken) {
        this.refreshToken = body.data.refreshToken
      }
      this.persist()
      return this.accessToken
    },

    /** 退出：调用后端加入黑名单，然后清空本地 */
    async logout() {
      try {
        if (this.accessToken) {
          await axios.post('/api/portal/auth/logout', null, {
            headers: { Authorization: this.tokenPrefix + this.accessToken }
          })
        }
      } catch (e) {
        // 忽略退出接口异常，本地状态照常清理
      }
      this.clear()
    },

    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      this.cartCount = 0
      this.unreadCount = 0
      localStorage.removeItem(LS_KEY)
    }
  }
})
