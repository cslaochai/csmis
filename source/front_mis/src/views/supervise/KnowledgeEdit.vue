<template>
  <el-dialog
    :model-value="modelValue"
    :title="row ? '编辑知识' : '新增知识'"
    width="620px"
    append-to-body
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="知识编码" prop="kbCode">
        <el-input v-model="form.kbCode" placeholder="如 KB_DRUG_001" :disabled="!!row" />
      </el-form-item>
      <el-form-item label="知识标题" prop="kbName">
        <el-input v-model="form.kbName" placeholder="如 格列美脲片每日最大剂量" />
      </el-form-item>
      <el-form-item label="知识类型" prop="kbType">
        <el-select v-model="form.kbType" style="width:100%">
          <el-option v-for="(v, k) in KB_TYPES" :key="k" :label="v" :value="k" />
        </el-select>
      </el-form-item>
      <el-form-item label="知识内容" prop="content">
        <el-input v-model="form.content" type="textarea" :rows="4" placeholder="规则要点 / 临床依据 / 政策条文" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model="form.source" placeholder="如 临床诊疗规范、药品说明书、医保政策文件" />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { upsertKnowledge } from '@/api/supervise'
import { KB_TYPES } from './knowledgeMeta'

const props = defineProps({ modelValue: Boolean, row: Object })
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const form = reactive({
  id: null, kbCode: '', kbName: '', kbType: '', content: '', source: '', status: 1,
})
const rules = {
  kbCode: [{ required: true, message: '请输入知识编码', trigger: 'blur' }],
  kbName: [{ required: true, message: '请输入知识标题', trigger: 'blur' }],
  kbType: [{ required: true, message: '请选择知识类型', trigger: 'change' }],
}

watch(
  () => [props.modelValue, props.row],
  () => {
    if (!props.modelValue) return
    const r = props.row
    Object.assign(form, {
      id: r?.id || null,
      kbCode: r?.kbCode || '',
      kbName: r?.kbName || '',
      kbType: r?.kbType || '',
      content: r?.content || '',
      source: r?.source || '',
      status: r?.status ?? 1,
    })
    formRef.value?.clearValidate()
  },
  { immediate: true }
)

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await upsertKnowledge({ ...form })
    ElMessage.success('保存成功')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
