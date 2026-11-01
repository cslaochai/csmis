<template>
  <div class="space-y-3">
    <!-- 抽查任务 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="tq.keyword" placeholder="任务编号/名称" clearable @keyup.enter="searchTask" />
          </div>
          <div class="q-item">
            <el-select v-model="tq.checkType" placeholder="检查类型" clearable style="width:100%">
              <el-option v-for="(v, k) in CHECK_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="tq.status" placeholder="任务状态" clearable style="width:100%">
              <el-option v-for="(v, k) in TASK_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchTask">查询</el-button>
          <el-button @click="resetTask">重置</el-button>
          <el-button type="success" size="small" @click="openTask()">新建抽查任务</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">双随机抽查任务（随机抽机构 + 随机抽人员，结果公开）</div>
      <el-table :data="tasks" size="small" v-loading="taskLoading">
        <el-table-column prop="taskNo" label="任务编号" width="150" />
        <el-table-column prop="taskName" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="检查类型" width="110">
          <template #default="{ row }"><el-tag size="small" type="primary">{{ checkTypeText(row.checkType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="抽取机构" width="100" align="center">
          <template #default="{ row }">{{ row.drawCount ?? 0 }} / {{ row.planCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="抽取人员" width="100" align="center">
          <template #default="{ row }">{{ row.checkerCount ?? 0 }} / {{ row.planCheckerCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="负责部门" width="130" />
        <el-table-column prop="drawTime" label="抽取时间" width="165" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="taskStatusType(row.status)">{{ taskStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTask(row)">编辑</el-button>
            <el-button link type="warning" :disabled="row.status === 3" @click="doDraw(row)">随机抽取</el-button>
            <el-button link type="success" :disabled="row.status !== 2" @click="doPublish(row)">结果公开</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="tq.size" :current-page="tq.page" :total="taskTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { tq.page = p; loadTask() }"
          @size-change="(s) => { tq.size = s; tq.page = 1; loadTask() }"
        />
      </div>
    </div>

    <!-- 抽取结果 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="rq.keyword" placeholder="对象名称/编码/机构" clearable @keyup.enter="searchResult" />
          </div>
          <div class="q-item">
            <el-select v-model="rq.targetType" placeholder="对象类型" clearable style="width:100%">
              <el-option v-for="(v, k) in TARGET_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="rq.result" placeholder="检查结果" clearable style="width:100%">
              <el-option v-for="(v, k) in CHECK_RESULTS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="rq.publicFlag" placeholder="公开状态" clearable style="width:100%">
              <el-option label="未公开" :value="0" />
              <el-option label="已公开" :value="1" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="searchResult">查询</el-button>
          <el-button @click="resetResult">重置</el-button>
        </div>
      </div>
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">抽取名单与检查结果（一公开）</div>
      <el-table :data="results" size="small" v-loading="resultLoading">
        <el-table-column prop="targetType" label="对象类型" width="110">
          <template #default="{ row }"><el-tag size="small" :type="row.targetType === 1 ? 'primary' : 'warning'">{{ targetTypeText(row.targetType) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="targetName" label="对象名称" width="180" show-overflow-tooltip />
        <el-table-column prop="targetCode" label="对象编码" width="130" />
        <el-table-column prop="orgName" label="所属机构/部门" width="160" show-overflow-tooltip />
        <el-table-column prop="taskName" label="所属任务" width="170" show-overflow-tooltip />
        <el-table-column label="检查结果" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" :type="checkResultType(row.result)">{{ checkResultText(row.result) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="checkResult" label="检查情况" min-width="200" show-overflow-tooltip />
        <el-table-column label="公开" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="publicType(row.publicFlag)">{{ publicText(row.publicFlag) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.result === 1" link type="primary" @click="openHandle(row)">录入结果</el-button>
            <el-button v-else link type="info" @click="openHandle(row)">修改/公开</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex justify-end mt-3">
        <el-pagination
          :page-sizes="PAGE_SIZES" :page-size="rq.size" :current-page="rq.page" :total="resultTotal"
          layout="total, sizes, prev, pager, next"
          @current-change="(p) => { rq.page = p; loadResult() }"
          @size-change="(s) => { rq.size = s; rq.page = 1; loadResult() }"
        />
      </div>
    </div>

    <!-- 任务编辑 -->
    <el-dialog v-model="taskVisible" :title="taskRow ? '编辑抽查任务' : '新建抽查任务'" width="540px" append-to-body>
      <el-form ref="taskRef" :model="taskForm" :rules="taskRules" label-width="110px">
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="taskForm.taskName" />
        </el-form-item>
        <el-form-item label="检查类型" prop="checkType">
          <el-select v-model="taskForm.checkType" style="width:100%">
            <el-option v-for="(v, k) in CHECK_TYPES" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="抽取机构数">
          <el-input-number v-model="taskForm.planCount" :min="1" :max="50" style="width:100%" />
        </el-form-item>
        <el-form-item label="抽取检查人员数">
          <el-input-number v-model="taskForm.planCheckerCount" :min="1" :max="20" style="width:100%" />
        </el-form-item>
        <el-form-item label="负责部门">
          <el-input v-model="taskForm.deptName" placeholder="如 基金监管科" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="taskForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入检查结果 -->
    <el-dialog v-model="handleVisible" title="录入检查结果并公开" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="对象">{{ handleRow?.targetName }}（{{ targetTypeText(handleRow?.targetType) }}）</el-form-item>
        <el-form-item label="检查结果">
          <el-select v-model="handleForm.result" style="width:100%">
            <el-option v-for="(v, k) in CHECK_RESULTS" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="检查情况">
          <el-input v-model="handleForm.checkResult" type="textarea" :rows="3" placeholder="存在的问题 / 整改要求" />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-switch v-model="handleForm.publicFlag" :active-value="1" :inactive-value="0" active-text="公开" inactive-text="不公开" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveHandle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  randomTaskListPage, randomTaskUpsert, randomTaskDraw, randomTaskPublish,
  randomResultListPage, randomResultHandle,
} from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import {
  CHECK_TYPES, TARGET_TYPES, CHECK_RESULTS, TASK_STATUS,
  checkTypeText, targetTypeText, checkResultText, taskStatusText, checkResultType, taskStatusType, publicType, publicText,
} from './randomMeta'

const tasks = ref([])
const taskTotal = ref(0)
const taskLoading = ref(false)
const tq = reactive({ keyword: '', checkType: '', status: '', page: 1, size: 10 })

async function loadTask() {
  taskLoading.value = true
  const res = await randomTaskListPage({ page: tq.page, size: tq.size, keyword: tq.keyword || undefined, checkType: tq.checkType || undefined, status: tq.status === '' ? undefined : tq.status })
  taskLoading.value = false
  tasks.value = res.data.list
  taskTotal.value = res.data.total
}
function searchTask() { tq.page = 1; loadTask() }
function resetTask() { tq.keyword = ''; tq.checkType = ''; tq.status = ''; tq.page = 1; loadTask() }

const taskVisible = ref(false)
const taskRow = ref(null)
const taskRef = ref()
const saving = ref(false)
const taskForm = reactive({ id: null, taskName: '', checkType: 1, planCount: 5, planCheckerCount: 2, deptName: '', remark: '' })
const taskRules = { taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }] }
function openTask(row) {
  taskRow.value = row || null
  Object.assign(taskForm, row
    ? { id: row.id, taskName: row.taskName, checkType: row.checkType, planCount: row.planCount, planCheckerCount: row.planCheckerCount, deptName: row.deptName, remark: row.remark }
    : { id: null, taskName: '', checkType: 1, planCount: 5, planCheckerCount: 2, deptName: '', remark: '' })
  taskVisible.value = true
}
async function saveTask() {
  await taskRef.value.validate()
  saving.value = true
  try {
    const res = await randomTaskUpsert({ ...taskForm })
    if (res.data.ok) { ElMessage.success('保存成功'); taskVisible.value = false; loadTask() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function doDraw(row) {
  try {
    await ElMessageBox.confirm(`将从已签订协议的定点机构中随机抽取 ${row.planCount} 个对象、从在职人员中随机抽取 ${row.planCheckerCount} 名检查人员，确认抽取？`, '双随机抽取', { type: 'warning' })
    const res = await randomTaskDraw(row.id)
    if (res.data.ok) { ElMessage.success(`抽取完成，共产生 ${res.data.matched ?? 0} 条名单`); loadTask(); loadResult() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}
async function doPublish(row) {
  const res = await randomTaskPublish(row.id)
  if (res.data.ok) { ElMessage.success(`已公开 ${res.data.matched ?? 0} 条检查结果`); loadTask(); loadResult() }
  else ElMessage.warning(res.data.error || '失败')
}

const results = ref([])
const resultTotal = ref(0)
const resultLoading = ref(false)
const rq = reactive({ keyword: '', targetType: '', result: '', publicFlag: '', page: 1, size: 10 })

async function loadResult() {
  resultLoading.value = true
  const res = await randomResultListPage({ page: rq.page, size: rq.size, keyword: rq.keyword || undefined, targetType: rq.targetType || undefined, result: rq.result === '' ? undefined : rq.result, publicFlag: rq.publicFlag === '' ? undefined : rq.publicFlag })
  resultLoading.value = false
  results.value = res.data.list
  resultTotal.value = res.data.total
}
function searchResult() { rq.page = 1; loadResult() }
function resetResult() { rq.keyword = ''; rq.targetType = ''; rq.result = ''; rq.publicFlag = ''; rq.page = 1; loadResult() }

const handleVisible = ref(false)
const handleRow = ref(null)
const handleForm = reactive({ result: 2, checkResult: '', publicFlag: 1 })
function openHandle(row) {
  handleRow.value = row
  handleForm.result = row.result
  handleForm.checkResult = row.checkResult || ''
  handleForm.publicFlag = row.publicFlag ?? 1
  handleVisible.value = true
}
async function saveHandle() {
  saving.value = true
  try {
    const res = await randomResultHandle({ id: handleRow.value.id, ...handleForm })
    if (res.data.ok) { ElMessage.success('已保存'); handleVisible.value = false; loadResult() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

onMounted(() => { loadTask(); loadResult() })
</script>
