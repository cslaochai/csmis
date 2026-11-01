// 药品耗材进销存 元数据（与后端口径一致）
export const TXN_TYPES = {
  1: '采购入库',
  2: '销售出库',
  3: '退药入库',
  4: '报损出库',
  5: '盘点调整',
}
export const STOCK_CATEGORIES = {
  西药: '西药',
  中成药: '中成药',
  中草药: '中草药',
  医用耗材: '医用耗材',
}

export function txnTypeText(t) { return TXN_TYPES[t] || t || '-' }
export function txnTypeType(t) {
  return { 1: 'success', 2: 'primary', 3: 'success', 4: 'danger', 5: 'warning' }[t] || 'info'
}
export function isInbound(t) {
  return t === 1 || t === 3 || t === 5
}
