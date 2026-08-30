<template>
  <div class="log-viewer-wrapper">
    <div class="log-header">
      <div class="log-search">
        <Icon name="search" :size="14" />
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Поиск по логам..."
          class="log-search-input"
        />
      </div>

      <div class="log-controls">
        <label class="auto-scroll-toggle">
          <input type="checkbox" v-model="autoScroll" />
          <span>Автоскролл</span>
        </label>
        <button class="btn btn-sm btn-ghost" @click="copyAllLogs" title="Копировать логи">
          <Icon name="copy" :size="14" />
        </button>
      </div>
    </div>

    <div class="log-container" ref="logContainerRef" @scroll="onScroll">
      <div v-if="loading && lines.length === 0" class="loading-state">
        <div class="spinner"></div>
        <span>Загрузка логов...</span>
      </div>

      <div v-else-if="filteredLines.length === 0" class="empty-state">
        <span>{{ searchQuery ? 'Ничего не найдено по запросу' : 'Логи пока пусты' }}</span>
      </div>

      <div v-else class="log-lines">
        <div
          v-for="line in filteredLines"
          :key="line.lineNumber"
          class="log-line"
          :class="{ stderr: line.stream === 'STDERR' }"
        >
          <span class="log-line-number">{{ line.lineNumber }}</span>
          <span class="log-line-content">{{ line.content }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { getHistoricalLogs, type HistoricalLogLine } from '@/api/client'
import type { WsLogPayload } from '@/composables/useWebSocket'
import Icon from '@/components/common/Icon.vue'
import { useToast } from '@/composables/useToast'

const props = defineProps<{
  buildId: number
  stepId: number
  isRunning: boolean
  newLogLine?: WsLogPayload | null
}>()

const toast = useToast()

const lines = ref<HistoricalLogLine[]>([])
const loading = ref(true)
const searchQuery = ref('')
const autoScroll = ref(true)
const logContainerRef = ref<HTMLElement | null>(null)

const filteredLines = computed(() => {
  if (!searchQuery.value.trim()) return lines.value
  const query = searchQuery.value.toLowerCase()
  return lines.value.filter(line => line.content.toLowerCase().includes(query))
})

const scrollToBottom = async () => {
  if (!autoScroll.value) return
  await nextTick()
  if (logContainerRef.value) {
    logContainerRef.value.scrollTop = logContainerRef.value.scrollHeight
  }
}

const onScroll = () => {
  if (!logContainerRef.value) return
  const { scrollTop, scrollHeight, clientHeight } = logContainerRef.value
  const isAtBottom = scrollHeight - (scrollTop + clientHeight) < 50
  if (!isAtBottom && autoScroll.value) {
    autoScroll.value = false
  }
}

const loadHistory = async () => {
  loading.value = true
  lines.value = []
  try {
    const res = await getHistoricalLogs(props.buildId, props.stepId)
    lines.value = res.data.data.content || []
    scrollToBottom()
  } catch (e) {
    console.error('Failed to load log history', e)
  } finally {
    loading.value = false
  }
}

const copyAllLogs = () => {
  const text = lines.value.map(l => l.content).join('\n')
  navigator.clipboard.writeText(text)
  toast.success('Скопировано', 'Логи скопированы в буфер обмена')
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
      lines.value.push({
        id: Date.now(),
        buildId: props.buildId,
        stepId: props.stepId,
        lineNumber: newLog.lineNumber,
        content: newLog.content,
        stream: (newLog.stream === 'STDERR' ? 'STDERR' : 'STDOUT') as 'STDOUT' | 'STDERR',
        timestamp: newLog.timestamp,
      })
      scrollToBottom()
    }
  }
)
</script>

<style scoped>
.log-viewer-wrapper {
  background: #090d16;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid var(--border-color);
}

.log-search {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 4px 10px;
  width: 260px;
}

.log-search-input {
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 12px;
  width: 100%;
  outline: none;
}

.log-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-scroll-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  user-select: none;
}

.log-container {
  height: 480px;
  overflow-y: auto;
  padding: 12px 16px;
  font-family: var(--font-mono);
  font-size: 12.5px;
  line-height: 1.6;
  color: #e2e8f0;
}

.loading-state, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 100%;
  color: var(--text-muted);
  font-size: 13px;
}

.log-lines {
  display: flex;
  flex-direction: column;
}

.log-line {
  display: flex;
  gap: 16px;
  font-size: 12.5px;
}

.log-line:hover {
  background: rgba(255, 255, 255, 0.03);
}

.log-line-number {
  width: 36px;
  text-align: right;
  color: #475569;
  user-select: none;
  flex-shrink: 0;
}

.log-line-content {
  white-space: pre-wrap;
  word-break: break-all;
  flex: 1;
}

.log-line-content.stderr {
  color: #f87171;
}
</style>
