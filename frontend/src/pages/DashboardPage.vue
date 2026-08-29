<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Дашборд</h1>
        <p class="page-subtitle">Обзор системы и недавних запусков рабочих процессов RabotyagaCI</p>
      </div>
      <div class="system-status-indicator">
        <span class="live-dot"></span>
        <span class="status-label">Система активна</span>
      </div>
    </div>

    <!-- GitHub Style Flat Metrics Grid -->
    <div class="stats-grid">
      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Проекты</span>
        </div>
        <div class="stat-value">{{ projectsCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Успешных сборок</span>
          <span class="stat-indicator success">✓</span>
        </div>
        <div class="stat-value text-success">{{ successCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Ошибок</span>
          <span class="stat-indicator failure">✕</span>
        </div>
        <div class="stat-value text-danger">{{ failureCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Выполняется сейчас</span>
          <span class="stat-indicator running">↻</span>
        </div>
        <div class="stat-value text-running">{{ runningCount }}</div>
      </div>
    </div>

    <div class="recent-builds-section">
      <div class="section-header">
        <h2 class="section-title">Последние запуски</h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>Загрузка сборок...</span>
      </div>

      <div v-else-if="recentBuilds.length === 0" class="empty-state card">
        <div class="empty-state-text">Пока нет сборок. Создайте проект и запустите первый пайплайн.</div>
      </div>

      <div v-else class="builds-list">
        <BuildCard v-for="build in recentBuilds" :key="build.id" :build="build" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getRecentBuilds, getProjects } from '@/api/client'
import type { BuildResponse } from '@/types'
import BuildCard from '@/components/build/BuildCard.vue'

const projectsCount = ref(0)
const recentBuilds = ref<BuildResponse[]>([])
const loading = ref(true)

const successCount = computed(() => recentBuilds.value.filter((b) => b.status === 'SUCCESS').length)
const failureCount = computed(() => recentBuilds.value.filter((b) => b.status === 'FAILURE').length)
const runningCount = computed(() => recentBuilds.value.filter((b) => b.status === 'RUNNING').length)

onMounted(async () => {
  try {
    const [buildRes, projRes] = await Promise.all([
      getRecentBuilds(10),
      getProjects(0, 100),
    ])
    recentBuilds.value = buildRes.data.data || []
    projectsCount.value = projRes.data.data ? projRes.data.data.totalElements : 0
  } catch (e) {
    console.error('Failed to load dashboard data', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.system-status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 2em;
  font-size: 12px;
  color: var(--text-secondary);
}

.live-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-green);
  box-shadow: 0 0 6px var(--accent-green);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  border-radius: 6px;
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 12px;
  color: var(--text-secondary);
  font-weight: 500;
}

.stat-indicator {
  font-size: 11px;
  font-weight: 700;
}

.stat-indicator.success { color: var(--accent-green); }
.stat-indicator.failure { color: var(--accent-red); }
.stat-indicator.running { color: var(--accent-blue); }

.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.text-success { color: var(--accent-green); }
.text-danger { color: var(--accent-red); }
.text-running { color: var(--accent-blue); }

.section-header {
  margin-bottom: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.builds-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.loading-state, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 36px;
  color: var(--text-secondary);
}
</style>
