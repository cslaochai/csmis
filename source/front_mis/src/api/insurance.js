// ============================================================
// 医保结算经办平台 - 接口层（真实后端 mss-back-end，context-path /mss）
// 统一返回 { code:200, message, data }，分页 data 为 { list, total }
// ============================================================
import request, { cleanParams } from './request'

const idBody = (id) => ({ id: String(id) })

// ---------------- 经办工作台 ----------------
export function dashboardSummary() {
  return request.get('/workbench/summary')
}

// ---------------- 统计报表 ----------------
export function statsData() {
  return request.get('/workbench/stats')
}

// ---------------- 结算单 ----------------
export function listBills(params) {
  return request.get('/bill/listPage', { params: cleanParams(params) })
}
export function getBillDetail(id) {
  return request.get('/bill/getDetailById', { params: { id } })
}
export function reverseBill(id) {
  return request.post('/bill/reverse', idBody(id))
}

// ---------------- 机构 2304 报盘接收台账（前置网关/对方账） ----------------
export function listGatewaySettlements(params) {
  return request.get('/admin/settlement/listPage', { params: cleanParams(params) })
}
export function getGatewaySettlement(id) {
  return request.get('/admin/settlement/getById', { params: { id } })
}

// ---------------- 门诊即时结算 ----------------
export function listPendingOutpatient(params) {
  return request.get('/settle/outpatient/listPage', { params: cleanParams(params) })
}
export function previewOutpatient(bizNo) {
  return request.post('/settle/outpatient/preview', { bizNo })
}
export function settleOutpatient(bizNo) {
  return request.post('/settle/outpatient/confirm', { bizNo })
}

// ---------------- 住院即时结算 ----------------
export function listPendingInpatient(params) {
  return request.get('/settle/inpatient/listPage', { params: cleanParams(params) })
}
export function previewInpatient(bizNo) {
  return request.post('/settle/inpatient/preview', { bizNo })
}
export function settleInpatient(bizNo) {
  return request.post('/settle/inpatient/confirm', { bizNo })
}

// ---------------- 三大目录 ----------------
export function listCatalog(params) {
  return request.get('/catalog/listPage', { params: cleanParams(params) })
}
export function catalogStats() {
  return request.get('/catalog/stats')
}

// ---------------- 院内项目 ↔ 医保目录对照 ----------------
export function listMappings(params) {
  return request.get('/mapping/listPage', { params: cleanParams(params) })
}
export function mapItem(data) {
  return request.post('/mapping/map', data)
}
export function unmapItem(id) {
  return request.post('/mapping/unmap', idBody(id))
}
export function autoMatch() {
  return request.post('/mapping/autoMatch')
}

// ---------------- 异地就医备案 ----------------
export function listFilings(params) {
  return request.get('/filing/listPage', { params: cleanParams(params) })
}
export function upsertFiling(data) {
  return request.post('/filing/upsert', data)
}
export function revokeFiling(id) {
  return request.post('/filing/revoke', idBody(id))
}

// ---------------- 清算对账 ----------------
export function listReconciliation(params) {
  return request.get('/recon/listPage', { params: cleanParams(params) })
}
export function doReconcile(period) {
  return request.post('/recon/doReconcile', { period })
}

// ---------------- 费用合规性审核 ----------------
export function feeAuditList(params) {
  return request.get('/audit/listPage', { params: cleanParams(params) })
}
export function auditPass(id) {
  return request.post('/audit/pass', idBody(id))
}
export function auditReject(id) {
  return request.post('/audit/reject', idBody(id))
}
export function auditAppeal(data) {
  return request.post('/audit/appeal', data)
}
export function auditReview(data) {
  return request.post('/audit/review', data)
}
export function auditConfer(data) {
  return request.post('/audit/confer', data)
}

// ---------------- 月度结算管理（申报→审核→拨付→年终清算） ----------------
export function monthlyPage(params) {
  return request.get('/monthly/listPage', { params: cleanParams(params) })
}
export function monthlyResult(month) {
  return request.get('/monthly/result', { params: { month } })
}
export function monthlyDeclare(month) {
  return request.post('/monthly/declare', { month })
}
export function monthlyAudit(month) {
  return request.post('/monthly/audit', { month })
}
export function monthlyAllocate(month) {
  return request.post('/monthly/allocate', { month })
}
export function monthlyYearSettle(month) {
  return request.post('/monthly/yearSettle', { month })
}

// ---------------- 日拨付（按笔）管理 ----------------
export function dailyListPage(params) {
  return request.get('/settle/daily/listPage', { params: cleanParams(params) })
}
export function dailyGenerate(bizDate) {
  return request.post('/settle/daily/generate', { bizDate })
}
export function dailyRemit(id) {
  return request.post('/settle/daily/remit', idBody(id))
}

// ---------------- 参保人管理 ----------------
export function personListPage(params) {
  return request.get('/insure/person/listPage', { params: cleanParams(params) })
}
export function personSave(data) {
  return request.post('/insure/person/save', data)
}
/** 参保状态变更（停保 / 恢复） */
export function personStatusChange(data) {
  return request.post('/insure/person/status', data)
}

// ---------------- 定点协议管理 ----------------
export function agreementListPage(params) {
  return request.get('/agreement/listPage', { params: cleanParams(params) })
}
export function agreementUpsert(data) {
  return request.post('/agreement/upsert', data)
}
export function agreementStatus(data) {
  return request.post('/agreement/status', data)
}
export function agreementAssess(id, score) {
  return request.post('/agreement/assess', { id: String(id), score })
}

// ---------------- 手工（零星）报销 ----------------
export function claimListPage(params) {
  return request.get('/claim/listPage', { params: cleanParams(params) })
}
export function claimSubmit(data) {
  return request.post('/claim/submit', data)
}
export function claimAudit(data) {
  return request.post('/claim/audit', data)
}
export function claimPay(id) {
  return request.post('/claim/pay', idBody(id))
}
