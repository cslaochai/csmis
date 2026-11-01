// 定点协议元数据（与后端口径一致）
export const MEDINS_TYPES = { 1: '定点医院', 2: '定点药店' }
export const AGREEMENT_STATUS = { 0: '申请中', 1: '已签订', 2: '已解除' }

export function medinsTypeText(t) { return MEDINS_TYPES[t] || t || '-' }
export function agreementStatusText(s) { return AGREEMENT_STATUS[s] || t || '-' }
export function agreementStatusType(s) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[s] || 'info'
}
