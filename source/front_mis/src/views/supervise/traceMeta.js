// 药品追溯码监管 元数据（与后端口径一致）
export const CODE_STATUS = { 1: '未使用', 2: '已使用', 3: '已作废', 4: '已退回' }
export const VERIFY_RESULTS = {
  1: '核验通过',
  2: '追溯码不存在',
  3: '重复使用',
  4: '与结算项目不符',
  5: '已过期',
  6: '已作废',
}

export function codeStatusText(s) { return CODE_STATUS[s] || s || '-' }
export function verifyResultText(r) { return VERIFY_RESULTS[r] || r || '-' }
export function codeStatusType(s) {
  return { 1: 'info', 2: 'success', 3: 'danger', 4: 'warning' }[s] || 'info'
}
export function verifyResultType(r) {
  return { 1: 'success', 2: 'danger', 3: 'danger', 4: 'warning', 5: 'warning', 6: 'danger' }[r] || 'info'
}
