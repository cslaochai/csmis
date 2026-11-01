// DRG/DIP 支付监管 元数据（与后端口径一致）
export const PAY_TYPES = { 1: 'DRG 按病组付费', 2: 'DIP 按病种分值付费' }
export const CASE_STATUS = {
  1: '正常',
  2: '偏差预警',
  3: '费用超支',
  4: '特例单议',
}

export function payTypeText(t) { return PAY_TYPES[t] || t || '-' }
export function caseStatusText(s) { return CASE_STATUS[s] || s || '-' }
export function caseStatusType(s) {
  return { 1: 'success', 2: 'warning', 3: 'danger', 4: 'info' }[s] || 'info'
}
export function devType(d) {
  const n = Number(d ?? 0)
  if (n > 0.5) return 'danger'
  if (n > 0.2) return 'warning'
  return 'success'
}
