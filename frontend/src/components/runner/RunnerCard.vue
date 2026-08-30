<template>
  <div class="runner-card card">
    <div class="runner-header">
      <div class="runner-info">
        <div class="runner-icon-wrap">
          <Icon name="package" :size="15" color="var(--text-secondary)" />
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
        <span class="hw-label">Ядра CPU</span>
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
        class="btn btn-sm btn-ghost"
        @click="emit('delete', runner.id)"
      >
        <Icon name="x" :size="12" />
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
  if (diffSec < 60) return `${diffSec}с назад`
  const min = Math.floor(diffSec / 60)
  if (min < 60) return `${min}м назад`
  return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.runner-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
}

.runner-card:hover {
  border-color: var(--border-color-hover);
  background: var(--bg-card-hover);
  transform: translateY(-1px);
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
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
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
  letter-spacing: -0.01em;
}

.os-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.runner-meta-text {
  font-size: 11.5px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.runner-hardware-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 14px;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
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
  font-size: 12.5px;
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
