<template>
  <div :class="nodeClass">
    <Handle type="target" :position="Position.Left" class="node-handle" />
    <div class="node-header">
      <span class="status-icon" :class="data.status.toLowerCase()">{{ statusIcon }}</span>
      <div class="node-details">
        <span class="node-label">{{ data.label }}</span>
        <span v-if="formattedDuration" class="node-duration">{{ formattedDuration }}</span>
      </div>
    </div>
    <Handle type="source" :position="Position.Right" class="node-handle" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'
import type { StepStatus } from '@/types'

export interface DAGNodeData {
  label: string
  status: StepStatus
  duration?: number | null
}

const props = defineProps<{
  id: string
  data: DAGNodeData
}>()

const nodeClass = computed(() => {
  return {
    'dag-node': true,
    'node-success': props.data.status === 'SUCCESS',
    'node-failure': props.data.status === 'FAILURE' || props.data.status === 'CANCELLED',
    'node-running': props.data.status === 'RUNNING',
    'node-pending': props.data.status === 'PENDING',
    'node-skipped': props.data.status === 'SKIPPED',
  }
})

const statusIcon = computed(() => {
  switch (props.data.status) {
    case 'SUCCESS': return '✓'
    case 'FAILURE': return '✕'
    case 'CANCELLED': return '⏹'
    case 'RUNNING': return '↻'
    case 'SKIPPED': return '↷'
    default: return '⋯'
  }
})

const formattedDuration = computed(() => {
  if (!props.data.duration || props.data.status === 'PENDING' || props.data.status === 'RUNNING') return ''
  const seconds = Math.round(props.data.duration / 1000)
  if (seconds < 1) return '<1с'
  if (seconds < 60) return `${seconds}с`
  const minutes = Math.floor(seconds / 60)
  const remSeconds = seconds % 60
  return remSeconds > 0 ? `${minutes}м ${remSeconds}с` : `${minutes}м`
})
</script>

<style scoped>
.dag-node {
  padding: 10px 16px;
  min-width: 170px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.dag-node:hover {
  border-color: var(--border-color-hover);
  background: var(--bg-card-hover);
}

.node-handle {
  width: 6px;
  height: 6px;
  background: var(--text-muted);
  border: 1px solid var(--bg-card);
  border-radius: 50%;
}

.node-success {
  border-color: var(--accent-green-border);
}

.node-failure {
  border-color: var(--accent-red-border);
}

.node-running {
  border-color: var(--accent-amber-border);
}

.node-pending {
  opacity: 0.6;
  border-style: dashed;
}

.node-skipped {
  opacity: 0.45;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.node-details {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}

.node-label {
  font-weight: 600;
  font-size: 12.5px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 160px;
}

.node-duration {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.status-icon {
  font-weight: 700;
  font-size: 12px;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-color);
  flex-shrink: 0;
}

.status-icon.success { 
  color: var(--accent-green); 
  background: var(--accent-green-bg);
  border-color: var(--accent-green-border);
}

.status-icon.failure, .status-icon.cancelled { 
  color: var(--accent-red); 
  background: var(--accent-red-bg);
  border-color: var(--accent-red-border);
}

.status-icon.running { 
  color: var(--accent-amber); 
  background: var(--accent-amber-bg);
  border-color: var(--accent-amber-border);
  animation: spin 1.5s linear infinite;
}

.status-icon.pending { color: var(--text-secondary); }
.status-icon.skipped { color: var(--text-muted); }

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
