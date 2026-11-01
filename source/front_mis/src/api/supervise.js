// ============================================================
// 医保基金监管 - 接口层（/supervise/*，需登录态 token）
// ============================================================
import request, { cleanParams } from './request'

const idBody = (id) => ({ id: String(id) })

// ---------------- 监管工作台 ----------------
export function superviseStats() {
  return request.get('/supervise/dashboard/stats')
}

// ---------------- 规则配置 ----------------
export function listRules(params) {
  return request.get('/supervise/rule/listPage', { params: cleanParams(params) })
}
export function upsertRule(data) {
  return request.post('/supervise/rule/upsert', data)
}
export function changeRuleStatus(id, status) {
  return request.post('/supervise/rule/status', { id: String(id), status })
}

// ---------------- 疑点稽核 ----------------
export function listSuspicions(params) {
  return request.get('/supervise/suspicion/listPage', { params: cleanParams(params) })
}
export function getSuspicion(id) {
  return request.get('/supervise/suspicion/getDetailById', { params: { id } })
}
export function runAudit() {
  return request.post('/supervise/suspicion/run')
}
export function confirmSuspicion(data) {
  return request.post('/supervise/suspicion/confirm', data)
}
export function excludeSuspicion(data) {
  return request.post('/supervise/suspicion/exclude', data)
}

// ---------------- 飞检任务 ----------------
export function listInspections(params) {
  return request.get('/supervise/inspection/listPage', { params: cleanParams(params) })
}
export function upsertInspection(data) {
  return request.post('/supervise/inspection/upsert', data)
}
export function startInspection(id) {
  return request.post('/supervise/inspection/start', idBody(id))
}
export function finishInspection(data) {
  return request.post('/supervise/inspection/finish', data)
}
export function getInspection(id) {
  return request.get('/supervise/inspection/getDetailById', { params: { id } })
}
export function addInspectionItem(data) {
  return request.post('/supervise/inspection/item/add', data)
}
export function deleteInspectionItem(id) {
  return request.post('/supervise/inspection/item/delete', idBody(id))
}
export function inspectionToRecover(id) {
  return request.post('/supervise/inspection/toRecover', idBody(id))
}

// ---------------- 知识库（两库之知识库） ----------------
export function listKnowledge(params) {
  return request.get('/supervise/knowledge/listPage', { params: cleanParams(params) })
}
export function upsertKnowledge(data) {
  return request.post('/supervise/knowledge/upsert', data)
}
export function changeKnowledgeStatus(id, status) {
  return request.post('/supervise/knowledge/status', { id: String(id), status })
}

// ---------------- 信用管理（记分/评价/联合奖惩） ----------------
export function listCredits(params) {
  return request.get('/supervise/credit/listPage', { params: cleanParams(params) })
}
export function upsertCredit(data) {
  return request.post('/supervise/credit/upsert', data)
}
export function recordCredit(data) {
  return request.post('/supervise/credit/record', data)
}

// ---------------- 违规追回 ----------------
export function listRecovers(params) {
  return request.get('/supervise/recover/listPage', { params: cleanParams(params) })
}
export function getRecover(id) {
  return request.get('/supervise/recover/getDetailById', { params: { id } })
}
export function doRecover(data) {
  return request.post('/supervise/recover/recover', data)
}
export function writeOffRecover(data) {
  return request.post('/supervise/recover/writeOff', data)
}

// ---------------- 大数据反欺诈（模型库 + 线索） ----------------
export function fraudModelListPage(params) {
  return request.get('/supervise/fraud/model/listPage', { params: cleanParams(params) })
}
export function fraudModelUpsert(data) {
  return request.post('/supervise/fraud/model/upsert', data)
}
export function fraudModelStatus(data) {
  return request.post('/supervise/fraud/model/status', data)
}
export function fraudClueListPage(params) {
  return request.get('/supervise/fraud/clue/listPage', { params: cleanParams(params) })
}
export function fraudClueRun(data) {
  return request.post('/supervise/fraud/clue/run', data || {})
}
export function fraudClueHandle(data) {
  return request.post('/supervise/fraud/clue/handle', data)
}

// ---------------- 事前提醒 / 事中审核拦截 ----------------
export function precheckRuleListPage(params) {
  return request.get('/supervise/precheck/rule/listPage', { params: cleanParams(params) })
}
export function precheckRuleUpsert(data) {
  return request.post('/supervise/precheck/rule/upsert', data)
}
export function precheckRuleStatus(data) {
  return request.post('/supervise/precheck/rule/status', data)
}
export function precheckResultListPage(params) {
  return request.get('/supervise/precheck/result/listPage', { params: cleanParams(params) })
}
export function precheckEvaluate() {
  return request.post('/supervise/precheck/result/evaluate')
}
/** 结算前事前提醒（按业务单号实时比对） */
export function precheckCheck(bizNo) {
  return request.post('/supervise/precheck/result/check', { bizNo })
}
export function precheckResultHandle(data) {
  return request.post('/supervise/precheck/result/handle', data)
}

// ---------------- 举报投诉与奖励 ----------------
export function reportListPage(params) {
  return request.get('/supervise/report/listPage', { params: cleanParams(params) })
}
export function reportSubmit(data) {
  return request.post('/supervise/report/submit', data)
}
export function reportProcess(data) {
  return request.post('/supervise/report/process', data)
}
export function reportReward(data) {
  return request.post('/supervise/report/reward', data)
}

// ---------------- 双随机一公开 ----------------
export function randomTaskListPage(params) {
  return request.get('/supervise/random/task/listPage', { params: cleanParams(params) })
}
export function randomTaskUpsert(data) {
  return request.post('/supervise/random/task/upsert', data)
}
export function randomTaskDraw(taskId) {
  return request.post('/supervise/random/task/draw', { taskId: String(taskId) })
}
export function randomTaskPublish(taskId) {
  return request.post('/supervise/random/task/publish', { taskId: String(taskId) })
}
export function randomResultListPage(params) {
  return request.get('/supervise/random/result/listPage', { params: cleanParams(params) })
}
export function randomResultHandle(data) {
  return request.post('/supervise/random/result/handle', data)
}

// ---------------- 机构自查自纠 ----------------
export function selfCheckListPage(params) {
  return request.get('/supervise/selfcheck/listPage', { params: cleanParams(params) })
}
export function selfCheckUpsert(data) {
  return request.post('/supervise/selfcheck/upsert', data)
}
export function selfCheckStart(id) {
  return request.post('/supervise/selfcheck/start', { id: String(id) })
}
export function selfCheckSubmit(data) {
  return request.post('/supervise/selfcheck/submit', data)
}
export function selfCheckReview(data) {
  return request.post('/supervise/selfcheck/review', data)
}

// ---------------- 结算清单质控 ----------------
export function qualityRuleListPage(params) {
  return request.get('/supervise/quality/rule/listPage', { params: cleanParams(params) })
}
export function qualityRuleUpsert(data) {
  return request.post('/supervise/quality/rule/upsert', data)
}
export function qualityRuleStatus(data) {
  return request.post('/supervise/quality/rule/status', { id: String(data.id), status: data.status })
}
export function qualityResultListPage(params) {
  return request.get('/supervise/quality/result/listPage', { params: cleanParams(params) })
}
export function qualityResultCheck() {
  return request.post('/supervise/quality/result/check')
}
export function qualityResultHandle(data) {
  return request.post('/supervise/quality/result/handle', data)
}
export function qualityStats() {
  return request.get('/supervise/quality/stats')
}

// ---------------- 药品追溯码监管 ----------------
export function traceCodeListPage(params) {
  return request.get('/supervise/trace/code/listPage', { params: cleanParams(params) })
}
export function traceCodeRegister(data) {
  return request.post('/supervise/trace/code/register', data)
}
export function traceVerify(data) {
  return request.post('/supervise/trace/verify', data)
}
export function traceVerifyBill(billNo) {
  return request.post('/supervise/trace/verifyBill', { billNo })
}
export function traceLogListPage(params) {
  return request.get('/supervise/trace/log/listPage', { params: cleanParams(params) })
}
export function traceStats() {
  return request.get('/supervise/trace/stats')
}
