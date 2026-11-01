<template>
  <div class="space-y-3">
    <!-- 模型库 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="mq.keyword" placeholder="模型编码/名称" clearable @keyup.enter="searchModel" />
          </div>
          <div class="q-item">
            <el-select v-model="mq.category" placeholder="欺诈类别" clearable style="width:100%">
              <el-option v-for="(v, k) in FRAUD_CATEGORIES" :key="k" :label="v" :value="k" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="mq.status" placeholder="状态" clearable style="width:100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchModel">查询</el-button>
          <el-button @click="resetModel">重置</el-button>
          <el-button type="success" size="small" @click="openModel">新增模型</el-button>
          <el-button type="warning" size="small" @click="runAll">运行全部模型</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">大数据反欺诈模型库</div>
      <el-table :data="models" size="small" v-loading="modelLoading">
        <el-table-column prop="modelCode" label="模型编码" width="130" />
        <el-table-column prop="modelName" label="模型名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类别" width="130">
          <template #default="{ row }">{{ categoryText(row.category) }}</template>
        </el-table-column>
        <el-table-column label="风险" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="riskType(row.riskLevel)">{{ riskText(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="algorithm" label="算法/规则" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openModel(row)">编辑</el-button>
            <el-button link type="success" @click="runOne(row)">运行</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleModel(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination :page-sizes="PAGE_SIZES" :page-size="mq.size" :current-page="mq.page" :total="modelTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { mq.page = p; loadModel() }"
          @size-change="(s) => { mq.size = s; mq.page = 1; loadModel() }" />
      </div>
    </div>

    <!-- 线索 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="cq.keyword" placeholder="线索号/机构/参保人" clearable @keyup.enter="searchClue" />
          </div>
          <div class="q-item">
            <el-select v-model="cq.status" placeholder="处置状态" clearable style="width:100%">
              <el-option v-for="(v, k) in CLUE_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="cq.riskLevel" placeholder="风险等级" clearable style="width:100%">
              <el-option v-for="(v, k) in RISK_LEVELS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchClue">查询</el-button>
          <el-button @click="resetClue">重置</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">反欺诈线索（核查处置闭环）</div>
      <el-table :data="clues" size="small" v-loading="clueLoading">
        <el-table-column prop="clueNo" label="线索号" width="150" />
        <el-table-column prop="modelName" label="来源模型" width="150" show-overflow-tooltip />
        <el-table-column prop="medinsName" label="涉及机构" width="130" show-overflow-tooltip />
        <el-table-column prop="insuredName" label="涉及参保人" width="100" />
        <el-table-column prop="bizNo" label="关联单号" width="130" show-overflow-tooltip />
        <el-table-column label="涉及金额" width="120" align="right">
          <template #default="{ row }">{{ fmt(row.involveAmount) }}</template>
        </el-table-column>
        <el-table-column prop="clueDetail" label="线索详情" min-width="260" show-overflow-tooltip />
        <el-table-column label="风险" width="80" align="center">
          <template #default="{ row }"><el-tag size="small" :type="riskType(row.riskLevel)">{{ riskText(row.riskLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="clueStatusType(row.status)">{{ clueStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button link type="success" @click="handleClue(row, 2)">确认</el-button>
              <el-button link type="info" @click="handleClue(row, 3)">排除</el-button>
              <el-button link type="danger" @click="handleClue(row, 4)">移送追回</el-button>
            </template>
            <span v-else class="text-[12px] text-gray-400">已处置</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="clueTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { cq.page = p; loadClue() }"
          @size-change="(s) => { cq.size = s; cq.page = 1; loadClue() }" />
      </div>
    </div>

    <!-- 模型新增/编辑 -->
    <el-dialog v-model="modelVisible" :title="modelRow ? '编辑模型' : '新增模型'" width="560px" append-to-body>
      <el-form ref="modelRef" :model="modelForm" :rules="modelRules" label-width="100px">
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="modelForm.modelCode" :disabled="!!modelRow" placeholder="如 FRAUD-001" />
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="modelForm.modelName" />
        </el-form-item>
        <el-form-item label="欺诈类别" prop="category">
          <el-select v-model="modelForm.category" style="width:100%">
            <el-option v-for="(v, k) in FRAUD_CATEGORIES" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="modelForm.riskLevel" style="width:100%">
            <el-option v-for="(v, k) in RISK_LEVELS" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="算法/规则">
          <el-input v-model="modelForm.algorithm" type="textarea" :rows="2" placeholder="如：近30天同一身份证跨机构高频就医" />
        </el-form-item>
        <el-form-item label="模型参数(JSON)">
          <el-input v-model="modelForm.params" type="textarea" :rows="2" placeholder='{"windowDays":30,"threshold":5}' />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="modelForm.status" style="width:100%">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="modelForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modelVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  fraudModelListPage, fraudModelUpsert, fraudModelStatus, fraudClueListPage, fraudClueRun, fraudClueHandle,
} from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { FRAUD_CATEGORIES, RISK_LEVELS, CLUE_STATUS, categoryText, riskText, clueStatusText, riskType, clueStatusType } from './fraudMeta'

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const models = ref([])
const modelTotal = ref(0)
const modelLoading = ref(false)
const mq = reactive({ keyword: '', category: '', status: '', page: 1, size: 10 })

async function loadModel() {
  modelLoading.value = true
  const res = await fraudModelListPage(mq)
  modelLoading.value = false
  models.value = res.data.list
  modelTotal.value = res.data.total
}
function searchModel() { mq.page = 1; loadModel() }
function resetModel() { mq.keyword = ''; mq.category = ''; mq.status = ''; mq.page = 1; loadModel() }

const clues = ref([])
const clueTotal = ref(0)
const clueLoading = ref(false)
const cq = reactive({ keyword: '', status: '', riskLevel: '', page: 1, size: 10 })

async function loadClue() {
  clueLoading.value = true
  const res = await fraudClueListPage(cq)
  clueLoading.value = false
  clues.value = res.data.list
  clueTotal.value = res.data.total
}
function searchClue() { cq.page = 1; loadClue() }
function resetClue() { cq.keyword = ''; cq.status = ''; cq.riskLevel = ''; cq.page = 1; loadClue() }

// 模型
const modelVisible = ref(false)
const modelRow = ref(null)
const modelRef = ref()
const saving = ref(false)
const modelForm = reactive({ id: null, modelCode: '', modelName: '', category: '02', riskLevel: 2, algorithm: '', params: '', status: 1, description: '' })
const modelRules = {
  modelCode: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择欺诈类别', trigger: 'change' }],
}
function openModel(row) {
  modelRow.value = row || null
  Object.assign(modelForm, row
    ? { id: row.id, modelCode: row.modelCode, modelName: row.modelName, category: row.category, riskLevel: row.riskLevel, algorithm: row.algorithm, params: row.params, status: row.status, description: row.description }
    : { id: null, modelCode: '', modelName: '', category: '02', riskLevel: 2, algorithm: '', params: '', status: 1, description: '' })
  modelVisible.value = true
}
async function saveModel() {
  await modelRef.value.validate()
  saving.value = true
  try {
    const res = await fraudModelUpsert({ ...modelForm })
    if (res.data.ok) { ElMessage.success('保存成功'); modelVisible.value = false; loadModel() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function toggleModel(row) {
  const res = await fraudModelStatus({ id: row.id, status: row.status === 1 ? 0 : 1 })
  if (res.data.ok) { ElMessage.success('已更新'); loadModel() }
  else ElMessage.warning(res.data.error || '失败')
}
async function runAll() {
  const res = await fraudClueRun({})
  if (res.data.ok) { ElMessage.success(`已生成线索 ${res.data.matched ?? 0} 条`); loadClue() }
  else ElMessage.warning(res.data.error || '失败')
}
async function runOne(row) {
  const res = await fraudClueRun({ modelId: row.id })
  if (res.data.ok) { ElMessage.success(`模型「${row.modelName}」命中 ${res.data.matched ?? 0} 条`); loadClue() }
  else ElMessage.warning(res.data.error || '失败')
}

async function handleClue(row, status) {
  const label = { 2: '确认', 3: '排除', 4: '移送' }[status]
  try {
    const { value } = await ElMessageBox.prompt(`请输入${label}意见`, label, { inputType: 'textarea' })
    const res = await fraudClueHandle({ id: row.id, status, handleOpinion: value })
    if (res.data.ok) { ElMessage.success(`已${label}`); loadClue() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}

onMounted(() => { loadModel(); loadClue() })
</script>
