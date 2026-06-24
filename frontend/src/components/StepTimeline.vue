<template>
  <div class="timeline">
    <div
      v-for="step in steps"
      :key="step.id"
      class="timeline-item"
      :class="{ active: activeStepId === step.id }"
      @click="$emit('select', step.id)"
    >
      <div class="timeline-dot" :class="step.status.toLowerCase()">
        <span v-if="step.status === 'RUNNING'" class="spinner-small"></span>
        <Icon v-else-if="step.status === 'SUCCESS'" name="check" :size="13" />
        <Icon v-else-if="step.status === 'FAILURE'" name="x-mark" :size="13" />
        <Icon v-else-if="step.status === 'SKIPPED'" name="minus" :size="13" />
        <Icon v-else name="dot" :size="8" />
      </div>
      <div class="timeline-content">
        <div class="step-name">{{ step.name }}</div>
        <div class="step-meta">
          <code class="step-image">{{ step.dockerImage }}</code>
          <span v-if="step.durationMs" class="step-duration">{{ formatDuration(step.durationMs) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BuildStepResponse } from '@/types'
import Icon from './Icon.vue'

defineProps<{
  steps: BuildStepResponse[]
  activeStepId: number | null
}>()

defineEmits<{
  select: [stepId: number]
}>()

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  return `${Math.floor(s / 60)}m ${s % 60}s`
}
</script>

<style scoped>
.timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: var(--radius);
  cursor: pointer;
  transition: background 0.15s;
}

.timeline-item:hover, .timeline-item.active {
  background: var(--surface-hover);
}

.timeline-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--surface-elevated);
  border: 1px solid var(--border);
  color: var(--text-muted);
}

.timeline-dot.success { color: var(--success); border-color: var(--success); }
.timeline-dot.failure { color: var(--failure); border-color: var(--failure); }
.timeline-dot.running { color: var(--running); border-color: var(--running); }
.timeline-dot.pending { color: var(--pending); }
.timeline-dot.skipped { color: var(--skipped); }

.spinner-small {
  width: 13px;
  height: 13px;
  border: 2px solid var(--border);
  border-top-color: var(--running);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.timeline-content { flex: 1; }

.step-name {
  font-size: 13px;
  font-weight: 500;
}

.step-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.step-image {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--text-muted);
}

.step-duration {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
