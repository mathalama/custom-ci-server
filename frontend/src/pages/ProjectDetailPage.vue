<template>
  <div v-if="loading" class="loading"><div class="spinner"></div></div>
  <div v-else-if="project">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ project.name }}</h1>
        <p class="page-subtitle">{{ project.repoUrl }}</p>
      </div>
      <div style="display: flex; gap: 8px">
        <button class="btn btn-primary" @click="showTriggerModal = true">
          <Icon name="play" :size="14"></Icon>
          Запустить билд
        </button>
        <button class="btn btn-danger" @click="handleDelete">
          <Icon name="trash" :size="14"></Icon>
          Удалить
        </button>
      </div>
    </div>

    <!-- Bento Grid Layout -->
    <div class="bento-grid">
      <!-- Main Bento Section (Left) -->
      <div class="bento-main">
        <!-- Project Parameters Card -->
        <div class="card">
          <h2 class="card-title">Параметры проекта</h2>
          <div class="project-info-grid">
            <div><span class="info-label">Провайдер:</span> {{ project.gitProvider }}</div>
            <div><span class="info-label">Ветка по умолчанию:</span> <code>{{ project.defaultBranch }}</code></div>
            <div><span class="info-label">Путь к конфигу:</span> <code>{{ project.pipelineConfigPath }}</code></div>
            <div><span class="info-label">GitHub Token:</span>
              <span v-if="project.githubToken" style="color: var(--success); font-weight: 600;">✓ Установлен</span>
              <span v-else style="color: var(--text-muted)">Не установлен</span>
            </div>
          </div>
        </div>

        <!-- Secrets Card -->
        <div class="card">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px">
            <h2 class="card-title" style="margin-bottom: 0">Секреты проекта</h2>
            <button class="btn btn-primary" @click="showSecretModal = true">
              <Icon name="plus" :size="14"></Icon>
              Добавить
            </button>
          </div>
          <div v-if="secrets.length === 0" class="empty-state" style="padding: 24px">
            <div class="empty-state-text">Нет добавленных секретов</div>
          </div>
          <div v-else class="secrets-list">
            <div v-for="secret in secrets" :key="secret.id" class="secret-item">
              <div class="secret-info">
                <span class="secret-name">{{ secret.name }}</span>
                <code class="secret-value">{{ secret.value }}</code>
              </div>
              <button class="btn btn-danger" @click="handleDeleteSecret(secret.id)">
                <Icon name="trash" :size="14"></Icon>
              </button>
            </div>
          </div>
        </div>

        <!-- Builds History Card (Properly wrapped) -->
        <div class="card">
          <h2 class="card-title" style="margin-bottom: 16px">История билдов</h2>
          <div v-if="builds.length === 0" class="empty-state">
            <div class="empty-state-icon">
              <Icon name="hammer" :size="40"></Icon>
            </div>
            <div class="empty-state-text">Нет билдов. Запустите первый!</div>
          </div>
          <div v-else class="builds-list">
            <BuildCard v-for="build in builds" :key="build.id" :build="build"></BuildCard>
          </div>
        </div>
      </div>

      <!-- Sidebar Bento Section (Right) -->
      <div class="bento-sidebar">
        <!-- Webhook Integration Card -->
        <div class="card">
          <h2 class="card-title">Интеграция вебхука</h2>
          <div style="display: flex; flex-direction: column; gap: 16px;">
            <div class="form-group" style="margin-bottom: 0;">
              <label class="info-label" style="font-size: 13px; margin-bottom: 4px; display: block;">Payload URL:</label>
              <code style="word-break: break-all; display: block;">{{ windowOrigin }}/api/v1/webhooks/github/{{ project.id }}</code>
            </div>
            <div class="form-group" style="margin-bottom: 0;">
              <label class="info-label" style="font-size: 13px; margin-bottom: 4px; display: block;">Secret Key:</label>
              <div style="display: flex; align-items: center; gap: 8px;">
                <code style="flex-grow: 1; word-break: break-all;">{{ displayedWebhookSecret }}</code>
                <button class="btn btn-icon-only" style="padding: 6px 10px;" @click="toggleWebhookSecret" type="button" title="Показать или скрыть">
                  <Icon :name="webhookSecretIcon" :size="13"></Icon>
                </button>
                <button class="btn btn-icon-only" style="padding: 6px 10px;" @click="copyToClipboard(project.webhookSecret)" type="button" title="Копировать">
                  <Icon name="copy" :size="13"></Icon>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Status Badge Card -->
        <div class="card">
          <h2 class="card-title">Статус-бейдж</h2>
          <div class="badge-content">
            <div class="badge-img-wrapper">
              <img :src="badgeUrl" alt="Build Status" />
            </div>
            <div class="badge-code-wrapper">
              <p class="badge-desc">Markdown код для README.md:</p>
              <code class="badge-code">{{ markdownBadgeCode }}</code>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Trigger Modal -->
    <div v-if="showTriggerModal" class="modal-overlay" @click.self="showTriggerModal = false">
      <div class="modal card">
        <h3 class="card-title">Запустить билд</h3>
        <div class="form-group">
          <label class="form-label">Ветка</label>
          <input v-model="triggerForm.branch" class="form-input" :placeholder="project.defaultBranch" />
        </div>
        <div class="form-group">
          <label class="form-label">Commit SHA</label>
          <input v-model="triggerForm.commitSha" class="form-input" placeholder="HEAD" />
        </div>
        <div style="display: flex; gap: 8px; margin-top: 16px">
          <button class="btn btn-primary" @click="handleTrigger">Запустить</button>
          <button class="btn" @click="showTriggerModal = false">Отмена</button>
        </div>
      </div>
    </div>
    <!-- Secret Modal -->
    <div v-if="showSecretModal" class="modal-overlay" @click.self="showSecretModal = false">
      <div class="modal card">
        <h3 class="card-title">Добавить секрет</h3>
        <div class="form-group">
          <label class="form-label">Имя (Например: NPM_TOKEN)</label>
          <input v-model="secretForm.name" class="form-input" placeholder="MY_SECRET" />
        </div>
        <div class="form-group">
          <label class="form-label">Значение</label>
          <input v-model="secretForm.value" type="password" class="form-input" placeholder="••••••••" />
        </div>
        <div style="display: flex; gap: 8px; margin-top: 16px">
          <button class="btn btn-primary" @click="handleAddSecret">Сохранить</button>
          <button class="btn" @click="showSecretModal = false">Отмена</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getProject, getBuilds, triggerBuild, deleteProject, getSecrets, addSecret, deleteSecret } from '@/api/client'
import type { ProjectResponse, BuildResponse, SecretResponse } from '@/types'
import BuildCard from '@/components/build/BuildCard.vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()

const project = ref<ProjectResponse | null>(null)
const builds = ref<BuildResponse[]>([])
const secrets = ref<SecretResponse[]>([])
const loading = ref(true)
const showTriggerModal = ref(false)
const triggerForm = ref({ branch: '', commitSha: '' })
const showSecretModal = ref(false)
const secretForm = ref({ name: '', value: '' })
const windowOrigin = typeof window !== 'undefined' ? window.location.origin : ''

const showWebhookSecret = ref(false)
const maskSecret = (secret: string | undefined) => {
  if (!secret) return ''
  if (secret.length <= 8) return '••••••••'
  return `${secret.slice(0, 4)}••••••••${secret.slice(-4)}`
}
const copyToClipboard = (text: string | undefined) => {
  if (!text) return
  navigator.clipboard.writeText(text)
  alert('Скопировано в буфер обмена!')
}

const webhookSecretIcon = computed(() => showWebhookSecret.value ? 'eye-off' : 'eye')
const displayedWebhookSecret = computed(() => {
  if (!project.value) return ''
  return showWebhookSecret.value ? project.value.webhookSecret : maskSecret(project.value.webhookSecret)
})
const markdownBadgeCode = computed(() => {
  if (!project.value) return ''
  return `[![Build Status](${windowOrigin}/api/v1/projects/${project.value.id}/badge)](${windowOrigin}/projects/${project.value.id})`
})
const badgeUrl = computed(() => {
  if (!project.value) return ''
  return `/api/v1/projects/${project.value.id}/badge`
})
const toggleWebhookSecret = () => {
  showWebhookSecret.value = !showWebhookSecret.value
}

onMounted(async () => {
  try {
    const projRes = await getProject(Number(props.id))
    project.value = projRes.data.data
    triggerForm.value.branch = project.value.defaultBranch

    const [buildsRes, secretsRes] = await Promise.all([
      getBuilds(Number(props.id), 0, 50),
      getSecrets(Number(props.id))
    ])
    builds.value = buildsRes.data.data.content
    secrets.value = secretsRes.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

const handleTrigger = async () => {
  try {
    const res = await triggerBuild(Number(props.id), {
      branch: triggerForm.value.branch || project.value!.defaultBranch,
      commitSha: triggerForm.value.commitSha || 'HEAD',
    })
    showTriggerModal.value = false
    router.push(`/builds/${res.data.data.id}`)
  } catch (e: any) {
    alert(e.response?.data?.error || 'Ошибка')
  }
}

const handleDelete = async () => {
  if (!confirm(`Удалить проект "${project.value?.name}"? Все билды тоже будут удалены.`)) return
  try {
    await deleteProject(Number(props.id))
    router.push('/projects')
  } catch (e: any) {
    alert(e.response?.data?.error || 'Ошибка удаления')
  }
}

const handleAddSecret = async () => {
  try {
    await addSecret(Number(props.id), secretForm.value)
    showSecretModal.value = false
    secretForm.value = { name: '', value: '' }
    const res = await getSecrets(Number(props.id))
    secrets.value = res.data.data
  } catch (e: any) {
    alert(e.response?.data?.error || 'Ошибка добавления секрета')
  }
}

const handleDeleteSecret = async (secretId: number) => {
  if (!confirm('Точно удалить этот секрет?')) return
  try {
    await deleteSecret(Number(props.id), secretId)
    secrets.value = secrets.value.filter(s => s.id !== secretId)
  } catch (e: any) {
    alert(e.response?.data?.error || 'Ошибка удаления секрета')
  }
}
</script>

<style scoped>
.bento-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 1024px) {
  .bento-grid {
    grid-template-columns: 1fr;
  }
}

.bento-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.bento-sidebar {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.project-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
  font-size: 13px;
}

.info-label {
  color: var(--text-secondary);
  font-weight: 500;
  margin-right: 4px;
}

code {
  font-family: var(--font-mono);
  font-size: 12px;
  background: var(--bg-surface);
  padding: 3px 6px;
  border-radius: var(--radius-sm);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.builds-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 420px;
}

.secrets-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.secret-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
}

.secret-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.secret-name {
  font-weight: 600;
  font-family: var(--font-mono);
  font-size: 12.5px;
}

.secret-value {
  color: var(--text-secondary);
}

.badge-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.badge-img-wrapper {
  padding: 10px 16px;
  background: var(--bg-surface);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  display: inline-flex;
  align-self: start;
}

.badge-desc {
  margin: 0 0 6px 0;
  font-size: 12.5px;
  color: var(--text-secondary);
}

.badge-code {
  display: block;
  width: 100%;
  box-sizing: border-box;
  word-break: break-all;
  white-space: pre-wrap;
}

.btn-icon-only {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.15s ease;
}

.btn-icon-only:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
  border-color: var(--border-color-hover);
}
</style>
