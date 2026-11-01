<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="清单号/结算单号/患者" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-input v-model="q.billNo" placeholder="结算单号" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.insuranceType" placeholder="险种" clearable style="width:100%">
              <el-option label="职工医保" value="职工医保" />
              <el-option label="居民医保" value="居民医保" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.encounterType" placeholder="就诊类型" clearable style="width:100%">
              <el-option label="门诊" value="门诊" />
              <el-option label="住院" value="住院" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.checkStatus" placeholder="质控状态" clearable style="width:100%">
              <el-option label="未质控" :value="0" />
              <el-option label="质控通过" :value="1" />
              <el-option label="质控异常" :value="2" />
              <el-option label="已作废" :value="3" />
            </el-select>
          </div>
        </div>
        <div class="flex gap-2 mt-2">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <el-table :data="rows" v-loading="loading" size="small" border stripe>
        <el-table-column prop="listNo" label="清单号" width="160" />
        <el-table-column prop="billNo" label="结算单号" width="160" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="idCardMask" label="证件号" width="160" />
        <el-table-column prop="insuranceType" label="险种" width="90" />
        <el-table-column prop="encounterType" label="就诊" width="70" />
        <el-table-column prop="itemCount" label="项目数" width="80" align="right" />
        <el-table-column label="总费用" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="基金支付" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.insurancePay) }}</template>
        </el-table-column>
        <el-table-column prop="listVersion" label="版本" width="70" align="center" />
        <el-table-column prop="uploadBatchNo" label="上传批次" width="120" />
        <el-table-column label="质控状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="checkTag(row.checkStatus)">{{ checkText(row.checkStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="uploadTime" label="上传时间" width="160" />
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
import { listSettleLists } from '@/api/system'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', billNo: '', insuranceType: '', encounterType: '', checkStatus: null, page: 1, size: 20 })

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function checkText(s) { return { 0: '未质控', 1: '质控通过', 2: '质控异常', 3: '已作废' }[s] || '未质控' }
function checkTag(s) { return { 0: 'info', 1: 'success', 2: 'danger', 3: 'warning' }[s] || 'info' }

async function load() {
  loading.value = true
  const res = await listSettleLists({ ...q })
  rows.value = res.data?.list || []
  total.value = res.data?.total || 0
  loading.value = false
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', billNo: '', insuranceType: '', encounterType: '', checkStatus: null })
  search()
}

onMounted(load)
</script>
