// 举报投诉与奖励 元数据（与后端口径一致）
export const REPORT_STATUS = { 1: '待受理', 2: '核查中', 3: '已办结', 4: '已奖励发放' }
export const PROBLEM_TYPES = { '违规收费': '违规收费', '欺诈骗保': '欺诈骗保', '服务态度': '服务态度', '其他': '其他' }

export function reportStatusText(s) { return REPORT_STATUS[s] || s || '-' }
export function reportStatusType(s) { return { 1: 'info', 2: 'warning', 3: 'success', 4: 'danger' }[s] || 'info' }
