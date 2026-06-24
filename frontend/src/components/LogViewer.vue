<template>
  <div class="log-container" ref="logContainerRef">
    <div v-if="lines.length === 0 && loading" class="loading">
      <div class="spinner"></div>
    </div>
    <div v-else-if="lines.length === 0" class="empty-state">
      <div class="empty-state-text">Логи пока пусты</div>
    </div>
    <div v-else>
      <div v-for="line in lines" :key="line.lineNumber" class="log-line">
        <span class="log-line-number">{{ line.lineNumber }}</span>
        <span class="log-line-content" :class="{ stderr: line.stream === 'STDERR' }">{{ line.content }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { getHistoricalLogs } from '@/api/client'

interface LogLine {
  stream: string
  content: string
  lineNumber: number
  timestamp: string
}

const props = defineProps<{
  buildId: number
  stepId: number
  isRunning: boolean
  newLogLine?: any
}>()

const lines = ref<LogLine[]>([])
const loading = ref(true)
const logContainerRef = ref<HTMLElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  if (logContainerRef.value) {
    logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
  }
}

const loadHistory = async () => {
  loading.value = true
  lines.value = []
  try {
    const res = await getHistoricalLogs(props.buildId, props.stepId)
    lines.value = res.data.data.content
    scrollToBottom()
  } catch (e) {
    console.error('Failed to load log history', e)
  } finally {
    loading.value = false
  }
}

watch(
  () => props.stepId,
  () => loadHistory(),
  { immediate: true }
)

watch(
  () => props.newLogLine,
  (newLog) => {
    if (newLog && newLog.stepId === props.stepId) {
      lines.value.push(newLog)
      scrollToBottom()
    }
  }
)
</script>
