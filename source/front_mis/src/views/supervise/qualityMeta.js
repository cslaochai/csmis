// 结算清单质控 元数据（与后端口径一致）
export const RULE_TYPES = {
  required_field: '必填项缺失',
  not_mapped: '目录未对照',
  over_limit_price: '超限价',
  invalid_qty: '数量异常',
  pay_class_missing: '支付类别缺失',
  duplicate_report: '重复上报',
  amount_mismatch: '金额勾稽不一致',
  catalog_disabled: '目录已停用',
  self_ratio_missing: '自付比例缺失',
  gender_limit: '性别限制',
}
export const SEVERITIES = { 1: '提示', 2: '警示', 3: '严重' }
export const HANDLE_STATUS = { 0: '待处理', 1: '已确认', 2: '已整改', 3: '已忽略' }

export function ruleTypeText(t) { return RULE_TYPES[t] || t || '-' }
export function severityText(s) { return SEVERITIES[s] || s || '-' }
export function handleStatusText(s) { return HANDLE_STATUS[s] || s || '-' }
export function severityType(s) {
  return { 1: 'info', 2: 'warning', 3: 'danger' }[s] || 'info'
}
export function handleStatusType(s) {
  return { 0: 'warning', 1: 'danger', 2: 'success', 3: 'info' }[s] || 'info'
}
