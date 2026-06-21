<template>
  <div class="package-list">
    <h3>📋 包裹列表</h3>

    <!-- 搜索区 -->
    <el-card style="margin-top: 20px">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="精确查询（后四位）" clearable
            @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="searchForm.keyword" placeholder="运单号/手机号/快递/取件码"
            clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Tabs + 排序 + 表格 -->
    <el-card style="margin-top: 20px">
      <div class="tabs-header">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="待取件" name="pending" />
          <el-tab-pane label="已取件" name="picked" />
          <el-tab-pane label="滞留包裹" name="stale" />
        </el-tabs>
        <div class="sort-toggle">
          <span class="sort-label">入库时间：</span>
          <el-radio-group v-model="sortOrder" @change="handleSortChange" size="small">
            <el-radio-button value="desc">最近优先</el-radio-button>
            <el-radio-button value="asc">最早优先</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-table :data="tableData" :row-class-name="tableRowClassName" v-loading="loading"
        stripe style="width: 100%">
        <el-table-column prop="waybillNo" label="运单号" width="140" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="courierDesc" label="快递公司" width="90" />
        <el-table-column prop="shelf" label="货架" width="80" />
        <el-table-column prop="pickupCode" label="取件码" width="120" />
        <el-table-column label="入库时间" width="180">
          <template #default="{ row }">
            <span>{{ row.checkinTime }}</span>
            <el-tag v-if="row.stale" type="danger" size="small" style="margin-left: 6px">滞留</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : 'success'" size="small">
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

      <div v-if="total > 0" class="pagination-bar">
        <el-button :disabled="currentPage <= 1" @click="handlePrevPage">上一页</el-button>
        <el-pagination
          v-model:current-page="currentPage" :page-size="pageSize"
          :total="total" layout="pager" @current-change="fetchData" />
        <el-button :disabled="currentPage * pageSize >= total" @click="handleNextPage">下一页</el-button>
        <span class="total-info">共 {{ total }} 条</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'

interface PackageItem {
  id: string
  waybillNo: string
  phone: string
  courier: string
  courierDesc: string
  shelf: string
  pickupCode: string
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
const activeTab = ref('pending')
const sortOrder = ref('desc')

const searchForm = reactive({
  phone: '',
  keyword: '',
})

onMounted(() => {
  fetchData()
})

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {
    page: currentPage.value,
    size: pageSize.value,
    sortOrder: sortOrder.value,
  }

  if (activeTab.value === 'stale') {
    params.status = 0
    params.stale = true
  } else if (activeTab.value === 'picked') {
    params.status = 1
  } else {
    params.status = 0
  }

  if (searchForm.phone) params.phone = searchForm.phone
  if (searchForm.keyword) params.keyword = searchForm.keyword

  return params
}

async function fetchData() {
  loading.value = true
  try {
    const data: any = await request.get('/package/list', { params: buildParams() })
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

function handleSortChange() {
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

function handlePrevPage() {
  if (currentPage.value > 1) {
    currentPage.value--
    fetchData()
  }
}

function handleNextPage() {
  if (currentPage.value * pageSize.value < total.value) {
    currentPage.value++
    fetchData()
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
.tabs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
.tabs-header .el-tabs {
  flex: 1;
}
.sort-toggle {
  display: flex;
  align-items: center;
  padding-bottom: 16px;
}
.sort-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
}
.pagination-bar {
  margin-top: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.total-info {
  font-size: 13px;
  color: #909399;
  margin-left: 8px;
}
</style>
