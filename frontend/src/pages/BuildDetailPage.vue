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

    <div style="margin-top: 24px;">
      <!-- Top: Pipeline DAG -->
      <PipelineGraph
        v-if="build.steps.length > 0"
        :steps="build.steps"
        :active-step-id="activeStepId"
        @select="activeStepId = $event"
      />
      <div v-else class="card empty-state" style="margin-bottom: 24px;">
        <div class="empty-state-text">Шаги ещё не созданы</div>
      </div>

      <!-- Bottom: Logs -->
      <div class="logs-panel">
        <h3 class="card-title">Логи {{ activeStepName }}</h3>
        <LogViewer
          v-if="activeStepId"
          :build-id="build.id"
          :step-id="activeStepId"
          :is-running="activeStepStatus === 'RUNNING'"
          :new-log-line="latestLogChunk"
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
import PipelineGraph from '@/components/PipelineGraph.vue'
import LogViewer from '@/components/LogViewer.vue'
import Icon from '@/components/Icon.vue'
import { Client } from '@stomp/stompjs'

const props = defineProps<{ id: string }>()

const build = ref<BuildResponse | null>(null)
const artifacts = ref<BuildArtifact[]>([])
const loading = ref(true)
const activeStepId = ref<number | null>(null)
const latestLogChunk = ref<any>(null)
let stompClient: Client | null = null

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

    if (!activeStepId.value && build.value.steps.length > 0) {
      const running = build.value.steps.find((s: any) => s.status === 'RUNNING')
      activeStepId.value = running?.id || build.value.steps[0].id
    }

    const artRes = await getArtifacts(Number(props.id))
    artifacts.value = artRes.data.data
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const connectWebSocket = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.hostname
  const port = import.meta.env.DEV ? '8080' : window.location.port
  const portStr = port ? `:${port}` : ''
  const wsUrl = `${protocol}//${host}${portStr}/api/ws`

  stompClient = new Client({
    brokerURL: wsUrl,
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('Connected to STOMP via WebSocket')
      
      // Subscribe to step status updates
      stompClient?.subscribe(`/topic/builds/${props.id}/steps`, (message) => {
        const payload = JSON.parse(message.body)
        if (build.value) {
          const step = build.value.steps.find(s => s.id === payload.stepId)
          if (step) {
            step.status = payload.status
            step.finishedAt = new Date().toISOString()
          }
          if (payload.status !== 'RUNNING') {
            loadBuild()
          }
        }
      })

      // Subscribe to live logs
      stompClient?.subscribe(`/topic/builds/${props.id}/logs`, (message) => {
        const payload = JSON.parse(message.body)
        latestLogChunk.value = payload
      })
    },
    onStompError: (frame) => {
      console.error('STOMP Error:', frame.headers['message'])
    }
  })
  stompClient.activate()
}

onMounted(async () => {
  await loadBuild()
  connectWebSocket()
})

onUnmounted(() => {
  if (stompClient) {
    stompClient.deactivate()
  }
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
