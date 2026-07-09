<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Новый проект</h1>
    </div>

    <div v-if="oAuthClientId" class="card bento-box" style="max-width: 600px; margin-bottom: 24px;">
      <div style="display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap;">
        <div>
          <h3 style="margin: 0 0 4px 0; font-size: 16px; font-weight: 700;">Интеграция с GitHub</h3>
          <p style="margin: 0; font-size: 13px; color: var(--text-secondary);">
            {{ oAuthToken ? 'Вы успешно авторизованы' : 'Войдите, чтобы выбрать репозиторий из списка' }}
          </p>
        </div>
        <div>
          <button 
            v-if="!oAuthToken" 
            type="button" 
            class="btn btn-primary" 
            @click="initiateOAuth"
          >
            Войти через GitHub
          </button>
          <div v-else style="display: flex; gap: 8px;">
            <button 
              type="button" 
              class="btn btn-primary" 
              @click="fetchUserRepos" 
              :disabled="reposLoading"
            >
              {{ reposLoading ? 'Загрузка...' : 'Выбрать репозиторий' }}
            </button>
            <button 
              type="button" 
              class="btn btn-danger" 
              @click="logoutOAuth"
              style="padding: 9px 12px;"
              title="Выйти из аккаунта"
            >
              Выйти
            </button>
          </div>
        </div>
      </div>
      <div v-if="reposError" class="field-error-text" style="margin-top: 10px;">{{ reposError }}</div>
    </div>

    <!-- Main Creation Form -->
    <form @submit.prevent="handleSubmit" class="card" style="max-width: 600px">
      <div class="form-group">
        <label class="form-label">Название проекта *</label>
        <input 
          v-model="form.name" 
          class="form-input" 
          :class="{ 'input-error': validationErrors.name }" 
          placeholder="my-awesome-app" 
          required 
        />
        <span v-if="validationErrors.name" class="field-error-text">{{ validationErrors.name }}</span>
      </div>

      <div class="form-group">
        <label class="form-label">URL репозитория *</label>
        <input 
          v-model="form.repoUrl" 
          class="form-input" 
          :class="{ 'input-error': validationErrors.repoUrl }" 
          placeholder="https://github.com/user/repo.git" 
          required 
        />
        <span v-if="validationErrors.repoUrl" class="field-error-text">{{ validationErrors.repoUrl }}</span>
        <div v-if="form.repoUrl && !validationErrors.repoUrl" class="provider-badge-info">
          Провайдер определен автоматически: <strong>{{ form.gitProvider }}</strong>
        </div>
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

    <!-- Repository Selection Modal (Bento-style) -->
    <div v-if="showRepoModal" class="modal-overlay" @click.self="showRepoModal = false">
      <div class="modal card repo-modal">
        <div class="modal-header">
          <h3>Выберите репозиторий</h3>
          <button class="close-btn" @click="showRepoModal = false">&times;</button>
        </div>

        <div class="search-box">
          <input 
            v-model="repoSearchQuery" 
            class="form-input" 
            placeholder="Поиск по названию..." 
            style="margin-bottom: 12px;"
          />
        </div>

        <div class="repos-list-container">
          <div v-if="filteredRepos.length === 0" class="empty-state" style="padding: 24px 0;">
            <p class="empty-state-text">Репозитории не найдены</p>
          </div>
          <div 
            v-else 
            v-for="repo in filteredRepos" 
            :key="repo.id" 
            class="repo-item"
            @click="selectRepo(repo)"
          >
            <div class="repo-meta">
              <div class="repo-name-row">
                <span class="repo-name-text">{{ repo.full_name }}</span>
                <span class="repo-privacy-badge" :class="{ 'private': repo.private }">
                  {{ repo.private ? 'Private' : 'Public' }}
                </span>
              </div>
              <p class="repo-desc">{{ repo.description || 'Нет описания' }}</p>
            </div>
            <div class="repo-branch-badge">
              <code>{{ repo.default_branch }}</code>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { createProject, getGithubOauthConfig } from '@/api/client'
import type { CreateProjectRequest, GitProvider } from '@/types'

const router = useRouter()
const error = ref('')
const submitting = ref(false)

const oAuthClientId = ref('')
const oAuthToken = ref(localStorage.getItem('github_oauth_token') || '')

interface GitHubRepo {
  id: number
  name: string
  full_name: string
  html_url: string
  clone_url: string
  default_branch: string
  private: boolean
  description: string
}

const gitHubRepos = ref<GitHubRepo[]>([])
const reposLoading = ref(false)
const reposError = ref('')
const showRepoModal = ref(false)
const repoSearchQuery = ref('')

const form = ref<CreateProjectRequest>({
  name: '',
  repoUrl: '',
  gitProvider: 'GITHUB' as GitProvider,
  defaultBranch: 'main',
  pipelineConfigPath: '.action.yaml',
  githubToken: '',
})

const validationErrors = ref({
  name: '',
  repoUrl: ''
})

onMounted(async () => {
  try {
    const res = await getGithubOauthConfig()
    if (res.data.success && res.data.data?.clientId) {
      oAuthClientId.value = res.data.data.clientId
    }
  } catch (err) {
    console.error("Failed to load GitHub OAuth client config", err)
  }
})

const initiateOAuth = () => {
  if (!oAuthClientId.value) return
  const redirectUri = `${window.location.origin}/callback`
  const authUrl = `https://github.com/login/oauth/authorize?client_id=${oAuthClientId.value}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=repo`
  window.location.href = authUrl
}

const fetchUserRepos = async () => {
  if (!oAuthToken.value) return
  reposLoading.value = true
  reposError.value = ''
  gitHubRepos.value = []
  try {
    const response = await axios.get('https://api.github.com/user/repos', {
      headers: {
        Authorization: `Bearer ${oAuthToken.value}`,
        Accept: 'application/vnd.github+json'
      },
      params: {
        per_page: 100,
        sort: 'updated'
      }
    })
    gitHubRepos.value = response.data
    showRepoModal.value = true
  } catch (err: any) {
    console.error('Failed to fetch repos from GitHub API', err)
    if (err.response?.status === 401) {
      reposError.value = 'Сессия авторизации устарела. Войдите заново.'
      logoutOAuth()
    } else {
      reposError.value = err.response?.data?.message || 'Не удалось загрузить список репозиториев'
    }
  } finally {
    reposLoading.value = false
  }
}

const selectRepo = (repo: GitHubRepo) => {
  form.value.name = repo.name
  form.value.repoUrl = repo.clone_url
  form.value.defaultBranch = repo.default_branch
  form.value.githubToken = oAuthToken.value
  showRepoModal.value = false
}

const logoutOAuth = () => {
  localStorage.removeItem('github_oauth_token')
  oAuthToken.value = ''
  gitHubRepos.value = []
}

const filteredRepos = computed(() => {
  if (!repoSearchQuery.value) return gitHubRepos.value
  const q = repoSearchQuery.value.toLowerCase()
  return gitHubRepos.value.filter(
    r => r.name.toLowerCase().includes(q) || r.full_name.toLowerCase().includes(q)
  )
})

// Watch repository URL and parse the provider automatically
watch(() => form.value.repoUrl, (url) => {
  if (!url) return;
  const normalized = url.toLowerCase();
  if (normalized.includes('github.com')) {
    form.value.gitProvider = 'GITHUB';
  } else if (normalized.includes('gitlab.com')) {
    form.value.gitProvider = 'GITLAB';
  } else if (normalized.includes('gitea')) {
    form.value.gitProvider = 'GITEA';
  }
  if (url.trim()) {
    validationErrors.value.repoUrl = '';
  }
});

// Watch project name to clear error
watch(() => form.value.name, (name) => {
  if (name.trim()) {
    validationErrors.value.name = '';
  }
});

const validateForm = () => {
  let isValid = true;
  validationErrors.value.name = '';
  validationErrors.value.repoUrl = '';
  
  if (!form.value.name.trim()) {
    validationErrors.value.name = 'Название проекта не может быть пустым';
    isValid = false;
  } else if (!/^[a-zA-Z0-9-_]+$/.test(form.value.name)) {
    validationErrors.value.name = 'Название может содержать только латинские буквы, цифры, дефисы и подчеркивания';
    isValid = false;
  }
  
  const url = form.value.repoUrl.trim();
  if (!url) {
    validationErrors.value.repoUrl = 'URL репозитория обязателен';
    isValid = false;
  } else {
    const isHttps = url.startsWith('http://') || url.startsWith('https://');
    const isSsh = url.startsWith('git@') || url.startsWith('ssh://');
    if (!isHttps && !isSsh) {
      validationErrors.value.repoUrl = 'Введите корректный URL (HTTPS или SSH, например https://github.com/... или git@github.com:...)';
      isValid = false;
    }
  }
  
  return isValid;
}

const handleSubmit = async () => {
  if (!validateForm()) return;
  
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

.field-error-text {
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
  font-weight: 500;
}

.input-error {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}

.provider-badge-info {
  font-size: 12px;
  color: var(--success);
  margin-top: 4px;
  font-weight: 500;
}

/* Modal and list styles */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.repo-modal {
  width: 100%;
  max-width: 550px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  padding: 24px;
  overflow: hidden;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.close-btn {
  background: transparent;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
}

.close-btn:hover {
  color: var(--text-primary);
}

.repos-list-container {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 4px;
}

.repo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--surface-hover);
  border: 1px solid var(--border);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.repo-item:hover {
  border-color: var(--accent);
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
}

.repo-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.repo-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.repo-name-text {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
  word-break: break-all;
}

.repo-privacy-badge {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 600;
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
}

.repo-privacy-badge.private {
  background: rgba(239, 68, 68, 0.1);
  color: var(--failure);
}

.repo-desc {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.repo-branch-badge code {
  background: var(--bg-dark);
  border: 1px solid var(--border);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 11px;
}
</style>
