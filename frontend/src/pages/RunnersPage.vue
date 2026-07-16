<template>
  <div class="runners-container">
    <div class="page-header">
      <div>
        <h1 class="page-title">Распределенные раннеры</h1>
        <p class="page-subtitle">Управление удаленными агентами сборок и автоматический деплой через SSH</p>
      </div>
      <button class="btn btn-primary" @click="showAddModal = true">
        <Icon name="plus" :size="16" />
        Добавить раннер
      </button>
    </div>

    <!-- Runners Grid -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <span>Загрузка списка раннеров...</span>
    </div>

    <div v-else-if="runners.length === 0" class="empty-state card">
      <div class="empty-icon">
        <Icon name="package" :size="48" />
      </div>
      <h2>Нет подключенных раннеров</h2>
      <p>Сборки по умолчанию будут выполняться локально на Мастере. Добавьте удаленный VPS сервер, чтобы распределить нагрузку.</p>
      <button class="btn btn-primary" style="margin-top: 16px;" @click="showAddModal = true">
        Развернуть первый раннер
      </button>
    </div>

    <div v-else class="runners-grid">
      <div v-for="runner in runners" :key="runner.id" class="runner-card card">
        <div class="runner-card-header">
          <div>
            <h3 class="runner-name">{{ runner.name }}</h3>
            <span class="runner-host">{{ runner.username }}@{{ runner.host }}:{{ runner.port }}</span>
          </div>
          <span :class="['status-badge', runner.status.toLowerCase()]">
            <span class="status-indicator-dot"></span>
            {{ getStatusLabel(runner.status) }}
          </span>
        </div>

        <div class="runner-details" v-if="runner.status === 'ONLINE' || runner.os">
          <div class="detail-item">
            <span class="detail-lbl">ОС</span>
            <span class="detail-val">{{ runner.os || 'Неизвестно' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-lbl">Ядра CPU</span>
            <span class="detail-val">{{ runner.cpuCores || '—' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-lbl">Память RAM</span>
            <span class="detail-val">{{ formatMemory(runner.memoryBytes) }}</span>
          </div>
        </div>

        <div class="runner-actions">
          <button 
            v-if="runner.installLog" 
            class="btn btn-secondary btn-sm" 
            @click="openLogModal(runner)"
          >
            <Icon name="eye" :size="14" />
            Лог установки
          </button>
          <button class="btn btn-danger btn-sm" @click="confirmDelete(runner)">
            <Icon name="trash" :size="14" />
            Удалить
          </button>
        </div>
      </div>
    </div>

    <!-- Modal: Add Runner -->
    <div v-if="showAddModal" class="modal-backdrop">
      <div class="modal card">
        <div class="modal-header">
          <h3>Развернуть новый раннер</h3>
          <button class="close-btn" @click="showAddModal = false">&times;</button>
        </div>
        <form @submit.prevent="submitCreateRunner" class="add-form">
          <div class="form-group">
            <label>Имя раннера</label>
            <input v-model="form.name" type="text" placeholder="Например, Server-Germany-1" required />
          </div>

          <div class="form-row">
            <div class="form-group flex-3">
              <label>IP-адрес / Хост</label>
              <input v-model="form.host" type="text" placeholder="192.168.1.100" required />
            </div>
            <div class="form-group flex-1">
              <label>Порт SSH</label>
              <input v-model.number="form.port" type="number" placeholder="22" required />
            </div>
          </div>

          <div class="form-group">
            <label>Имя пользователя (SSH)</label>
            <input v-model="form.username" type="text" placeholder="root" required />
          </div>

          <div class="form-group">
            <label>Тип аутентификации</label>
            <div class="radio-group">
              <label class="radio-label">
                <input type="radio" value="key" v-model="form.authType" />
                Приватный SSH-ключ (рекомендуется)
              </label>
              <label class="radio-label">
                <input type="radio" value="password" v-model="form.authType" />
                Пароль
              </label>
            </div>
          </div>

          <div v-if="form.authType === 'key'" class="form-group">
            <label>Содержимое SSH-ключа (Private Key)</label>
            <textarea 
              v-model="form.sshKey" 
              placeholder="-----BEGIN OPENSSH PRIVATE KEY-----&#10;..." 
              rows="6" 
              required
            ></textarea>
          </div>

          <div v-else class="form-group">
            <label>Пароль пользователя</label>
            <input v-model="form.password" type="password" placeholder="Введите пароль" required />
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="showAddModal = false">Отмена</button>
            <button type="submit" class="btn btn-primary" :disabled="creating">
              <span v-if="creating" class="spinner btn-spinner"></span>
              Запустить автодеплой
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Modal: View Install Log -->
    <div v-if="showLogModal" class="modal-backdrop">
      <div class="modal card log-modal-width">
        <div class="modal-header">
          <h3>Лог автодеплоя: {{ selectedRunner?.name }}</h3>
          <button class="close-btn" @click="closeLogModal">&times;</button>
        </div>
        <div class="terminal-log-container">
          <pre class="terminal-log" ref="terminalPre">{{ selectedRunner?.installLog || 'Подготовка к установке...\n' }}</pre>
        </div>
        <div class="modal-footer justify-between">
          <span class="status-info" v-if="selectedRunner?.status === 'PROVISIONING'">
            <span class="spinner log-spinner"></span>
            Установка продолжается, логи обновляются в реальном времени...
          </span>
          <span class="status-info success" v-else-if="selectedRunner?.status === 'ONLINE' || selectedRunner?.status === 'OFFLINE'">
            🟢 Установка успешно завершена!
          </span>
          <span class="status-info failed" v-else-if="selectedRunner?.status === 'FAILED'">
            🔴 Установка завершилась ошибкой. Проверьте параметры SSH.
          </span>
          <button type="button" class="btn btn-secondary" @click="closeLogModal">Закрыть</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { getRunners, createRunner, deleteRunner, getRunner } from '@/api/client'
import Icon from '@/components/Icon.vue'

interface Runner {
  id: number
  name: string
  host: string
  port: number
  username: string
  status: string
  os?: string
  cpuCores?: number
  memoryBytes?: number
  installLog?: string
  createdAt: string
}

const runners = ref<Runner[]>([])
const loading = ref(true)
const creating = ref(false)
const showAddModal = ref(false)
const showLogModal = ref(false)
const selectedRunner = ref<Runner | null>(null)
const terminalPre = ref<HTMLPreElement | null>(null)

const form = ref({
  name: '',
  host: '',
  port: 22,
  username: 'root',
  authType: 'key',
  sshKey: '',
  password: ''
})

let pollInterval: number | null = null

const getStatusLabel = (status: string) => {
  const labels: Record<string, String> = {
    ONLINE: 'В сети',
    OFFLINE: 'Выключен',
    BUSY: 'Собирает',
    PROVISIONING: 'Установка',
    FAILED: 'Ошибка'
  }
  return labels[status] || status
}

const formatMemory = (bytes?: number) => {
  if (!bytes) return '—'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb.toFixed(1) + ' ГБ'
}

const loadRunners = async () => {
  try {
    const res = await getRunners()
    runners.value = res.data.data
    
    // Update selected runner if viewing log
    if (selectedRunner.value) {
      const updated = runners.value.find(r => r.id === selectedRunner.value!.id)
      if (updated) {
        selectedRunner.value = updated
        nextTick(() => {
          if (terminalPre.value) {
            terminalPre.value.scrollTop = terminalPre.value.scrollHeight
          }
        })
      }
    }
  } catch (e) {
    console.error('Failed to load runners', e)
  }
}

const submitCreateRunner = async () => {
  creating.value = true
  try {
    const payload = {
      name: form.value.name,
      host: form.value.host,
      port: form.value.port,
      username: form.value.username,
      sshKey: form.value.authType === 'key' ? form.value.sshKey : null,
      password: form.value.authType === 'password' ? form.value.password : null
    }

    const res = await createRunner(payload)
    const newRunner = res.data.data
    
    showAddModal.value = false
    creating.value = false
    
    // Clear form
    form.value = {
      name: '',
      host: '',
      port: 22,
      username: 'root',
      authType: 'key',
      sshKey: '',
      password: ''
    }

    await loadRunners()
    
    // Open log modal for the newly deploying runner
    openLogModal(newRunner)
  } catch (e) {
    alert('Ошибка при регистрации раннера: ' + e)
    creating.value = false
  }
}

const confirmDelete = async (runner: Runner) => {
  if (confirm(`Вы уверены, что хотите удалить раннер ${runner.name}? Агент на удаленном сервере не удалится автоматически.`)) {
    try {
      await deleteRunner(runner.id)
      await loadRunners()
    } catch (e) {
      alert('Не удалось удалить раннер: ' + e)
    }
  }
}

const openLogModal = (runner: Runner) => {
  selectedRunner.value = runner
  showLogModal.value = true
  
  // Start polling log every 2 seconds if provisioning
  if (pollInterval) clearInterval(pollInterval)
  pollInterval = window.setInterval(async () => {
    if (selectedRunner.value && (selectedRunner.value.status === 'PROVISIONING' || selectedRunner.value.status === 'FAILED')) {
      try {
        const res = await getRunner(selectedRunner.value.id)
        const updated = res.data.data
        selectedRunner.value = updated
        
        // Update in the main list too
        const idx = runners.value.findIndex(r => r.id === updated.id)
        if (idx !== -1) {
          runners.value[idx] = updated
        }
        
        nextTick(() => {
          if (terminalPre.value) {
            terminalPre.value.scrollTop = terminalPre.value.scrollHeight;
          }
        })
      } catch (e) {
        console.error('Error polling runner log', e)
      }
    }
  }, 2000)
}

const closeLogModal = () => {
  showLogModal.value = false
  selectedRunner.value = null
  if (pollInterval) {
    clearInterval(pollInterval)
    pollInterval = null
  }
}

onMounted(async () => {
  await loadRunners()
  loading.value = false
  
  // Poll runners state every 5 seconds for status updates
  pollInterval = window.setInterval(loadRunners, 5000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})
</script>

<style scoped>
.runners-container {
  max-width: 1080px;
  margin: 0 auto;
  width: 100%;
}

.runners-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.runner-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px;
  min-height: 200px;
}

.runner-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
}

.runner-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.runner-host {
  font-size: 13px;
  color: var(--text-secondary);
  font-family: monospace;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-indicator-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

/* Badge colors */
.status-badge.online {
  background: rgba(16, 185, 129, 0.1);
  color: rgb(16, 185, 129);
}
.status-badge.online .status-indicator-dot {
  background: rgb(16, 185, 129);
}

.status-badge.offline {
  background: rgba(156, 163, 175, 0.1);
  color: rgb(156, 163, 175);
}
.status-badge.offline .status-indicator-dot {
  background: rgb(156, 163, 175);
}

.status-badge.busy {
  background: rgba(59, 130, 246, 0.1);
  color: rgb(59, 130, 246);
}
.status-badge.busy .status-indicator-dot {
  background: rgb(59, 130, 246);
}

.status-badge.provisioning {
  background: rgba(245, 158, 11, 0.1);
  color: rgb(245, 158, 11);
}
.status-badge.provisioning .status-indicator-dot {
  background: rgb(245, 158, 11);
  animation: pulse 1.5s infinite;
}

.status-badge.failed {
  background: rgba(239, 68, 68, 0.1);
  color: rgb(239, 68, 68);
}
.status-badge.failed .status-indicator-dot {
  background: rgb(239, 68, 68);
}

.runner-details {
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  padding: 12px 0;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}

.detail-lbl {
  color: var(--text-secondary);
}

.detail-val {
  font-weight: 500;
  color: var(--text-primary);
}

.runner-actions {
  display: flex;
  gap: 8px;
}

/* Modals */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 90%;
  max-width: 550px;
  padding: 24px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.3), 0 10px 10px -5px rgba(0, 0, 0, 0.2);
}

.log-modal-width {
  max-width: 800px;
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
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: var(--text-secondary);
}

.add-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-3 {
  flex: 3;
}

.flex-1 {
  flex: 1;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  cursor: pointer;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.justify-between {
  justify-content: space-between;
}

/* Terminal log block */
.terminal-log-container {
  background: #0f172a;
  border: 1px solid #1e293b;
  border-radius: 8px;
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.terminal-log {
  margin: 0;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  color: #f1f5f9;
  white-space: pre-wrap;
  text-align: left;
}

.status-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-info.success {
  color: rgb(16, 185, 129);
}

.status-info.failed {
  color: rgb(239, 68, 68);
}

.log-spinner {
  width: 14px;
  height: 14px;
  border-width: 2px;
  border-top-color: var(--text-primary);
}

.btn-spinner {
  width: 14px;
  height: 14px;
  border-width: 2px;
  border-top-color: #ffffff;
}

@keyframes pulse {
  0% { transform: scale(0.9); opacity: 0.5; }
  50% { transform: scale(1.1); opacity: 1; }
  100% { transform: scale(0.9); opacity: 0.5; }
}
</style>
