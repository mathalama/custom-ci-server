<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Новый проект</h1>
    </div>

    <form @submit.prevent="handleSubmit" class="card" style="max-width: 600px">
      <div class="form-group">
        <label class="form-label">Название проекта *</label>
        <input v-model="form.name" class="form-input" placeholder="my-awesome-app" required />
      </div>

      <div class="form-group">
        <label class="form-label">URL репозитория *</label>
        <input v-model="form.repoUrl" class="form-input" placeholder="https://github.com/user/repo.git" required />
      </div>

      <div class="form-group">
        <label class="form-label">Git провайдер *</label>
        <select v-model="form.gitProvider" class="form-select" required>
          <option value="GITHUB">GitHub</option>
          <option value="GITLAB">GitLab</option>
          <option value="GITEA">Gitea</option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">Ветка по умолчанию</label>
        <input v-model="form.defaultBranch" class="form-input" placeholder="main" />
      </div>

      <div class="form-group">
        <label class="form-label">Путь к конфигу пайплайна</label>
        <input v-model="form.pipelineConfigPath" class="form-input" placeholder=".rabotyaga.yaml" />
      </div>

      <div class="form-group">
        <label class="form-label">GitHub Token (опционально)</label>
        <input v-model="form.githubToken" type="password" class="form-input" placeholder="ghp_..." />
        <div style="font-size: 12px; color: var(--text-secondary); margin-top: 4px;">Нужен для отправки статусов сборок в GitHub PRs/Commits</div>
      </div>

      <div v-if="error" class="error-message">{{ error }}</div>

      <div style="display: flex; gap: 12px; margin-top: 24px">
        <button type="submit" class="btn btn-primary" :disabled="submitting">
          {{ submitting ? 'Создаю...' : 'Создать проект' }}
        </button>
        <router-link to="/projects" class="btn">Отмена</router-link>
      </div>
    </form>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProject } from '@/api/client'
import type { CreateProjectRequest, GitProvider } from '@/types'

const router = useRouter()
const error = ref('')
const submitting = ref(false)

const form = ref<CreateProjectRequest>({
  name: '',
  repoUrl: '',
  gitProvider: 'GITHUB' as GitProvider,
  defaultBranch: 'main',
  pipelineConfigPath: '.action.yaml',
  githubToken: '',
})

const handleSubmit = async () => {
  error.value = ''
  submitting.value = true
  try {
    const res = await createProject(form.value)
    router.push(`/projects/${res.data.data.id}`)
  } catch (e: any) {
    error.value = e.response?.data?.error || 'Ошибка при создании проекта'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.error-message {
  color: var(--failure);
  font-size: 14px;
  padding: 8px 12px;
  background: var(--surface-elevated);
  border: 1px solid var(--failure);
  border-radius: var(--radius);
  margin-top: 12px;
}
</style>
