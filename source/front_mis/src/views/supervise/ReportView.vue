<template>
  <div class="space-y-3">
    <!-- 查询 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="举报编号/机构/举报人/内容" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="办理状态" clearable style="width:100%">
              <el-option v-for="(v, k) in REPORT_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" size="small" @click="openAdd">登记举报</el-button>
        </div>
      </div>
    </div>

    <!-- 举报列表 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">举报投诉受理（受理→核查→办结→奖励发放）</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="reportNo" label="举报编号" width="140" />
        <el-table-column prop="reporterName" label="举报人" width="100" />
        <el-table-column prop="reportedMedinsName" label="被举报机构" width="160" show-overflow-tooltip />
        <el-table-column prop="reportedSubject" label="被举报对象" width="110" show-overflow-tooltip />
        <el-table-column prop="problemType" label="问题类型" width="100">
          <template #default="{ row }"><el-tag size="small" type="info">{{ row.problemType || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="content" label="举报内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="奖励金额" width="110" align="right">
          <template #default="{ row }">{{ row.rewardAmount ? fmt(row.rewardAmount) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag size="small" :type="reportStatusType(row.status)">{{ reportStatusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="举报时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button link type="primary" @click="handle(row, 2)">受理核查</el-button>
              <el-button link type="info" @click="handle(row, 3)">直接办结</el-button>
            </template>
            <template v-else-if="row.status === 2">
              <el-button link type="success" @click="handle(row, 3)">核查办结</el-button>
            </template>
            <template v-else-if="row.status === 3">
              <el-button link type="danger" @click="openReward(row)">发放奖励</el-button>
            </template>
            <span v-else class="text-[12px] text-gray-400">已办结并奖励</span>
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

    <!-- 登记举报 -->
    <el-dialog v-model="addVisible" title="登记举报投诉" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="举报人" prop="reporterName">
          <el-input v-model="form.reporterName" placeholder="可匿名，填写姓名便于反馈" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.reporterContact" placeholder="手机号/邮箱（隐私保护，仅核查人可见）" />
        </el-form-item>
        <el-form-item label="被举报机构" prop="reportedMedinsName">
          <el-input v-model="form.reportedMedinsName" />
        </el-form-item>
        <el-form-item label="机构编码">
          <el-input v-model="form.reportedMedinsCode" />
        </el-form-item>
        <el-form-item label="被举报对象">
          <el-input v-model="form.reportedSubject" placeholder="涉事医师/参保人等" />
        </el-form-item>
        <el-form-item label="问题类型" prop="problemType">
          <el-select v-model="form.problemType" style="width:100%">
            <el-option v-for="(v, k) in PROBLEM_TYPES" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="举报内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请描述时间、地点、经过等" />
        </el-form-item>
        <el-form-item label="附件地址">
          <el-input v-model="form.attachUrl" placeholder="图片/凭证链接（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">提交</el-button>
      </template>
    </el-dialog>

    <!-- 发放奖励 -->
    <el-dialog v-model="rewardVisible" title="举报奖励发放" width="460px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="举报编号">{{ rewardRow?.reportNo }}</el-form-item>
        <el-form-item label="举报人">{{ rewardRow?.reporterName || '匿名举报人' }}</el-form-item>
        <el-form-item label="奖励金额">
          <el-input-number v-model="rewardAmount" :min="0" :precision="2" :step="100" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rewardVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="onReward">确认发放</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportListPage, reportSubmit, reportProcess, reportReward } from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import { REPORT_STATUS, PROBLEM_TYPES, reportStatusText, reportStatusType } from './reportMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', status: '', page: 1, size: 10 })

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  const res = await reportListPage({ page: q.page, size: q.size, keyword: q.keyword || undefined, status: q.status || undefined })
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.keyword = ''; q.status = ''; q.page = 1; load() }

// 登记
const addVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  reporterName: '', reporterContact: '', reportedMedinsCode: '', reportedMedinsName: '',
  reportedSubject: '', problemType: '违规收费', content: '', attachUrl: '',
})
const rules = {
  reporterName: [{ required: true, message: '请输入举报人', trigger: 'blur' }],
  reportedMedinsName: [{ required: true, message: '请输入被举报机构', trigger: 'blur' }],
  problemType: [{ required: true, message: '请选择问题类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入举报内容', trigger: 'blur' }],
}
function openAdd() {
  Object.assign(form, {
    reporterName: '', reporterContact: '', reportedMedinsCode: '', reportedMedinsName: '',
    reportedSubject: '', problemType: '违规收费', content: '', attachUrl: '',
  })
  addVisible.value = true
}
async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    await reportSubmit({ ...form })
    ElMessage.success('举报已登记受理')
    addVisible.value = false
    load()
  } catch (e) {
    // 后端 message 已全局提示
  } finally {
    saving.value = false
  }
}

// 受理/核查/办结
async function handle(row, status) {
  const label = { 2: '受理核查', 3: '核查办结' }[status]
  try {
    const { value } = await ElMessageBox.prompt(`请输入${label}意见`, label, { inputType: 'textarea' })
    const res = await reportProcess({ id: row.id, status, handleOpinion: value })
    if (res.data.ok) { ElMessage.success(`已${label}`); load() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}

// 奖励
const rewardVisible = ref(false)
const rewardRow = ref(null)
const rewardAmount = ref(500)
function openReward(row) {
  rewardRow.value = row
  rewardAmount.value = 500
  rewardVisible.value = true
}
async function onReward() {
  saving.value = true
  try {
    const res = await reportReward({ id: rewardRow.value.id, rewardAmount: rewardAmount.value })
    if (res.data.ok) { ElMessage.success('奖励已发放'); rewardVisible.value = false; load() }
    else ElMessage.warning(res.data.error || '失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
