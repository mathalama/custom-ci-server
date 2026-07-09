<template>
  <div class="callback-container">
    <div class="card spinner-card">
      <div v-if="!error" class="spinner"></div>
      <h2>Авторизация в GitHub...</h2>
      <p class="status-text">{{ statusText }}</p>
      <div v-if="error" class="error-box">
        <p>{{ error }}</p>
        <button class="btn btn-primary" @click="goBack">Вернуться назад</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { exchangeGithubCode } from '@/api/client'

const route = useRoute()
const router = useRouter()
const statusText = ref('Пожалуйста, подождите, обмениваем код авторизации на токен...')
const error = ref('')

const goBack = () => {
  router.push('/settings')
}

onMounted(async () => {
  const code = route.query.code as string
  const state = route.query.state as string
  const savedState = sessionStorage.getItem('github_oauth_state')

  // Clear state immediately in all cases to prevent reuse and avoid leaving stale state on mismatch
  sessionStorage.removeItem('github_oauth_state')

  // CSRF validation
  if (!state || state !== savedState) {
    console.error('CSRF State mismatch!', { state, savedState })
    error.value = 'Ошибка безопасности: несовпадение проверочного кода CSRF (State mismatch). Попробуйте заново.'
    statusText.value = 'Ошибка безопасности'
    return
  }

  if (!code) {
    error.value = 'Код авторизации не найден в URL. Попробуйте войти заново.'
    statusText.value = 'Ошибка авторизации'
    return
  }

  try {
    const res = await exchangeGithubCode(code)
    if (res.data.success && res.data.data?.status === 'connected') {
      statusText.value = 'Успешно авторизовано! Перенаправление...'
      setTimeout(() => {
        router.push('/settings')
      }, 1000)
    } else {
      error.value = res.data.error || 'Не удалось завершить подключение аккаунта.'
      statusText.value = 'Ошибка при подключении'
    }
  } catch (err: any) {
    console.error('OAuth callback error', err)
    error.value = err.response?.data?.error || 'Сетевая ошибка при обмене кода на сервере.'
    statusText.value = 'Ошибка запроса'
  }
})
</script>

<style scoped>
.callback-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
}

.spinner-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  max-width: 450px;
  text-align: center;
  width: 100%;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--border);
  border-radius: 50%;
  border-top-color: var(--accent);
  animation: spin 1s cubic-bezier(0.4, 0, 0.2, 1) infinite;
  margin-bottom: 24px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.status-text {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 8px 0 16px 0;
}

.error-box {
  color: var(--failure);
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}
</style>
