// 信用管理元数据（与后端口径一致）
export const SUBJECT_TYPES = { 1: '定点机构', 2: '从业人员' }
export const PUNISH_TYPES = { 0: '无', 1: '守信激励', 2: '失信惩戒' }
export const CREDIT_LEVELS = { A: '优秀', B: '良好', C: '一般', D: '差' }

export function subjectTypeText(t) { return SUBJECT_TYPES[t] || t || '-' }
export function punishTypeText(t) { return PUNISH_TYPES[t] || t || '-' }
export function creditLevelText(l) { return CREDIT_LEVELS[l] || l || '-' }
export function punishTypeType(t) {
  return { 0: 'info', 1: 'success', 2: 'danger' }[t] || 'info'
}
