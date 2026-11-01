// 知识库类型（与后端 kb_type 口径一致）
export const KB_TYPES = {
  clinical_guideline: '临床指南',
  medical_knowledge: '医学知识',
  drug_knowledge: '药品知识',
  policy: '政策依据',
}

export function kbTypeText(t) {
  return KB_TYPES[t] || t || '-'
}
