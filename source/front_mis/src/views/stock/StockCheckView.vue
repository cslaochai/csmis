<template>
  <div class="space-y-3">
    <!-- 列表 -->
    <div class="his-card p-3">
      <div class="flex items-center gap-2 mb-2">
        <div class="font-semibold text-[15px] text-[#0B3B66]">库存盘点</div>
        <el-button type="primary" size="small" @click="onCreate">发起盘点</el-button>
      </div>

      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="盘点单号/机构" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="状态" clearable style="width:100%">
              <el-option label="待盘点" :value="1" />
              <el-option label="盘点中" :value="2" />
              <el-option label="待复核" :value="3" />
              <el-option label="已办结" :value="4" />
            </el-select>
          </div>
        </div>
        <div class="flex gap-2 mt-2">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" size="small" border stripe class="mt-2">
        <el-table-column prop="checkNo" label="盘点单号" width="160" />
        <el-table-column prop="medinsName" label="定点机构" min-width="140" show-overflow-tooltip />
        <el-table-column prop="checkDate" label="盘点日期" width="110" />
        <el-table-column prop="itemCount" label="盘点项" width="80" align="right" />
        <el-table-column prop="diffCount" label="差异项" width="80" align="right">
          <template #default="{ row }">
            <span :class="row.diffCount > 0 ? 'text-red-600 font-semibold' : ''">{{ row.diffCount ?? 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异金额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.diffAmount) }}</template>
        </el-table-column>
        <el-table-column prop="checker" label="盘点人" width="90" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewer" label="复核人" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openItems(row)">
              {{ row.status === 1 ? '录入实盘' : '查看明细' }}
            </el-button>
            <el-button v-if="row.status === 2" link type="warning" @click="onFinish(row)">完成盘点</el-button>
            <template v-if="row.status === 3">
              <el-button link type="success" @click="onReview(row, true)">复核通过</el-button>
              <el-button link type="info" @click="onReview(row, false)">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-3 flex justify-end">
        <el-pagination
          v-model:current-page="q.page"
          v-model:page-size="q.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search" />
      </div>
    </div>

    <!-- 明细 -->
    <el-dialog v-model="itemVisible" :title="`盘点明细 - ${current?.checkNo || ''}`" width="860px" top="6vh">
      <div class="mb-2 flex justify-end gap-2">
        <el-button v-if="current?.status === 1 || current?.status === 2" type="primary" size="small" @click="onSubmitItems">
          提交实盘数
        </el-button>
      </div>
      <el-table :data="items" size="small" border stripe max-height="52vh">
        <el-table-column prop="itemCode" label="品项编码" width="110" />
        <el-table-column prop="itemName" label="品项名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="账面数" width="100" align="right">
          <template #default="{ row }">{{ qty(row.bookQty) }}</template>
        </el-table-column>
        <el-table-column label="实盘数" width="110" align="right">
          <template #default="{ row }">
            <el-input-number v-model="row.realQty" :precision="2" :controls="false" size="small"
                             style="width: 100%" :disabled="current?.status >= 3" />
          </template>
        </el-table-column>
        <el-table-column label="差异数" width="100" align="right">
          <template #default="{ row }">
            <span :class="diffClass(row)">{{ qty(row.diffQty) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异金额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.diffAmount) }}</template>
        </el-table-column>
        <el-table-column prop="diffReason" label="差异原因" min-width="150" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listStockChecks, createStockCheck, getStockCheckItems,
  submitStockCheckItems, finishStockCheck, reviewStockCheck,
} from '@/api/system'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const items = ref([])
const current = ref(null)
const itemVisible = ref(false)

const q = reactive({ keyword: '', status: null, page: 1, size: 20 })

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function qty(n) {
  return n == null ? '-' : Number(n).toFixed(2)
}
function statusText(s) { return { 1: '待盘点', 2: '盘点中', 3: '待复核', 4: '已办结' }[s] || '待盘点' }
function statusTag(s) { return { 1: 'info', 2: 'primary', 3: 'warning', 4: 'success' }[s] || 'info' }
function diffClass(row) {
  const d = row.diffQty ?? 0
  if (Number(d) > 0) return 'text-blue-600 font-semibold'
  if (Number(d) < 0) return 'text-red-600 font-semibold'
  return ''
}

async function load() {
  loading.value = true
  const res = await listStockChecks({ ...q })
  rows.value = res.data?.list || []
  total.value = res.data?.total || 0
  loading.value = false
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', status: null })
  search()
}

async function onCreate() {
  const { value } = await ElMessageBox.prompt('请输入定点机构编码（留空表示全部机构）', '发起盘点', {
    confirmButtonText: '发起',
    cancelButtonText: '取消',
    inputPlaceholder: '如 H4301000001',
  }).catch(() => ({ value: null }))
  if (value === null) return
  const res = await createStockCheck({ medinsCode: value, medinsName: value ? '' : '全部机构' })
  if (res.data?.ok) {
    ElMessage.success(`已生成盘点单，共 ${res.data.matched ?? 0} 个品项待盘`)
    load()
  } else {
    ElMessage.warning(res.data?.error || '发起失败')
  }
}

async function openItems(row) {
  current.value = row
  const res = await getStockCheckItems(row.checkNo)
  items.value = res.data || []
  itemVisible.value = true
}

async function onSubmitItems() {
  const payload = items.value
    .filter(it => it.realQty != null)
    .map(it => ({
      checkNo: current.value.checkNo,
      itemCode: it.itemCode,
      realQty: it.realQty,
      diffReason: it.diffReason,
    }))
  if (!payload.length) {
    ElMessage.warning('请先录入实盘数量')
    return
  }
  const res = await submitStockCheckItems(payload)
  if (res.data?.ok) {
    ElMessage.success(`已提交 ${res.data.matched ?? payload.length} 项实盘数`)
    load()
  } else {
    ElMessage.warning(res.data?.error || '提交失败')
  }
}

async function onFinish(row) {
  await ElMessageBox.confirm('完成盘点将按实盘数调整库存并生成盘点差异流水，是否继续？', '完成盘点', { type: 'warning' })
  const res = await finishStockCheck(row.checkNo)
  if (res.data?.ok) {
    ElMessage.success(`盘点完成，差异品项 ${res.data.matched ?? 0} 项`)
    load()
  } else {
    ElMessage.warning(res.data?.error || '操作失败')
  }
}

async function onReview(row, pass) {
  const { value } = await ElMessageBox.prompt(pass ? '请输入复核意见' : '请输入驳回原因', pass ? '复核通过' : '驳回', {
    confirmButtonText: '提交',
    cancelButtonText: '取消',
    inputValue: pass ? '差异已核实，同意调整' : '',
  }).catch(() => ({ value: null }))
  if (value === null) return
  const res = await reviewStockCheck(row.checkNo, pass, value)
  if (res.data?.ok) {
    ElMessage.success('已提交')
    load()
  } else {
    ElMessage.warning(res.data?.error || '操作失败')
  }
}

onMounted(load)
</script>
