<template>
  <span class="status-badge" :class="status.toLowerCase()">
    <span class="status-dot" :class="{ pulse: status === 'RUNNING' || status === 'PROVISIONING' }"></span>
    <span class="status-text">{{ label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BuildStatus, StepStatus, RunnerStatus } from '@/types'

const props = defineProps<{
  status: BuildStatus | StepStatus | RunnerStatus | string
}>()

const label = computed(() => {
  const map: Record<string, string> = {
    PENDING: 'В очереди',
    RUNNING: 'Выполняется',
    SUCCESS: 'Успешно',
    FAILURE: 'Ошибка',
    CANCELLED: 'Отменён',
    SKIPPED: 'Пропущен',
    ONLINE: 'Онлайн',
    OFFLINE: 'Оффлайн',
    PROVISIONING: 'Настройка',
    ERROR: 'Сбой',
  }
  return map[props.status] || props.status
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 11.5px;
  font-weight: 500;
  line-height: 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-muted);
  flex-shrink: 0;
}

.status-dot.pulse {
  animation: pulse-ring 1.5s cubic-bezier(0.215, 0.61, 0.355, 1) infinite;
}

.success .status-dot, .online .status-dot { background: var(--accent-green); }
.success, .online {
  border-color: var(--accent-green-border);
  color: var(--accent-green);
  background: var(--accent-green-bg);
}

.failure .status-dot, .error .status-dot { background: var(--accent-red); }
.failure, .error {
  border-color: var(--accent-red-border);
  color: var(--accent-red);
  background: var(--accent-red-bg);
}

.running .status-dot, .provisioning .status-dot { background: var(--accent-amber); }
.running, .provisioning {
  border-color: var(--accent-amber-border);
  color: var(--accent-amber);
  background: var(--accent-amber-bg);
}

.pending .status-dot { background: var(--accent-amber); }
.pending {
  border-color: var(--accent-amber-border);
  color: var(--accent-amber);
  background: var(--accent-amber-bg);
}

.cancelled, .offline, .skipped {
  border-color: var(--border-color);
  color: var(--text-secondary);
  background: rgba(255, 255, 255, 0.02);
}

@keyframes pulse-ring {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(251, 191, 36, 0.5); }
  70% { transform: scale(1); box-shadow: 0 0 0 4px rgba(251, 191, 36, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(251, 191, 36, 0); }
}
</style>
