import { ElMessage } from 'element-plus'

/* =====================================================================
 *  通用格式化与状态映射
 *  状态数值与后端枚举一一对应，显示文案优先用后端返回的 xxxDesc 字段
 * ===================================================================== */

/** 金额：分 -> 元 不需要（库中就是元），统一保留两位 */
export function money(v) {
  if (v === null || v === undefined || v === '') return '—'
  const n = Number(v)
  if (Number.isNaN(n)) return String(v)
  return '¥' + n.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function num(v, dash = '—') {
  if (v === null || v === undefined || v === '') return dash
  const n = Number(v)
  return Number.isNaN(n) ? String(v) : n.toLocaleString()
}

export function dateTime(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 16)
}

export function date(v) {
  if (!v) return '—'
  return String(v).replace('T', ' ').slice(0, 10)
}

/** 秒 -> 「x天 HH:mm:ss」 */
export function countdown(seconds) {
  if (seconds === null || seconds === undefined || seconds <= 0) return '已截止'
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  const pad = (x) => String(x).padStart(2, '0')
  return (d > 0 ? d + '天 ' : '') + `${pad(h)}:${pad(m)}:${pad(s)}`
}

/* ------------------------- 状态映射 ------------------------- */

export const GOODS_STATUS = {
  0: { text: '已下架', type: 'info' },
  1: { text: '已上架', type: 'success' },
  2: { text: '待审核', type: 'warning' }
}

export const AUDIT_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'success' },
  2: { text: '已驳回', type: 'danger' }
}

export const COURSE_STATUS = {
  0: { text: '未开始', type: 'primary' },
  1: { text: '进行中', type: 'success' },
  2: { text: '已结束', type: 'info' }
}

export const BOOKING_STATUS = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已通过', type: 'primary' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已取消', type: 'info' },
  4: { text: '已核销', type: 'success' },
  5: { text: '已完成', type: 'success' }
}

/** 预约状态进度条节点（用于我的预约） */
export const BOOKING_STEPS = [0, 1, 4, 5]
export const BOOKING_STEP_NAMES = ['提交预约', '匠人审核', '到场核销', '体验完成']

export const ORDER_STATUS = {
  0: { text: '待付款', type: 'warning' },
  1: { text: '待发货', type: 'primary' },
  2: { text: '待收货', type: 'success' },
  3: { text: '已完成', type: 'success' },
  4: { text: '已退款', type: 'info' },
  5: { text: '已取消', type: 'info' }
}

export const ROLE_NAME = { 1: '普通用户', 2: '匠人', 3: '管理员' }

/** 图片地址兜底：后端返回 /api/uploads/... 直接可用，空值用占位 SVG */
export const PLACEHOLDER =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300"><rect width="100%" height="100%" fill="#f2ece2"/><text x="50%" y="52%" font-size="20" fill="#c9bda9" text-anchor="middle" font-family="sans-serif">非遗展示</text></svg>`
  )

export function img(v) {
  if (!v) return PLACEHOLDER
  if (String(v).startsWith('http') || String(v).startsWith('data:')) return v
  return v
}

/** 图片加载失败统一替换占位图（模板里 @error="onImgErr"） */
export function onImgErr(e) {
  if (e.target.src !== PLACEHOLDER) e.target.src = PLACEHOLDER
}

/** 富文本详情：后端存的是 LONGTEXT，兼容纯文本换行 */
export function richText(v) {
  if (!v) return ''
  if (/<[a-z][\s\S]*>/i.test(v)) return v
  return String(v)
    .split(/\n+/)
    .map((s) => `<p style="margin:0 0 10px">${s}</p>`)
    .join('')
}

/** 复制文本 */
export async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}
