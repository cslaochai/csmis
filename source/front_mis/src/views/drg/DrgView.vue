<template>
  <div class="space-y-3">
    <!-- 监管看板 -->
    <div class="his-card p-3">
      <div class="flex items-center justify-between mb-2">
        <div class="font-semibold text-[15px] text-[#0B3B66]">DRG/DIP 支付监管看板</div>
        <div class="flex gap-2">
          <el-button type="primary" size="small" :loading="grouping" @click="doGroup">住院病例批量入组</el-button>
        </div>
      </div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">已入组病例</div>
          <div class="amount-strong">{{ stats.totalCases ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
          <div class="text-[12px] text-gray-500">偏差预警</div>
          <div class="amount-strong !text-[#D97706]">{{ stats.warnCases ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
          <div class="text-[12px] text-gray-500">费用超支</div>
          <div class="amount-strong !text-[#DC2626]">{{ stats.overCases ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
          <div class="text-[12px] text-gray-500">特例单议</div>
          <div class="amount-strong !text-[#7C3AED]">{{ stats.specialCases ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">涉及费用合计</div>
          <div class="amount-strong">{{ fmt(stats.totalCost) }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">整体偏差率</div>
          <div class="amount-strong">{{ stats.deviationPercent ?? 0 }}%</div>
        </div>
      </div>
    </div>

    <!-- 病组 / 病种池 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="gq.keyword" placeholder="病组编码/名称" clearable @keyup.enter="searchGroup" />
          </div>
          <div class="q-item">
            <el-select v-model="gq.payType" placeholder="支付方式" clearable style="width:100%">
              <el-option v-for="(v, k) in PAY_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="gq.status" placeholder="状态" clearable style="width:100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchGroup">查询</el-button>
          <el-button @click="resetGroup">重置</el-button>
          <el-button type="success" size="small" @click="openGroup()">新增病组/病种</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">病组 / 病种分值池</div>
      <el-table :data="groups" size="small" v-loading="groupLoading">
        <el-table-column prop="groupCode" label="编码" width="140" />
        <el-table-column prop="groupName" label="病组/病种名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="支付方式" width="170">
          <template #default="{ row }"><el-tag size="small" :type="row.payType === 2 ? 'warning' : 'primary'">{{ payTypeText(row.payType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="权重/分值" width="100" align="right">
          <template #default="{ row }">{{ row.payType === 2 ? fmt(row.pointValue) : fmt(row.weight) }}</template>
        </el-table-column>
        <el-table-column label="费率/分值成本" width="130" align="right">
          <template #default="{ row }">{{ row.payType === 2 ? fmt(row.pointCost) : fmt(row.rate) }}</template>
        </el-table-column>
        <el-table-column label="组均次费用" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.avgCost) }}</template>
        </el-table-column>
        <el-table-column label="病例数" width="80" align="center">
          <template #default="{ row }">{{ row.caseCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openGroup(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleGroup(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="gq.size" :current-page="gq.page" :total="groupTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { gq.page = p; loadGroup() }"
          @size-change="(s) => { gq.size = s; gq.page = 1; loadGroup() }"
        />
      </div>
    </div>

    <!-- 病例监管 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="cq.keyword" placeholder="病例号/患者/病组" clearable @keyup.enter="searchCase" />
          </div>
          <div class="q-item">
            <el-select v-model="cq.payType" placeholder="支付方式" clearable style="width:100%">
              <el-option v-for="(v, k) in PAY_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="cq.status" placeholder="监管状态" clearable style="width:100%">
              <el-option v-for="(v, k) in CASE_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchCase">查询</el-button>
          <el-button @click="resetCase">重置</el-button>
          <el-button type="warning" size="small" @click="onlyOver = !onlyOver" :type="onlyOver ? 'danger' : 'warning'">
            {{ onlyOver ? '已筛选超支' : '只看超支' }}
          </el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">病例入组与偏差监管</div>
      <el-table :data="cases" size="small" v-loading="caseLoading">
        <el-table-column prop="caseNo" label="病例号" width="140" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="groupName" label="病组/病种" min-width="180" show-overflow-tooltip />
        <el-table-column label="支付方式" width="150">
          <template #default="{ row }"><el-tag size="small" :type="row.payType === 2 ? 'warning' : 'primary'">{{ payTypeText(row.payType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="支付标准" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.estCost) }}</template>
        </el-table-column>
        <el-table-column label="实际费用" width="110" align="right">
          <template #default="{ row }">{{ fmt(row.actualCost) }}</template>
        </el-table-column>
        <el-table-column label="偏差率" width="100" align="right">
          <template #default="{ row }">
            <el-tag size="small" :type="devType(row.deviationRate)">{{ pct(row.deviationRate) }}%</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deviationReason" label="监管意见" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="caseStatusType(row.status)">{{ caseStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 4" link type="warning" @click="special(row)">特例单议</el-button>
            <el-button v-if="row.status === 4" link type="success" @click="restore(row)">恢复正常</el-button>
            <span v-if="row.status === 1" class="text-[12px] text-gray-400">—</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="caseTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { cq.page = p; loadCase() }"
          @size-change="(s) => { cq.size = s; cq.page = 1; loadCase() }"
        />
      </div>
    </div>

    <!-- 病组编辑 -->
    <el-dialog v-model="groupVisible" :title="groupRow ? '编辑病组/病种' : '新增病组/病种'" width="560px" append-to-body>
      <el-form ref="groupRef" :model="groupForm" :rules="groupRules" label-width="110px">
        <el-form-item label="病组编码" prop="groupCode">
          <el-input v-model="groupForm.groupCode" :disabled="!!groupRow" placeholder="如 DRG-001 / DIP-001" />
        </el-form-item>
        <el-form-item label="名称" prop="groupName">
          <el-input v-model="groupForm.groupName" />
        </el-form-item>
        <el-form-item label="支付方式" prop="payType">
          <el-radio-group v-model="groupForm.payType" @change="onPayTypeChange">
            <el-radio :value="1">DRG（权重×费率）</el-radio>
            <el-radio :value="2">DIP（分值×分值成本）</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="groupForm.payType === 1">
          <el-form-item label="权重">
            <el-input-number v-model="groupForm.weight" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="费率(元/权重)">
            <el-input-number v-model="groupForm.rate" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="分值">
            <el-input-number v-model="groupForm.pointValue" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
          <el-form-item label="分值成本(元/分)">
            <el-input-number v-model="groupForm.pointCost" :min="0" :precision="2" style="width:100%" />
          </el-form-item>
        </template>
        <el-form-item label="组均次费用">
          <el-input-number v-model="groupForm.avgCost" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="groupForm.status" style="width:100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveGroup">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { drgGroupListPage, drgGroupUpsert, drgGroupStatus, drgCaseListPage, drgCaseGroup, drgCaseHandle, drgStats } from '@/api/drg'
import { PAGE_SIZES } from '@/lib/pagination'
import { PAY_TYPES, CASE_STATUS, payTypeText, caseStatusText, caseStatusType, devType } from './drgMeta'

const stats = ref({})
const grouping = ref(false)

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function pct(n) {
  return ((n ?? 0) * 100).toFixed(1)
}

async function loadStats() {
  const res = await drgStats()
  stats.value = res.data || {}
}

// ---------------- 病组 / 病种池 ----------------
const groups = ref([])
const groupTotal = ref(0)
const groupLoading = ref(false)
const gq = reactive({ keyword: '', payType: '', status: '', page: 1, size: 10 })

async function loadGroup() {
  groupLoading.value = true
  const res = await drgGroupListPage({ page: gq.page, size: gq.size, keyword: gq.keyword || undefined, payType: gq.payType || undefined, status: gq.status === '' ? undefined : gq.status })
  groupLoading.value = false
  groups.value = res.data.list
  groupTotal.value = res.data.total
}
function searchGroup() { gq.page = 1; loadGroup() }
function resetGroup() { gq.keyword = ''; gq.payType = ''; gq.status = ''; gq.page = 1; loadGroup() }

const groupVisible = ref(false)
const groupRow = ref(null)
const groupRef = ref()
const saving = ref(false)
const groupForm = reactive({ id: null, groupCode: '', groupName: '', payType: 1, weight: 1, rate: 3000, pointValue: 1000, pointCost: 50, avgCost: 5000, status: 1, description: '' })
const groupRules = {
  groupCode: [{ required: true, message: '请输入病组编码', trigger: 'blur' }],
  groupName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
}
function onPayTypeChange() {
  // 切换支付方式时给出建议默认值，避免权重/分值混用
  if (groupForm.payType === 2) {
    groupForm.pointValue = 1000
    groupForm.pointCost = 50
  } else {
    groupForm.weight = 1
    groupForm.rate = 3000
  }
}
function openGroup(row) {
  groupRow.value = row || null
  Object.assign(groupForm, row
    ? { id: row.id, groupCode: row.groupCode, groupName: row.groupName, payType: row.payType, weight: row.weight, rate: row.rate, pointValue: row.pointValue, pointCost: row.pointCost, avgCost: row.avgCost, status: row.status, description: row.description }
    : { id: null, groupCode: '', groupName: '', payType: 1, weight: 1, rate: 3000, pointValue: 1000, pointCost: 50, avgCost: 5000, status: 1, description: '' })
  groupVisible.value = true
}
async function saveGroup() {
  await groupRef.value.validate()
  saving.value = true
  try {
    const res = await drgGroupUpsert({ ...groupForm })
    if (res.data.ok) { ElMessage.success('保存成功'); groupVisible.value = false; loadGroup() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function toggleGroup(row) {
  const res = await drgGroupStatus({ id: row.id, status: row.status === 1 ? 0 : 1 })
  if (res.data.ok) { ElMessage.success('已更新'); loadGroup() }
  else ElMessage.warning(res.data.error || '失败')
}

// ---------------- 病例监管 ----------------
const cases = ref([])
const caseTotal = ref(0)
const caseLoading = ref(false)
const cq = reactive({ keyword: '', payType: '', status: '', page: 1, size: 10 })
const onlyOver = ref(false)

async function loadCase() {
  caseLoading.value = true
  const res = await drgCaseListPage({ page: cq.page, size: cq.size, keyword: cq.keyword || undefined, payType: cq.payType || undefined, status: cq.status === '' ? undefined : cq.status, overBudget: onlyOver.value ? true : undefined })
  caseLoading.value = false
  cases.value = res.data.list
  caseTotal.value = res.data.total
}
function searchCase() { cq.page = 1; loadCase() }
function resetCase() { cq.keyword = ''; cq.payType = ''; cq.status = ''; onlyOver.value = false; cq.page = 1; loadCase() }

async function doGroup() {
  grouping.value = true
  const res = await drgCaseGroup()
  grouping.value = false
  if (res.data.ok) {
    ElMessage.success(`已入组 ${res.data.matched ?? 0} 例`)
    loadCase(); loadStats()
  } else ElMessage.warning(res.data.error || '失败')
}
async function special(row) {
  try {
    const { value } = await ElMessageBox.prompt('请输入特例单议说明（高倍率/超长住院/使用新药新技术等）', '特例单议', { inputType: 'textarea' })
    const res = await drgCaseHandle({ id: row.id, action: 4, opinion: value })
    if (res.data.ok) { ElMessage.success('已标记特例单议'); loadCase(); loadStats() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}
async function restore(row) {
  const res = await drgCaseHandle({ id: row.id, action: 1, opinion: '恢复正常监管状态' })
  if (res.data.ok) { ElMessage.success('已恢复'); loadCase(); loadStats() }
  else ElMessage.warning(res.data.error || '失败')
}

onMounted(() => { loadGroup(); loadCase(); loadStats() })
</script>
