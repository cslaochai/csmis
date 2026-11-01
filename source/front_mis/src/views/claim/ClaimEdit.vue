<template>
  <el-dialog
    :model-value="modelValue"
    title="报销申请登记"
    width="600px"
    append-to-body
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="姓名" prop="personName">
        <el-input v-model="form.personName" placeholder="参保人姓名" />
      </el-form-item>
      <el-form-item label="身份证号" prop="idCard">
        <el-input v-model="form.idCard" placeholder="如 430102199001011234" />
      </el-form-item>
      <el-form-item label="险种" prop="insuranceType">
        <el-select v-model="form.insuranceType" style="width:100%">
          <el-option label="城镇职工医保" value="2" />
          <el-option label="城乡居民医保" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item label="报销类型" prop="claimType">
        <el-radio-group v-model="form.claimType">
          <el-radio :value="1">门诊</el-radio>
          <el-radio :value="2">住院</el-radio>
          <el-radio :value="3">药店</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="就诊日期">
        <el-date-picker v-model="form.visitDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
      </el-form-item>
      <el-form-item label="费用总额" prop="totalAmount">
        <el-input-number v-model="form.totalAmount" :min="0" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="申请事由">
        <el-input v-model="form.applyReason" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">提交申请</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { claimSubmit } from '@/api/insurance'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'saved'])

const formRef = ref()
const saving = ref(false)
const form = reactive({
  personName: '', idCard: '', insuranceType: '2', claimType: 1, visitDate: '',
  totalAmount: 0, applyReason: '',
})
const rules = {
  personName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  insuranceType: [{ required: true, message: '请选择险种', trigger: 'change' }],
  claimType: [{ required: true, message: '请选择报销类型', trigger: 'change' }],
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    const res = await claimSubmit({ ...form })
    if (res.data.ok) {
      ElMessage.success('申请已登记')
      emit('update:modelValue', false)
      emit('saved')
    } else {
      ElMessage.warning(res.data.error || '提交失败')
    }
  } finally {
    saving.value = false
  }
}
</script>
