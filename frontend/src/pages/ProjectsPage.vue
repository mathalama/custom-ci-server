<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Проекты</h1>
        <p class="page-subtitle">Все зарегистрированные CI проектов</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">+ Новый проект</router-link>
    </div>

    <div v-if="loading" class="loading"><div class="spinner"></div></div>
    <div v-else-if="projects.length === 0" class="empty-state">
      <div class="empty-state-icon">📁</div>
      <div class="empty-state-text">Проектов пока нет</div>
      <router-link to="/projects/new" class="btn btn-primary" style="margin-top: 16px">
        Создать первый проект
      </router-link>
    </div>
    <div v-else class="projects-grid">
      <router-link
        v-for="project in projects"
        :key="project.id"
        :to="`/projects/${project.id}`"
        class="project-card card"
      >
        <div class="project-header">
          <span class="project-name">{{ project.name }}</span>
          <span class="project-provider">{{ providerIcon(project.gitProvider) }}</span>
        </div>
        <div class="project-repo">{{ project.repoUrl }}</div>
        <div class="project-footer">
          <span class="project-branch">🌿 {{ project.defaultBranch }}</span>
          <span :class="['project-status', project.isActive ? 'active' : 'inactive']">
            {{ project.isActive ? 'Активен' : 'Неактивен' }}
          </span>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getProjects } from '@/api/client'
import type { ProjectResponse } from '@/types'

const projects = ref<ProjectResponse[]>([])
const loading = ref(true)

const providerIcon = (provider: string) => {
  const icons: Record<string, string> = { GITHUB: '🐙', GITLAB: '🦊', GITEA: '🍵' }
  return icons[provider] || '📦'
}

onMounted(async () => {
  try {
    const res = await getProjects(0, 100)
    projects.value = res.data.data.content
  } catch (e) {
    console.error('Failed to load projects', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.project-card {
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
}

.project-provider {
  font-size: 20px;
}

.project-repo {
  font-size: 13px;
  color: var(--text-secondary);
  word-break: break-all;
  margin-bottom: 12px;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.project-branch {
  font-size: 13px;
  color: var(--text-secondary);
}

.project-status {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.project-status.active {
  background: var(--success-bg);
  color: var(--success);
}

.project-status.inactive {
  background: var(--pending-bg);
  color: var(--pending);
}
</style>
