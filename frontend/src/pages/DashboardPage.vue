<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Обзор системы</h1>
        <p class="page-subtitle">Метрики и журнал выполнения CI-пайплайнов</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">
        <span>+ Новый проект</span>
      </router-link>
    </div>

    <!-- Editorial Metrics Grid -->
    <div class="stats-grid">
      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Проекты</span>
        </div>
        <div class="stat-value">{{ projectsCount }}</div>
      </div>

      <div class="stat-card card">
        <div class="stat-header">
          <span class="stat-label">Успешно</span>
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
          <span class="stat-label">В процессе</span>
          <span class="stat-indicator running">↻</span>
        </div>
        <div class="stat-value text-running">{{ runningCount }}</div>
      </div>
    </div>

    <div class="recent-builds-section">
      <div class="section-header">
        <h2 class="section-title">Журнал последних сборок</h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>Загрузка данных...</span>
      </div>

      <div v-else-if="recentBuilds.length === 0" class="empty-state card">
        <div class="empty-state-text">Пока нет запусков. Создайте проект и запустите первый пайплайн.</div>
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
  gap: 28px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-radius: var(--radius-sm);
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
  width: 18px;
  height: 18px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-indicator.success { color: var(--accent-green); background: var(--accent-green-bg); }
.stat-indicator.failure { color: var(--accent-red); background: var(--accent-red-bg); }
.stat-indicator.running { color: var(--accent-amber); background: var(--accent-amber-bg); }

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.1;
  font-family: var(--font-mono);
}

.text-success { color: var(--accent-green); }
.text-danger { color: var(--accent-red); }
.text-running { color: var(--accent-amber); }

.section-header {
  margin-bottom: 14px;
}

.section-title {
  font-size: 14.5px;
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
  padding: 48px;
  color: var(--text-secondary);
}
</style>
