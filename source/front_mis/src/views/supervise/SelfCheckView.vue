<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="任务编号/名称/机构" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="任务状态" clearable style="width:100%">
              <el-option v-for="(v, k) in SELF_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" size="small" @click="openEdit()">下发自查任务</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">机构自查自纠（下发 → 自查 → 上报 → 整改 → 复核办结）</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="taskNo" label="任务编号" width="150" />
        <el-table-column prop="medinsName" label="定点机构" width="180" show-overflow-tooltip />
        <el-table-column prop="taskName" label="任务名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="checkScope" label="自查范围" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deadline" label="整改截止" width="110" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" :type="selfStatusType(row.status)">{{ selfStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reviewer" label="复核人" width="100" />
        <el-table-column prop="reviewOpinion" label="复核意见" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" link type="primary" @click="doStart(row)">开始自查</el-button>
            <el-button v-if="row.status === 2 || row.status === 4" link type="warning" @click="openSubmit(row)">
              {{ row.status === 4 ? '整改上报' : '自查上报' }}
            </el-button>
            <el-button v-if="row.status === 3 || row.status === 4" link type="success" @click="openReview(row)">复核</el-button>
            <span v-if="row.status === 5" class="text-[12px] text-gray-400">已办结</span>
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

    <!-- 下发 / 编辑 -->
    <el-dialog v-model="editVisible" :title="editRow ? '编辑自查任务' : '下发自查自纠任务'" width="540px" append-to-body>
      <el-form ref="editRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="定点机构" prop="medinsName">
          <el-input v-model="form.medinsName" />
        </el-form-item>
        <el-form-item label="机构编码">
          <el-input v-model="form.medinsCode" />
        </el-form-item>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" />
        </el-form-item>
        <el-form-item label="自查范围">
          <el-input v-model="form.checkScope" type="textarea" :rows="3" placeholder="如：2026年10月医保基金使用自查（含物价执行、串换项目、慢特病用药）" />
        </el-form-item>
        <el-form-item label="整改截止">
          <el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 上报 -->
    <el-dialog v-model="submitVisible" :title="submitRow?.status === 4 ? '整改结果上报' : '自查结果上报'" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="机构">{{ submitRow?.medinsName }}</el-form-item>
        <el-form-item label="上报内容">
          <el-input v-model="submitContent" type="textarea" :rows="5" placeholder="自查发现的问题、涉及金额、已采取措施等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">提交上报</el-button>
      </template>
    </el-dialog>

    <!-- 复核 -->
    <el-dialog v-model="reviewVisible" title="自查自纠复核" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="机构">{{ reviewRow?.medinsName }}</el-form-item>
        <el-form-item label="复核结论">
          <el-radio-group v-model="reviewPassed">
            <el-radio :value="true">通过，办结</el-radio>
            <el-radio :value="false">不通过，退回整改</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="复核意见">
          <el-input v-model="reviewOpinion" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onReview">提交复核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { selfCheckListPage, selfCheckUpsert, selfCheckStart, selfCheckSubmit, selfCheckReview } from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { SELF_STATUS, selfStatusText, selfStatusType } from './randomMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  const res = await selfCheckListPage({ page: q.page, size: q.size, keyword: q.keyword || undefined, status: q.status === '' ? undefined : q.status })
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.keyword = ''; q.status = ''; q.page = 1; load() }

const editVisible = ref(false)
const editRow = ref(null)
const editRef = ref()
const saving = ref(false)
const form = reactive({ id: null, medinsCode: '', medinsName: '', taskName: '', checkScope: '', deadline: '', remark: '' })
const rules = {
  medinsName: [{ required: true, message: '请输入定点机构', trigger: 'blur' }],
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
}
function openEdit(row) {
  editRow.value = row || null
  Object.assign(form, row
    ? { id: row.id, medinsCode: row.medinsCode, medinsName: row.medinsName, taskName: row.taskName, checkScope: row.checkScope, deadline: row.deadline, remark: row.remark }
    : { id: null, medinsCode: '', medinsName: '', taskName: '', checkScope: '', deadline: '', remark: '' })
  editVisible.value = true
}
async function onSave() {
  await editRef.value.validate()
  saving.value = true
  try {
    const res = await selfCheckUpsert({ ...form })
    if (res.data.ok) { ElMessage.success('已下发'); editVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function doStart(row) {
  const res = await selfCheckStart(row.id)
  if (res.data.ok) { ElMessage.success('已开始自查'); load() }
  else ElMessage.warning(res.data.error || '失败')
}

const submitVisible = ref(false)
const submitRow = ref(null)
const submitContent = ref('')
function openSubmit(row) {
  submitRow.value = row
  submitContent.value = row.submitContent || ''
  submitVisible.value = true
}
async function onSubmit() {
  if (!submitContent.value.trim()) { ElMessage.warning('请填写上报内容'); return }
  saving.value = true
  try {
    const res = await selfCheckSubmit({ id: submitRow.value.id, content: submitContent.value })
    if (res.data.ok) { ElMessage.success('已上报'); submitVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

const reviewVisible = ref(false)
const reviewRow = ref(null)
const reviewPassed = ref(true)
const reviewOpinion = ref('')
function openReview(row) {
  reviewRow.value = row
  reviewPassed.value = true
  reviewOpinion.value = row.reviewOpinion || ''
  reviewVisible.value = true
}
async function onReview() {
  saving.value = true
  try {
    const res = await selfCheckReview({ id: reviewRow.value.id, passed: reviewPassed.value, opinion: reviewOpinion.value })
    if (res.data.ok) { ElMessage.success('复核完成'); reviewVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

onMounted(load)
</script>
