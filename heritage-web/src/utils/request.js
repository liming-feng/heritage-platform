import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/store/user'

/** 后端 context-path 已是 /api，这里 baseURL 直接用 /api，由 Vite 代理转发 */
const service = axios.create({
  baseURL: '/api',
  timeout: 20000
})

/* ------------------------------------------------------------------
 *  请求拦截：自动携带 access_token
 * ------------------------------------------------------------------ */
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.accessToken && !config.headers?.noAuth) {
      config.headers[userStore.tokenHeader] = userStore.tokenPrefix + userStore.accessToken
    }
    return config
  },
  (error) => Promise.reject(error)
)

/* ------------------------------------------------------------------
 *  响应拦截：
 *   - 业务码 200  -> 直接返回 data
 *   - 业务码 1002 -> 未登录
 *   - HTTP 401    -> access_token 过期，用 refresh_token 换发后重试
 *   - HTTP 403    -> 无权限 / refresh 过期，跳登录
 *   - blob 响应   -> 用于 Excel 导出，原样返回
 * ------------------------------------------------------------------ */
let refreshing = false
let pendingQueue = []

function flushQueue(token) {
  pendingQueue.forEach((cb) => cb(token))
  pendingQueue = []
}

service.interceptors.response.use(
  (response) => {
    // 文件下载（Excel 导出）
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 1002) {
        handleLogout()
        ElMessage.warning(res.message || '请先登录')
        return Promise.reject(new Error(res.message || '未登录'))
      }
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    return res
  },
  async (error) => {
    const { response, config } = error
    const userStore = useUserStore()

    if (!response) {
      ElMessage.error('网络异常，请检查后端服务是否已启动')
      return Promise.reject(error)
    }

    // access_token 过期 -> 静默刷新并重放原请求
    if (response.status === 401 && !config._retried) {
      if (refreshing) {
        // 正在刷新中，排队等待
        return new Promise((resolve) => {
          pendingQueue.push((newToken) => {
            config._retried = true
            config.headers[userStore.tokenHeader] = userStore.tokenPrefix + newToken
            resolve(service(config))
          })
        })
      }

      refreshing = true
      try {
        const newToken = await userStore.doRefresh()
        flushQueue(newToken)
        config._retried = true
        config.headers[userStore.tokenHeader] = userStore.tokenPrefix + newToken
        return service(config)
      } catch (e) {
        pendingQueue = []
        handleLogout()
        return Promise.reject(e)
      } finally {
        refreshing = false
      }
    }

    if (response.status === 403) {
      ElMessage.error(response.data?.message || '没有操作权限')
      return Promise.reject(error)
    }

    ElMessage.error(response.data?.message || `请求失败(${response.status})`)
    return Promise.reject(error)
  }
)

function handleLogout() {
  const userStore = useUserStore()
  userStore.clear()
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.replace({ path: '/login', query: { redirect: current.fullPath } })
  }
}

export default service

/* ------------------------------------------------------------------
 *  便捷方法
 * ------------------------------------------------------------------ */
export const http = {
  get: (url, params, config = {}) => service.get(url, { params, ...config }),
  post: (url, data, config = {}) => service.post(url, data, config),
  put: (url, data, config = {}) => service.put(url, data, config),
  delete: (url, params, config = {}) => service.delete(url, { params, ...config }),
  /** 下载文件流 */
  download: (url, params) => service.get(url, { params, responseType: 'blob' })
}

/** 触发浏览器下载 */
export function saveBlob(response, defaultName = 'export.xlsx') {
  const blob = new Blob([response.data], {
    type: response.headers['content-type'] || 'application/octet-stream'
  })
  let filename = defaultName
  const disposition = response.headers['content-disposition']
  if (disposition) {
    const match = /filename\*?=(?:UTF-8'')?"?([^;"]+)"?/i.exec(disposition)
    if (match && match[1]) {
      filename = decodeURIComponent(match[1])
    }
  }
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}
