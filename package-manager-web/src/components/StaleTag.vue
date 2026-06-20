<template>
  <el-tag v-if="stale" type="danger" size="small">&#9888; 滞留</el-tag>
</template>

<script setup lang="ts">
defineProps<{
  stale: boolean
}>()
</script>
