<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-select v-model="q.medinsType" placeholder="机构类型" clearable style="width:100%">
              <el-option v-for="(v, k) in MEDINS_TYPES" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.agreementStatus" placeholder="协议状态" clearable style="width:100%">
              <el-option v-for="(v, k) in AGREEMENT_STATUS" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" size="small" @click="openEdit()">新增协议</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">定点医药机构协议</div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="medinsCode" label="机构编码" width="140" />
        <el-table-column prop="medinsName" label="机构名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="机构类型" width="100">
          <template #default="{ row }">{{ medinsTypeText(row.medinsType) }}</template>
        </el-table-column>
        <el-table-column prop="agreementNo" label="协议编号" width="140" />
        <el-table-column label="签订日期" width="120">
          <template #default="{ row }">{{ row.signDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag size="small" :type="agreementStatusType(row.agreementStatus)">{{ agreementStatusText(row.agreementStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="年度考核" width="90" align="center">
          <template #default="{ row }">{{ row.annualScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="success" :disabled="row.agreementStatus === 1" @click="sign(row)">签订</el-button>
            <el-button link type="danger" :disabled="row.agreementStatus !== 1" @click="terminate(row)">解除</el-button>
            <el-button link type="warning" :disabled="row.agreementStatus !== 1" @click="assess(row)">考核</el-button>
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

    <AgreementEdit v-model="editVisible" :row="currentRow" @saved="load" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agreementListPage, agreementStatus, agreementAssess } from '@/api/insurance'
import { PAGE_SIZES } from '@/lib/pagination'
import AgreementEdit from './AgreementEdit.vue'
import { MEDINS_TYPES, AGREEMENT_STATUS, medinsTypeText, agreementStatusText, agreementStatusType } from './agreementMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ medinsType: '', agreementStatus: '', page: 1, size: 10 })

const editVisible = ref(false)
const currentRow = ref(null)
function openEdit(row) {
  currentRow.value = row || null
  editVisible.value = true
}

async function load() {
  loading.value = true
  const res = await agreementListPage(q)
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.medinsType = ''; q.agreementStatus = ''; q.page = 1; load() }

async function sign(row) {
  const res = await agreementStatus({ id: row.id, agreementStatus: 1, remark: '正式签订服务协议' })
  if (res.data.ok) { ElMessage.success('已签订'); load() } else ElMessage.warning(res.data.error || '失败')
}
async function terminate(row) {
  await ElMessageBox.confirm('确认解除该机构服务协议？', '提示', { type: 'warning' })
  const res = await agreementStatus({ id: row.id, agreementStatus: 2, remark: '解除服务协议' })
  if (res.data.ok) { ElMessage.success('已解除'); load() } else ElMessage.warning(res.data.error || '失败')
}
async function assess(row) {
  const { value } = await ElMessageBox.prompt('请输入年度考核得分（0-100）', '年度考核', { inputValue: row.annualScore || 90 })
  const res = await agreementAssess(row.id, Number(value))
  if (res.data.ok) { ElMessage.success('已记录考核得分'); load() } else ElMessage.warning(res.data.error || '失败')
}
onMounted(load)
</script>
