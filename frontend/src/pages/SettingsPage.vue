<template>
  <div style="max-width: 1080px; margin: 0 auto; width: 100%;">
    <div class="page-header">
      <h1 class="page-title">Настройки</h1>
      <p class="page-subtitle">Управление интеграциями и системными параметрами RabotyagaCI</p>
    </div>

    <div class="bento-grid">
      <!-- 1. GitHub Integration Bento Card -->
      <div class="card github-card">
        <h2 class="card-title" style="display: flex; align-items: center; gap: 10px;">
          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-github"><path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4"/><path d="M9 18c-4.51 2-5-2-7-2"/></svg>
          Интеграция с GitHub
        </h2>

        <div v-if="userLoading" class="loading-state">
          <div class="spinner"></div>
          <span>Загрузка данных профиля...</span>
        </div>

        <div v-else-if="oAuthConnected && gitHubUser" class="profile-container">
          <div class="profile-header">
            <img :src="gitHubUser.avatar_url" alt="Avatar" class="avatar" />
            <div class="profile-info">
              <span class="profile-name">{{ gitHubUser.name || gitHubUser.login }}</span>
              <a :href="gitHubUser.html_url" target="_blank" class="profile-login">@{{ gitHubUser.login }}</a>
            </div>
          </div>
          
          <div class="profile-stats">
            <div class="stat-item">
              <span class="stat-lbl">Публичные репозитории</span>
              <span class="stat-val">{{ gitHubUser.public_repos }}</span>
            </div>
            <div class="stat-item" v-if="gitHubUser.total_private_repos !== undefined">
              <span class="stat-lbl">Приватные репозитории</span>
              <span class="stat-val">{{ gitHubUser.total_private_repos }}</span>
            </div>
          </div>

          <div class="action-bar">
            <span class="status-badge connected">Активен</span>
            <button type="button" class="btn btn-danger" @click="logoutOAuth">
              Отвязать аккаунт
            </button>
          </div>
        </div>

        <div v-else class="connect-container">
          <p class="connect-desc">
            Подключите вашу учетную запись GitHub, чтобы выбирать репозитории и ветки прямо из интерфейса RabotyagaCI при создании новых проектов.
          </p>
          <button 
            type="button" 
            class="btn btn-primary" 
            @click="initiateOAuth"
            :disabled="!oAuthClientId"
            style="width: 100%; justify-content: center; padding: 12px;"
          >
            <span v-if="!oAuthClientId" style="display: flex; align-items: center; gap: 8px;">
              <span class="spinner" style="width: 16px; height: 16px; border-width: 2px; border-top-color: #ffffff;"></span>
              Загрузка конфигурации...
            </span>
            <span v-else>Подключить GitHub аккаунт</span>
          </button>
        </div>

        <div v-if="error" class="error-message">{{ error }}</div>
      </div>

      <!-- 2. System Status Bento Card -->
      <div class="card system-card">
        <h2 class="card-title" style="display: flex; align-items: center; gap: 10px;">
          <Icon name="gear" :size="20" />
          Системная информация
        </h2>

        <div class="system-details">
          <div class="detail-row">
            <span class="detail-label">Версия приложения</span>
            <span class="detail-value font-mono">{{ systemVersion || 'Загрузка...' }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">Статус бэкенда</span>
            <span class="detail-value">
              <span class="status-indicator" :class="{ 'ok': backendConnected }"></span>
              {{ backendConnected ? 'Подключен' : 'Нет связи' }}
            </span>
          </div>
          <div class="detail-row">
            <span class="detail-label">База данных</span>
            <span class="detail-value">
              <span class="status-indicator" :class="{ 'ok': dbHealthy }"></span>
              {{ dbHealthy ? 'Здорова' : 'Неизвестно' }}
            </span>
          </div>
        </div>

        <div class="system-about">
          <h3>О платформе</h3>
          <p>
            RabotyagaCI — это легковесный, производительный CI/CD сервер, спроектированный для сборки проектов в изолированных Docker-контейнерах с поддержкой кэширования шагов и уведомлений.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { 
  getGithubOauthConfig, 
  getGithubOauthStatus, 
  disconnectGithubOauth, 
  getGithubProfile,
  getSystemInfo
} from '@/api/client'
import Icon from '@/components/common/Icon.vue'

const error = ref('')
const oAuthClientId = ref('')
const oAuthConnected = ref(false)

const gitHubUser = ref<any>(null)
const userLoading = ref(false)

const systemVersion = ref('')
const backendConnected = ref(false)
const dbHealthy = ref(false)

const fetchGitHubUser = async () => {
  userLoading.value = true
  error.value = ''
  try {
    const res = await getGithubProfile()
    if (res.data.success) {
      gitHubUser.value = res.data.data
    }
  } catch (err: any) {
    console.error("Failed to load GitHub profile", err)
    if (err.response?.status === 401) {
      oAuthConnected.value = false
      gitHubUser.value = null
      error.value = 'Сессия GitHub устарела. Пожалуйста, подключите аккаунт заново.'
    } else {
      error.value = 'Не удалось загрузить данные профиля с бэкенда.'
    }
  } finally {
    userLoading.value = false
  }
}

const checkSystemStatus = async () => {
  try {
    // Get version and connection status from custom endpoint
    const res = await getSystemInfo()
    if (res.data.success) {
      systemVersion.value = res.data.data.version
      backendConnected.value = true
    }
    
    // Check db status through health endpoint
    const healthRes = await axios.get('/actuator/health')
    if (healthRes.status === 200 && healthRes.data.status === 'UP') {
      dbHealthy.value = true
    }
  } catch (err) {
    console.error("Failed to check backend health status", err)
    if (systemVersion.value) {
      backendConnected.value = true
    } else {
      backendConnected.value = false
    }
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

const logoutOAuth = async () => {
  try {
    await disconnectGithubOauth()
  } catch (err) {
    console.error("Failed to disconnect GitHub integration", err)
  }
  oAuthConnected.value = false
  gitHubUser.value = null
}

onMounted(async () => {
  // Load OAuth client ID configuration
  try {
    const res = await getGithubOauthConfig()
    if (res.data.success && res.data.data?.clientId) {
      oAuthClientId.value = res.data.data.clientId
    }
  } catch (err) {
    console.error("Failed to load GitHub OAuth client configuration", err)
  }

  await checkSystemStatus()
  
  // Check backend integration status
  try {
    const res = await getGithubOauthStatus()
    if (res.data.success) {
      oAuthConnected.value = !!res.data.data?.connected
    }
  } catch (err) {
    console.error("Failed to load OAuth integration status", err)
  }

  if (oAuthConnected.value) {
    await fetchGitHubUser()
  }
})
</script>

<style scoped>
.bento-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  align-items: stretch;
}

@media (max-width: 768px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }
}

.github-card, .system-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 14px;
}

.profile-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-muted);
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.profile-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.profile-login {
  font-size: 13px;
  color: var(--text-secondary);
  text-decoration: none;
  font-family: var(--font-mono);
}

.profile-login:hover {
  color: var(--text-primary);
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  background: var(--bg-surface);
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-lbl {
  font-size: 11px;
  color: var(--text-muted);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.stat-val {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 10px;
}

.status-badge {
  font-size: 11.5px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-weight: 500;
}

.status-badge.connected {
  background: var(--accent-green-bg);
  border: 1px solid var(--accent-green-border);
  color: var(--accent-green);
}

.connect-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.connect-desc {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.55;
  margin: 0;
}

.error-message {
  color: var(--accent-red);
  font-size: 12.5px;
  padding: 10px 12px;
  background: var(--accent-red-bg);
  border: 1px solid var(--accent-red-border);
  border-radius: var(--radius-sm);
}

/* System Status Styles */
.system-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-muted);
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: var(--text-secondary);
  font-weight: 500;
}

.detail-value {
  color: var(--text-primary);
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.font-mono {
  font-family: var(--font-mono);
  font-size: 12px;
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
}

.status-indicator.ok {
  background: var(--accent-green);
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.4);
}

.system-about h3 {
  font-size: 13.5px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: var(--text-primary);
}

.system-about p {
  font-size: 12.5px;
  color: var(--text-secondary);
  line-height: 1.55;
  margin: 0;
}
</style>
