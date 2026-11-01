<template>
  <div class="space-y-3">
    <!-- 看板 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">药品耗材进销存看板</div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">在册品项</div>
          <div class="amount-strong">{{ stats.itemCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
          <div class="text-[12px] text-gray-500">库存总金额</div>
          <div class="amount-strong !text-[#7C3AED]">{{ fmt(stats.totalAmount) }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
          <div class="text-[12px] text-gray-500">低于预警下限</div>
          <div class="amount-strong !text-[#D97706]">{{ stats.warnCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
          <div class="text-[12px] text-gray-500">负库存（账实不符）</div>
          <div class="amount-strong !text-[#DC2626]">{{ stats.abnormalCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">近30天入库</div>
          <div class="amount-strong">{{ fmt(stats.inAmount30) }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">账实相符率</div>
          <div class="amount-strong">{{ stats.matchRate ?? 100 }}%</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="tab" class="his-card p-3" @tab-change="onTabChange">
      <!-- ============ 库存台账 ============ -->
      <el-tab-pane label="库存台账" name="items">
        <div class="query-grid-wrap">
          <div class="query-grid">
            <div class="q-item">
              <el-input v-model="iq.keyword" placeholder="编码/名称/医保编码/厂家" clearable @keyup.enter="searchItem" />
            </div>
            <div class="q-item">
              <el-select v-model="iq.category" placeholder="类别" clearable style="width:100%">
                <el-option v-for="(v, k) in STOCK_CATEGORIES" :key="k" :label="v" :value="k" />
              </el-select>
            </div>
            <div class="q-item">
              <el-select v-model="iq.status" placeholder="状态" clearable style="width:100%">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </div>
          </div>
          <div class="query-grid-ops">
            <el-button type="primary" @click="searchItem">查询</el-button>
            <el-button @click="resetItem">重置</el-button>
            <el-button type="success" size="small" @click="openItem()">新增品项</el-button>
            <el-button type="warning" size="small" :type="onlyWarn ? 'danger' : 'warning'" @click="onlyWarn = !onlyWarn">
              {{ onlyWarn ? '已筛选预警' : '只看库存预警' }}
            </el-button>
            <el-button type="danger" size="small" :type="onlyAbnormal ? 'danger' : 'danger'" @click="onlyAbnormal = !onlyAbnormal">
              {{ onlyAbnormal ? '已筛选负库存' : '只看负库存' }}
            </el-button>
          </div>
        </div>

        <div class="font-semibold text-[15px] text-[#0B3B66] mb-2 mt-3">库存主档</div>
        <el-table :data="items" size="small" v-loading="itemLoading">
          <el-table-column prop="itemCode" label="品项编码" width="110" />
          <el-table-column prop="itemName" label="品项名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="category" label="类别" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" type="info">{{ row.category }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="110" show-overflow-tooltip />
          <el-table-column prop="unit" label="单位" width="70" />
          <el-table-column prop="ybCode" label="医保编码" width="130" />
          <el-table-column label="当前库存" width="110" align="right">
            <template #default="{ row }">
              <span :class="row.abnormalFlag ? 'text-[#DC2626] font-semibold' : ''">{{ fmt(row.stockQty) }}</span>
              <el-tag v-if="row.abnormalFlag" size="small" type="danger" class="ml-1">负库存</el-tag>
              <el-tag v-else-if="row.warnFlag" size="small" type="warning" class="ml-1">预警</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="平均成本" width="110" align="right">
            <template #default="{ row }">{{ fmt(row.avgPrice) }}</template>
          </el-table-column>
          <el-table-column label="库存金额" width="120" align="right">
            <template #default="{ row }">{{ fmt(row.stockAmount) }}</template>
          </el-table-column>
          <el-table-column label="预警下限" width="100" align="right">
            <template #default="{ row }">{{ fmt(row.warnQty) }}</template>
          </el-table-column>
          <el-table-column label="近30天出库" width="120" align="right">
            <template #default="{ row }">{{ fmt(row.outQty30) }}</template>
          </el-table-column>
          <el-table-column prop="location" label="库位" width="120" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openItem(row)">编辑</el-button>
              <el-button link type="success" @click="openOp(row, 1)">入库</el-button>
              <el-button link type="warning" @click="openOp(row, 2)">出库</el-button>
              <el-button link type="info" @click="openOp(row, 5)">盘点</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end mt-3">
          <el-pagination
            :page-sizes="PAGE_SIZES" :page-size="iq.size" :current-page="iq.page" :total="itemTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="(p) => { iq.page = p; loadItem() }"
            @size-change="(s) => { iq.size = s; iq.page = 1; loadItem() }"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 进销存流水 ============ -->
      <el-tab-pane label="进销存流水" name="txns">
        <div class="query-grid-wrap">
          <div class="query-grid">
            <div class="q-item">
              <el-input v-model="tq.keyword" placeholder="流水号/品项/关联单号" clearable @keyup.enter="searchTxn" />
            </div>
            <div class="q-item">
              <el-select v-model="tq.txnType" placeholder="交易类型" clearable style="width:100%">
                <el-option v-for="(v, k) in TXN_TYPES" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
            <div class="q-item">
              <el-input v-model="tq.refNo" placeholder="关联单号（结算单号）" clearable @keyup.enter="searchTxn" />
            </div>
          </div>
          <div class="query-grid-ops">
            <el-button type="primary" @click="searchTxn">查询</el-button>
            <el-button @click="resetTxn">重置</el-button>
          </div>
        </div>
        <div class="font-semibold text-[15px] text-[#0B3B66] mb-2 mt-3">
          进销存流水（结算出库 / 退费红冲 / 采购入库 / 盘点调整）
        </div>
        <el-table :data="txns" size="small" v-loading="txnLoading">
          <el-table-column prop="txnNo" label="流水号" width="200" />
          <el-table-column label="类型" width="100" align="center">
            <template #default="{ row }"><el-tag size="small" :type="txnTypeType(row.txnType)">{{ txnTypeText(row.txnType) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="itemName" label="品项" width="150" show-overflow-tooltip />
          <el-table-column prop="itemCode" label="品项编码" width="110" />
          <el-table-column label="数量" width="100" align="right">
            <template #default="{ row }">
              <span :class="row.qty < 0 ? 'text-[#DC2626]' : 'text-[#16A34A]'">{{ fmt(row.qty) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100" align="right">
            <template #default="{ row }">{{ fmt(row.price) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">{{ fmt(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="变动前 → 后" width="140" align="right">
            <template #default="{ row }">{{ fmt(row.beforeQty) }} → {{ fmt(row.afterQty) }}</template>
          </el-table-column>
          <el-table-column prop="batchNo" label="批次号" width="120" />
          <el-table-column prop="refNo" label="关联单号" width="140" show-overflow-tooltip />
          <el-table-column prop="bizDate" label="业务日期" width="110" />
          <el-table-column prop="operator" label="操作人" width="90" />
          <el-table-column prop="warnMsg" label="异常提示" min-width="220" show-overflow-tooltip />
        </el-table>
        <div class="flex justify-end mt-3">
          <el-pagination
            :page-sizes="PAGE_SIZES" :page-size="tq.size" :current-page="tq.page" :total="txnTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="(p) => { tq.page = p; loadTxn() }"
            @size-change="(s) => { tq.size = s; tq.page = 1; loadTxn() }"
          />
        </div>
      </el-tab-pane>

      <!-- ============ 库存分析 ============ -->
      <el-tab-pane label="库存分析" name="analysis">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">近 30 天出入库金额趋势</div>
            <div ref="trendChart" class="h-[300px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">各类别库存金额与预警</div>
            <div ref="catChart" class="h-[300px]"></div>
          </div>
        </div>
        <div class="mt-3">
          <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">库存预警清单（低于预警下限 / 负库存）</div>
          <el-table :data="warnItems" size="small" v-loading="itemLoading">
            <el-table-column prop="itemCode" label="品项编码" width="110" />
            <el-table-column prop="itemName" label="品项名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="category" label="类别" width="90" />
            <el-table-column label="当前库存" width="120" align="right">
              <template #default="{ row }">
                <span :class="row.abnormalFlag ? 'text-[#DC2626] font-semibold' : 'text-[#D97706]'">{{ fmt(row.stockQty) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="预警下限" width="110" align="right">
              <template #default="{ row }">{{ fmt(row.warnQty) }}</template>
            </el-table-column>
            <el-table-column label="缺口" width="110" align="right">
              <template #default="{ row }">{{ fmt(Number(row.warnQty || 0) - Number(row.stockQty || 0)) }}</template>
            </el-table-column>
            <el-table-column prop="manufacturer" label="生产企业" min-width="200" show-overflow-tooltip />
            <el-table-column prop="location" label="库位" width="120" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 品项编辑 -->
    <el-dialog v-model="itemVisible" :title="itemRow ? '编辑库存品项' : '新增库存品项'" width="540px" append-to-body>
      <el-form ref="itemRef" :model="itemForm" :rules="itemRules" label-width="100px">
        <el-form-item label="品项编码" prop="itemCode">
          <el-input v-model="itemForm.itemCode" :disabled="!!itemRow" placeholder="如 YP0001" />
        </el-form-item>
        <el-form-item label="品项名称" prop="itemName">
          <el-input v-model="itemForm.itemName" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="itemForm.category" style="width:100%">
            <el-option v-for="(v, k) in STOCK_CATEGORIES" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="itemForm.spec" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="itemForm.unit" />
        </el-form-item>
        <el-form-item label="医保编码">
          <el-input v-model="itemForm.ybCode" />
        </el-form-item>
        <el-form-item label="生产企业">
          <el-input v-model="itemForm.manufacturer" />
        </el-form-item>
        <el-form-item label="预警下限">
          <el-input-number v-model="itemForm.warnQty" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model="itemForm.location" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="itemForm.status" style="width:100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>

    <!-- 出入库 / 盘点 -->
    <el-dialog v-model="opVisible" :title="opTitle" width="500px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="品项">
          <el-input :model-value="opForm.itemName" disabled />
        </el-form-item>
        <el-form-item label="品项编码">
          <el-input v-model="opForm.itemCode" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="opForm.opType" style="width:100%" :disabled="opLocked">
            <el-option label="采购入库" :value="1" />
            <el-option label="销售出库" :value="2" />
            <el-option label="退药入库" :value="3" />
            <el-option label="报损出库" :value="4" />
            <el-option label="盘点调整（填实盘数）" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item :label="opForm.opType === 5 ? '实盘数量' : '数量'">
          <el-input-number v-model="opForm.qty" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="opForm.opType === 1 || opForm.opType === 3" label="入库单价">
          <el-input-number v-model="opForm.price" :min="0" :precision="4" style="width:100%" />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="opForm.batchNo" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker v-model="opForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="关联单号">
          <el-input v-model="opForm.refNo" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="opForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="opVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveOp">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { stockItemListPage, stockItemUpsert, stockTxnListPage, stockOperate, stockStats, stockTrend } from '@/api/stock'
import { PAGE_SIZES } from '@/lib/pagination'
import { TXN_TYPES, STOCK_CATEGORIES, txnTypeText, txnTypeType } from './stockMeta'

const tab = ref('items')
const saving = ref(false)
const stats = ref({})
const trend = ref([])

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/* ---------------- 库存台账 ---------------- */
const items = ref([])
const itemTotal = ref(0)
const itemLoading = ref(false)
const iq = reactive({ keyword: '', category: '', status: '', page: 1, size: 10 })
const onlyWarn = ref(false)
const onlyAbnormal = ref(false)

async function loadItem() {
  itemLoading.value = true
  const res = await stockItemListPage({
    page: iq.page, size: iq.size,
    keyword: iq.keyword || undefined,
    category: iq.category || undefined,
    status: iq.status === '' ? undefined : iq.status,
    onlyWarn: onlyWarn.value ? true : undefined,
    onlyAbnormal: onlyAbnormal.value ? true : undefined,
  })
  itemLoading.value = false
  items.value = res.data.list
  itemTotal.value = res.data.total
}
function searchItem() { iq.page = 1; loadItem() }
function resetItem() {
  iq.keyword = ''; iq.category = ''; iq.status = ''; iq.page = 1
  onlyWarn.value = false; onlyAbnormal.value = false
  loadItem()
}
const warnItems = computed(() => items.value.filter((i) => i.warnFlag || i.abnormalFlag))

/* ---------------- 流水 ---------------- */
const txns = ref([])
const txnTotal = ref(0)
const txnLoading = ref(false)
const tq = reactive({ keyword: '', txnType: '', refNo: '', page: 1, size: 10 })

async function loadTxn() {
  txnLoading.value = true
  const res = await stockTxnListPage({
    page: tq.page, size: tq.size,
    keyword: tq.keyword || undefined,
    txnType: tq.txnType === '' ? undefined : tq.txnType,
    refNo: tq.refNo || undefined,
  })
  txnLoading.value = false
  txns.value = res.data.list
  txnTotal.value = res.data.total
}
function searchTxn() { tq.page = 1; loadTxn() }
function resetTxn() { tq.keyword = ''; tq.txnType = ''; tq.refNo = ''; tq.page = 1; loadTxn() }

/* ---------------- 品项编辑 ---------------- */
const itemVisible = ref(false)
const itemRow = ref(null)
const itemRef = ref()
const itemForm = reactive({ id: null, itemCode: '', itemName: '', category: '西药', spec: '', unit: '', ybCode: '', manufacturer: '', warnQty: 0, location: '', status: 1 })
const itemRules = {
  itemCode: [{ required: true, message: '请输入品项编码', trigger: 'blur' }],
  itemName: [{ required: true, message: '请输入品项名称', trigger: 'blur' }],
}
function openItem(row) {
  itemRow.value = row || null
  Object.assign(itemForm, row
    ? { id: row.id, itemCode: row.itemCode, itemName: row.itemName, category: row.category, spec: row.spec, unit: row.unit, ybCode: row.ybCode, manufacturer: row.manufacturer, warnQty: row.warnQty ?? 0, location: row.location, status: row.status }
    : { id: null, itemCode: '', itemName: '', category: '西药', spec: '', unit: '', ybCode: '', manufacturer: '', warnQty: 0, location: '', status: 1 })
  itemVisible.value = true
}
async function saveItem() {
  await itemRef.value.validate()
  saving.value = true
  try {
    const res = await stockItemUpsert({ ...itemForm })
    if (res.data.ok) { ElMessage.success('保存成功'); itemVisible.value = false; loadItem() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

/* ---------------- 出入库 / 盘点 ---------------- */
const opVisible = ref(false)
const opRow = ref(null)
const opLocked = ref(false)
const opForm = reactive({ itemCode: '', itemName: '', opType: 1, qty: 1, price: 0, batchNo: '', expireDate: '', refNo: '', remark: '' })
const opTitle = computed(() => ({
  1: '采购入库', 2: '销售出库', 3: '退药入库', 4: '报损出库', 5: '库存盘点',
}[opForm.opType] || '库存操作'))

function openOp(row, opType) {
  opRow.value = row
  opLocked.value = opType !== 5
  Object.assign(opForm, {
    itemCode: row.itemCode,
    itemName: row.itemName,
    opType,
    qty: opType === 5 ? Number(row.stockQty || 0) : 1,
    price: Number(row.avgPrice || 0),
    batchNo: '',
    expireDate: '',
    refNo: '',
    remark: '',
  })
  opVisible.value = true
}
async function saveOp() {
  if (opForm.opType === 5) {
    try {
      await ElMessageBox.confirm(
        `实盘数量 ${opForm.qty}，账面库存 ${opRow.value.stockQty}，系统将按差额生成盘点调整流水，确认？`,
        '库存盘点', { type: 'warning' })
    } catch (e) {
      return
    }
  }
  saving.value = true
  try {
    const res = await stockOperate({ ...opForm })
    if (res.data.ok) {
      ElMessage.success('操作成功')
      opVisible.value = false
      loadItem(); loadTxn(); loadStats()
    } else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

/* ---------------- 图表 ---------------- */
const trendChart = ref(null)
const catChart = ref(null)
const chartRefs = { trendChart, catChart }
const C = { blue: '#1269B5', teal: '#0E9488', purple: '#7C3AED', orange: '#D97706', red: '#DC2626', yellow: '#F59E0B', gray: '#94A3B8' }

function draw(refEl, option) {
  if (!refEl.value) return
  let c = echarts.getInstanceByDom(refEl.value)
  if (!c) c = echarts.init(refEl.value)
  c.setOption(option, true)
}
function disposeAll() {
  Object.values(chartRefs).forEach((r) => {
    if (r.value) {
      const c = echarts.getInstanceByDom(r.value)
      if (c) c.dispose()
    }
  })
}

function renderAnalysis() {
  disposeAll()
  draw(trendChart, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['入库金额', '出库金额'] },
    grid: { left: 60, right: 20, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: trend.value.map((t) => t.day.slice(5)) },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    series: [
      { name: '入库金额', type: 'bar', data: trend.value.map((t) => Number(t.inAmount || 0)), itemStyle: { color: C.teal } },
      { name: '出库金额', type: 'bar', data: trend.value.map((t) => Number(t.outAmount || 0)), itemStyle: { color: C.blue } },
    ],
  })
  // 按类别汇总库存金额与预警数
  const byCat = {}
  for (const i of items.value) {
    const k = i.category || '其他'
    if (!byCat[k]) byCat[k] = { amount: 0, warn: 0 }
    byCat[k].amount += Number(i.stockAmount || 0)
    if (i.warnFlag || i.abnormalFlag) byCat[k].warn += 1
  }
  const keys = Object.keys(byCat)
  draw(catChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['库存金额', '预警品项数'] },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: keys },
    yAxis: [{ type: 'value', name: '金额', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } }, { type: 'value', name: '品项数' }],
    series: [
      { name: '库存金额', type: 'bar', data: keys.map((k) => byCat[k].amount), itemStyle: { color: C.blue } },
      { name: '预警品项数', type: 'line', yAxisIndex: 1, data: keys.map((k) => byCat[k].warn), itemStyle: { color: C.red }, label: { show: true } },
    ],
  })
}

function onTabChange() {
  nextTick(renderAnalysis)
}

async function loadStats() {
  const res = await stockStats()
  stats.value = res.data || {}
}
async function loadTrend() {
  const res = await stockTrend(30)
  trend.value = res.data || []
}

function onResize() {
  Object.values(chartRefs).forEach((r) => {
    if (r.value) {
      const c = echarts.getInstanceByDom(r.value)
      if (c) c.resize()
    }
  })
}
onMounted(async () => {
  loadItem()
  loadTxn()
  await loadStats()
  await loadTrend()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeAll()
})
</script>
