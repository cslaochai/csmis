<template>
  <div class="space-y-3">
    <!-- 追溯码看板 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">药品追溯码监管看板</div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">台账码总数</div>
          <div class="amount-strong">{{ stats.totalCodes ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">未使用</div>
          <div class="amount-strong">{{ stats.unusedCodes ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
          <div class="text-[12px] text-gray-500">已核销</div>
          <div class="amount-strong !text-[#7C3AED]">{{ stats.usedCodes ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
          <div class="text-[12px] text-gray-500">码不存在</div>
          <div class="amount-strong !text-[#DC2626]">{{ stats.missingCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
          <div class="text-[12px] text-gray-500">重复使用</div>
          <div class="amount-strong !text-[#D97706]">{{ stats.reusedCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">核验通过率</div>
          <div class="amount-strong">{{ passRate }}%</div>
        </div>
      </div>
    </div>

    <!-- 追溯码台账 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="cq.keyword" placeholder="追溯码/药品/批次/结算单" clearable @keyup.enter="searchCode" />
          </div>
          <div class="q-item">
            <el-select v-model="cq.status" placeholder="码状态" clearable style="width:100%">
              <el-option v-for="(v, k) in CODE_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-input v-model="verifyCode" placeholder="扫码核验：输入追溯码" clearable @keyup.enter="doVerify" />
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchCode">查询</el-button>
          <el-button @click="resetCode">重置</el-button>
          <el-button type="success" size="small" @click="openRegister()">登记追溯码</el-button>
          <el-button type="warning" size="small" @click="openBillVerify()">按结算单核验</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">追溯码台账</div>
      <el-table :data="codes" size="small" v-loading="codeLoading">
        <el-table-column prop="code" label="追溯码" width="200" show-overflow-tooltip />
        <el-table-column prop="drugName" label="药品名称" width="160" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="120" show-overflow-tooltip />
        <el-table-column prop="manufacturer" label="生产企业" width="170" show-overflow-tooltip />
        <el-table-column prop="batchNo" label="批次号" width="120" />
        <el-table-column prop="expireDate" label="有效期" width="110" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="codeStatusType(row.status)">{{ codeStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="billNo" label="核销结算单" width="140" />
        <el-table-column label="核验次数" width="90" align="center">
          <template #default="{ row }">{{ row.verifyCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="doVerify(row.code)">核验</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="codeTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { cq.page = p; loadCode() }"
          @size-change="(s) => { cq.size = s; cq.page = 1; loadCode() }"
        />
      </div>
    </div>

    <!-- 核验日志 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="lq.keyword" placeholder="追溯码/项目/结算单" clearable @keyup.enter="searchLog" />
          </div>
          <div class="q-item">
            <el-select v-model="lq.verifyResult" placeholder="核验结果" clearable style="width:100%">
              <el-option v-for="(v, k) in VERIFY_RESULTS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchLog">查询</el-button>
          <el-button @click="resetLog">重置</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">核验日志（异常追溯留痕）</div>
      <el-table :data="logs" size="small" v-loading="logLoading">
        <el-table-column prop="code" label="追溯码" width="190" show-overflow-tooltip />
        <el-table-column prop="billNo" label="结算单号" width="140" />
        <el-table-column prop="itemName" label="结算项目" width="160" show-overflow-tooltip />
        <el-table-column prop="drugName" label="台账药品" width="150" show-overflow-tooltip />
        <el-table-column label="核验结果" width="130" align="center">
          <template #default="{ row }"><el-tag size="small" :type="verifyResultType(row.verifyResult)">{{ verifyResultText(row.verifyResult) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="problemDesc" label="核验说明" min-width="280" show-overflow-tooltip />
        <el-table-column prop="verifier" label="核验人" width="100" />
        <el-table-column prop="verifyTime" label="核验时间" width="165" />
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="lq.size" :current-page="lq.page" :total="logTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { lq.page = p; loadLog() }"
          @size-change="(s) => { lq.size = s; lq.page = 1; loadLog() }"
        />
      </div>
    </div>

    <!-- 登记追溯码 -->
    <el-dialog v-model="regVisible" title="登记药品追溯码" width="600px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="登记方式">
          <el-radio-group v-model="regMode">
            <el-radio :value="1">单条登记</el-radio>
            <el-radio :value="2">批量登记（每行一个码）</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="regMode === 1">
          <el-form-item label="追溯码">
            <el-input v-model="regForm.code" placeholder="扫码枪扫描或手工输入" />
          </el-form-item>
          <el-form-item label="药品名称">
            <el-input v-model="regForm.drugName" />
          </el-form-item>
          <el-form-item label="规格">
            <el-input v-model="regForm.spec" />
          </el-form-item>
          <el-form-item label="生产企业">
            <el-input v-model="regForm.manufacturer" />
          </el-form-item>
          <el-form-item label="批次号">
            <el-input v-model="regForm.batchNo" />
          </el-form-item>
          <el-form-item label="有效期">
            <el-date-picker v-model="regForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="药品名称">
            <el-input v-model="batchDrugName" placeholder="批量登记的统一药品名称" />
          </el-form-item>
          <el-form-item label="追溯码">
            <el-input v-model="batchCodes" type="textarea" :rows="8" placeholder="每行一个追溯码" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="regVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRegister">提交登记</el-button>
      </template>
    </el-dialog>

    <!-- 按结算单核验 -->
    <el-dialog v-model="billVerifyVisible" title="按结算单批量核验追溯码" width="440px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="结算单号">
          <el-input v-model="verifyBillNo" placeholder="如 YL20261101000001" />
        </el-form-item>
        <el-form-item label="说明">
          <el-text type="info" size="small">对结算单内药品类明细逐条匹配追溯码，核验通过即核销，异常写入核验日志。</el-text>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="billVerifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doVerifyBill">开始核验</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { traceCodeListPage, traceCodeRegister, traceVerify, traceVerifyBill, traceLogListPage, traceStats } from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { CODE_STATUS, VERIFY_RESULTS, codeStatusText, verifyResultText, codeStatusType, verifyResultType } from './traceMeta'

const stats = ref({})
const saving = ref(false)

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
const passRate = computed(() => {
  const t = stats.value.verifyCount || 0
  if (!t) return '0.0'
  return (((stats.value.passCount || 0) / t) * 100).toFixed(1)
})

async function loadStats() {
  const res = await traceStats()
  stats.value = res.data || {}
}

/* ---------------- 台账 ---------------- */
const codes = ref([])
const codeTotal = ref(0)
const codeLoading = ref(false)
const cq = reactive({ keyword: '', status: '', page: 1, size: 10 })

async function loadCode() {
  codeLoading.value = true
  const res = await traceCodeListPage({ page: cq.page, size: cq.size, keyword: cq.keyword || undefined, status: cq.status === '' ? undefined : cq.status })
  codeLoading.value = false
  codes.value = res.data.list
  codeTotal.value = res.data.total
}
function searchCode() { cq.page = 1; loadCode() }
function resetCode() { cq.keyword = ''; cq.status = ''; cq.page = 1; loadCode() }

/* ---------------- 登记 ---------------- */
const regVisible = ref(false)
const regMode = ref(1)
const regForm = reactive({ code: '', drugName: '', spec: '', manufacturer: '', batchNo: '', expireDate: '' })
const batchDrugName = ref('')
const batchCodes = ref('')
function openRegister() {
  Object.assign(regForm, { code: '', drugName: '', spec: '', manufacturer: '', batchNo: '', expireDate: '' })
  batchDrugName.value = ''
  batchCodes.value = ''
  regMode.value = 1
  regVisible.value = true
}
async function saveRegister() {
  saving.value = true
  try {
    let payload
    if (regMode.value === 1) {
      if (!regForm.code) { ElMessage.warning('请输入追溯码'); saving.value = false; return }
      payload = { ...regForm }
    } else {
      const list = batchCodes.value.split('\n').map(s => s.trim()).filter(Boolean)
      if (!list.length) { ElMessage.warning('请输入追溯码'); saving.value = false; return }
      payload = {
        drugName: batchDrugName.value,
        items: list.map(c => ({ code: c, drugName: batchDrugName.value })),
      }
    }
    const res = await traceCodeRegister(payload)
    if (res.data.ok) {
      ElMessage.success(`登记成功 ${res.data.matched ?? 0} 条${res.data.error ? '，' + res.data.error : ''}`)
      regVisible.value = false
      loadCode(); loadStats()
    } else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

/* ---------------- 核验 ---------------- */
const verifyCode = ref('')
async function doVerify(code) {
  const c = code || verifyCode.value
  if (!c) { ElMessage.warning('请输入追溯码'); return }
  const res = await traceVerify({ code: c })
  const d = res.data || {}
  if (d.verifyResult === 1) {
    ElMessage.success(`核验通过：${d.verifyDesc}`)
  } else {
    ElMessage.error(`核验异常：${d.verifyDesc}`)
  }
  loadCode(); loadLog(); loadStats()
}

const billVerifyVisible = ref(false)
const verifyBillNo = ref('')
function openBillVerify() {
  verifyBillNo.value = ''
  billVerifyVisible.value = true
}
async function doVerifyBill() {
  if (!verifyBillNo.value) { ElMessage.warning('请输入结算单号'); return }
  saving.value = true
  try {
    const res = await traceVerifyBill(verifyBillNo.value)
    if (res.data.ok) { ElMessage.success(`已核验 ${res.data.matched ?? 0} 条药品明细`); billVerifyVisible.value = false; loadCode(); loadLog(); loadStats() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

/* ---------------- 日志 ---------------- */
const logs = ref([])
const logTotal = ref(0)
const logLoading = ref(false)
const lq = reactive({ keyword: '', verifyResult: '', page: 1, size: 10 })

async function loadLog() {
  logLoading.value = true
  const res = await traceLogListPage({ page: lq.page, size: lq.size, keyword: lq.keyword || undefined, verifyResult: lq.verifyResult === '' ? undefined : lq.verifyResult })
  logLoading.value = false
  logs.value = res.data.list
  logTotal.value = res.data.total
}
function searchLog() { lq.page = 1; loadLog() }
function resetLog() { lq.keyword = ''; lq.verifyResult = ''; lq.page = 1; loadLog() }

onMounted(() => { loadCode(); loadLog(); loadStats() })
</script>
