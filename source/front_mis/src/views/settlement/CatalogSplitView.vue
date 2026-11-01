<template>
  <div class="space-y-3">
    <!-- 统计 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">医保目录分表（药品 / 诊疗 / 耗材）</div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">药品目录</div>
          <div class="amount-strong">{{ stats.drug ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">药品启用</div>
          <div class="amount-strong !text-[#059669]">{{ stats.drugEnabled ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">诊疗目录</div>
          <div class="amount-strong">{{ stats.treat ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">诊疗启用</div>
          <div class="amount-strong !text-[#059669]">{{ stats.treatEnabled ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">耗材目录</div>
          <div class="amount-strong">{{ stats.material ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">耗材启用</div>
          <div class="amount-strong !text-[#059669]">{{ stats.materialEnabled ?? 0 }}</div>
        </div>
      </div>
      <div class="mt-2 text-[11px] text-gray-400">
        说明：药品、诊疗项目、医用耗材按各自业务口径分表管理；三张分表的限价与支付类别是事前提醒、清单质控的判定依据。
      </div>
    </div>

    <!-- 查询 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="名称/编码/医保编码/通用名" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.catalogType" placeholder="目录类型" clearable style="width:100%" @change="search">
              <el-option label="药品目录" value="drug" />
              <el-option label="诊疗目录" value="treat" />
              <el-option label="耗材目录" value="material" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.payClass" placeholder="支付类别" clearable style="width:100%">
              <el-option label="甲类" value="甲" />
              <el-option label="乙类" value="乙" />
              <el-option label="丙类" value="丙" />
            </el-select>
          </div>
        </div>
        <div class="flex gap-2 mt-2">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 列表 -->
    <div class="his-card p-3">
      <el-table :data="rows" v-loading="loading" size="small" border stripe>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.catalogType)">{{ row.catalogTypeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="目录编码" width="110" />
        <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="genericName" label="通用名" width="120" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="100" />
        <el-table-column prop="dosageForm" label="剂型" width="80" />
        <el-table-column prop="ybCode" label="医保编码" width="120" />
        <el-table-column label="支付类别" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="payTag(row.payClass)">{{ row.payClass || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="限价" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.limitPrice) }}</template>
        </el-table-column>
        <el-table-column label="支付比例" width="90" align="right">
          <template #default="{ row }">{{ row.insRatio != null ? (row.insRatio * 100).toFixed(0) + '%' : '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-3 flex justify-end">
        <el-pagination
          v-model:current-page="q.page"
          v-model:page-size="q.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listCatalogSplit, catalogSplitStats } from '@/api/system'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stats = ref({})
const q = reactive({ keyword: '', catalogType: '', payClass: '', page: 1, size: 20 })

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function typeTag(t) {
  return { drug: 'success', treat: 'primary', material: 'warning' }[t] || 'info'
}
function payTag(p) {
  return { 甲: 'success', 乙: 'warning', 丙: 'danger' }[p] || 'info'
}

async function load() {
  loading.value = true
  const res = await listCatalogSplit({ ...q })
  rows.value = res.data?.list || []
  total.value = res.data?.total || 0
  loading.value = false
}

async function loadStats() {
  const res = await catalogSplitStats()
  stats.value = res.data || {}
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', catalogType: '', payClass: '' })
  search()
}

onMounted(() => {
  load()
  loadStats()
})
</script>
