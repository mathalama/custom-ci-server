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
    'glow-success': props.data.status === 'SUCCESS',
    'glow-failure': props.data.status === 'FAILURE' || props.data.status === 'CANCELLED',
    'glow-running': props.data.status === 'RUNNING',
    'pending': props.data.status === 'PENDING',
    'skipped': props.data.status === 'SKIPPED',
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
  if (seconds < 1) return '<1s'
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  const remSeconds = seconds % 60
  return remSeconds > 0 ? `${minutes}m ${remSeconds}s` : `${minutes}m`
})
</script>

<style scoped>
.dag-node {
  padding: 12px 18px;
  min-width: 160px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
  transition: all 0.25s ease;
  position: relative;
}

.dag-node:hover {
  transform: translateY(-2px);
  border-color: var(--accent-blue);
}

.node-handle {
  width: 8px;
  height: 8px;
  background: var(--accent-blue);
  border: 2px solid var(--bg-card);
}

.glow-success {
  border-color: rgba(34, 197, 94, 0.5);
  box-shadow: 0 0 16px rgba(34, 197, 94, 0.15);
}

.glow-failure {
  border-color: rgba(239, 68, 68, 0.5);
  box-shadow: 0 0 16px rgba(239, 68, 68, 0.15);
}

.glow-running {
  border-color: rgba(59, 130, 246, 0.7);
  box-shadow: 0 0 16px rgba(59, 130, 246, 0.25);
}

.pending {
  opacity: 0.7;
  border-style: dashed;
}

.skipped {
  opacity: 0.55;
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
}

.node-label {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
}

.node-duration {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.status-icon {
  font-weight: bold;
  font-size: 15px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
}

.status-icon.success { color: var(--accent-green); background: rgba(34, 197, 94, 0.1); }
.status-icon.failure, .status-icon.cancelled { color: var(--accent-red); background: rgba(239, 68, 68, 0.1); }
.status-icon.running { 
  color: var(--accent-blue);
  background: rgba(59, 130, 246, 0.1);
  animation: spin 2s linear infinite;
}
.status-icon.pending { color: var(--text-secondary); }
.status-icon.skipped { color: var(--text-muted); }

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
