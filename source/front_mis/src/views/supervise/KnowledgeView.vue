<template>
  <div class="space-y-3">
    <div class="his-card p-3">
      <div class="query-grid-wrap">
        <div class="query-grid">
          <div class="q-item"><el-input v-model="q.keyword" placeholder="标题/编码" clearable /></div>
          <div class="q-item">
            <el-select v-model="q.kbType" placeholder="知识类型" clearable style="width:100%">
              <el-option v-for="(v, k) in KB_TYPES" :key="k" :label="v" :value="k" />
            </el-select>
          </div>
          <div class="q-item">
            <el-select v-model="q.status" placeholder="状态" clearable style="width:100%">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </div>
        </div>
        <div class="query-grid-ops">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="his-card p-3">
      <div class="flex items-center justify-between mb-2">
        <div class="font-semibold text-[15px] text-[#0B3B66]">监管知识库 <el-tag size="small" type="success">{{ enabled }} 启用</el-tag></div>
        <el-button type="primary" size="small" @click="openEdit()">新增知识</el-button>
      </div>
      <el-table :data="list" size="small" v-loading="loading">
        <el-table-column prop="kbCode" label="知识编码" width="150" />
        <el-table-column prop="kbName" label="知识标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="知识类型" width="120">
          <template #default="{ row }">{{ kbTypeText(row.kbType) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
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

    <KnowledgeEdit v-model="editVisible" :row="currentRow" @saved="load" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listKnowledge, changeKnowledgeStatus } from '@/api/supervise'
import { PAGE_SIZES } from '@/lib/pagination'
import KnowledgeEdit from './KnowledgeEdit.vue'
import { KB_TYPES, kbTypeText } from './knowledgeMeta'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ keyword: '', kbType: '', status: '', page: 1, size: 10 })

const enabled = computed(() => list.value.filter((x) => x.status === 1).length)

const editVisible = ref(false)
const currentRow = ref(null)
function openEdit(row) {
  currentRow.value = row || null
  editVisible.value = true
}

async function load() {
  loading.value = true
  const res = await listKnowledge(q)
  loading.value = false
  list.value = res.data.list
  total.value = res.data.total
}
function search() { q.page = 1; load() }
function reset() { q.keyword = q.kbType = ''; q.status = ''; q.page = 1; load() }

async function toggle(row) {
  const next = row.status === 1 ? 0 : 1
  await changeKnowledgeStatus(row.id, next)
  ElMessage.success(next === 1 ? '已启用' : '已停用')
  load()
}

onMounted(load)
</script>
