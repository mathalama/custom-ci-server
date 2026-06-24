<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">Проекты</h1>
        <p class="page-subtitle">Все зарегистрированные CI проекты</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">
        <Icon name="plus" :size="16" />
        Новый проект
      </router-link>
    </div>

    <div v-if="loading" class="loading"><div class="spinner"></div></div>
    <div v-else-if="projects.length === 0" class="empty-state">
      <div class="empty-state-icon">
        <Icon name="folder" :size="40" />
      </div>
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
import Icon from '@/components/Icon.vue'

const projects = ref<ProjectResponse[]>([])
const loading = ref(true)

const providerIcon = (provider: string) => {
  const icons: Record<string, string> = { GITHUB: 'github', GITLAB: 'gitlab', GITEA: 'gitea' }
  return icons[provider] || 'package'
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
  font-size: 15px;
  font-weight: 600;
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
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.project-status {
  font-size: 12px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 6px;
  border: 1px solid var(--border);
  background: var(--surface-elevated);
  color: var(--text-secondary);
}

.project-status.active {
  color: var(--success);
  border-color: var(--success);
}

.project-status.inactive {
  color: var(--text-muted);
}
</style>
