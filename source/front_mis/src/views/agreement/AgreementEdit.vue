<template>
  <el-dialog
    :model-value="modelValue"
    :title="row ? '编辑协议' : '新增协议'"
    width="600px"
    append-to-body
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="机构编码" prop="medinsCode">
        <el-input v-model="form.medinsCode" placeholder="如 H4301000001" :disabled="!!row" />
      </el-form-item>
      <el-form-item label="机构名称" prop="medinsName">
        <el-input v-model="form.medinsName" placeholder="如 长沙市麓康医院" />
      </el-form-item>
      <el-form-item label="机构类型" prop="medinsType">
        <el-radio-group v-model="form.medinsType">
          <el-radio :value="1">定点医院</el-radio>
          <el-radio :value="2">定点药店</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="协议编号">
        <el-input v-model="form.agreementNo" placeholder="如 AG2026-001" />
      </el-form-item>
      <el-form-item label="签订日期">
        <el-date-picker v-model="form.signDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="到期日期">
        <el-date-picker v-model="form.expireDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="服务范围">
        <el-input v-model="form.serviceScope" type="textarea" :rows="2" placeholder="如 门诊统筹、住院统筹、慢特病门诊" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { agreementUpsert } from '@/api/insurance'

const props = defineProps({ modelValue: Boolean, row: Object })
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const form = reactive({
  id: null, medinsCode: '', medinsName: '', medinsType: 1, agreementNo: '',
  signDate: '', expireDate: '', serviceScope: '', remark: '',
})
const rules = {
  medinsCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  medinsName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  medinsType: [{ required: true, message: '请选择机构类型', trigger: 'change' }],
}

watch(
  () => [props.modelValue, props.row],
  () => {
    if (!props.modelValue) return
    const r = props.row
    Object.assign(form, {
      id: r?.id || null,
      medinsCode: r?.medinsCode || '',
      medinsName: r?.medinsName || '',
      medinsType: r?.medinsType ?? 1,
      agreementNo: r?.agreementNo || '',
      signDate: r?.signDate || '',
      expireDate: r?.expireDate || '',
      serviceScope: r?.serviceScope || '',
      remark: r?.remark || '',
    })
    formRef.value?.clearValidate()
  },
  { immediate: true }
)

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    await agreementUpsert({ ...form })
    ElMessage.success('保存成功')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
