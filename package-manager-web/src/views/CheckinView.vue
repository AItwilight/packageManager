<template>
  <div class="checkin">
    <h3>📦 包裹入库</h3>
    <el-card style="max-width: 600px; margin-top: 20px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="运单号" prop="waybillNo">
          <el-input v-model="form.waybillNo" placeholder="请输入运单号" />
        </el-form-item>
        <el-form-item label="收件人手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="快递公司" prop="courier">
          <el-select v-model="form.courier" placeholder="请选择快递公司" style="width: 100%">
            <el-option v-for="c in couriers" :key="c.code" :label="c.label" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="货架位置" prop="shelf">
          <el-input v-model="form.shelf" placeholder="大写字母-两位数字，如 A-13" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleCheckin">
            确认入库
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import request from '@/utils/request'

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  waybillNo: '',
  phone: '',
  courier: '',
  shelf: '',
})

const couriers = [
  { code: 'SF', label: '顺丰' },
  { code: 'YTO', label: '圆通' },
  { code: 'ZTO', label: '中通' },
  { code: 'STO', label: '申通' },
  { code: 'YD', label: '韵达' },
  { code: 'JD', label: '京东' },
  { code: 'DB', label: '德邦' },
  { code: 'OTHER', label: '其他' },
]

const validatePhone = (_rule: any, value: string, callback: any) => {
  if (!/^1\d{10}$/.test(value)) {
    callback(new Error('手机号格式不正确'))
  } else {
    callback()
  }
}

const validateShelf = (_rule: any, value: string, callback: any) => {
  if (!/^[A-Z]-\d{2}$/.test(value)) {
    callback(new Error('货架格式：大写字母-两位数字，如 A-13'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  waybillNo: [{ required: true, message: '请输入运单号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { validator: validatePhone, trigger: 'blur' },
  ],
  courier: [{ required: true, message: '请选择快递公司', trigger: 'change' }],
  shelf: [
    { required: true, message: '请输入货架位置', trigger: 'blur' },
    { validator: validateShelf, trigger: 'blur' },
  ],
}

const lastPickupCode = ref('')

async function handleCheckin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const data: any = await request.post('/package/checkin', { ...form })
    lastPickupCode.value = data.pickupCode
    ElMessage.success('入库成功，取件码：' + data.pickupCode)
    handleReset()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
  lastPickupCode.value = ''
}
</script>

<style scoped>
.checkin h3 {
  margin-bottom: 10px;
}
</style>
