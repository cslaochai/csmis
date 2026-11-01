<template>
  <div class="space-y-3">
    <el-alert type="info" :closable="false" show-icon
      title="即时结算改革：按笔日拨付"
      description="对每日已接收的有效结算单，按 60% 比例于 T+1 生成日拨付批次并推送基金财务；银行电子回单回传后标记已回单，次月月结时从应付费用中扣减。体现'即时结算'与月度结算的衔接。" />

    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item"><el-date-picker v-model="q.bizDate" type="date" value-format="YYYY-MM-DD" placeholder="业务日期" style="width:100%" /></div>
          <div class="q-item">
            <el-select v-model="q.remitStatus" placeholder="回单状态" clearable style="width:100%">
              <el-option label="待拨付" :value="0" />
              <el-option label="已回单" :value="1" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="generate">生成日拨付批次</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">日拨付批次</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="bizDate" label="业务日期" width="130" />
        <el-table-column prop="batchNo" label="批次号" width="160" />
        <el-table-column prop="settleCount" label="结算笔数" width="100" align="center" />
        <el-table-column label="基金支付总额" align="right" width="140">
          <template #default="{ row }">{{ fmt(row.totalFund) }}</template>
        </el-table-column>
        <el-table-column label="本次拨付" align="right" width="140">
          <template #default="{ row }"><span class="text-[#C0392B] font-semibold">{{ fmt(row.payAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="回单状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.remitStatus === 1 ? 'success' : 'warning'">{{ row.remitStatus === 1 ? '已回单' : '待拨付' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiptNo" label="电子回单号" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :disabled="row.remitStatus === 1" @click="remit(row)">回单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="q.size" :current-page="q.page" :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { q.page = p; load() }"
          @size-change="(s) => { q.size = s; q.page = 1; load() }"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { dailyListPage, dailyGenerate, dailyRemit } from '@/api/insurance'
import { PAGE_SIZES } from '@/lib/pagination'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ bizDate: '', remitStatus: '', page: 1, size: 10 })

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
async function load() {
  loading.value = true
  const res = await dailyListPage(q)
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.bizDate = ''; q.remitStatus = ''; q.page = 1; load() }
async function generate() {
  const bizDate = q.bizDate || new Date().toISOString().slice(0, 10)
  const res = await dailyGenerate(bizDate)
  if (res.data.ok) {
    ElMessage.success('已生成日拨付批次')
    load()
  } else {
    ElMessage.warning(res.data.error || '生成失败')
  }
}
async function remit(row) {
  const res = await dailyRemit(row.id)
  if (res.data.ok) {
    ElMessage.success('已回单')
    load()
  } else {
    ElMessage.warning(res.data.error || '操作失败')
  }
}
onMounted(load)
</script>
