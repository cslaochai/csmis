// ============================================================
// 药品耗材进销存
// ============================================================
import request, { cleanParams } from './request'

// ---------------- 库存主档 ----------------
export function stockItemListPage(params) {
  return request.get('/stock/item/listPage', { params: cleanParams(params) })
}
export function stockItemUpsert(data) {
  return request.post('/stock/item/upsert', data)
}
export function stockItemStatus(data) {
  return request.post('/stock/item/status', { id: String(data.id), status: data.status })
}

// ---------------- 进销存流水 ----------------
export function stockTxnListPage(params) {
  return request.get('/stock/txn/listPage', { params: cleanParams(params) })
}
/** 出入库 / 盘点调整 */
export function stockOperate(data) {
  return request.post('/stock/txn/operate', data)
}

// ---------------- 统计 ----------------
export function stockStats() {
  return request.get('/stock/stats')
}
export function stockTrend(rangeDays = 30) {
  return request.get('/stock/trend', { params: { rangeDays } })
}
