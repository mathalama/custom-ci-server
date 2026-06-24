<template>
  <div v-if="loading" class="loading"><div class="spinner"></div></div>
  <div v-else-if="build">
    <div class="page-header">
      <div>
        <h1 class="page-title">Билд #{{ build.id }}</h1>
        <p class="page-subtitle">
          <router-link :to="`/projects/${build.projectId}`" class="back-link">
            <Icon name="arrow-left" :size="14" />
            Проект
          </router-link>
          <span class="separator">·</span> {{ build.branch }} <span class="separator">·</span> {{ build.commitSha.slice(0, 7) }}
        </p>
      </div>
      <div style="display: flex; gap: 8px; align-items: center">
        <StatusBadge :status="build.status" />
        <button
          v-if="build.status === 'RUNNING' || build.status === 'PENDING'"
          class="btn btn-danger"
          @click="handleCancel"
        >
          Отменить
        </button>
      </div>
    </div>

    <div class="build-layout">
      <!-- Left: Step timeline -->
      <div class="card steps-panel">
        <h3 class="card-title">Шаги</h3>
        <StepTimeline
          v-if="build.steps.length > 0"
          :steps="build.steps"
          :active-step-id="activeStepId"
          @select="activeStepId = $event"
        />
        <div v-else class="empty-state">
          <div class="empty-state-text">Шаги ещё не созданы</div>
        </div>
      </div>

      <!-- Right: Logs -->
      <div class="logs-panel">
        <h3 class="card-title">Логи {{ activeStepName }}</h3>
        <LogViewer
          v-if="activeStepId"
          :build-id="build.id"
          :step-id="activeStepId"
          :is-running="activeStepStatus === 'RUNNING'"
        />
        <div v-else class="card empty-state">
          <div class="empty-state-text">Выберите шаг для просмотра логов</div>
        </div>
      </div>
    </div>

    <!-- Artifacts -->
    <div v-if="artifacts.length > 0" class="card" style="margin-top: 24px">
      <h3 class="card-title">
        <Icon name="package" :size="16" style="margin-right: 6px" />
        Артефакты
      </h3>
      <div v-for="artifact in artifacts" :key="artifact.id" class="artifact-item">
        <span>{{ artifact.fileName }}</span>
        <span class="artifact-size">{{ formatSize(artifact.fileSize) }}</span>
        <a :href="getDownloadUrl(artifact.id)" class="btn" download>
          <Icon name="download" :size="14" />
          Скачать
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getBuild, cancelBuild, getArtifacts, getArtifactDownloadUrl } from '@/api/client'
import type { BuildResponse, BuildArtifact } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'
import StepTimeline from '@/components/StepTimeline.vue'
import LogViewer from '@/components/LogViewer.vue'
import Icon from '@/components/Icon.vue'

const props = defineProps<{ id: string }>()

const build = ref<BuildResponse | null>(null)
const artifacts = ref<BuildArtifact[]>([])
const loading = ref(true)
const activeStepId = ref<number | null>(null)
let pollInterval: ReturnType<typeof setInterval> | null = null

const activeStepName = computed(() => {
  if (!activeStepId.value || !build.value) return ''
  const step = build.value.steps.find((s: any) => s.id === activeStepId.value)
  return step ? `— ${step.name}` : ''
})

const activeStepStatus = computed(() => {
  if (!activeStepId.value || !build.value) return null
  return build.value.steps.find((s: any) => s.id === activeStepId.value)?.status
})

const loadBuild = async () => {
  try {
    const res = await getBuild(Number(props.id))
    build.value = res.data.data

    // Auto-select first running or last step
    if (!activeStepId.value && build.value.steps.length > 0) {
      const running = build.value.steps.find((s: any) => s.status === 'RUNNING')
      activeStepId.value = running?.id || build.value.steps[0].id
    }

    // Load artifacts
    const artRes = await getArtifacts(Number(props.id))
    artifacts.value = artRes.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadBuild()
  // Poll for updates while build is running
  pollInterval = setInterval(async () => {
    if (build.value && (build.value.status === 'RUNNING' || build.value.status === 'PENDING')) {
      await loadBuild()
    }
  }, 3000)
})

onUnmounted(() => {
  if (pollInterval) clearInterval(pollInterval)
})

const handleCancel = async () => {
  if (!confirm('Отменить билд?')) return
  await cancelBuild(Number(props.id))
  await loadBuild()
}

const getDownloadUrl = (artifactId: number) =>
  getArtifactDownloadUrl(Number(props.id), artifactId)

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.build-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
}

.steps-panel {
  position: sticky;
  top: 32px;
  align-self: start;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: var(--text-primary);
}

.separator {
  color: var(--text-muted);
  margin: 0 2px;
}

.artifact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}

.artifact-item:last-child {
  border-bottom: none;
}

.artifact-size {
  color: var(--text-secondary);
  font-size: 12px;
  margin-left: auto;
}

@media (max-width: 768px) {
  .build-layout {
    grid-template-columns: 1fr;
  }
}
</style>
