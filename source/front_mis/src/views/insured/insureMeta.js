// 参保服务 元数据（与后端口径一致）
export const TRANSFER_TYPES = { 1: '省内转移', 2: '跨省转移', 3: '转出', 4: '转入' }
export const TRANSFER_STATUS = { 1: '待受理', 2: '已受理', 3: '已转出', 4: '已转入', 5: '已完成' }
export const TXN_TYPES = {
  1: '划入', 2: '结算划扣', 3: '一次性支取', 4: '转移出', 5: '转移入', 6: '清退', 7: '退保冲回',
}
export const DISEASE_LEVELS = { 1: '门诊特殊病种', 2: '门诊慢特病' }
export const CHRONIC_STATUS = { 1: '待审核', 2: '已认定', 3: '不通过', 4: '已复审' }

export function transferTypeText(t) { return TRANSFER_TYPES[t] || t || '-' }
export function transferStatusText(s) { return TRANSFER_STATUS[s] || s || '-' }
export function txnTypeText(t) { return TXN_TYPES[t] || t || '-' }
export function diseaseLevelText(l) { return DISEASE_LEVELS[l] || l || '-' }
export function chronicStatusText(s) { return CHRONIC_STATUS[s] || s || '-' }

export function transferStatusType(s) {
  return { 1: 'info', 2: 'warning', 3: 'primary', 4: 'primary', 5: 'success' }[s] || 'info'
}
export function chronicStatusType(s) {
  return { 1: 'info', 2: 'success', 3: 'danger', 4: 'success' }[s] || 'info'
}
export function txnTypeType(t) {
  return { 1: 'success', 2: 'primary', 3: 'danger', 4: 'warning', 5: 'success', 6: 'danger', 7: 'warning' }[t] || 'info'
}
