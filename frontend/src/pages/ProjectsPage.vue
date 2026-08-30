<template>
  <div class="projects-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Проекты</h1>
        <p class="page-subtitle">Подключенные репозитории и пайплайны</p>
      </div>
      <router-link to="/projects/new" class="btn btn-primary">
        <span>+ Новый проект</span>
      </router-link>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>Загрузка проектов...</span>
    </div>

    <div v-else-if="projects.length === 0" class="bento-card">
      <EmptyState
        type="no-pipelines"
        title="Проекты пока не созданы"
        description="Подключите GitHub или любой Git-репозиторий, чтобы настроить автоматическую сборку и тестирование."
        action-text="+ Создать первый проект"
        @action="$router.push('/projects/new')"
      />
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
              <Icon name="folder" :size="15" color="var(--text-secondary)" />
            </div>
            <span class="project-name">{{ project.name }}</span>
          </div>
          <Icon :name="providerIcon(project.gitProvider)" :size="16" color="var(--text-secondary)" />
        </div>
        <div class="project-repo">{{ project.repoUrl }}</div>
        <div class="project-footer">
          <span class="project-branch">
            <Icon name="git-branch" :size="12" />
            <span>{{ project.defaultBranch }}</span>
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
import EmptyState from '@/components/common/EmptyState.vue'

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
  gap: 16px;
}

.project-card {
  text-decoration: none;
  color: inherit;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 18px 20px;
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
}

.project-card:hover {
  transform: translateY(-1px);
  border-color: var(--border-color-hover);
  background: var(--bg-card-hover);
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
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.project-name {
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.project-repo {
  font-size: 12px;
  color: var(--text-secondary);
  word-break: break-all;
  margin-bottom: 16px;
  font-family: var(--font-mono);
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid var(--border-muted);
  padding-top: 12px;
}

.project-branch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.project-status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  color: var(--text-muted);
}

.project-status.active {
  color: var(--accent-green);
  border-color: var(--accent-green-border);
  background: var(--accent-green-bg);
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
