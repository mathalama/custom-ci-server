<template>
  <div class="projects-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Проекты</h1>
        <p class="page-subtitle">Управление репозиториями и CI-пайплайнами</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">
        <Icon name="plus" :size="16" />
        <span>Новый проект</span>
      </router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>Загрузка проектов...</span>
    </div>

    <div v-else-if="projects.length === 0" class="empty-state card">
      <div class="empty-state-icon">
        <Icon name="folder" :size="44" color="#64748b" />
      </div>
      <div class="empty-state-text">Проекты пока не созданы</div>
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
          <div class="project-title-wrap">
            <div class="project-icon-box">
              <Icon name="folder" :size="18" color="#60a5fa" />
            </div>
            <span class="project-name">{{ project.name }}</span>
          </div>
          <Icon :name="providerIcon(project.gitProvider)" :size="20" />
        </div>
        <div class="project-repo">{{ project.repoUrl }}</div>
        <div class="project-footer">
          <span class="project-branch">
            <Icon name="git-branch" :size="14" />
            {{ project.defaultBranch }}
          </span>
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
import Icon from '@/components/common/Icon.vue'

const projects = ref<ProjectResponse[]>([])
const loading = ref(true)

const providerIcon = (provider: string) => {
  const icons: Record<string, string> = { GITHUB: 'github', GITLAB: 'gitlab', GITEA: 'gitea' }
  return icons[provider] || 'folder'
}

onMounted(async () => {
  try {
    const res = await getProjects(0, 100)
    projects.value = res.data.data ? res.data.data.content : []
  } catch (e) {
    console.error('Failed to load projects', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.projects-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.project-card {
  text-decoration: none;
  color: inherit;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  transition: all 0.2s ease;
}

.project-card:hover {
  transform: translateY(-2px);
  border-color: var(--accent-blue);
  box-shadow: 0 8px 24px -6px rgba(0, 0, 0, 0.4);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.project-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.project-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.project-repo {
  font-size: 13px;
  color: var(--text-secondary);
  word-break: break-all;
  margin-bottom: 16px;
  font-family: var(--font-mono);
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  padding-top: 12px;
}

.project-branch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary);
}

.project-status {
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
}

.project-status.active {
  color: var(--accent-green);
  border-color: rgba(34, 197, 94, 0.3);
  background: rgba(34, 197, 94, 0.08);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 16px;
  color: var(--text-muted);
}
</style>
