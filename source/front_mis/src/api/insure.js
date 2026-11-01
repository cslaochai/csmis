// ============================================================
// 参保服务（医保关系转移接续 / 个人账户管理 / 门诊慢特病认定）
// ============================================================
import request, { cleanParams } from './request'

// ---------------- 医保关系转移接续 ----------------
export function transferListPage(params) {
  return request.get('/insure/transfer/listPage', { params: cleanParams(params) })
}
export function transferUpsert(data) {
  return request.post('/insure/transfer/upsert', data)
}
export function transferHandle(data) {
  return request.post('/insure/transfer/handle', data)
}

// ---------------- 个人账户管理 ----------------
export function accountTxnListPage(params) {
  return request.get('/insure/account/listPage', { params: cleanParams(params) })
}
export function accountInfo(idCard) {
  return request.get('/insure/account/info', { params: { idCard } })
}
export function accountOperate(data) {
  return request.post('/insure/account/operate', data)
}

// ---------------- 门诊慢特病待遇认定 ----------------
export function chronicListPage(params) {
  return request.get('/insure/chronic/listPage', { params: cleanParams(params) })
}
export function chronicUpsert(data) {
  return request.post('/insure/chronic/upsert', data)
}
export function chronicAudit(data) {
  return request.post('/insure/chronic/audit', data)
}
