<template>
  <div v-if="loading" class="loading"><div class="spinner"></div></div>
  <div v-else-if="project">
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ project.name }}</h1>
        <p class="page-subtitle">{{ project.repoUrl }}</p>
      </div>
      <div style="display: flex; gap: 8px">
        <button class="btn btn-primary" @click="showTriggerModal = true">🚀 Запустить билд</button>
        <button class="btn btn-danger" @click="handleDelete">🗑️ Удалить</button>
      </div>
    </div>

    <!-- Project info -->
    <div class="card" style="margin-bottom: 24px">
      <div class="project-info-grid">
        <div><span class="info-label">Провайдер:</span> {{ project.gitProvider }}</div>
        <div><span class="info-label">Ветка:</span> {{ project.defaultBranch }}</div>
        <div><span class="info-label">Конфиг:</span> <code>{{ project.pipelineConfigPath }}</code></div>
        <div><span class="info-label">Webhook URL:</span>
          <code>/api/v1/webhooks/github/{{ project.id }}</code>
        </div>
        <div><span class="info-label">Webhook Secret:</span>
          <code>{{ project.webhookSecret }}</code>
        </div>
      </div>
    </div>

    <!-- Builds -->
    <h2 class="card-title">История билдов</h2>
    <div v-if="builds.length === 0" class="empty-state">
      <div class="empty-state-icon">🔨</div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProject, getBuilds, triggerBuild, deleteProject } from '@/api/client'
import type { ProjectResponse, BuildResponse } from '@/types'
import BuildCard from '@/components/BuildCard.vue'

const props = defineProps<{ id: string }>()
const router = useRouter()

const project = ref<ProjectResponse | null>(null)
const builds = ref<BuildResponse[]>([])
const loading = ref(true)
const showTriggerModal = ref(false)
const triggerForm = ref({ branch: '', commitSha: '' })

onMounted(async () => {
  try {
    const projRes = await getProject(Number(props.id))
    project.value = projRes.data.data
    triggerForm.value.branch = project.value.defaultBranch

    const buildsRes = await getBuilds(Number(props.id), 0, 50)
    builds.value = buildsRes.data.data.content
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
  background: var(--surface);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--accent);
}

.builds-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  width: 400px;
}
</style>
