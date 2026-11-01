<template>
  <div class="space-y-3">
    <!-- 质控看板 -->
    <div class="his-card p-3">
      <div class="flex items-center justify-between mb-2">
        <div class="font-semibold text-[15px] text-[#0B3B66]">结算清单质控看板</div>
        <el-button type="primary" size="small" :loading="checking" @click="doCheck">执行清单批量质控</el-button>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">质控结算单</div>
          <div class="amount-strong">{{ stats.checkedBills ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">质控明细</div>
          <div class="amount-strong">{{ stats.checkedItems ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
          <div class="text-[12px] text-gray-500">警示级</div>
          <div class="amount-strong !text-[#D97706]">{{ stats.warnCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
          <div class="text-[12px] text-gray-500">严重级</div>
          <div class="amount-strong !text-[#DC2626]">{{ stats.seriousCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
          <div class="text-[12px] text-gray-500">建议扣款合计</div>
          <div class="amount-strong !text-[#7C3AED]">{{ fmt(stats.deductAmount) }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">待处理</div>
          <div class="amount-strong">{{ stats.pendingCount ?? 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 质控规则 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="rq.keyword" placeholder="规则编码/名称" clearable @keyup.enter="searchRule" />
          </div>
          <div class="q-item">
            <el-select v-model="rq.ruleType" placeholder="校验类型" clearable style="width:100%">
              <el-option v-for="(v, k) in RULE_TYPES" :key="k" :label="v" :value="k" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="rq.severity" placeholder="严重程度" clearable style="width:100%">
              <el-option v-for="(v, k) in SEVERITIES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchRule">查询</el-button>
          <el-button @click="resetRule">重置</el-button>
          <el-button type="success" size="small" @click="openRule()">新增规则</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">质控规则库</div>
      <el-table :data="rules" size="small" v-loading="ruleLoading">
        <el-table-column prop="ruleCode" label="规则编码" width="130" />
        <el-table-column prop="ruleName" label="规则名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="校验类型" width="150">
          <template #default="{ row }"><el-tag size="small" type="primary">{{ ruleTypeText(row.ruleType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="严重程度" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="severityType(row.severity)">{{ severityText(row.severity) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="扣款方式" width="170" align="center">
          <template #default="{ row }">
            <span v-if="row.deductType === 2">固定 {{ fmt(row.deductAmount) }} 元</span>
            <span v-else-if="row.deductRatio > 0">按比例 {{ (row.deductRatio * 100).toFixed(0) }}%</span>
            <span v-else class="text-gray-400">仅提示</span>
          </template>
        </el-table-column>
        <el-table-column prop="params" label="规则参数" min-width="140" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRule(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleRule(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="rq.size" :current-page="rq.page" :total="ruleTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { rq.page = p; loadRule() }"
          @size-change="(s) => { rq.size = s; rq.page = 1; loadRule() }"
        />
      </div>
    </div>

    <!-- 质控结果 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="cq.keyword" placeholder="结算单号/项目/规则" clearable @keyup.enter="searchResult" />
          </div>
          <div class="q-item">
            <el-select v-model="cq.severity" placeholder="严重程度" clearable style="width:100%">
              <el-option v-for="(v, k) in SEVERITIES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="cq.handleStatus" placeholder="处理状态" clearable style="width:100%">
              <el-option v-for="(v, k) in HANDLE_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchResult">查询</el-button>
          <el-button @click="resetResult">重置</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">质控命中结果</div>
      <el-table :data="results" size="small" v-loading="resultLoading">
        <el-table-column prop="billNo" label="结算单号" width="140" />
        <el-table-column prop="itemName" label="涉及项目" width="160" show-overflow-tooltip />
        <el-table-column prop="ruleName" label="命中规则" width="160" show-overflow-tooltip />
        <el-table-column label="校验类型" width="130">
          <template #default="{ row }">{{ ruleTypeText(row.ruleType) }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="severityType(row.severity)">{{ severityText(row.severity) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="fieldName" label="质控字段" width="110" />
        <el-table-column prop="originValue" label="原始值" width="110" show-overflow-tooltip />
        <el-table-column prop="qualityDesc" label="质控说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="涉及金额" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.involveAmount) }}</template>
        </el-table-column>
        <el-table-column label="建议扣款" width="100" align="right">
          <template #default="{ row }">{{ fmt(row.deductAmount) }}</template>
        </el-table-column>
        <el-table-column label="处理状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="handleStatusType(row.handleStatus)">{{ handleStatusText(row.handleStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handleStatus === 0" link type="primary" @click="openHandle(row)">处理</el-button>
            <span v-else class="text-[12px] text-gray-400">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="resultTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { cq.page = p; loadResult() }"
          @size-change="(s) => { cq.size = s; cq.page = 1; loadResult() }"
        />
      </div>
    </div>

    <!-- 规则编辑 -->
    <el-dialog v-model="ruleVisible" :title="ruleRow ? '编辑质控规则' : '新增质控规则'" width="560px" append-to-body>
      <el-form ref="ruleRef" :model="ruleForm" :rules="ruleRules" label-width="110px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="ruleForm.ruleCode" :disabled="!!ruleRow" placeholder="如 QC-001" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" />
        </el-form-item>
        <el-form-item label="校验类型" prop="ruleType">
          <el-select v-model="ruleForm.ruleType" style="width:100%">
            <el-option v-for="(v, k) in RULE_TYPES" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度" prop="severity">
          <el-select v-model="ruleForm.severity" style="width:100%">
            <el-option v-for="(v, k) in SEVERITIES" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="扣款方式">
          <el-radio-group v-model="ruleForm.deductType">
            <el-radio :value="1">按比例扣减</el-radio>
            <el-radio :value="2">按固定金额扣减</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="ruleForm.deductType === 1" label="扣款比例">
          <el-input-number v-model="ruleForm.deductRatio" :min="0" :max="1" :step="0.05" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item v-else label="固定金额">
          <el-input-number v-model="ruleForm.deductAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="规则参数(JSON)">
          <el-input v-model="ruleForm.params" type="textarea" :rows="2"
            placeholder='如 {"overRatio":0,"maxQty":50,"minAmount":100}' />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="ruleForm.status" style="width:100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="ruleForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 处理质控结果 -->
    <el-dialog v-model="handleVisible" title="质控结果处理" width="500px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="命中规则">{{ handleRow?.ruleName }}</el-form-item>
        <el-form-item label="质控说明">{{ handleRow?.qualityDesc }}</el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="handleForm.handleStatus" style="width:100%">
            <el-option label="已确认（违规成立）" :value="1" />
            <el-option label="已整改" :value="2" />
            <el-option label="已忽略（误报）" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="生成扣款单">
          <el-switch v-model="handleForm.createRecover" :active-value="true" :inactive-value="false" />
          <span class="ml-2 text-[12px] text-gray-500">确认后按建议扣款金额生成违规追回单</span>
        </el-form-item>
        <el-form-item label="处理意见">
          <el-input v-model="handleForm.handleOpinion" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveHandle">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  qualityRuleListPage, qualityRuleUpsert, qualityRuleStatus,
  qualityResultListPage, qualityResultCheck, qualityResultHandle, qualityStats,
} from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { RULE_TYPES, SEVERITIES, HANDLE_STATUS, ruleTypeText, severityText, severityType, handleStatusText, handleStatusType } from './qualityMeta'

const stats = ref({})
const checking = ref(false)
const saving = ref(false)

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadStats() {
  const res = await qualityStats()
  stats.value = res.data || {}
}

/* ---------------- 规则 ---------------- */
const rules = ref([])
const ruleTotal = ref(0)
const ruleLoading = ref(false)
const rq = reactive({ keyword: '', ruleType: '', severity: '', page: 1, size: 10 })

async function loadRule() {
  ruleLoading.value = true
  const res = await qualityRuleListPage({ page: rq.page, size: rq.size, keyword: rq.keyword || undefined, ruleType: rq.ruleType || undefined, severity: rq.severity === '' ? undefined : rq.severity })
  ruleLoading.value = false
  rules.value = res.data.list
  ruleTotal.value = res.data.total
}
function searchRule() { rq.page = 1; loadRule() }
function resetRule() { rq.keyword = ''; rq.ruleType = ''; rq.severity = ''; rq.page = 1; loadRule() }

const ruleVisible = ref(false)
const ruleRow = ref(null)
const ruleRef = ref()
const ruleForm = reactive({ id: null, ruleCode: '', ruleName: '', ruleType: 'required_field', severity: 2, deductRatio: 0, deductType: 1, deductAmount: 0, params: '', status: 1, description: '' })
const ruleRules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择校验类型', trigger: 'change' }],
}
function openRule(row) {
  ruleRow.value = row || null
  Object.assign(ruleForm, row
    ? { id: row.id, ruleCode: row.ruleCode, ruleName: row.ruleName, ruleType: row.ruleType, severity: row.severity, deductRatio: row.deductRatio ?? 0, deductType: row.deductType ?? 1, deductAmount: row.deductAmount ?? 0, params: row.params || '', status: row.status, description: row.description }
    : { id: null, ruleCode: '', ruleName: '', ruleType: 'required_field', severity: 2, deductRatio: 0, deductType: 1, deductAmount: 0, params: '', status: 1, description: '' })
  ruleVisible.value = true
}
async function saveRule() {
  await ruleRef.value.validate()
  saving.value = true
  try {
    const res = await qualityRuleUpsert({ ...ruleForm })
    if (res.data.ok) { ElMessage.success('保存成功'); ruleVisible.value = false; loadRule() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function toggleRule(row) {
  const res = await qualityRuleStatus({ id: row.id, status: row.status === 1 ? 0 : 1 })
  if (res.data.ok) { ElMessage.success('已更新'); loadRule() }
  else ElMessage.warning(res.data.error || '失败')
}

/* ---------------- 结果 ---------------- */
const results = ref([])
const resultTotal = ref(0)
const resultLoading = ref(false)
const cq = reactive({ keyword: '', severity: '', handleStatus: '', page: 1, size: 10 })

async function loadResult() {
  resultLoading.value = true
  const res = await qualityResultListPage({ page: cq.page, size: cq.size, keyword: cq.keyword || undefined, severity: cq.severity === '' ? undefined : cq.severity, handleStatus: cq.handleStatus === '' ? undefined : cq.handleStatus })
  resultLoading.value = false
  results.value = res.data.list
  resultTotal.value = res.data.total
}
function searchResult() { cq.page = 1; loadResult() }
function resetResult() { cq.keyword = ''; cq.severity = ''; cq.handleStatus = ''; cq.page = 1; loadResult() }

async function doCheck() {
  try {
    await ElMessageBox.confirm('将对全部已结算结算单执行清单质控，命中结果留痕，确认执行？', '清单批量质控', { type: 'warning' })
    checking.value = true
    const res = await qualityResultCheck()
    checking.value = false
    if (res.data.ok) { ElMessage.success(`质控完成，命中 ${res.data.matched ?? 0} 条`); loadResult(); loadStats() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    checking.value = false
    if (e !== 'cancel') {/* 全局提示 */}
  }
}

const handleVisible = ref(false)
const handleRow = ref(null)
const handleForm = reactive({ handleStatus: 1, handleOpinion: '', createRecover: false })
function openHandle(row) {
  handleRow.value = row
  handleForm.handleStatus = 1
  handleForm.handleOpinion = ''
  handleForm.createRecover = (row.deductAmount ?? 0) > 0
  handleVisible.value = true
}
async function saveHandle() {
  saving.value = true
  try {
    const res = await qualityResultHandle({ id: handleRow.value.id, ...handleForm })
    if (res.data.ok) { ElMessage.success('已处理'); handleVisible.value = false; loadResult(); loadStats() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

onMounted(() => { loadRule(); loadResult(); loadStats() })
</script>
