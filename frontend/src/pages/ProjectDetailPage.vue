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
          <Icon name="play" :size="14" />
          Запустить билд
        </button>
        <button class="btn btn-danger" @click="handleDelete">
          <Icon name="trash" :size="14" />
          Удалить
        </button>
      </div>
    </div>

    <!-- Project info -->
    <div class="card" style="margin-bottom: 24px">
      <div class="project-info-grid">
        <div><span class="info-label">Провайдер:</span> {{ project.gitProvider }}</div>
        <div><span class="info-label">Ветка:</span> {{ project.defaultBranch }}</div>
        <div><span class="info-label">Конфиг:</span> <code>{{ project.pipelineConfigPath }}</code></div>
        <div><span class="info-label">GitHub Token:</span>
          <span v-if="project.githubToken" style="color: var(--success)">✓ Установлен</span>
          <span v-else style="color: var(--text-secondary)">Не установлен</span>
        </div>
        <div><span class="info-label">Webhook URL:</span>
          <code>/api/v1/webhooks/github/{{ project.id }}</code>
        </div>
        <div><span class="info-label">Webhook Secret:</span>
          <code>{{ project.webhookSecret }}</code>
        </div>
      </div>
    </div>

    <!-- Secrets -->
    <div class="card" style="margin-bottom: 24px">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
        <h2 class="card-title" style="margin-bottom: 0">Секреты</h2>
        <button class="btn btn-primary" @click="showSecretModal = true">
          <Icon name="plus" :size="14" />
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
            <Icon name="trash" :size="14" />
          </button>
        </div>
      </div>
    </div>

    <!-- Builds -->
    <h2 class="card-title">История билдов</h2>
    <div v-if="builds.length === 0" class="empty-state">
      <div class="empty-state-icon">
        <Icon name="hammer" :size="40" />
      </div>
      <div class="empty-state-text">Нет билдов. Запустите первый!</div>
    </div>
    <div v-else class="builds-list">
      <BuildCard v-for="build in builds" :key="build.id" :build="build" />
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProject, getBuilds, triggerBuild, deleteProject, getSecrets, addSecret, deleteSecret } from '@/api/client'
import type { ProjectResponse, BuildResponse, SecretResponse } from '@/types'
import BuildCard from '@/components/BuildCard.vue'
import Icon from '@/components/Icon.vue'

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
.project-info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 8px;
  font-size: 14px;
}

.info-label {
  color: var(--text-secondary);
  font-weight: 500;
}

code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: var(--surface-elevated);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--text-primary);
  border: 1px solid var(--border);
}

.builds-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 400px;
  box-shadow: var(--shadow-md);
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
  padding: 12px;
  background: var(--surface-default);
  border: 1px solid var(--border);
  border-radius: 6px;
}

.secret-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.secret-name {
  font-weight: 500;
  font-family: 'JetBrains Mono', monospace;
}

.secret-value {
  color: var(--text-secondary);
}
</style>
