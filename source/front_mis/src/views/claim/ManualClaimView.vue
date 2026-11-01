<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-select v-model="q.claimType" placeholder="报销类型" clearable style="width:100%">
              <el-option v-for="(v, k) in CLAIM_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="状态" clearable style="width:100%">
              <el-option v-for="(v, k) in CLAIM_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" size="small" @click="openAdd">报销登记</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">手工（零星）报销</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="claimNo" label="报销单号" width="160" />
        <el-table-column prop="personName" label="姓名" width="90" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ claimTypeText(row.claimType) }}</template>
        </el-table-column>
        <el-table-column label="费用总额" align="right" width="110">
          <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="核定统筹" align="right" width="110">
          <template #default="{ row }">{{ fmt(row.insurancePay) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="claimStatusType(row.status)">{{ claimStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="row.status !== 0" @click="openAudit(row)">审核</el-button>
            <el-button link type="success" :disabled="row.status !== 1" @click="pay(row)">拨付</el-button>
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

    <ClaimEdit v-model="addVisible" @saved="load" />

    <el-dialog v-model="auditVisible" title="报销审核" width="480px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="报销单号"><span>{{ current.claimNo }}</span></el-form-item>
        <el-form-item label="核定统筹支付">
          <el-input-number v-model="auditForm.insurancePay" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.auditOpinion" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="danger" @click="doAudit(false)">退回</el-button>
        <el-button type="success" @click="doAudit(true)">通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { claimListPage, claimAudit, claimPay } from '@/api/insurance'
import { PAGE_SIZES } from '@/lib/pagination'
import ClaimEdit from './ClaimEdit.vue'
import { CLAIM_TYPES, CLAIM_STATUS, claimTypeText, claimStatusText, claimStatusType } from './claimMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ claimType: '', status: '', page: 1, size: 10 })

const addVisible = ref(false)
function openAdd() { addVisible.value = true }

const auditVisible = ref(false)
const current = ref({})
const auditForm = reactive({ insurancePay: 0, auditOpinion: '' })
function openAudit(row) {
  current.value = row
  auditForm.insurancePay = row.totalAmount || 0
  auditForm.auditOpinion = ''
  auditVisible.value = true
}

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
async function load() {
  loading.value = true
  const res = await claimListPage(q)
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.claimType = ''; q.status = ''; q.page = 1; load() }

async function doAudit(passed) {
  const res = await claimAudit({ id: current.value.id, insurancePay: auditForm.insurancePay, auditOpinion: auditForm.auditOpinion, passed })
  if (res.data.ok) { ElMessage.success(passed ? '已通过' : '已退回'); auditVisible.value = false; load() }
  else ElMessage.warning(res.data.error || '失败')
}
async function pay(row) {
  const res = await claimPay(row.id)
  if (res.data.ok) { ElMessage.success('已拨付'); load() } else ElMessage.warning(res.data.error || '失败')
}
onMounted(load)
</script>
