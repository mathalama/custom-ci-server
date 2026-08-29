<template>
  <div class="runners-page">
    <div class="page-header">
      <div>
        <h1 class="page-title">Раннеры сборок</h1>
        <p class="page-subtitle">Управление удаленными исполнителями через легковесный Go-агент (Pull-модель)</p>
      </div>
      <button class="btn btn-primary" @click="openRegisterModal">
        <Icon name="plus" :size="14" />
        <span>Зарегистрировать агент</span>
      </button>
    </div>

    <!-- Loading & Empty state -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>Загрузка раннеров...</span>
    </div>

    <div v-else-if="runners.length === 0" class="empty-state card">
      <h2>Нет подключенных удаленных раннеров</h2>
      <p>Сборки по умолчанию выполняются локально во встроенном Docker. Запустите Go-агент на любом сервере или ПК, чтобы распределить нагрузку.</p>
      <button class="btn btn-primary" style="margin-top: 14px;" @click="openRegisterModal">
        Подключить первый агент
      </button>
    </div>

    <div v-else class="runners-grid">
      <RunnerCard
        v-for="runner in runners"
        :key="runner.id"
        :runner="runner"
        @delete="handleDelete"
      />
    </div>

    <!-- Register Agent Modal Dialog -->
    <ModalDialog
      :is-open="showRegisterModal"
      title="Регистрация нового раннера (Go Agent)"
      width="640px"
      @close="showRegisterModal = false"
    >
      <div class="register-modal-content">
        <div v-if="!generatedToken" class="token-generation-form">
          <p class="guide-text">
            Для добавления раннера сгенерируйте одноразовый registration token (действителен 15 минут).
          </p>
          <div class="form-group">
            <label class="form-label">Имя ноды (опционально)</label>
            <input
              v-model="customRunnerName"
              type="text"
              placeholder="worker-prod-01"
              class="form-input"
            />
          </div>
          <button
            class="btn btn-primary"
            :disabled="generating"
            @click="generateToken"
          >
            <span v-if="generating" class="spinner"></span>
            <span>{{ generating ? 'Генерация...' : 'Получить токен регистрации' }}</span>
          </button>
        </div>

        <div v-else class="token-result">
          <div class="token-banner">
            <div class="token-info">
              <span class="token-label">Registration Token (действует 15 минут):</span>
              <code class="token-code">{{ generatedToken }}</code>
            </div>
            <button class="btn btn-sm btn-ghost" @click="copyToClipboard(generatedToken)">
              <span>Копировать</span>
            </button>
          </div>

          <div class="instructions-block">
            <h4 class="instruction-title">Инструкция по запуску:</h4>
            <div class="instruction-step">
              <span class="step-num">1</span>
              <span>Скомпилируйте или скачайте Go-агент из папки <code>agents/go/</code>:</span>
            </div>
            <pre class="code-box"><code>cd agents/go
go build -o rabotyaga-agent .</code></pre>

            <div class="instruction-step">
              <span class="step-num">2</span>
              <span>Зарегистрируйте агент с полученным токеном:</span>
            </div>
            <pre class="code-box"><code>./rabotyaga-agent register --url {{ currentOrigin }} --token {{ generatedToken }} --name {{ customRunnerName || 'my-runner' }}</code></pre>

            <div class="instruction-step">
              <span class="step-num">3</span>
              <span>Запустите демон раннера:</span>
            </div>
            <pre class="code-box"><code>./rabotyaga-agent run</code></pre>
          </div>

          <div class="modal-footer-actions">
            <button class="btn btn-primary" @click="finishRegistration">
              <span>Готово</span>
            </button>
          </div>
        </div>
      </div>
    </ModalDialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getRunners, generateRunnerRegistrationToken, deleteRunner } from '@/api/client'
import type { RunnerResponse } from '@/types'
import RunnerCard from '@/components/runner/RunnerCard.vue'
import ModalDialog from '@/components/common/ModalDialog.vue'
import Icon from '@/components/common/Icon.vue'
import { useToast } from '@/composables/useToast'

const runners = ref<RunnerResponse[]>([])
const loading = ref(true)
const showRegisterModal = ref(false)
const generating = ref(false)
const customRunnerName = ref('')
const generatedToken = ref('')
const currentOrigin = ref(window.location.origin)

const toast = useToast()

const loadRunners = async () => {
  try {
    const res = await getRunners()
    runners.value = res.data.data || []
  } catch (e) {
    toast.error('Не удалось загрузить список раннеров')
  } finally {
    loading.value = false
  }
}

const openRegisterModal = () => {
  generatedToken.value = ''
  customRunnerName.value = ''
  showRegisterModal.value = true
}

const generateToken = async () => {
  generating.value = true
  try {
    const res = await generateRunnerRegistrationToken(customRunnerName.value || undefined)
    if (res.data.data) {
      generatedToken.value = res.data.data.registrationToken
      toast.success('Токен успешно создан')
    }
  } catch (e) {
    toast.error('Ошибка при создании токена')
  } finally {
    generating.value = false
  }
}

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text)
  toast.success('Скопировано в буфер обмена')
}

const finishRegistration = () => {
  showRegisterModal.value = false
  loadRunners()
}

const handleDelete = async (id: number) => {
  if (!confirm('Вы уверены, что хотите отозвать доступ и удалить этот раннер?')) return
  try {
    await deleteRunner(id)
    toast.success('Раннер успешно удален')
    await loadRunners()
  } catch (e) {
    toast.error('Ошибка при удалении раннера')
  }
}

onMounted(() => {
  loadRunners()
  // Refresh runner status every 10 seconds
  setInterval(loadRunners, 10000)
})
</script>

<style scoped>
.runners-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.runners-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  text-align: center;
  gap: 8px;
}

.empty-state h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-state p {
  color: var(--text-secondary);
  max-width: 480px;
  font-size: 13px;
}

.register-modal-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-text {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.token-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  background: var(--bg-inset);
  border: 1px solid var(--accent-blue);
  border-radius: 6px;
}

.token-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.token-label {
  font-size: 11px;
  color: var(--text-muted);
}

.token-code {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--accent-blue);
}

.instructions-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 10px;
}

.instruction-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.instruction-step {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-primary);
  margin-top: 4px;
}

.step-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #21262d;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-blue);
  flex-shrink: 0;
}

.code-box {
  background: var(--bg-inset);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 12px;
  overflow-x: auto;
  color: var(--text-primary);
}

.modal-footer-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
