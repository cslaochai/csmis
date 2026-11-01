<template>
  <div class="space-y-3">
    <el-tabs v-model="tab" class="his-card p-3" @tab-change="onTabChange">
      <!-- ==================== 医保关系转移接续 ==================== -->
      <el-tab-pane label="医保关系转移接续" name="transfer">
        <div class="query-grid-wrap">
          <div class="query-grid">
            <div class="q-item">
              <el-input v-model="tq.keyword" placeholder="转移单号/姓名/身份证" clearable @keyup.enter="searchTransfer" />
            </div>
            <div class="q-item">
              <el-select v-model="tq.transferType" placeholder="转移类型" clearable style="width:100%">
                <el-option v-for="(v, k) in TRANSFER_TYPES" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
            <div class="q-item">
              <el-select v-model="tq.status" placeholder="办理状态" clearable style="width:100%">
                <el-option v-for="(v, k) in TRANSFER_STATUS" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
          </div>
          <div class="query-grid-ops">
            <el-button type="primary" @click="searchTransfer">查询</el-button>
            <el-button @click="resetTransfer">重置</el-button>
            <el-button type="success" size="small" @click="openTransfer()">发起转移申请</el-button>
          </div>
        </div>

        <el-table :data="transfers" size="small" v-loading="transferLoading" class="mt-3">
          <el-table-column prop="transferNo" label="转移单号" width="150" />
          <el-table-column prop="personName" label="参保人" width="100" />
          <el-table-column prop="idCard" label="身份证号" width="180" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">{{ transferTypeText(row.transferType) }}</template>
          </el-table-column>
          <el-table-column prop="fromRegion" label="转出统筹区" width="120" />
          <el-table-column prop="toRegion" label="转入统筹区" width="120" />
          <el-table-column label="缴费年限" width="100" align="right">
            <template #default="{ row }">{{ row.payYears ?? '-' }} 年</template>
          </el-table-column>
          <el-table-column label="结转余额" width="110" align="right">
            <template #default="{ row }">{{ row.fundBalance != null ? fmt(row.fundBalance) : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" :type="transferStatusType(row.status)">{{ transferStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" @click="doTransfer(row, 2)">受理</el-button>
              <el-button v-if="row.status === 2" link type="warning" @click="doTransfer(row, 3)">转出</el-button>
              <el-button v-if="row.status === 3" link type="warning" @click="doTransfer(row, 4)">转入</el-button>
              <el-button v-if="row.status === 4" link type="success" @click="doTransfer(row, 5)">完成转移</el-button>
              <span v-if="row.status === 5" class="text-[12px] text-gray-400">已结转</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end mt-3">
          <el-pagination
            :page-sizes="PAGE_SIZES" :page-size="tq.size" :current-page="tq.page" :total="transferTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="(p) => { tq.page = p; loadTransfer() }"
            @size-change="(s) => { tq.size = s; tq.page = 1; loadTransfer() }"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 个人账户管理 ==================== -->
      <el-tab-pane label="个人账户管理" name="account">
        <div class="query-grid-wrap">
          <div class="query-grid">
            <div class="q-item">
              <el-input v-model="aq.keyword" placeholder="流水号/姓名/身份证" clearable @keyup.enter="searchAccount" />
            </div>
            <div class="q-item">
              <el-select v-model="aq.txnType" placeholder="交易类型" clearable style="width:100%">
                <el-option v-for="(v, k) in TXN_TYPES" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
            <div class="q-item">
              <el-input v-model="accountIdCard" placeholder="查询账户余额（身份证号）" clearable
                @keyup.enter="loadAccountInfo" />
            </div>
          </div>
          <div class="query-grid-ops">
            <el-button type="primary" @click="searchAccount">查询</el-button>
            <el-button @click="resetAccount">重置</el-button>
            <el-button type="primary" size="small" @click="loadAccountInfo">查余额</el-button>
            <el-button type="warning" size="small" @click="openAccountOp()">支取/转出</el-button>
          </div>
        </div>

        <div v-if="accountInfoData.idCard" class="grid grid-cols-2 md:grid-cols-4 gap-3 mt-3">
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">账户余额</div>
            <div class="amount-strong">{{ fmt(accountInfoData.balance) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
            <div class="text-[12px] text-gray-500">累计划入</div>
            <div class="amount-strong !text-[#7C3AED]">{{ fmt(accountInfoData.totalIn) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-[#FEE2E2] text-center">
            <div class="text-[12px] text-gray-500">累计支出</div>
            <div class="amount-strong !text-[#DC2626]">{{ fmt(accountInfoData.totalOut) }}</div>
          </div>
          <div class="p-3 rounded-lg bg-gray-50 text-center">
            <div class="text-[12px] text-gray-500">流水笔数</div>
            <div class="amount-strong">{{ accountInfoData.txnCount ?? 0 }}</div>
          </div>
        </div>

        <el-table :data="accounts" size="small" v-loading="accountLoading" class="mt-3">
          <el-table-column prop="txnNo" label="流水号" width="180" />
          <el-table-column prop="personName" label="参保人" width="100" />
          <el-table-column prop="idCard" label="身份证号" width="180" />
          <el-table-column label="类型" width="110" align="center">
            <template #default="{ row }"><el-tag size="small" :type="txnTypeType(row.txnType)">{{ txnTypeText(row.txnType) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="发生金额" width="120" align="right">
            <template #default="{ row }">
              <span :class="row.amount < 0 ? 'text-[#DC2626]' : 'text-[#16A34A]'">{{ fmt(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="交易后余额" width="120" align="right">
            <template #default="{ row }">{{ fmt(row.balanceAfter) }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="事由" min-width="200" show-overflow-tooltip />
          <el-table-column prop="txnTime" label="交易时间" width="165" />
        </el-table>
        <div class="flex justify-end mt-3">
          <el-pagination
            :page-sizes="PAGE_SIZES" :page-size="aq.size" :current-page="aq.page" :total="accountTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="(p) => { aq.page = p; loadAccount() }"
            @size-change="(s) => { aq.size = s; aq.page = 1; loadAccount() }"
          />
        </div>
      </el-tab-pane>

      <!-- ==================== 门诊慢特病认定 ==================== -->
      <el-tab-pane label="门诊慢特病认定" name="chronic">
        <div class="query-grid-wrap">
          <div class="query-grid">
            <div class="q-item">
              <el-input v-model="cq.keyword" placeholder="认定单号/姓名/病种" clearable @keyup.enter="searchChronic" />
            </div>
            <div class="q-item">
              <el-select v-model="cq.diseaseLevel" placeholder="待遇类别" clearable style="width:100%">
                <el-option v-for="(v, k) in DISEASE_LEVELS" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
            <div class="q-item">
              <el-select v-model="cq.status" placeholder="审核状态" clearable style="width:100%">
                <el-option v-for="(v, k) in CHRONIC_STATUS" :key="k" :label="v" :value="Number(k)" />
              </el-select>
            </div>
          </div>
          <div class="query-grid-ops">
            <el-button type="primary" @click="searchChronic">查询</el-button>
            <el-button @click="resetChronic">重置</el-button>
            <el-button type="success" size="small" @click="openChronic()">登记认定申请</el-button>
          </div>
        </div>

        <el-table :data="chronics" size="small" v-loading="chronicLoading" class="mt-3">
          <el-table-column prop="recordNo" label="认定单号" width="150" />
          <el-table-column prop="personName" label="参保人" width="100" />
          <el-table-column prop="idCard" label="身份证号" width="180" />
          <el-table-column prop="diseaseName" label="病种" min-width="160" show-overflow-tooltip />
          <el-table-column label="待遇类别" width="130">
            <template #default="{ row }"><el-tag size="small" type="primary">{{ diseaseLevelText(row.diseaseLevel) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="doctor" label="申请医师" width="100" />
          <el-table-column prop="hospital" label="定点机构" width="160" show-overflow-tooltip />
          <el-table-column label="有效期" width="180">
            <template #default="{ row }">{{ row.validFrom || '-' }} ~ {{ row.validTo || '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90" align="center">
            <template #default="{ row }"><el-tag size="small" :type="chronicStatusType(row.status)">{{ chronicStatusText(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.status === 1" link type="primary" @click="openAudit(row)">审核认定</el-button>
              <span v-else class="text-[12px] text-gray-400">已审核</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="flex justify-end mt-3">
          <el-pagination
            :page-sizes="PAGE_SIZES" :page-size="cq.size" :current-page="cq.page" :total="chronicTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="(p) => { cq.page = p; loadChronic() }"
            @size-change="(s) => { cq.size = s; cq.page = 1; loadChronic() }"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 转移申请 -->
    <el-dialog v-model="transferVisible" :title="transferRow ? '编辑转移申请' : '发起转移接续申请'" width="540px" append-to-body>
      <el-form ref="transferRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-form-item label="参保人" prop="personName">
          <el-input v-model="transferForm.personName" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="transferForm.idCard" placeholder="须为参保库已登记人员" />
        </el-form-item>
        <el-form-item label="医保卡号">
          <el-input v-model="transferForm.insuranceNo" />
        </el-form-item>
        <el-form-item label="转移类型" prop="transferType">
          <el-select v-model="transferForm.transferType" style="width:100%">
            <el-option v-for="(v, k) in TRANSFER_TYPES" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="转出统筹区">
          <el-input v-model="transferForm.fromRegion" placeholder="默认取参保地" />
        </el-form-item>
        <el-form-item label="转入统筹区">
          <el-input v-model="transferForm.toRegion" placeholder="如 株洲市" />
        </el-form-item>
        <el-form-item label="缴费年限">
          <el-input-number v-model="transferForm.payYears" :min="0" :precision="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTransfer">保存</el-button>
      </template>
    </el-dialog>

    <!-- 个人账户支取/转出 -->
    <el-dialog v-model="accountOpVisible" title="个人账户支取 / 转移出" width="500px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="身份证号">
          <el-input v-model="accountForm.idCard" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="accountForm.opType" style="width:100%">
            <el-option label="一次性支取（清退）" :value="3" />
            <el-option label="转移出" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="accountForm.amount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="事由">
          <el-input v-model="accountForm.reason" type="textarea" :rows="2"
            placeholder="一次性支取须注明：死亡/退休/出国定居/终止参保等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountOpVisible = false">取消</el-button>
        <el-button type="danger" :loading="saving" @click="saveAccountOp">确认</el-button>
      </template>
    </el-dialog>

    <!-- 慢特病申请 -->
    <el-dialog v-model="chronicVisible" :title="chronicRow ? '编辑认定申请' : '登记慢特病认定申请'" width="540px" append-to-body>
      <el-form ref="chronicRef" :model="chronicForm" :rules="chronicRules" label-width="100px">
        <el-form-item label="参保人" prop="personName">
          <el-input v-model="chronicForm.personName" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="chronicForm.idCard" />
        </el-form-item>
        <el-form-item label="病种名称" prop="diseaseName">
          <el-input v-model="chronicForm.diseaseName" placeholder="如 2型糖尿病" />
        </el-form-item>
        <el-form-item label="病种编码">
          <el-input v-model="chronicForm.diseaseCode" />
        </el-form-item>
        <el-form-item label="待遇类别" prop="diseaseLevel">
          <el-select v-model="chronicForm.diseaseLevel" style="width:100%">
            <el-option v-for="(v, k) in DISEASE_LEVELS" :key="k" :label="v" :value="Number(k)" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请医师">
          <el-input v-model="chronicForm.doctor" />
        </el-form-item>
        <el-form-item label="定点医疗机构">
          <el-input v-model="chronicForm.hospital" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chronicVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveChronic">保存</el-button>
      </template>
    </el-dialog>

    <!-- 慢特病审核 -->
    <el-dialog v-model="auditVisible" title="门诊慢特病待遇认定审核" width="520px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="参保人">{{ auditRow?.personName }} · {{ auditRow?.diseaseName }}</el-form-item>
        <el-form-item label="审核结论">
          <el-select v-model="auditForm.result" style="width:100%">
            <el-option label="认定通过" :value="2" />
            <el-option label="不通过" :value="3" />
            <el-option label="复审通过" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期起">
          <el-date-picker v-model="auditForm.validFrom" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="有效期止">
          <el-date-picker v-model="auditForm.validTo" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveAudit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  transferListPage, transferUpsert, transferHandle,
  accountTxnListPage, accountInfo, accountOperate,
  chronicListPage, chronicUpsert, chronicAudit,
} from '@/api/insure'
import { PAGE_SIZES } from '@/lib/pagination'
import {
  TRANSFER_TYPES, TRANSFER_STATUS, TXN_TYPES, DISEASE_LEVELS, CHRONIC_STATUS,
  transferTypeText, transferStatusText, txnTypeText, diseaseLevelText, chronicStatusText,
  transferStatusType, chronicStatusType, txnTypeType,
} from './insureMeta'

const tab = ref('transfer')
const saving = ref(false)

function fmt(n) {
  return (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function onTabChange() {
  if (tab.value === 'transfer') loadTransfer()
  else if (tab.value === 'account') loadAccount()
  else loadChronic()
}

/* ---------------- 转移接续 ---------------- */
const transfers = ref([])
const transferTotal = ref(0)
const transferLoading = ref(false)
const tq = reactive({ keyword: '', transferType: '', status: '', page: 1, size: 10 })

async function loadTransfer() {
  transferLoading.value = true
  const res = await transferListPage({ page: tq.page, size: tq.size, keyword: tq.keyword || undefined, transferType: tq.transferType || undefined, status: tq.status === '' ? undefined : tq.status })
  transferLoading.value = false
  transfers.value = res.data.list
  transferTotal.value = res.data.total
}
function searchTransfer() { tq.page = 1; loadTransfer() }
function resetTransfer() { tq.keyword = ''; tq.transferType = ''; tq.status = ''; tq.page = 1; loadTransfer() }

const transferVisible = ref(false)
const transferRow = ref(null)
const transferRef = ref()
const transferForm = reactive({ id: null, personName: '', idCard: '', insuranceNo: '', transferType: 1, fromRegion: '', toRegion: '', payYears: 0, remark: '' })
const transferRules = {
  personName: [{ required: true, message: '请输入参保人姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
}
function openTransfer(row) {
  transferRow.value = row || null
  Object.assign(transferForm, row
    ? { id: row.id, personName: row.personName, idCard: row.idCard, insuranceNo: row.insuranceNo, transferType: row.transferType, fromRegion: row.fromRegion, toRegion: row.toRegion, payYears: row.payYears ?? 0, remark: row.remark }
    : { id: null, personName: '', idCard: '', insuranceNo: '', transferType: 1, fromRegion: '', toRegion: '', payYears: 0, remark: '' })
  transferVisible.value = true
}
async function saveTransfer() {
  await transferRef.value.validate()
  saving.value = true
  try {
    const res = await transferUpsert({ ...transferForm })
    if (res.data.ok) { ElMessage.success('已提交'); transferVisible.value = false; loadTransfer() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
async function doTransfer(row, action) {
  const labels = { 2: '受理', 3: '办理转出', 4: '办理转入', 5: '完成转移' }
  const label = labels[action]
  try {
    const { value } = await ElMessageBox.prompt(`请输入${label}意见${action === 5 ? '（完成转移将结转个人账户余额并清零转出方余额）' : ''}`, label, { inputType: 'textarea' })
    const res = await transferHandle({ id: row.id, action, opinion: value, payYears: row.payYears })
    if (res.data.ok) { ElMessage.success(`已${label}`); loadTransfer() }
    else ElMessage.warning(res.data.error || '失败')
  } catch (e) {
    if (e !== 'cancel') {/* 全局提示 */}
  }
}

/* ---------------- 个人账户 ---------------- */
const accounts = ref([])
const accountTotal = ref(0)
const accountLoading = ref(false)
const aq = reactive({ keyword: '', txnType: '', page: 1, size: 10 })
const accountIdCard = ref('')
const accountInfoData = ref({})

async function loadAccount() {
  accountLoading.value = true
  const res = await accountTxnListPage({ page: aq.page, size: aq.size, keyword: aq.keyword || undefined, txnType: aq.txnType || undefined })
  accountLoading.value = false
  accounts.value = res.data.list
  accountTotal.value = res.data.total
}
function searchAccount() { aq.page = 1; loadAccount() }
function resetAccount() { aq.keyword = ''; aq.txnType = ''; aq.page = 1; loadAccount() }
async function loadAccountInfo() {
  if (!accountIdCard.value) { ElMessage.warning('请输入身份证号'); return }
  const res = await accountInfo(accountIdCard.value)
  accountInfoData.value = res.data || {}
}
const accountOpVisible = ref(false)
const accountForm = reactive({ idCard: '', opType: 3, amount: 0, reason: '' })
function openAccountOp() {
  Object.assign(accountForm, { idCard: accountIdCard.value, opType: 3, amount: 0, reason: '' })
  accountOpVisible.value = true
}
async function saveAccountOp() {
  saving.value = true
  try {
    const res = await accountOperate({ ...accountForm })
    if (res.data.ok) { ElMessage.success('办理成功'); accountOpVisible.value = false; loadAccount(); loadAccountInfo() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

/* ---------------- 慢特病 ---------------- */
const chronics = ref([])
const chronicTotal = ref(0)
const chronicLoading = ref(false)
const cq = reactive({ keyword: '', diseaseLevel: '', status: '', page: 1, size: 10 })

async function loadChronic() {
  chronicLoading.value = true
  const res = await chronicListPage({ page: cq.page, size: cq.size, keyword: cq.keyword || undefined, diseaseLevel: cq.diseaseLevel || undefined, status: cq.status === '' ? undefined : cq.status })
  chronicLoading.value = false
  chronics.value = res.data.list
  chronicTotal.value = res.data.total
}
function searchChronic() { cq.page = 1; loadChronic() }
function resetChronic() { cq.keyword = ''; cq.diseaseLevel = ''; cq.status = ''; cq.page = 1; loadChronic() }

const chronicVisible = ref(false)
const chronicRow = ref(null)
const chronicRef = ref()
const chronicForm = reactive({ id: null, personName: '', idCard: '', insuranceNo: '', diseaseCode: '', diseaseName: '', diseaseLevel: 2, doctor: '', hospital: '' })
const chronicRules = {
  personName: [{ required: true, message: '请输入参保人姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  diseaseName: [{ required: true, message: '请输入病种名称', trigger: 'blur' }],
}
function openChronic(row) {
  chronicRow.value = row || null
  Object.assign(chronicForm, row
    ? { id: row.id, personName: row.personName, idCard: row.idCard, insuranceNo: row.insuranceNo, diseaseCode: row.diseaseCode, diseaseName: row.diseaseName, diseaseLevel: row.diseaseLevel, doctor: row.doctor, hospital: row.hospital }
    : { id: null, personName: '', idCard: '', insuranceNo: '', diseaseCode: '', diseaseName: '', diseaseLevel: 2, doctor: '', hospital: '' })
  chronicVisible.value = true
}
async function saveChronic() {
  await chronicRef.value.validate()
  saving.value = true
  try {
    const res = await chronicUpsert({ ...chronicForm })
    if (res.data.ok) { ElMessage.success('已登记'); chronicVisible.value = false; loadChronic() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}
const auditVisible = ref(false)
const auditRow = ref(null)
const auditForm = reactive({ result: 2, validFrom: '', validTo: '', opinion: '' })
function openAudit(row) {
  auditRow.value = row
  auditForm.result = 2
  auditForm.validFrom = ''
  auditForm.validTo = ''
  auditForm.opinion = ''
  auditVisible.value = true
}
async function saveAudit() {
  saving.value = true
  try {
    const res = await chronicAudit({ id: auditRow.value.id, ...auditForm })
    if (res.data.ok) { ElMessage.success('审核完成'); auditVisible.value = false; loadChronic() }
    else ElMessage.warning(res.data.error || '失败')
  } finally { saving.value = false }
}

onMounted(loadTransfer)
</script>
