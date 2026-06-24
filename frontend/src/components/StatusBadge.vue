<template>
  <span class="status-badge" :class="status.toLowerCase()">
    <span class="status-dot" :class="{ pulse: status === 'RUNNING' }"></span>
    {{ label }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: string
}>()

const label = computed(() => {
  const map: Record<string, string> = {
    PENDING: 'Ожидание',
    RUNNING: 'Запущен',
    SUCCESS: 'Успех',
    FAILURE: 'Ошибка',
    CANCELLED: 'Отменён',
    SKIPPED: 'Пропущен',
  }
  return map[props.status] || props.status
})
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  background: var(--surface-elevated);
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--text-muted);
}

.success .status-dot { background: var(--success); }
.failure .status-dot { background: var(--failure); }
.running .status-dot { background: var(--running); }
.pending .status-dot { background: var(--pending); }
.cancelled .status-dot { background: var(--cancelled); }
.skipped .status-dot { background: var(--skipped); }
</style>
