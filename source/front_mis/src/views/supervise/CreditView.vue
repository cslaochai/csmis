<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-select v-model="q.subjectType" placeholder="主体类型" clearable style="width:100%">
              <el-option v-for="(v, k) in SUBJECT_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.punishType" placeholder="奖惩类型" clearable style="width:100%">
              <el-option v-for="(v, k) in PUNISH_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" size="small" @click="openAdd">新增主体</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">信用管理（"驾照式"记分 / 联合奖惩）</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column label="主体类型" width="100">
          <template #default="{ row }">{{ subjectTypeText(row.subjectType) }}</template>
        </el-table-column>
        <el-table-column prop="subjectCode" label="主体编码" width="140" />
        <el-table-column prop="subjectName" label="主体名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="信用等级" width="90" align="center">
          <template #default="{ row }"><el-tag size="small" :type="levelType(row.creditLevel)">{{ creditLevelText(row.creditLevel) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center"><template #default="{ row }">{{ row.score ?? '-' }}</template></el-table-column>
        <el-table-column label="违规记分" width="90" align="center"><template #default="{ row }">{{ row.violationCount ?? 0 }}</template></el-table-column>
        <el-table-column label="奖惩" width="100">
          <template #default="{ row }"><el-tag size="small" :type="punishTypeType(row.punishType)">{{ punishTypeText(row.punishType) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openRecord(row)">记分</el-button>
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

    <!-- 新增 / 编辑 -->
    <el-dialog v-model="editVisible" :title="row ? '编辑主体' : '新增主体'" width="560px" append-to-body>
      <el-form ref="editRef" :model="editForm" :rules="rules" label-width="100px">
        <el-form-item label="主体类型" prop="subjectType">
          <el-radio-group v-model="editForm.subjectType">
            <el-radio :value="1">定点机构</el-radio>
            <el-radio :value="2">从业人员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主体编码" prop="subjectCode">
          <el-input v-model="editForm.subjectCode" :disabled="!!row" placeholder="机构编码 / 工号" />
        </el-form-item>
        <el-form-item label="主体名称" prop="subjectName">
          <el-input v-model="editForm.subjectName" />
        </el-form-item>
        <el-form-item label="信用等级">
          <el-select v-model="editForm.creditLevel" style="width:100%">
            <el-option v-for="(v, k) in CREDIT_LEVELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分"><el-input-number v-model="editForm.score" :min="0" :max="100" style="width:100%" /></el-form-item>
        <el-form-item label="奖惩类型">
          <el-select v-model="editForm.punishType" style="width:100%">
            <el-option v-for="(v, k) in PUNISH_TYPES" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 记分 / 评价 -->
    <el-dialog v-model="recordVisible" title="记分 / 评价" width="480px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="当前评分"><span>{{ current.score }}</span></el-form-item>
        <el-form-item label="新评分"><el-input-number v-model="recordForm.score" :min="0" :max="100" style="width:100%" /></el-form-item>
        <el-form-item label="信用等级">
          <el-select v-model="recordForm.creditLevel" style="width:100%">
            <el-option v-for="(v, k) in CREDIT_LEVELS" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="违规记分"><el-input-number v-model="recordForm.violationCount" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="奖惩类型">
          <el-select v-model="recordForm.punishType" style="width:100%">
            <el-option v-for="(v, k) in PUNISH_TYPES" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRecord">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listCredits, upsertCredit, recordCredit } from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { SUBJECT_TYPES, PUNISH_TYPES, CREDIT_LEVELS, subjectTypeText, punishTypeText, creditLevelText, punishTypeType } from './creditMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ subjectType: '', punishType: '', page: 1, size: 10 })

const editVisible = ref(false)
const row = ref(null)
const editRef = ref()
const saving = ref(false)
const editForm = reactive({ id: null, subjectType: 1, subjectCode: '', subjectName: '', creditLevel: 'B', score: 80, punishType: 0, remark: '' })
const rules = {
  subjectType: [{ required: true, message: '请选择主体类型', trigger: 'change' }],
  subjectCode: [{ required: true, message: '请输入主体编码', trigger: 'blur' }],
  subjectName: [{ required: true, message: '请输入主体名称', trigger: 'blur' }],
}

const recordVisible = ref(false)
const current = ref({})
const recordForm = reactive({ score: 80, creditLevel: 'B', violationCount: 0, punishType: 0 })

function levelType(l) { return { A: 'success', B: 'info', C: 'warning', D: 'danger' }[l] || 'info' }

async function load() {
  loading.value = true
  const res = await listCredits(q)
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.subjectType = ''; q.punishType = ''; q.page = 1; load() }

function openAdd() { row.value = null; Object.assign(editForm, { id: null, subjectType: 1, subjectCode: '', subjectName: '', creditLevel: 'B', score: 80, punishType: 0, remark: '' }); editVisible.value = true }
function openEdit(r) { row.value = r; Object.assign(editForm, { id: r.id, subjectType: r.subjectType, subjectCode: r.subjectCode, subjectName: r.subjectName, creditLevel: r.creditLevel, score: r.score, punishType: r.punishType, remark: r.remark }); editVisible.value = true }
async function saveEdit() {
  await editRef.value.validate()
  saving.value = true
  try {
    const res = await upsertCredit({ ...editForm })
    if (res.data.ok) { ElMessage.success('保存成功'); editVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
function openRecord(r) { current.value = r; Object.assign(recordForm, { score: r.score ?? 80, creditLevel: r.creditLevel || 'B', violationCount: r.violationCount ?? 0, punishType: r.punishType ?? 0 }); recordVisible.value = true }
async function saveRecord() {
  saving.value = true
  try {
    const res = await recordCredit({ id: current.value.id, ...recordForm })
    if (res.data.ok) { ElMessage.success('已记录'); recordVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
onMounted(load)
</script>
