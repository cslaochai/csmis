// 大数据反欺诈元数据（与后端口径一致）
export const FRAUD_CATEGORIES = {
  '01': '分解住院',
  '02': '虚假就医/冒名',
  '03': '串换药品',
  '04': '过度医疗',
  '05': '空刷医保卡',
  '06': '其他',
}
export const RISK_LEVELS = { 1: '低', 2: '中', 3: '高' }
export const CLUE_STATUS = { 1: '待核查', 2: '已确认', 3: '已排除', 4: '已移送' }

export function categoryText(c) { return FRAUD_CATEGORIES[c] || c || '-' }
export function riskText(r) { return RISK_LEVELS[r] || r || '-' }
export function clueStatusText(s) { return CLUE_STATUS[s] || s || '-' }
export function riskType(r) { return { 1: 'info', 2: 'warning', 3: 'danger' }[r] || 'info' }
export function clueStatusType(s) { return { 1: 'warning', 2: 'danger', 3: 'info', 4: 'success' }[s] || 'info' }
