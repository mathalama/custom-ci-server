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
        <span v-else-if="step.status === 'SUCCESS'">✓</span>
        <span v-else-if="step.status === 'FAILURE'">✕</span>
        <span v-else-if="step.status === 'SKIPPED'">⊘</span>
        <span v-else>•</span>
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
  transition: background 0.2s;
}

.timeline-item:hover, .timeline-item.active {
  background: var(--surface-hover);
}

.timeline-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.timeline-dot.success { background: var(--success-bg); color: var(--success); }
.timeline-dot.failure { background: var(--failure-bg); color: var(--failure); }
.timeline-dot.running { background: var(--running-bg); color: var(--running); }
.timeline-dot.pending { background: var(--pending-bg); color: var(--pending); }
.timeline-dot.skipped { background: var(--pending-bg); color: var(--skipped); }

.spinner-small {
  width: 14px;
  height: 14px;
  border: 2px solid var(--running-bg);
  border-top-color: var(--running);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.timeline-content { flex: 1; }

.step-name {
  font-size: 14px;
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
