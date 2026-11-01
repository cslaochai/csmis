// ============================================================
// DRG/DIP 支付方式监管 - 接口层（context-path /mss）
// ============================================================
import request, { cleanParams } from './request'

// ---------------- 病组 / 病种池 ----------------
export function drgGroupListPage(params) {
  return request.get('/drg/group/listPage', { params: cleanParams(params) })
}
export function drgGroupUpsert(data) {
  return request.post('/drg/group/upsert', data)
}
export function drgGroupStatus(data) {
  return request.post('/drg/group/status', { id: String(data.id), status: data.status })
}

// ---------------- 病例入组与监管 ----------------
export function drgCaseListPage(params) {
  return request.get('/drg/case/listPage', { params: cleanParams(params) })
}
export function drgCaseGroup() {
  return request.post('/drg/case/group')
}
export function drgCaseHandle(data) {
  return request.post('/drg/case/handle', data)
}
export function drgStats() {
  return request.get('/drg/stats')
}
