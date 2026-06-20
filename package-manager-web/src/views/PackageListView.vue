<template>
  <div class="package-list">
    <h3>📋 包裹列表</h3>

    <!-- 搜索区 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="精确查询" clearable
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="运单号/手机号/快递"
            clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tabs + 表格 -->
    <el-card style="margin-top: 20px">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待取件" :name="0" />
        <el-tab-pane label="已取件" :name="1" />
      </el-tabs>

      <el-table :data="tableData" :row-class-name="tableRowClassName" v-loading="loading"
        stripe style="width: 100%">
        <el-table-column prop="waybillNo" label="运单号" width="140" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="courierDesc" label="快递公司" width="90" />
        <el-table-column prop="shelf" label="货架" width="80" />
        <el-table-column prop="checkinTime" label="入库时间" width="160" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <StaleTag :stale="row.stale" />
            <el-tag v-if="!row.stale" :type="row.status === 0 ? 'warning' : 'success'"
              size="small">
              {{ row.statusDesc }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-popconfirm v-if="row.status === 0" title="确认该包裹已被收件人取走？"
              @confirm="handlePickup(row.id)">
              <template #reference>
                <el-button type="primary" size="small" link>取件</el-button>
              </template>
            </el-popconfirm>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-if="total > 0" style="margin-top: 20px; justify-content: flex-end"
        v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import StaleTag from '@/components/StaleTag.vue'
import request from '@/utils/request'

interface PackageItem {
  id: string
  waybillNo: string
  phone: string
  courier: string
  courierDesc: string
  shelf: string
  status: number
  statusDesc: string
  checkinTime: string
  pickupTime: string | null
  stale: boolean
}

const loading = ref(false)
const tableData = ref<PackageItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const activeTab = ref(0)

const searchForm = reactive({
  phone: '',
  keyword: '',
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      page: currentPage.value,
      size: pageSize.value,
      status: activeTab.value,
    }
    if (searchForm.phone) params.phone = searchForm.phone
    if (searchForm.keyword) params.keyword = searchForm.keyword

    const data: any = await request.get('/package/list', { params })
    tableData.value = data.list
    total.value = data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  searchForm.phone = ''
  searchForm.keyword = ''
  currentPage.value = 1
  fetchData()
}

function handleTabChange() {
  currentPage.value = 1
  fetchData()
}

async function handlePickup(id: string) {
  try {
    await request.put(`/package/pickup/${id}`)
    fetchData()
  } catch {
    // 错误已在拦截器中处理
  }
}

function tableRowClassName({ row }: { row: PackageItem }) {
  return row.stale ? 'stale-row' : ''
}
</script>

<style scoped>
.package-list h3 {
  margin-bottom: 10px;
}
</style>
