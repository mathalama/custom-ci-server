<template>
  <div class="timeline">
    <div
      v-for="step in steps"
      :key="step.id"
      class="timeline-item"
      :class="[
        step.status.toLowerCase(),
        { active: activeStepId === step.id }
      ]"
      @click="emit('select', step.id)"
    >
      <div class="timeline-dot" :class="step.status.toLowerCase()">
        <span v-if="step.status === 'RUNNING'" class="spinner-small"></span>
        <span v-else-if="step.status === 'SUCCESS'" class="status-symbol">✓</span>
        <span v-else-if="step.status === 'FAILURE'" class="status-symbol">✕</span>
        <span v-else-if="step.status === 'SKIPPED'" class="status-symbol">↷</span>
        <span v-else class="status-symbol">⋯</span>
      </div>

      <div class="timeline-content">
        <div class="step-header-row">
          <span class="step-name">{{ step.name }}</span>
          <span v-if="step.durationMs" class="step-duration">{{ formatDuration(step.durationMs) }}</span>
        </div>

        <div class="step-meta-row">
          <code class="step-image">{{ step.dockerImage }}</code>
          <span v-if="getDeps(step).length > 0" class="step-deps-tag">
            ждёт: {{ getDeps(step).join(', ') }}
          </span>
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

const emit = defineEmits<{
  (e: 'select', id: number): void
}>()

const getDeps = (step: BuildStepResponse): string[] => {
  if (!step.dependsOn) return []
  try {
    const parsed = JSON.parse(step.dependsOn)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}с`
  const m = Math.floor(s / 60)
  return `${m}м ${s % 60}с`
}
</script>

<style scoped>
.timeline {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
}

.timeline-item:hover {
  background: var(--bg-card-hover);
  border-color: var(--border-color-hover);
}

.timeline-item.active {
  background: var(--bg-elevated);
  border-color: rgba(255, 255, 255, 0.25);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1);
}

.timeline-dot {
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bg-inset);
  border: 1px solid var(--border-color);
  font-size: 11px;
  color: var(--text-muted);
}

.status-symbol {
  line-height: 1;
  font-weight: 700;
}

.timeline-dot.success { 
  color: var(--accent-green); 
  border-color: var(--accent-green-border); 
  background: var(--accent-green-bg); 
}

.timeline-dot.failure { 
  color: var(--accent-red); 
  border-color: var(--accent-red-border); 
  background: var(--accent-red-bg); 
}

.timeline-dot.running { 
  color: var(--accent-amber); 
  border-color: var(--accent-amber-border); 
  background: var(--accent-amber-bg); 
}

.spinner-small {
  width: 10px;
  height: 10px;
  border: 1.5px solid rgba(251, 191, 36, 0.3);
  border-top-color: var(--accent-amber);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}

.timeline-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.step-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.step-name {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
}

.step-duration {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.step-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.step-image {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.step-deps-tag {
  font-size: 10.5px;
  padding: 1px 6px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  white-space: nowrap;
  font-family: var(--font-mono);
}
</style>
