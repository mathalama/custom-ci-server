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
  gap: 5px;
  padding: 2px 8px;
  border-radius: 2em;
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  white-space: nowrap;
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
  border-color: rgba(63, 185, 80, 0.4);
  color: var(--accent-green);
  background: rgba(63, 185, 80, 0.12);
}

.failure .status-dot, .error .status-dot { background: var(--accent-red); }
.failure, .error {
  border-color: rgba(248, 81, 73, 0.4);
  color: var(--accent-red);
  background: rgba(248, 81, 73, 0.12);
}

.running .status-dot, .provisioning .status-dot { background: var(--accent-blue); }
.running, .provisioning {
  border-color: rgba(88, 166, 255, 0.4);
  color: var(--accent-blue);
  background: rgba(88, 166, 255, 0.12);
}

.pending .status-dot { background: var(--accent-amber); }
.pending {
  border-color: rgba(210, 153, 34, 0.4);
  color: var(--accent-amber);
  background: rgba(210, 153, 34, 0.12);
}

.cancelled, .offline, .skipped {
  border-color: var(--border-color);
  color: var(--text-secondary);
  background: var(--border-muted);
}

@keyframes pulse-ring {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(88, 166, 255, 0.6); }
  70% { transform: scale(1); box-shadow: 0 0 0 5px rgba(88, 166, 255, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(88, 166, 255, 0); }
}
</style>
