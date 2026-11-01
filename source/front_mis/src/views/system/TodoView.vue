<template>
  <div class="space-y-3">
    <!-- 统计 -->
    <div class="his-card p-3">
      <div class="font-semibold text-[15px] text-[#0B3B66] mb-2">待办中心</div>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3">
        <div class="p-3 rounded-lg bg-[#FEF3C7] text-center">
          <div class="text-[12px] text-gray-500">待处理</div>
          <div class="amount-strong !text-[#D97706]">{{ counts['待处理'] ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">处理中</div>
          <div class="amount-strong">{{ counts['处理中'] ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">已完成</div>
          <div class="amount-strong !text-[#059669]">{{ counts['已完成'] ?? 0 }}</div>
        </div>
        <div class="p-3 rounded-lg bg-gray-50 text-center">
          <div class="text-[12px] text-gray-500">已取消</div>
          <div class="amount-strong">{{ counts['已取消'] ?? 0 }}</div>
        </div>
      </div>
    </div>

    <!-- 查询 -->
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item">
            <el-input v-model="q.keyword" placeholder="标题/单号" clearable @keyup.enter="search" />
          </div>
          <div class="q-item">
            <el-select v-model="q.todoType" placeholder="待办类型" clearable style="width:100%">
              <el-option label="费用审核" value="mis_fee_audit" />
              <el-option label="疑点稽核" value="mif_suspicion" />
              <el-option label="反欺诈线索" value="mif_fraud_clue" />
              <el-option label="库存盘点" value="mif_stock_check" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="状态" clearable style="width:100%">
              <el-option label="待处理" :value="0" />
              <el-option label="处理中" :value="1" />
              <el-option label="已完成" :value="2" />
              <el-option label="已取消" :value="3" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.priority" placeholder="优先级" clearable style="width:100%">
              <el-option label="高" :value="1" />
              <el-option label="中" :value="2" />
              <el-option label="低" :value="3" />
            </el-select>
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
        <el-table-column prop="todoTitle" label="待办事项" min-width="280" show-overflow-tooltip />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ typeText(row.todoType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="关联单号" width="170" show-overflow-tooltip />
        <el-table-column prop="assignee" label="处理人" width="90" />
        <el-table-column label="优先级" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="priTag(row.priority)">{{ priText(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="生成时间" width="160" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0 || row.status === 1">
              <el-button link type="success" @click="handle(row, 2)">办结</el-button>
              <el-button link type="info" @click="handle(row, 3)">取消</el-button>
            </template>
            <span v-else class="text-[12px] text-gray-400">已处理</span>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listTodos, countTodos, handleTodo } from '@/api/system'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const counts = ref({})

const q = reactive({ keyword: '', todoType: '', status: null, priority: null, page: 1, size: 20 })

function typeText(t) {
  return {
    mis_fee_audit: '费用审核',
    mif_suspicion: '疑点稽核',
    mif_fraud_clue: '反欺诈线索',
    mif_stock_check: '库存盘点',
  }[t] || t || '其他'
}
function priText(p) { return { 1: '高', 2: '中', 3: '低' }[p] || '中' }
function priTag(p) { return { 1: 'danger', 2: 'warning', 3: 'info' }[p] || 'info' }
function statusText(s) { return { 0: '待处理', 1: '处理中', 2: '已完成', 3: '已取消' }[s] || '待处理' }
function statusTag(s) { return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' }[s] || 'info' }

async function load() {
  loading.value = true
  const res = await listTodos({ ...q })
  rows.value = res.data?.list || []
  total.value = res.data?.total || 0
  loading.value = false
}

async function loadCount() {
  const res = await countTodos()
  counts.value = res.data || {}
}

function search() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { keyword: '', todoType: '', status: null, priority: null })
  search()
}

async function handle(row, status) {
  const action = status === 2 ? '办结' : '取消'
  await ElMessageBox.confirm(`确认${action}该待办？`, `${action}待办`, { type: 'warning' })
  const res = await handleTodo({ id: row.id, status, finishOpinion: `${action}待办` })
  if (res.data?.ok) {
    ElMessage.success('已处理')
    load()
    loadCount()
  }
}

onMounted(() => {
  load()
  loadCount()
})
</script>
