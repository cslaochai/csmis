// ============================================================
// 基金运行分析与监管成效分析（统计报表深化）
// ============================================================
import request, { cleanParams } from './request'

/** 基金运行总览 */
export function fundOverview(rangeDays) {
  return request.get('/stats/fund/overview', { params: { rangeDays } })
}
/** 险种结构 */
export function fundByInsuranceType(rangeDays) {
  return request.get('/stats/fund/insurance-type', { params: { rangeDays } })
}
/** 就诊类型构成 */
export function fundByEncounterType(rangeDays) {
  return request.get('/stats/fund/encounter-type', { params: { rangeDays } })
}
/** 支付类别构成 */
export function fundByPayClass(rangeDays) {
  return request.get('/stats/fund/pay-class', { params: { rangeDays } })
}
/** 项目类别构成 */
export function fundByCategory(rangeDays) {
  return request.get('/stats/fund/category', { params: { rangeDays } })
}
/** 定点机构结算排名 */
export function fundOrgRank(rangeDays, limit = 10) {
  return request.get('/stats/fund/org-rank', { params: { rangeDays, limit } })
}
/** 基金运行日趋势 */
export function fundTrend(rangeDays) {
  return request.get('/stats/fund/trend', { params: { rangeDays } })
}
/** 月度拨付分析 */
export function fundMonthlyAlloc() {
  return request.get('/stats/fund/monthly-alloc')
}
/** 基金监管成效 */
export function fundSupervisionEffect() {
  return request.get('/stats/fund/supervision-effect')
}
