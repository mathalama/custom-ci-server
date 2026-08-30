<template>
  <div class="dashboard-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Панель управления</h1>
        <p class="page-subtitle">Обзор системы, производительность и активность CI-сборок в реальном времени</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">
        <span>+ Новый проект</span>
      </router-link>
    </div>

    <!-- Mathalama Style Bento Metrics Grid -->
    <div class="stats-grid">
      <div class="stat-card bento-card">
        <div class="stat-header">
          <span class="stat-label">Всего проектов</span>
          <div class="stat-icon-box">
            <Icon name="folder" :size="16" color="var(--accent-brand)" />
          </div>
        </div>
        <div class="stat-value">{{ projectsCount }}</div>
      </div>

      <div class="stat-card bento-card">
        <div class="stat-header">
          <span class="stat-label">Успешных сборок</span>
          <div class="stat-icon-box success">
            <Icon name="check" :size="14" color="var(--accent-green)" />
          </div>
        </div>
        <div class="stat-value text-success">{{ successCount }}</div>
      </div>

      <div class="stat-card bento-card">
        <div class="stat-header">
          <span class="stat-label">Сборок с ошибкой</span>
          <div class="stat-icon-box failure">
            <Icon name="x-mark" :size="14" color="var(--accent-red)" />
          </div>
        </div>
        <div class="stat-value text-danger">{{ failureCount }}</div>
      </div>

      <div class="stat-card bento-card">
        <div class="stat-header">
          <span class="stat-label">В процессе</span>
          <div class="stat-icon-box running">
            <Icon name="clock" :size="14" color="var(--accent-amber)" />
          </div>
        </div>
        <div class="stat-value text-running">{{ runningCount }}</div>
      </div>
    </div>

    <!-- Recent Builds Fullscreen Section -->
    <div class="recent-builds-section">
      <div class="section-header">
        <h2 class="section-title">Журнал последних сборок</h2>
      </div>

      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <span>Загрузка данных...</span>
      </div>

      <div v-else-if="recentBuilds.length === 0" class="bento-card">
        <EmptyState
          type="no-pipelines"
          title="Пока нет запусков пайплайнов"
          description="В системе ещё не запущен ни один билд. Создайте проект, подключите репозиторий и запустите первый пайплайн."
          action-text="+ Создать проект"
          @action="$router.push('/projects/new')"
        />
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
import Icon from '@/components/common/Icon.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const projectsCount = ref(0)
const recentBuilds = ref<BuildResponse[]>([])
const loading = ref(true)

const successCount = computed(() => recentBuilds.value.filter((b) => b.status === 'SUCCESS').length)
const failureCount = computed(() => recentBuilds.value.filter((b) => b.status === 'FAILURE').length)
const runningCount = computed(() => recentBuilds.value.filter((b) => b.status === 'RUNNING').length)

onMounted(async () => {
  try {
    const [buildRes, projRes] = await Promise.all([
      getRecentBuilds(15),
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
  width: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  width: 100%;
}

.stat-card {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 600;
}

.stat-icon-box {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon-box.success { background: var(--accent-green-bg); border-color: var(--accent-green-border); }
.stat-icon-box.failure { background: var(--accent-red-bg); border-color: var(--accent-red-border); }
.stat-icon-box.running { background: var(--accent-amber-bg); border-color: var(--accent-amber-border); }

.stat-indicator {
  font-size: 13px;
  font-weight: 800;
}

.stat-indicator.success { color: var(--accent-green); }
.stat-indicator.failure { color: var(--accent-red); }
.stat-indicator.running { color: var(--accent-amber); }

.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1;
  font-family: var(--font-mono);
}

.text-success { color: var(--accent-green); }
.text-danger { color: var(--accent-red); }
.text-running { color: var(--accent-amber); }

.recent-builds-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
}

.section-header {
  margin-bottom: 2px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.builds-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.loading-state, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px;
  color: var(--text-secondary);
}
</style>
