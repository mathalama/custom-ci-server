<template>
  <div class="runner-card card">
    <div class="runner-header">
      <div class="runner-info">
        <div class="runner-icon-wrap">
          <span class="runner-icon-code">>_</span>
        </div>
        <div>
          <div class="runner-title-row">
            <h3 class="runner-name">{{ runner.name }}</h3>
            <span v-if="runner.os" class="os-tag">{{ runner.os }}</span>
          </div>
          <span class="runner-meta-text">
            ID #{{ runner.id }}
            <template v-if="runner.host">• {{ runner.host }}</template>
          </span>
        </div>
      </div>
      <StatusBadge :status="runner.status" />
    </div>

    <div class="runner-hardware-grid">
      <div class="hardware-item">
        <span class="hw-label">CPU Cores</span>
        <span class="hw-value">{{ runner.cpuCores || '—' }}</span>
      </div>
      <div class="hardware-item">
        <span class="hw-label">Память</span>
        <span class="hw-value">{{ formatMemory(runner.memoryBytes) }}</span>
      </div>
      <div class="hardware-item">
        <span class="hw-label">Последний пинг</span>
        <span class="hw-value" :class="{ 'live-text': isLive(runner.lastSeenAt) }">
          {{ formatLastSeen(runner.lastSeenAt) }}
        </span>
      </div>
    </div>

    <div class="runner-actions">
      <button
        class="btn btn-sm btn-danger-ghost"
        @click="emit('delete', runner.id)"
      >
        <Icon name="x" :size="13" />
        <span>Отозвать токен</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RunnerResponse } from '@/types'
import StatusBadge from '@/components/common/StatusBadge.vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps<{
  runner: RunnerResponse
}>()

const emit = defineEmits<{
  (e: 'delete', id: number): void
}>()

const formatMemory = (bytes?: number) => {
  if (!bytes) return '—'
  const gb = (bytes / (1024 * 1024 * 1024)).toFixed(1)
  return `${gb} GB`
}

const isLive = (iso: string | null) => {
  if (!iso) return false
  return Date.now() - new Date(iso).getTime() < 30000
}

const formatLastSeen = (iso: string | null) => {
  if (!iso) return 'Никогда'
  const date = new Date(iso)
  const diffSec = Math.floor((Date.now() - date.getTime()) / 1000)
  if (diffSec < 15) return 'Только что'
  if (diffSec < 60) return `${diffSec} сек. назад`
  const min = Math.floor(diffSec / 60)
  if (min < 60) return `${min} мин. назад`
  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.runner-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: border-color 0.15s ease;
}

.runner-card:hover {
  border-color: var(--border-color-hover);
}

.runner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.runner-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.runner-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #21262d;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.runner-icon-code {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-blue);
}

.runner-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.runner-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.os-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 2em;
  background: var(--border-muted);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.runner-meta-text {
  font-size: 12px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.runner-hardware-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 10px 12px;
  background: var(--bg-inset);
  border: 1px solid var(--border-muted);
  border-radius: 6px;
}

.hardware-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hw-label {
  font-size: 11px;
  color: var(--text-muted);
}

.hw-value {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.live-text {
  color: var(--accent-green);
}

.runner-actions {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-muted);
  padding-top: 10px;
}
</style>
