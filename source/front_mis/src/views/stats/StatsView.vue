<template>
  <div class="space-y-3">
    <!-- 顶部：区间 + 页签 -->
    <div class="his-card p-3">
      <div class="flex items-center justify-between">
        <el-radio-group v-model="rangeDays" size="small" @change="reload">
          <el-radio-button :value="7">近 7 日</el-radio-button>
          <el-radio-button :value="30">近 30 日</el-radio-button>
          <el-radio-button :value="90">近 90 日</el-radio-button>
        </el-radio-group>
        <div class="text-[12px] text-gray-400">
          统计区间 {{ ov.startDate || '-' }} ~ {{ ov.endDate || '-' }} · 口径：基金统筹收入 = 统筹支付 + 个人账户支付
        </div>
      </div>
    </div>

    <!-- ============ 基金运行总览 ============ -->
    <el-tabs v-model="tab" class="his-card p-3" @tab-change="onTabChange">
      <el-tab-pane label="基金运行总览" name="overview">
        <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">总医疗费用</div>
            <div class="amount-strong">{{ fmt(ov.totalCost) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
            <div class="text-[12px] text-gray-500">基金统筹收入</div>
            <div class="amount-strong !text-[#7C3AED]">{{ fmt(ov.fundIncome) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
            <div class="text-[12px] text-gray-500">基金支出（已拨付）</div>
            <div class="amount-strong !text-[#DC2626]">{{ fmt(ov.fundExpense) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
            <div class="text-[12px] text-gray-500">基金结余</div>
            <div class="amount-strong !text-[#D97706]">{{ fmt(ov.fundBalance) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">基金率</div>
            <div class="amount-strong">{{ ov.fundRate ?? 0 }}%</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">基金支付占比</div>
            <div class="amount-strong">{{ ov.payRatio ?? 0 }}%</div>
          </div>
        </div>

        <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">结算笔数</div>
            <div class="amount-strong">{{ ov.settleCount ?? 0 }}</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">次均医疗费用</div>
            <div class="amount-strong">{{ fmt(ov.avgCost) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">人均医疗费用</div>
            <div class="amount-strong">{{ fmt(ov.perCapitaCost) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">人均基金支出</div>
            <div class="amount-strong">{{ fmt(ov.perCapitaExpense) }}</div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">基金收支与参保结构</div>
            <div ref="fundChart" class="h-[300px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">费用支付分拆（统筹 / 个账 / 现金）</div>
            <div ref="paySplitChart" class="h-[300px]"></div>
          </div>
        </div>

        <div class="mt-4">
          <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">月度拨付分析</div>
          <el-table :data="monthly" size="small" v-loading="loading">
            <el-table-column prop="month" label="账期" width="110" />
            <el-table-column label="申报笔数" width="100" align="right">
              <template #default="{ row }">{{ row.declareCount ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="申报金额" align="right" min-width="130">
              <template #default="{ row }">{{ fmt(row.declareAmount) }}</template>
            </el-table-column>
            <el-table-column label="扣减金额" align="right" min-width="130">
              <template #default="{ row }">{{ fmt(row.deductAmount) }}</template>
            </el-table-column>
            <el-table-column label="扣减率" width="100" align="right">
              <template #default="{ row }">{{ row.deductRate ?? 0 }}%</template>
            </el-table-column>
            <el-table-column label="拨付金额" align="right" min-width="130">
              <template #default="{ row }">{{ fmt(row.allocatedAmount) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="monthStatusType(row.status)">{{ monthStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- ============ 结构分析 ============ -->
      <el-tab-pane label="结构分析" name="structure">
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">险种结构（按医疗费用）</div>
            <div ref="insChart" class="h-[300px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">就诊类型构成</div>
            <div ref="encChart" class="h-[300px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">支付类别构成（甲/乙/丙）</div>
            <div ref="payClassChart" class="h-[300px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">项目类别构成</div>
            <div ref="catChart" class="h-[300px]"></div>
          </div>
        </div>
        <div class="mt-3">
          <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">各科室结算金额</div>
          <div ref="deptChart" class="h-[300px]"></div>
        </div>
      </el-tab-pane>

      <!-- ============ 机构排名 ============ -->
      <el-tab-pane label="机构排名" name="org">
        <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">定点机构结算与违规扣减排名</div>
        <div ref="orgChart" class="h-[340px]"></div>
        <el-table :data="orgs" size="small" v-loading="loading" class="mt-3">
          <el-table-column prop="rank" label="排名" width="70" align="center" />
          <el-table-column prop="medinsName" label="定点机构" min-width="200" show-overflow-tooltip />
          <el-table-column prop="medinsCode" label="机构编码" width="140" />
          <el-table-column label="结算笔数" width="100" align="right">
            <template #default="{ row }">{{ row.settleCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column label="医疗费用" align="right" min-width="130">
            <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="统筹支付" align="right" min-width="130">
            <template #default="{ row }">{{ fmt(row.insurancePay) }}</template>
          </el-table-column>
          <el-table-column label="次均费用" align="right" width="120">
            <template #default="{ row }">{{ fmt(row.avgAmount) }}</template>
          </el-table-column>
          <el-table-column label="违规扣减" align="right" width="120">
            <template #default="{ row }">
              <span :class="row.deductAmount > 0 ? 'text-[#DC2626]' : ''">{{ fmt(row.deductAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="扣减笔数" width="90" align="right">
            <template #default="{ row }">{{ row.deductCount ?? 0 }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- ============ 趋势对比 ============ -->
      <el-tab-pane label="趋势对比" name="trend">
        <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">基金运行日趋势（医疗费用 / 统筹支付 / 个人账户 / 现金 / 日拨付）</div>
        <div ref="trendChart" class="h-[360px]"></div>
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">结算笔数趋势</div>
            <div ref="countChart" class="h-[280px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">各险种结算金额</div>
            <div ref="insBarChart" class="h-[280px]"></div>
          </div>
        </div>
      </el-tab-pane>

      <!-- ============ 监管成效 ============ -->
      <el-tab-pane label="监管成效" name="supervision">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">审核通过率</div>
            <div class="amount-strong">{{ eff.auditPassRate ?? 0 }}%</div>
            <div class="text-[11px] text-gray-400 mt-1">共 {{ eff.auditTotal ?? 0 }} 条审核任务</div>
          </div>
          <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
            <div class="text-[12px] text-gray-500">疑点确认率</div>
            <div class="amount-strong !text-[#D97706]">
              {{ suspicionRate }}%
            </div>
            <div class="text-[11px] text-gray-400 mt-1">疑点 {{ eff.suspicionTotal ?? 0 }} 条</div>
          </div>
          <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
            <div class="text-[12px] text-gray-500">违规追回金额</div>
            <div class="amount-strong !text-[#DC2626]">{{ fmt(eff.recoverAmount) }}</div>
            <div class="text-[11px] text-gray-400 mt-1">追回单 {{ eff.recoverTotal ?? 0 }} 笔</div>
          </div>
          <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
            <div class="text-[12px] text-gray-500">自查自纠办结率</div>
            <div class="amount-strong !text-[#7C3AED]">{{ eff.selfCheckDoneRate ?? 0 }}%</div>
            <div class="text-[11px] text-gray-400 mt-1">任务 {{ eff.selfCheckTotal ?? 0 }} 项</div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">监管各环节产出</div>
            <div ref="effectChart" class="h-[320px]"></div>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">各环节待办分布</div>
            <div ref="pendingChart" class="h-[320px]"></div>
          </div>
        </div>

        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4 mt-4">
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">智能审核流转</div>
            <el-table :data="auditRows" size="small">
              <el-table-column prop="name" label="环节" />
              <el-table-column prop="value" label="数量" width="100" align="right" />
            </el-table>
          </div>
          <div>
            <div class="font-semibold text-[15px] mb-2 text-[#0B3B66]">反欺诈线索处置</div>
            <el-table :data="clueRows" size="small">
              <el-table-column prop="name" label="环节" />
              <el-table-column prop="value" label="数量" width="100" align="right" />
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { statsData } from '@/api/insurance'
import {
  fundOverview, fundByInsuranceType, fundByEncounterType, fundByPayClass, fundByCategory,
  fundOrgRank, fundTrend, fundMonthlyAlloc, fundSupervisionEffect,
} from '@/api/stats'

const tab = ref('overview')
const rangeDays = ref(30)
const loading = ref(false)

const ov = ref({})
const ins = ref([])
const enc = ref([])
const payClass = ref([])
const cat = ref([])
const orgs = ref([])
const trend = ref([])
const monthly = ref([])
const eff = ref({})

const fundChart = ref(null)
const paySplitChart = ref(null)
const insChart = ref(null)
const encChart = ref(null)
const payClassChart = ref(null)
const catChart = ref(null)
const deptChart = ref(null)
const orgChart = ref(null)
const trendChart = ref(null)
const countChart = ref(null)
const insBarChart = ref(null)
const effectChart = ref(null)
const pendingChart = ref(null)

const chartRefs = {
  fundChart, paySplitChart, insChart, encChart, payClassChart, catChart,
  deptChart, orgChart, trendChart, countChart, insBarChart, effectChart, pendingChart,
}
const deptData = ref({})
const C = {
  blue: '#1269B5', teal: '#0E9488', purple: '#7C3AED', orange: '#D97706',
  red: '#DC2626', green: '#16A34A', gray: '#94A3B8', yellow: '#F59E0B',
}
const PALETTE = [C.blue, C.teal, C.purple, C.orange, C.red, C.green, C.yellow, C.gray]

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function monthStatusText(s) {
  return { 1: '待申报', 2: '已审核', 3: '已拨付', 4: '已结算' }[s] || '待申报'
}
function monthStatusType(s) {
  return { 1: 'info', 2: 'warning', 3: 'primary', 4: 'success' }[s] || 'info'
}
const suspicionRate = computed(() => {
  const t = eff.value.suspicionTotal || 0
  if (!t) return '0.0'
  return (((eff.value.suspicionConfirmed || 0) / t) * 100).toFixed(1)
})

/** 取得/复用图表实例
 *  注意：Element Plus 的 el-tab-pane 未激活时容器宽度为 0，此时 echarts.init 会得到 0 尺寸画布，
 *  因此切页时统一销毁重建（见 disposeAll / renderCurrent）。 */
function chartOf(refEl) {
  if (!refEl.value) return null
  let c = echarts.getInstanceByDom(refEl.value)
  if (!c) {
    c = echarts.init(refEl.value)
  }
  return c
}
function draw(refEl, option) {
  const c = chartOf(refEl)
  if (c) c.setOption(option, true)
}
/** 销毁全部图表实例，避免隐藏页签留下 0 尺寸画布 */
function disposeAll() {
  Object.values(chartRefs).forEach((refEl) => {
    if (refEl.value) {
      const c = echarts.getInstanceByDom(refEl.value)
      if (c) c.dispose()
    }
  })
}

async function reload() {
  loading.value = true
  const [o, i, e, p, c, org, t, m, s, dept] = await Promise.all([
    fundOverview(rangeDays.value),
    fundByInsuranceType(rangeDays.value),
    fundByEncounterType(rangeDays.value),
    fundByPayClass(rangeDays.value),
    fundByCategory(rangeDays.value),
    fundOrgRank(rangeDays.value, 10),
    fundTrend(rangeDays.value),
    fundMonthlyAlloc(),
    fundSupervisionEffect(),
    statsData(),
  ])
  ov.value = o.data || {}
  ins.value = i.data || []
  enc.value = e.data || []
  payClass.value = p.data || []
  cat.value = c.data || []
  orgs.value = org.data || []
  trend.value = t.data || []
  monthly.value = m.data || []
  eff.value = s.data || {}
  deptData.value = dept.data?.byDept || {}
  loading.value = false
  await nextTick()
  renderCurrent()
}

/** 只渲染当前页签的图表（其余页签容器此时宽度为 0，渲染无意义） */
function renderCurrent() {
  disposeAll()
  switch (tab.value) {
    case 'overview':
      renderFund()
      renderPaySplit()
      break
    case 'structure':
      renderStructure()
      renderDept()
      break
    case 'org':
      renderOrg()
      break
    case 'trend':
      renderTrend()
      break
    case 'supervision':
      renderEffect()
      break
    default:
      break
  }
}

/** 切换页签后重新渲染（容器此时才有实际宽度） */
function onTabChange() {
  nextTick(renderCurrent)
}

function renderFund() {
  draw(fundChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['金额'] },
    grid: { left: 70, right: 30, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: ['基金统筹收入', '基金支出', '基金结余'] },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    series: [{
      type: 'bar',
      data: [ov.value.fundIncome, ov.value.fundExpense, ov.value.fundBalance],
      itemStyle: { color: (p) => [C.purple, C.red, C.teal][p.dataIndex] },
      label: { show: true, position: 'top', formatter: (p) => fmt(p.value) },
    }],
  })
}

function renderPaySplit() {
  draw(paySplitChart, {
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${fmt(p.value)} 元（${p.percent}%）` },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '66%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}\n{d}%' },
      data: [
        { name: '统筹基金支付', value: ov.value.insurancePay || 0, itemStyle: { color: C.purple } },
        { name: '个人账户支付', value: ov.value.personalAccount || 0, itemStyle: { color: C.teal } },
        { name: '个人现金支付', value: ov.value.cashPay || 0, itemStyle: { color: C.orange } },
        { name: '其他自付', value: Math.max(0, (ov.value.selfPay || 0) - (ov.value.cashPay || 0)), itemStyle: { color: C.gray } },
      ],
    }],
  })
}

function pieOf(refEl, rows, title) {
  draw(refEl, {
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${fmt(p.value)} 元（${p.percent}%）` },
    legend: { bottom: 0 },
    series: [{
      name: title,
      type: 'pie',
      radius: '58%',
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{d}%' },
      data: rows.map((r, i) => ({
        name: r.name || '未分类',
        value: Number(r.amount || 0),
        itemStyle: { color: PALETTE[i % PALETTE.length] },
      })),
    }],
  })
}

function renderStructure() {
  pieOf(insChart, ins.value, '险种')
  pieOf(encChart, enc.value, '就诊类型')
  pieOf(payClassChart, payClass.value, '支付类别')
  draw(catChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    yAxis: { type: 'category', data: cat.value.map((r) => r.name || '未分类'), inverse: true },
    series: [{
      type: 'bar',
      data: cat.value.map((r) => Number(r.amount || 0)),
      itemStyle: { color: C.teal },
      label: { show: true, position: 'right', formatter: (p) => (p.value / 1000).toFixed(1) + 'k' },
    }],
  })
}

function renderDept() {
  const byDept = deptData.value || {}
  const keys = Object.keys(byDept)
  draw(deptChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => (v / 1000).toFixed(1) + 'k' },
    grid: { left: 60, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: (v) => v / 1000 + 'k' } },
    yAxis: { type: 'category', data: keys, inverse: true },
    series: [{
      type: 'bar',
      data: keys.map((k) => Math.round(byDept[k].amount)),
      itemStyle: { color: C.teal },
      label: { show: true, position: 'right', formatter: (p) => (p.value / 1000).toFixed(1) + 'k' },
    }],
  })
}

function renderOrg() {
  const rows = orgs.value.slice().reverse()
  draw(orgChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['医疗费用', '统筹支付', '违规扣减'] },
    grid: { left: 140, right: 30, top: 40, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    yAxis: { type: 'category', data: rows.map((r) => r.medinsName), inverse: true },
    series: [
      { name: '医疗费用', type: 'bar', data: rows.map((r) => Number(r.totalAmount || 0)), itemStyle: { color: C.blue } },
      { name: '统筹支付', type: 'bar', data: rows.map((r) => Number(r.insurancePay || 0)), itemStyle: { color: C.purple } },
      { name: '违规扣减', type: 'bar', data: rows.map((r) => Number(r.deductAmount || 0)), itemStyle: { color: C.red } },
    ],
  })
}

function renderTrend() {
  const days = trend.value.map((t) => t.day.slice(5))
  draw(trendChart, {
    tooltip: { trigger: 'axis' },
    legend: { data: ['医疗费用', '统筹支付', '个人账户', '现金支付', '日拨付'] },
    grid: { left: 60, right: 30, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    series: [
      { name: '医疗费用', type: 'bar', data: trend.value.map((t) => Number(t.amount || 0)), itemStyle: { color: C.blue } },
      { name: '统筹支付', type: 'line', smooth: true, data: trend.value.map((t) => Number(t.insurancePay || 0)), itemStyle: { color: C.purple } },
      { name: '个人账户', type: 'line', smooth: true, data: trend.value.map((t) => Number(t.personalAccount || 0)), itemStyle: { color: C.teal } },
      { name: '现金支付', type: 'line', smooth: true, data: trend.value.map((t) => Number(t.cashPay || 0)), itemStyle: { color: C.orange } },
      { name: '日拨付', type: 'line', smooth: true, data: trend.value.map((t) => Number(t.allocAmount || 0)), itemStyle: { color: C.red } },
    ],
  })
  draw(countChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value', name: '笔数' },
    series: [{ type: 'bar', data: trend.value.map((t) => t.count || 0), itemStyle: { color: C.blue } }],
  })
  draw(insBarChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, valueFormatter: (v) => (v / 1000).toFixed(1) + 'k' },
    legend: { data: ins.value.map((r) => r.name || '未分类') },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'value', axisLabel: { formatter: (v) => (v / 1000).toFixed(0) + 'k' } },
    yAxis: { type: 'category', data: ins.value.map((r) => r.name || '未分类') },
    series: ins.value.map((r, i) => ({
      name: r.name || '未分类',
      type: 'bar',
      stack: 'total',
      data: [Number(r.amount || 0)],
      itemStyle: { color: PALETTE[i % PALETTE.length] },
    })),
  })
}

function renderEffect() {
  const e = eff.value
  draw(effectChart, {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 60, right: 20, top: 30, bottom: 60 },
    xAxis: { type: 'category', data: ['审核任务', '疑点', '反欺诈线索', '事前事中', '清单质控', '追溯码核验'] },
    yAxis: { type: 'value', name: '数量' },
    series: [{
      type: 'bar',
      data: [e.auditTotal || 0, e.suspicionTotal || 0, e.clueTotal || 0, e.precheckTotal || 0, e.qualityTotal || 0, e.traceTotal || 0],
      itemStyle: { color: (p) => PALETTE[p.dataIndex % PALETTE.length] },
      label: { show: true, position: 'top' },
    }],
  })
  draw(pendingChart, {
    tooltip: { trigger: 'item', formatter: (p) => `${p.name}<br/>${p.value} 条待处理` },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '58%',
      center: ['50%', '44%'],
      label: { formatter: '{b}\n{c}' },
      data: [
        { name: '费用审核', value: Math.max(0, (e.auditTotal || 0) - (e.auditPassed || 0) - (e.auditRejected || 0)), itemStyle: { color: C.blue } },
        { name: '疑点稽核', value: e.suspicionPending || 0, itemStyle: { color: C.teal } },
        { name: '反欺诈线索', value: e.cluePending || 0, itemStyle: { color: C.purple } },
        { name: '事前事中', value: e.precheckPending || 0, itemStyle: { color: C.orange } },
        { name: '清单质控', value: e.qualityPending || 0, itemStyle: { color: C.red } },
        { name: '违规追回', value: e.recoverPending || 0, itemStyle: { color: C.gray } },
      ].filter((x) => x.value > 0),
    }],
  })
}

const auditRows = computed(() => {
  const e = eff.value
  return [
    { name: '审核任务总数', value: e.auditTotal || 0 },
    { name: '已通过', value: e.auditPassed || 0 },
    { name: '已驳回', value: e.auditRejected || 0 },
    { name: '申诉中', value: e.auditAppealing || 0 },
    { name: '已合议', value: e.auditConferred || 0 },
    { name: '涉及金额（元）', value: fmt(e.auditAmount) },
  ]
})
const clueRows = computed(() => {
  const e = eff.value
  return [
    { name: '线索总数', value: e.clueTotal || 0 },
    { name: '待核查', value: e.cluePending || 0 },
    { name: '已确认', value: e.clueConfirmed || 0 },
    { name: '已排除', value: e.clueExcluded || 0 },
    { name: '已移送', value: e.clueTransferred || 0 },
    { name: '追溯码异常核验', value: e.traceAbnormal || 0 },
  ]
})

onMounted(reload)

// 窗口尺寸变化时同步图表
function onResize() {
  Object.values(chartRefs).forEach((refEl) => {
    if (refEl.value) {
      const c = echarts.getInstanceByDom(refEl.value)
      if (c) c.resize()
    }
  })
}
onMounted(() => window.addEventListener('resize', onResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  disposeAll()
})
</script>
