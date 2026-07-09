<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">Новый проект</h1>
    </div>

    <!-- 1. NOT AUTHORIZED STATE -->
    <div v-if="!oAuthConnected" class="card empty-state-container" style="max-width: 600px; margin: 40px auto; padding: 48px 32px; text-align: center; display: flex; flex-direction: column; align-items: center; justify-content: center;">
      <div class="github-large-icon" style="color: var(--accent); margin-bottom: 24px; display: flex; align-items: center; justify-content: center;">
        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-github"><path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"/><path d="M9 18c-4.51 2-5-2-7-2"/></svg>
      </div>
      <h2 style="margin: 0 0 12px 0; font-size: 20px; font-weight: 700; color: var(--text-primary);">Интеграция с GitHub</h2>
      <p style="margin: 0 0 28px 0; font-size: 14px; color: var(--text-secondary); max-width: 440px; line-height: 1.5;">
        Подключите ваш GitHub аккаунт, чтобы просмотреть список доступных репозиториев, выбрать ветки сборки и автоматически найти конфигурационные файлы пайплайна.
      </p>
      
      <div v-if="oAuthClientId === ''" class="info-box-warning" style="margin-top: 16px; padding: 12px 16px; font-size: 13.5px; color: var(--text-secondary); background: var(--surface-hover); border-radius: 8px; border: 1px solid var(--border); display: flex; align-items: center; gap: 8px; max-width: 440px; line-height: 1.4; text-align: left;">
        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--text-secondary); flex-shrink: 0;"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
        GitHub OAuth не настроен на сервере. Укажите GITHUB_CLIENT_ID и GITHUB_CLIENT_SECRET в конфигурации.
      </div>
      
      <button 
        v-else
        type="button" 
        class="btn btn-primary" 
        style="padding: 12px 28px; font-size: 15px; font-weight: 600;" 
        @click="initiateOAuth"
        :disabled="oAuthClientId === null"
      >
        <span v-if="oAuthClientId === null" style="display: flex; align-items: center; gap: 8px;">
          <span class="spinner" style="width: 16px; height: 16px; border-width: 2px; border-top-color: #ffffff;"></span>
          Загрузка конфигурации...
        </span>
        <span v-else>Подключить GitHub аккаунт</span>
      </button>
      
      <div v-if="error" class="error-message" style="margin-top: 20px; width: 100%;">{{ error }}</div>
    </div>

    <!-- 2. AUTHORIZED STATE -->
    <div v-else style="max-width: 600px; margin: 0 auto; display: flex; flex-direction: column; gap: 20px;">
      <!-- Active session status card -->
      <div class="card" style="padding: 14px 20px; display: flex; align-items: center; justify-content: space-between; gap: 16px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <div style="width: 8px; height: 8px; border-radius: 50%; background: var(--success); box-shadow: 0 0 8px var(--success);"></div>
          <span style="font-size: 13.5px; font-weight: 600; color: var(--text-secondary);">GitHub подключен</span>
        </div>
        <button type="button" class="btn btn-danger" style="padding: 6px 12px; font-size: 12px; font-weight: 600;" @click="logoutOAuth">
          Отключить
        </button>
      </div>

      <!-- Main Project Creation Form -->
      <form @submit.prevent="handleSubmit" class="card" style="display: flex; flex-direction: column; gap: 20px;">
        <!-- 2.1 Repository select dropdown -->
        <div class="form-group repo-select-container" style="position: relative;">
          <label class="form-label">Репозиторий *</label>
          
          <div v-if="reposLoading && gitHubRepos.length === 0" style="display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--text-secondary); padding: 11px 0;">
            <div class="spinner" style="width: 18px; height: 18px; border-width: 2px;"></div>
            Загрузка списка репозиториев с GitHub...
          </div>
          
          <div v-else-if="reposError" class="field-error-text" style="margin-bottom: 8px;">{{ reposError }}</div>
          
          <div v-else>
            <button 
              type="button" 
              class="form-input" 
              style="width: 100%; text-align: left; display: flex; justify-content: space-between; align-items: center; cursor: pointer; background: #ffffff; padding: 11px 14px;"
              @click="showRepoDropdown = !showRepoDropdown"
            >
              <span v-if="selectedRepo" style="font-weight: 600; color: var(--text-primary);">{{ selectedRepo.full_name }}</span>
              <span v-else style="color: var(--text-muted);">Выберите репозиторий...</span>
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="transition: transform 0.2s; color: var(--text-secondary);" :style="showRepoDropdown ? 'transform: rotate(180deg);' : ''"><path d="m6 9 6 6 6-6"/></svg>
            </button>

            <!-- Custom Dropdown Menu -->
            <div v-if="showRepoDropdown" class="card dropdown-card" style="position: absolute; top: calc(100% + 6px); left: 0; right: 0; z-index: 100; padding: 12px; max-height: 320px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.05); display: flex; flex-direction: column; gap: 8px;">
              <input 
                v-model="repoSearchQuery" 
                class="form-input" 
                placeholder="Поиск репозитория..." 
                style="margin-bottom: 4px; background: var(--surface-hover);"
                @click.stop
              />
              
              <div class="repos-list-scroll" style="overflow-y: auto; display: flex; flex-direction: column; gap: 4px; max-height: 200px;">
                <div v-if="filteredRepos.length === 0" style="padding: 16px; text-align: center; color: var(--text-muted); font-size: 13px;">
                  Репозитории не найдены
                </div>
                <div 
                  v-else 
                  v-for="repo in filteredRepos" 
                  :key="repo.id" 
                  class="dropdown-repo-item"
                  :class="{ 'active-repo': selectedRepo && selectedRepo.id === repo.id }"
                  @click="handleSelectRepo(repo)"
                >
                  <div style="display: flex; flex-direction: column; gap: 3px; flex: 1; min-width: 0;">
                    <span class="repo-title-text">{{ repo.full_name }}</span>
                    <span v-if="repo.description" class="repo-desc-text">{{ repo.description }}</span>
                  </div>
                  <div style="display: flex; align-items: center; gap: 8px; flex-shrink: 0;">
                    <span class="privacy-tag" :class="{ 'private': repo.private }">
                      {{ repo.private ? 'Private' : 'Public' }}
                    </span>
                    <code style="font-size: 11px; padding: 1px 5px; border-radius: 4px; background: var(--surface); color: var(--text-secondary); border: 1px solid var(--border);">{{ repo.default_branch }}</code>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <span v-if="validationErrors.repoUrl" class="field-error-text">{{ validationErrors.repoUrl }}</span>
        </div>

        <!-- 2.2 Project Name (Auto-filled but editable) -->
        <div class="form-group">
          <label class="form-label">Название проекта *</label>
          <input 
            v-model="form.name" 
            class="form-input" 
            :class="{ 'input-error': validationErrors.name }" 
            placeholder="my-awesome-app" 
            required 
            :disabled="!selectedRepo"
          />
          <span v-if="validationErrors.name" class="field-error-text">{{ validationErrors.name }}</span>
          <div style="font-size: 11.5px; color: var(--text-secondary); margin-top: 2px;">
            Название может содержать только латинские буквы, цифры, дефисы и подчеркивания.
          </div>
        </div>

        <!-- 2.3 Branch Selector -->
        <div class="form-group">
          <label class="form-label">Ветка по умолчанию *</label>
          
          <div v-if="branchesLoading" style="display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--text-secondary); padding: 11px 0;">
            <div class="spinner" style="width: 18px; height: 18px; border-width: 2px;"></div>
            Загрузка веток с GitHub...
          </div>
          
          <select 
            v-else 
            v-model="form.defaultBranch" 
            class="form-select"
            :disabled="!selectedRepo || branches.length === 0"
            style="width: 100%;"
          >
            <option v-if="branches.length === 0" value="">Сначала выберите репозиторий...</option>
            <option v-for="branch in branches" :key="branch.name" :value="branch.name">
              {{ branch.name }}
            </option>
          </select>
        </div>

        <!-- 2.4 Pipeline config file picker -->
        <div class="form-group">
          <label class="form-label">Путь к конфигу пайплайна *</label>
          
          <div v-if="contentsLoading" style="display: flex; align-items: center; gap: 10px; font-size: 14px; color: var(--text-secondary); padding: 11px 0;">
            <div class="spinner" style="width: 18px; height: 18px; border-width: 2px;"></div>
            Поиск файлов конфигурации в репозитории...
          </div>
          
          <div v-else style="display: flex; flex-direction: column; gap: 10px;">
            <select 
              v-model="configSelectionType" 
              class="form-select"
              :disabled="!selectedRepo"
              style="width: 100%;"
            >
              <option v-if="!selectedRepo" value="">Сначала выберите репозиторий...</option>
              <option v-else-if="configOptions.length === 0" value="custom">Файлы конфигов не найдены (ввести вручную)</option>
              <template v-else>
                <option v-for="opt in configOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
                <option value="custom">Ввести путь вручную...</option>
              </template>
            </select>

            <!-- Custom config text input if "custom" selected -->
            <input 
              v-if="configSelectionType === 'custom'" 
              v-model="form.pipelineConfigPath" 
              class="form-input" 
              placeholder=".action.yaml"
              required
              :disabled="!selectedRepo"
            />
          </div>
        </div>

        <!-- Submit actions -->
        <div v-if="error" class="error-message">{{ error }}</div>

        <div style="display: flex; gap: 12px; margin-top: 16px;">
          <button type="submit" class="btn btn-primary" :disabled="submitting || !selectedRepo">
            {{ submitting ? 'Создаю...' : 'Создать проект' }}
          </button>
          <router-link to="/projects" class="btn">Отмена</router-link>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { 
  createProject, 
  getGithubOauthConfig, 
  getGithubOauthStatus, 
  disconnectGithubOauth,
  getGithubUserRepos,
  getGithubRepoBranches,
  getGithubRepoContents
} from '@/api/client'
import type { CreateProjectRequest, GitProvider } from '@/types'

const router = useRouter()
const error = ref('')
const submitting = ref(false)

const oAuthClientId = ref<string | null>(null)
const oAuthConnected = ref(false)

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

interface GitHubBranch {
  name: string
}

const gitHubRepos = ref<GitHubRepo[]>([])
const selectedRepo = ref<GitHubRepo | null>(null)
const showRepoDropdown = ref(false)
const repoSearchQuery = ref('')
const reposLoading = ref(false)
const reposError = ref('')

const branches = ref<GitHubBranch[]>([])
const branchesLoading = ref(false)

const contentsLoading = ref(false)
const configOptions = ref<{ label: string; value: string }[]>([])
const configSelectionType = ref('.action.yaml')

const form = ref<CreateProjectRequest>({
  name: '',
  repoUrl: '',
  gitProvider: 'GITHUB' as GitProvider,
  defaultBranch: '',
  pipelineConfigPath: '.action.yaml',
  githubToken: '',
})

const validationErrors = ref({
  name: '',
  repoUrl: ''
})

const checkConnectionStatus = async () => {
  try {
    const res = await getGithubOauthStatus()
    if (res.data.success) {
      oAuthConnected.value = !!res.data.data?.connected
    }
  } catch (err) {
    console.error("Failed to check GitHub integration status", err)
  }
}

// Load branches for selected repo through backend proxy
const fetchRepoBranches = async () => {
  if (!selectedRepo.value) return
  branchesLoading.value = true
  branches.value = []
  try {
    const parts = selectedRepo.value.full_name.split('/')
    if (parts.length !== 2) return
    const [owner, name] = parts
    
    const response = await getGithubRepoBranches(owner, name)
    if (response.data.success) {
      branches.value = response.data.data
    }
  } catch (err: any) {
    console.error("Failed to load branches from GitHub API", err)
    if (err.response?.status === 401) {
      handleUnauthorized()
    }
  } finally {
    branchesLoading.value = false
  }
}

// Fetch root contents of repository to look for configuration files through backend proxy
const fetchRepoContents = async () => {
  if (!selectedRepo.value) return
  contentsLoading.value = true
  configOptions.value = []
  try {
    const parts = selectedRepo.value.full_name.split('/')
    if (parts.length !== 2) return
    const [owner, name] = parts
    
    const refBranch = form.value.defaultBranch || selectedRepo.value.default_branch
    const response = await getGithubRepoContents(owner, name, refBranch)
    if (response.data.success && Array.isArray(response.data.data)) {
      // Scan root directory for potential config files
      const files = response.data.data.filter((item: any) => 
        item.type === 'file' && 
        (item.name.endsWith('.yaml') || item.name.endsWith('.yml') || item.name.endsWith('.json'))
      )
      
      configOptions.value = files.map((file: any) => ({
        label: file.name,
        value: file.path
      }))

      // Find default config candidate (.action.yaml or .rabotyaga.yaml)
      const defaultCandidate = configOptions.value.find(opt => 
        opt.value === '.action.yaml' || opt.value === '.rabotyaga.yaml' || opt.value === '.rabotyaga-ci.yaml'
      )
      
      if (defaultCandidate) {
        configSelectionType.value = defaultCandidate.value
        form.value.pipelineConfigPath = defaultCandidate.value
      } else if (configOptions.value.length > 0) {
        configSelectionType.value = configOptions.value[0].value
        form.value.pipelineConfigPath = configOptions.value[0].value
      } else {
        configSelectionType.value = 'custom'
        form.value.pipelineConfigPath = '.action.yaml'
      }
    }
  } catch (err: any) {
    console.error("Failed to load repo contents from GitHub API", err)
    if (err.response?.status === 401) {
      handleUnauthorized()
    } else {
      configSelectionType.value = 'custom'
      form.value.pipelineConfigPath = '.action.yaml'
    }
  } finally {
    contentsLoading.value = false
  }
}

const handleSelectRepo = async (repo: GitHubRepo) => {
  selectedRepo.value = repo
  form.value.name = repo.name
  form.value.repoUrl = repo.clone_url
  form.value.defaultBranch = repo.default_branch
  form.value.githubToken = '' // Omit sending raw token to backend, backend resolves it
  showRepoDropdown.value = false
  
  validationErrors.value.repoUrl = ''
  validationErrors.value.name = ''
  
  // Concurrently load branches and scan files
  await Promise.all([
    fetchRepoBranches(),
    fetchRepoContents()
  ])
}

const fetchUserRepos = async () => {
  reposLoading.value = true
  reposError.value = ''
  gitHubRepos.value = []
  try {
    const response = await getGithubUserRepos()
    if (response.data.success) {
      gitHubRepos.value = response.data.data
    }
  } catch (err: any) {
    console.error('Failed to fetch repos from GitHub API proxy', err)
    if (err.response?.status === 401) {
      handleUnauthorized()
    } else {
      reposError.value = err.response?.data?.message || 'Не удалось загрузить список репозиториев'
    }
  } finally {
    reposLoading.value = false
  }
}

const initiateOAuth = () => {
  if (!oAuthClientId.value) return
  const redirectUri = `${window.location.origin}/callback`
  
  // Generate random state for CSRF protection
  const state = Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15)
  sessionStorage.setItem('github_oauth_state', state)
  
  const authUrl = `https://github.com/login/oauth/authorize?client_id=${oAuthClientId.value}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=repo&state=${state}`
  window.location.href = authUrl
}

const handleUnauthorized = () => {
  reposError.value = 'Сессия авторизации в GitHub устарела. Подключите аккаунт заново.'
  logoutOAuth()
}

const logoutOAuth = async () => {
  try {
    await disconnectGithubOauth()
  } catch (err) {
    console.error("Failed to disconnect oauth on backend", err)
  }
  oAuthConnected.value = false
  gitHubRepos.value = []
  selectedRepo.value = null
  branches.value = []
  configOptions.value = []
}

const filteredRepos = computed(() => {
  if (!repoSearchQuery.value) return gitHubRepos.value
  const q = repoSearchQuery.value.toLowerCase()
  return gitHubRepos.value.filter(
    r => r.name.toLowerCase().includes(q) || r.full_name.toLowerCase().includes(q)
  )
})

// Listen to branch change to reload configuration candidate files
watch(() => form.value.defaultBranch, async (newBranch) => {
  if (newBranch && selectedRepo.value) {
    await fetchRepoContents()
  }
})

// Bind config picker value to form field
watch(configSelectionType, (newType) => {
  if (newType !== 'custom') {
    form.value.pipelineConfigPath = newType
  }
})

// Click outside logic to close repo dropdown
const closeDropdownOnOutsideClick = (e: MouseEvent) => {
  const container = document.querySelector('.repo-select-container')
  if (container && !container.contains(e.target as Node)) {
    showRepoDropdown.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', closeDropdownOnOutsideClick)
  
  try {
    const res = await getGithubOauthConfig()
    if (res.data.success && res.data.data?.clientId) {
      oAuthClientId.value = res.data.data.clientId
    }
  } catch (err) {
    console.error("Failed to load GitHub OAuth client config", err)
  }

  await checkConnectionStatus()

  if (oAuthConnected.value) {
    await fetchUserRepos()
  }
})

onUnmounted(() => {
  document.removeEventListener('click', closeDropdownOnOutsideClick)
})

watch(() => form.value.name, (name) => {
  if (name.trim()) {
    validationErrors.value.name = '';
  }
})

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
  
  if (!form.value.repoUrl.trim()) {
    validationErrors.value.repoUrl = 'Репозиторий не выбран';
    isValid = false;
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
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius);
  margin-top: 10px;
}

.field-error-text {
  color: #ef4444;
  font-size: 12px;
  margin-top: 4px;
  font-weight: 600;
}

.input-error {
  border-color: #ef4444 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.15) !important;
}

.dropdown-card {
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: 10px;
}

.dropdown-repo-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s ease;
  gap: 12px;
  border: 1px solid transparent;
}

.dropdown-repo-item:hover {
  background: var(--surface-hover);
  border-color: rgba(79, 70, 229, 0.15);
}

.dropdown-repo-item.active-repo {
  background: rgba(79, 70, 229, 0.05);
  border-color: var(--accent);
}

.repo-title-text {
  font-weight: 600;
  font-size: 13.5px;
  color: var(--text-primary);
  word-break: break-all;
}

.repo-desc-text {
  font-size: 11px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.privacy-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: 700;
  background: rgba(16, 185, 129, 0.1);
  color: var(--success);
}

.privacy-tag.private {
  background: rgba(239, 68, 68, 0.1);
  color: var(--failure);
}
</style>
