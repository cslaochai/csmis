// 双随机一公开 / 自查自纠 元数据（与后端口径一致）
export const CHECK_TYPES = { 1: '日常检查', 2: '专项检查', 3: '监督抽查' }
export const TARGET_TYPES = { 1: '被检查机构', 2: '检查人员' }
export const CHECK_RESULTS = { 1: '待检查', 2: '正常', 3: '存在问题', 4: '已整改' }
export const TASK_STATUS = { 1: '已发布', 2: '执行中', 3: '已完成' }
export const SELF_STATUS = { 1: '待自查', 2: '自查中', 3: '已上报', 4: '整改中', 5: '已办结' }

export function checkTypeText(t) { return CHECK_TYPES[t] || t || '-' }
export function targetTypeText(t) { return TARGET_TYPES[t] || t || '-' }
export function checkResultText(r) { return CHECK_RESULTS[r] || r || '-' }
export function taskStatusText(s) { return TASK_STATUS[s] || s || '-' }
export function selfStatusText(s) { return SELF_STATUS[s] || s || '-' }

export function checkResultType(r) {
  return { 1: 'info', 2: 'success', 3: 'danger', 4: 'warning' }[r] || 'info'
}
export function taskStatusType(s) {
  return { 1: 'info', 2: 'warning', 3: 'success' }[s] || 'info'
}
export function selfStatusType(s) {
  return { 1: 'info', 2: 'warning', 3: 'primary', 4: 'warning', 5: 'success' }[s] || 'info'
}
export function publicType(p) {
  return p === 1 ? 'success' : 'info'
}
export function publicText(p) {
  return p === 1 ? '已公开' : '未公开'
}
