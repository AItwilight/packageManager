<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">📦 包裹管理</div>
      <el-menu :default-active="activeMenu" router background-color="#304156"
        text-color="#bfcbd9" active-text-color="#409EFF">
        <el-menu-item index="/dashboard">
          <span>📊 统计概览</span>
        </el-menu-item>
        <el-menu-item index="/checkin">
          <span>📦 包裹入库</span>
        </el-menu-item>
        <el-menu-item index="/packages">
          <span>📋 包裹列表</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="header-title">末端驿站包裹管理系统</span>
        <div class="header-right">
          <span class="username">admin</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)

function handleLogout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #4a5e77;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.username {
  color: #606266;
}
</style>
