<template>
  <div class="space-y-3">
    <!-- 规则 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="rq.keyword" placeholder="规则编码/名称" clearable @keyup.enter="searchRule" />
          </div>
          <div class="q-item">
            <el-select v-model="rq.triggerStage" placeholder="触发阶段" clearable style="width:100%">
              <el-option v-for="(v, k) in TRIGGER_STAGES" :key="k" :label="v" :value="k" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="rq.status" placeholder="状态" clearable style="width:100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchRule">查询</el-button>
          <el-button @click="resetRule">重置</el-button>
          <el-button type="success" size="small" @click="openRule">新增规则</el-button>
          <el-button type="warning" size="small" @click="evaluate">运行事前/事中评估</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">事前提醒 / 事中拦截规则（三道防线·前两道）</div>
      <el-table :data="rules" size="small" v-loading="ruleLoading">
        <el-table-column prop="ruleCode" label="规则编码" width="130" />
        <el-table-column prop="ruleName" label="规则名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="阶段" width="100">
          <template #default="{ row }"><el-tag size="small" :type="row.triggerStage === 'in_settle' ? 'danger' : 'primary'">{{ stageText(row.triggerStage) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="判定类别" width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="风险" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="riskType(row.riskLevel)">{{ riskText(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处置" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="blockType(row.blockFlag)">{{ row.blockFlag === 1 ? '阻断' : '提醒' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="promptText" label="提示语" min-width="200" show-overflow-tooltip />
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
        <el-pagination :page-sizes="PAGE_SIZES" :page-size="rq.size" :current-page="rq.page" :total="ruleTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { rq.page = p; loadRule() }"
          @size-change="(s) => { rq.size = s; rq.page = 1; loadRule() }" />
      </div>
    </div>

    <!-- 命中结果 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="cq.keyword" placeholder="业务单号/机构/参保人" clearable @keyup.enter="searchResult" />
          </div>
          <div class="q-item">
            <el-select v-model="cq.triggerStage" placeholder="阶段" clearable style="width:100%">
              <el-option v-for="(v, k) in TRIGGER_STAGES" :key="k" :label="v" :value="k" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="cq.handleStatus" placeholder="处理状态" clearable style="width:100%">
              <el-option label="待处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchResult">查询</el-button>
          <el-button @click="resetResult">重置</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">命中结果（事前/事中留痕）</div>
      <el-table :data="results" size="small" v-loading="resultLoading">
        <el-table-column prop="ruleName" label="规则" width="150" show-overflow-tooltip />
        <el-table-column label="阶段" width="100">
          <template #default="{ row }"><el-tag size="small" :type="row.triggerStage === 'in_settle' ? 'danger' : 'primary'">{{ stageText(row.triggerStage) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="medinsName" label="机构" width="150" show-overflow-tooltip />
        <el-table-column prop="insuredName" label="参保人" width="100" />
        <el-table-column prop="bizNo" label="业务单号" width="150" />
        <el-table-column prop="promptText" label="提示语" min-width="200" show-overflow-tooltip />
        <el-table-column label="风险" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="riskType(row.riskLevel)">{{ riskText(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="处置" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="blockType(row.blockFlag)">{{ row.blockFlag === 1 ? '阻断' : '提醒' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.handleStatus === 1 ? 'success' : 'warning'">{{ row.handleStatus === 1 ? '已处理' : '待处理' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.handleStatus === 0" link type="primary" @click="handleResult(row)">处理</el-button>
            <span v-else class="text-[12px] text-gray-400">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="resultTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { cq.page = p; loadResult() }"
          @size-change="(s) => { cq.size = s; cq.page = 1; loadResult() }" />
      </div>
    </div>

    <el-dialog v-model="ruleVisible" :title="ruleRow ? '编辑规则' : '新增规则'" width="560px" append-to-body>
      <el-form ref="ruleRef" :model="ruleForm" :rules="ruleRules" label-width="100px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="ruleForm.ruleCode" :disabled="!!ruleRow" placeholder="如 PRE-001" />
        </el-form-item>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" />
        </el-form-item>
        <el-form-item label="触发阶段" prop="triggerStage">
          <el-radio-group v-model="ruleForm.triggerStage">
            <el-radio value="before_settle">事前提醒</el-radio>
            <el-radio value="in_settle">事中拦截</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="判定类别" prop="category">
          <el-select v-model="ruleForm.category" placeholder="选择判定类别（决定引擎判定逻辑）" style="width:100%">
            <el-option v-for="(desc, k) in RULE_CATEGORIES" :key="k" :label="k" :value="k">
              <span>{{ k }}</span>
              <span class="text-gray-400 ml-2 text-[12px]">{{ desc }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="ruleForm.riskLevel" style="width:100%">
            <el-option v-for="(v, k) in RISK_LEVELS" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置方式">
          <el-radio-group v-model="ruleForm.blockFlag">
            <el-radio :value="0">仅提醒</el-radio>
            <el-radio :value="1">阻断结算</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="提示语">
          <el-input v-model="ruleForm.promptText" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="规则参数(JSON)">
          <el-input v-model="ruleForm.params" type="textarea" :rows="2"
            placeholder='如 {"threshold":5,"windowDays":30}；阈值随类别不同，详见「判定类别」说明' />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  precheckRuleListPage, precheckRuleUpsert, precheckRuleStatus, precheckResultListPage, precheckEvaluate, precheckResultHandle,
} from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { TRIGGER_STAGES, RISK_LEVELS, RULE_CATEGORIES, stageText, riskText, riskType, blockType } from './precheckMeta'

const rules = ref([])
const ruleTotal = ref(0)
const ruleLoading = ref(false)
const rq = reactive({ keyword: '', triggerStage: '', status: '', page: 1, size: 10 })

async function loadRule() {
  ruleLoading.value = true
  const res = await precheckRuleListPage(rq)
  ruleLoading.value = false
  rules.value = res.data.list
  ruleTotal.value = res.data.total
}
function searchRule() { rq.page = 1; loadRule() }
function resetRule() { rq.keyword = ''; rq.triggerStage = ''; rq.status = ''; rq.page = 1; loadRule() }

const results = ref([])
const resultTotal = ref(0)
const resultLoading = ref(false)
const cq = reactive({ keyword: '', triggerStage: '', handleStatus: '', page: 1, size: 10 })

async function loadResult() {
  resultLoading.value = true
  const res = await precheckResultListPage(cq)
  resultLoading.value = false
  results.value = res.data.list
  resultTotal.value = res.data.total
}
function searchResult() { cq.page = 1; loadResult() }
function resetResult() { cq.keyword = ''; cq.triggerStage = ''; cq.handleStatus = ''; cq.page = 1; loadResult() }

const ruleVisible = ref(false)
const ruleRow = ref(null)
const ruleRef = ref()
const saving = ref(false)
const ruleForm = reactive({ id: null, ruleCode: '', ruleName: '', triggerStage: 'before_settle', category: '超限价', riskLevel: 2, blockFlag: 0, promptText: '', params: '', status: 1, description: '' })
const ruleRules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  triggerStage: [{ required: true, message: '请选择触发阶段', trigger: 'change' }],
  category: [{ required: true, message: '请选择判定类别', trigger: 'change' }],
}
function openRule(row) {
  ruleRow.value = row || null
  Object.assign(ruleForm, row
    ? { id: row.id, ruleCode: row.ruleCode, ruleName: row.ruleName, triggerStage: row.triggerStage, category: row.category || '超限价', riskLevel: row.riskLevel, blockFlag: row.blockFlag, promptText: row.promptText, params: row.params || '', status: row.status, description: row.description }
    : { id: null, ruleCode: '', ruleName: '', triggerStage: 'before_settle', category: '超限价', riskLevel: 2, blockFlag: 0, promptText: '', params: '', status: 1, description: '' })
  ruleVisible.value = true
}
async function saveRule() {
  await ruleRef.value.validate()
  saving.value = true
  try {
    const res = await precheckRuleUpsert({ ...ruleForm })
    if (res.data.ok) { ElMessage.success('保存成功'); ruleVisible.value = false; loadRule() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function toggleRule(row) {
  const res = await precheckRuleStatus({ id: row.id, status: row.status === 1 ? 0 : 1 })
  if (res.data.ok) { ElMessage.success('已更新'); loadRule() }
  else ElMessage.warning(res.data.error || '失败')
}
async function evaluate() {
  const res = await precheckEvaluate()
  if (res.data.ok) { ElMessage.success(`已生成命中 ${res.data.matched ?? 0} 条`); loadResult() }
  else ElMessage.warning(res.data.error || '失败')
}
async function handleResult(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入处理意见', '处理命中', { inputType: 'textarea' })
    const res = await precheckResultHandle({ id: row.id, handleOpinion: value })
    if (res.data.ok) { ElMessage.success('已处理'); loadResult() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}

onMounted(() => { loadRule(); loadResult() })
</script>
