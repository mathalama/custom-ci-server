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
import { ref, watch, onUnmounted, nextTick } from 'vue'
import { createLogStream } from '@/api/client'

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
}>()

const lines = ref<LogLine[]>([])
const loading = ref(true)
const logContainerRef = ref<HTMLElement | null>(null)
let eventSource: EventSource | null = null

const scrollToBottom = async () => {
  await nextTick()
  if (logContainerRef.value) {
    logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
  }
}

const startStream = () => {
  if (eventSource) {
    eventSource.close()
  }

  lines.value = []
  loading.value = true

  eventSource = createLogStream(
    props.buildId,
    props.stepId,
    (data: LogLine) => {
      loading.value = false
      lines.value.push(data)
      scrollToBottom()
    },
    () => {
      loading.value = false
    }
  )
}

watch(
  () => props.stepId,
  () => startStream(),
  { immediate: true }
)

onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
  }
})
</script>
