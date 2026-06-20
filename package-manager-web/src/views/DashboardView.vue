<template>
  <div class="dashboard">
    <h3>📊 数据概览</h3>
    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日入库</div>
          <div class="stat-value" style="color: #409EFF">{{ stats.todayCheckin }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">待取件总数</div>
          <div class="stat-value" style="color: #E6A23C">{{ stats.pendingTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card stat-danger">
          <div class="stat-label">滞留包裹</div>
          <div class="stat-value" style="color: #F56C6C">{{ stats.staleTotal }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">今日取件</div>
          <div class="stat-value" style="color: #67C23A">{{ stats.todayPickup }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

interface Stats {
  todayCheckin: number
  pendingTotal: number
  staleTotal: number
  todayPickup: number
}

const stats = ref<Stats>({
  todayCheckin: 0,
  pendingTotal: 0,
  staleTotal: 0,
  todayPickup: 0,
})

onMounted(async () => {
  try {
    stats.value = await request.get('/dashboard/stats') as any
  } catch {
    // 错误已在拦截器中处理
  }
})
</script>

<style scoped>
.dashboard h3 {
  margin-bottom: 20px;
}
.stats-row {
  margin-top: 10px;
}
.stat-card {
  text-align: center;
  padding: 10px;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}
.stat-value {
  font-size: 32px;
  font-weight: bold;
}
.stat-danger {
  border: 1px solid #F56C6C;
}
</style>
