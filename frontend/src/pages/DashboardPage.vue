<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">Обзор системы RabotyagaCI</p>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ projects.length }}</div>
        <div class="stat-label">Проектов</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ successCount }}</div>
        <div class="stat-label">Успешных билдов</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ failureCount }}</div>
        <div class="stat-label">Упавших билдов</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ runningCount }}</div>
        <div class="stat-label">В процессе</div>
      </div>
    </div>

    <h2 class="card-title">Последние билды</h2>
    <div v-if="loading" class="loading"><div class="spinner"></div></div>
    <div v-else-if="recentBuilds.length === 0" class="empty-state">
      <div class="empty-state-icon">
        <Icon name="package" :size="40" />
      </div>
      <div class="empty-state-text">Пока нет билдов. Создайте проект и запустите первый билд!</div>
    </div>
    <div v-else class="builds-list">
      <BuildCard v-for="build in recentBuilds" :key="build.id" :build="build" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRecentBuilds } from '@/api/client'
import type { BuildResponse } from '@/types'
import BuildCard from '@/components/BuildCard.vue'
import Icon from '@/components/Icon.vue'

const projects = ref<any[]>([])
const recentBuilds = ref<BuildResponse[]>([])
const loading = ref(true)

const successCount = computed(() => recentBuilds.value.filter((b: BuildResponse) => b.status === 'SUCCESS').length)
const failureCount = computed(() => recentBuilds.value.filter((b: BuildResponse) => b.status === 'FAILURE').length)
const runningCount = computed(() => recentBuilds.value.filter((b: BuildResponse) => b.status === 'RUNNING').length)

onMounted(async () => {
  try {
    const buildRes = await getRecentBuilds(10)
    recentBuilds.value = buildRes.data.data
  } catch (e) {
    console.error('Failed to load dashboard data', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.builds-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
