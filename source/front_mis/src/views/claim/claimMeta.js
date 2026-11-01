// 手工报销元数据（与后端口径一致）
export const CLAIM_TYPES = { 1: '门诊', 2: '住院', 3: '药店' }
export const CLAIM_STATUS = { 0: '待审核', 1: '已审核', 2: '已拨付', 3: '已退回' }

export function claimTypeText(t) { return CLAIM_TYPES[t] || t || '-' }
export function claimStatusText(s) { return CLAIM_STATUS[s] || t || '-' }
export function claimStatusType(s) {
  return { 0: 'warning', 1: 'success', 2: 'primary', 3: 'danger' }[s] || 'info'
}
