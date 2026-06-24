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
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.success { background: var(--success-bg); color: var(--success); }
.success .status-dot { background: var(--success); }

.failure { background: var(--failure-bg); color: var(--failure); }
.failure .status-dot { background: var(--failure); }

.running { background: var(--running-bg); color: var(--running); }
.running .status-dot { background: var(--running); }

.pending { background: var(--pending-bg); color: var(--pending); }
.pending .status-dot { background: var(--pending); }

.cancelled { background: var(--cancelled-bg); color: var(--cancelled); }
.cancelled .status-dot { background: var(--cancelled); }

.skipped { background: var(--pending-bg); color: var(--skipped); }
.skipped .status-dot { background: var(--skipped); }
</style>
