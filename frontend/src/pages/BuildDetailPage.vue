<template>
  <div v-if="loading" class="loading-screen">
    <div class="spinner"></div>
    <span>Загрузка информации о сборке...</span>
  </div>

  <div v-else-if="build" class="build-detail-page">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <router-link :to="`/projects/${build.projectId}`" class="back-link">
          <Icon name="arrow-left" :size="16" />
          <span>Проект</span>
        </router-link>
        <h1 class="page-title">Сборка #{{ build.id }}</h1>
        <div class="build-submeta">
          <span class="branch-tag">
            <Icon name="git-branch" :size="14" />
            {{ build.branch }}
          </span>
          <span class="dot-separator">•</span>
          <code class="commit-tag">
            <Icon name="git-commit" :size="14" />
            {{ build.commitSha ? build.commitSha.slice(0, 7) : 'head' }}
          </code>
          <span class="dot-separator">•</span>
          <span class="trigger-tag">
            <Icon name="webhook" :size="14" />
            {{ build.triggerType }}
          </span>
        </div>
      </div>

      <div class="header-actions">
        <StatusBadge :status="build.status" />
        <button
          v-if="build.status === 'RUNNING' || build.status === 'PENDING'"
          class="btn btn-danger"
          @click="handleCancel"
        >
          <Icon name="x" :size="14" />
          <span>Отменить</span>
        </button>
      </div>
    </div>

    <!-- Main Content Layout -->
    <div class="build-grid">
      <!-- Pipeline Steps & Graph -->
      <div class="left-panel">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Пайплайн выполнения</h3>
          </div>
          <PipelineGraph
            v-if="build.steps && build.steps.length > 0"
            :steps="build.steps"
            :active-step-id="activeStepId"
            @select="selectStep"
          />
          <div v-else class="empty-state">
            <span>Шаги ещё не созданы</span>
          </div>
        </div>

        <!-- Artifacts Section -->
        <div v-if="artifacts.length > 0" class="card">
          <div class="card-header">
            <h3 class="card-title">
              <Icon name="package" :size="16" />
              <span>Артефакты сборки ({{ artifacts.length }})</span>
            </h3>
          </div>
          <div class="artifacts-list">
            <div v-for="artifact in artifacts" :key="artifact.id" class="artifact-item">
              <div class="artifact-info">
                <Icon name="folder" :size="16" color="#60a5fa" />
                <span class="artifact-name">{{ artifact.fileName }}</span>
                <span class="artifact-size">({{ formatSize(artifact.fileSize) }})</span>
              </div>
              <a
                :href="getArtifactDownloadUrl(build.id, artifact.id)"
                download
                class="btn btn-sm btn-outline"
              >
                <Icon name="download" :size="14" />
                <span>Скачать</span>
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Panel: Step List & Logs -->
      <div class="right-panel">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              <span>Шаги сборки</span>
            </h3>
          </div>
          <StepTimeline
            :steps="build.steps"
            :active-step-id="activeStepId"
            @select="selectStep"
          />
        </div>

        <!-- Logs Box -->
        <div v-if="activeStepId" class="card logs-card">
          <div class="card-header">
            <h3 class="card-title">
              <span>Логи: {{ activeStepName }}</span>
            </h3>
          </div>
          <LogViewer
            :build-id="build.id"
            :step-id="activeStepId"
            :is-running="activeStepStatus === 'RUNNING'"
            :new-log-line="latestLogChunk"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  getBuild,
  cancelBuild,
  getArtifacts,
  getArtifactDownloadUrl,
} from '@/api/client'
import type { BuildResponse, BuildArtifact, StepStatus } from '@/types'
import { useWebSocket, type WsLogPayload } from '@/composables/useWebSocket'
import { useToast } from '@/composables/useToast'
import StatusBadge from '@/components/common/StatusBadge.vue'
import Icon from '@/components/common/Icon.vue'
import PipelineGraph from '@/components/build/PipelineGraph.vue'
import StepTimeline from '@/components/build/StepTimeline.vue'
import LogViewer from '@/components/build/LogViewer.vue'

const route = useRoute()
const toast = useToast()
const buildId = Number(route.params.id)

const build = ref<BuildResponse | null>(null)
const artifacts = ref<BuildArtifact[]>([])
const loading = ref(true)
const activeStepId = ref<number | null>(null)
const latestLogChunk = ref<WsLogPayload | null>(null)

const { logChunks, stepUpdates, buildUpdate } = useWebSocket(buildId)

const activeStepName = computed(() => {
  if (!build.value || !activeStepId.value) return ''
  const step = build.value.steps.find((s) => s.id === activeStepId.value)
  return step ? step.name : ''
})

const activeStepStatus = computed<StepStatus>(() => {
  if (!build.value || !activeStepId.value) return 'PENDING'
  const step = build.value.steps.find((s) => s.id === activeStepId.value)
  return step ? step.status : 'PENDING'
})

const selectStep = (stepId: number) => {
  activeStepId.value = stepId
}

const fetchBuildDetails = async () => {
  try {
    const res = await getBuild(buildId)
    build.value = res.data.data
    if (build.value.steps.length > 0 && !activeStepId.value) {
      const running = build.value.steps.find((s) => s.status === 'RUNNING')
      activeStepId.value = running ? running.id : build.value.steps[0].id
    }
    fetchArtifactsList()
  } catch (err) {
    toast.error('Ошибка', 'Не удалось загрузить данные о сборке')
  } finally {
    loading.value = false
  }
}

const fetchArtifactsList = async () => {
  try {
    const res = await getArtifacts(buildId)
    artifacts.value = res.data.data || []
  } catch (err) {
    console.error('Failed to fetch artifacts', err)
  }
}

const handleCancel = async () => {
  try {
    await cancelBuild(buildId)
    toast.info('Отмена', 'Запрос на отмену сборки отправлен')
    fetchBuildDetails()
  } catch (err) {
    toast.error('Ошибка', 'Не удалось отменить сборку')
  }
}

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

let pollTimer: any = null

watch(logChunks, (chunks) => {
  if (chunks.length > 0) {
    latestLogChunk.value = chunks[chunks.length - 1]
  }
}, { deep: true })

watch(stepUpdates, (updates) => {
  if (updates.length > 0 && build.value) {
    // If steps were empty when page first loaded, re-fetch whole build
    if (!build.value.steps || build.value.steps.length === 0) {
      fetchBuildDetails()
      return
    }

    const lastUpdate = updates[updates.length - 1]
    const step = build.value.steps.find((s) => s.id === lastUpdate.stepId)
    if (step) {
      step.status = lastUpdate.status
      if (lastUpdate.durationMs) step.durationMs = lastUpdate.durationMs
    } else {
      fetchBuildDetails()
    }
  }
}, { deep: true })

watch(buildUpdate, (update) => {
  if (update && build.value) {
    build.value.status = update.status
    if (!build.value.steps || build.value.steps.length === 0) {
      fetchBuildDetails()
    }
  }
})

onMounted(() => {
  fetchBuildDetails()
  pollTimer = setInterval(() => {
    if (
      !build.value ||
      build.value.status === 'PENDING' ||
      build.value.status === 'RUNNING' ||
      !build.value.steps ||
      build.value.steps.length === 0
    ) {
      fetchBuildDetails()
    }
  }, 2000)
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.build-detail-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.loading-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  gap: 16px;
  color: var(--text-muted);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  padding: 24px;
  border-radius: 12px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 13px;

}

.back-link:hover {
  color: var(--accent-blue);
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.build-submeta {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-secondary);
}

.branch-tag, .trigger-tag, .commit-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.commit-tag {
  font-family: var(--font-mono);
  color: var(--accent-blue);
}

.dot-separator {
  color: var(--text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.build-grid {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 24px;
}

@media (max-width: 1024px) {
  .build-grid {
    grid-template-columns: 1fr;
  }
}

.left-panel, .right-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.artifacts-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
}

.artifact-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid var(--border-color);
  border-radius: 8px;
}

.artifact-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.artifact-name {
  font-weight: 600;
  color: var(--text-primary);
}

.artifact-size {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}
</style>
