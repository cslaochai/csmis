<template>
  <div class="space-y-3">
    <!-- 看板 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">操作日志审计概览</div>
      <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">日志总量</div>
          <div class="amount-strong">{{ stat.total ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">成功</div>
          <div class="amount-strong !text-[#059669]">{{ stat.successCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">失败</div>
          <div class="amount-strong !text-[#DC2626]">{{ stat.failCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">涉及操作人</div>
          <div class="amount-strong">{{ stat.userCount ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">平均耗时</div>
          <div class="amount-strong">{{ stat.avgCostMs ?? 0 }} ms</div>
        </div>
        <div class="p-3 rounded-lg bg-[#F3E8FF] text-center">
          <div class="text-[12px] text-gray-500">统计天数</div>
          <div class="amount-strong !text-[#7C3AED]">{{ stat.days ?? 30 }} 天</div>
        </div>
      </div>
      <div class="mt-3 flex items-center gap-2">
        <span class="text-[12px] text-gray-500">统计区间</span>
        <el-radio-group v-model="days" size="small" @change="loadStat">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
          <el-radio-button :value="90">近 90 天</el-radio-button>
        </el-radio-group>
        <el-button size="small" class="ml-auto" @click="onClean">清理历史日志</el-button>
      </div>
    </div>

    <!-- 查询 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="描述/单号/地址" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.operType" placeholder="操作类型" clearable style="width:100%">
              <el-option v-for="t in OPER_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.operModule" placeholder="功能模块" clearable style="width:100%">
              <el-option v-for="m in MODULES" :key="m" :label="m" :value="m" />
            </el-select>
          </div>
          <div class="q-item">
            <el-input v-model="q.operUser" placeholder="操作人" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.operResult" placeholder="结果" clearable style="width:100%">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </div>
          <div class="q-item">
            <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD"
                            start-placeholder="开始日期" end-placeholder="结束日期" style="width:100%" />
          </div>
        </div>
        <div class="flex gap-2 mt-2">
          <el-button type="primary" size="small" @click="search">查询</el-button>
          <el-button size="small" @click="reset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 列表 -->
    <div class="his-card p-3">
      <el-table :data="rows" v-loading="loading" size="small" border stripe>
        <el-table-column prop="createTime" label="操作时间" width="160" />
        <el-table-column prop="operType" label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTag(row.operType)">{{ row.operType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operModule" label="模块" width="100" />
        <el-table-column prop="operUser" label="操作人" width="100" />
        <el-table-column prop="operDesc" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestUri" label="请求地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.operResult === 1 ? 'success' : 'danger'">
              {{ row.operResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时" width="80" align="right">
          <template #default="{ row }">{{ row.costMs ?? 0 }} ms</template>
        </el-table-column>
        <el-table-column prop="operIp" label="IP" width="120" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="mt-3 flex justify-end">
        <el-pagination
          v-model:current-page="q.page"
          v-model:page-size="q.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="search" />
      </div>
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="操作时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ detail.operUser }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ detail.operType }}</el-descriptions-item>
        <el-descriptions-item label="功能模块">{{ detail.operModule }}</el-descriptions-item>
        <el-descriptions-item label="请求方式">{{ detail.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ detail.costMs ?? 0 }} ms</el-descriptions-item>
        <el-descriptions-item label="请求地址" :span="2">{{ detail.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="业务对象" :span="2">
          {{ detail.targetType || '-' }} / {{ detail.targetNo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="text-[11px] whitespace-pre-wrap break-all">{{ detail.requestParams || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="detail.errorMsg" label="错误信息" :span="2">
          <span class="text-red-600">{{ detail.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOperLogs, operLogStat, cleanOperLogs } from '@/api/system'

const OPER_TYPES = ['新增', '修改', '删除', '查询', '审核', '导出', '登录', '其他']
const MODULES = ['基金监管', '结算管理', '参保管理', '费用审核', '定点协议', '手工报销',
  '统计报表', '进销存', 'DRG监管', '系统管理']

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const stat = ref({})
const days = ref(30)
const range = ref([])
const detailVisible = ref(false)
const detail = ref({})

const q = reactive({ keyword: '', operType: '', operModule: '', operUser: '', operResult: null, page: 1, size: 20 })

function typeTag(t) {
  return { 新增: 'success', 修改: 'warning', 删除: 'danger', 审核: 'primary', 登录: 'info' }[t] || 'info'
}

async function load() {
  loading.value = true
  const params = { ...q }
  if (range.value && range.value.length === 2) {
    params.beginDate = range.value[0]
    params.endDate = range.value[1]
  }
  const res = await listOperLogs(params)
  rows.value = res.data?.list || []
  total.value = res.data?.total || 0
  loading.value = false
}

async function loadStat() {
  const res = await operLogStat(days.value)
  stat.value = res.data || {}
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', operType: '', operModule: '', operUser: '', operResult: null })
  range.value = []
  search()
}

function showDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function onClean() {
  await ElMessageBox.confirm('将删除 180 天前的操作日志，是否继续？', '清理历史日志', { type: 'warning' })
  const res = await cleanOperLogs(180)
  if (res.data?.ok) {
    ElMessage.success(`已清理 ${res.data.matched ?? 0} 条`)
    load()
    loadStat()
  }
}

onMounted(() => {
  load()
  loadStat()
})
</script>
