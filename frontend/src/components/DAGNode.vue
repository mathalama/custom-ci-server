<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{
  id: string;
  data: {
    label: string;
    status: string; // PENDING, RUNNING, SUCCESS, FAILURE, CANCELLED
    duration?: number;
  };
}>();

const nodeClass = computed(() => {
  return {
    'glass-panel': true,
    'dag-node': true,
    'glow-success': props.data.status === 'SUCCESS',
    'glow-failure': props.data.status === 'FAILURE' || props.data.status === 'CANCELLED',
    'glow-running': props.data.status === 'RUNNING',
    'pending': props.data.status === 'PENDING',
    'skipped': props.data.status === 'SKIPPED'
  };
});

const statusIcon = computed(() => {
  switch (props.data.status) {
    case 'SUCCESS': return '✓';
    case 'FAILURE': return '✕';
    case 'CANCELLED': return '⏹';
    case 'RUNNING': return '↻';
    case 'SKIPPED': return '↷';
    default: return '⋯';
  }
});

const formattedDuration = computed(() => {
  if (props.data.duration === undefined || props.data.duration === null) return '';
  if (props.data.status === 'PENDING' || props.data.status === 'RUNNING') return '';
  
  const seconds = Math.round(props.data.duration / 1000);
  if (seconds < 1) return '<1s';
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remSeconds = seconds % 60;
  return remSeconds > 0 ? `${minutes}m ${remSeconds}s` : `${minutes}m`;
});
</script>

<template>
  <div :class="nodeClass">
    <div class="node-header">
      <span class="status-icon" :class="data.status.toLowerCase()">{{ statusIcon }}</span>
      <div class="node-text-wrapper" style="display: flex; flex-direction: column; align-items: flex-start; gap: 2px;">
        <span class="node-label">{{ data.label }}</span>
        <span v-if="formattedDuration" class="node-duration" style="font-size: 11px; color: var(--text-muted); font-weight: 600;">{{ formattedDuration }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dag-node {
  padding: 12px 20px;
  min-width: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  cursor: pointer;
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

.node-label {
  font-weight: 600;
  font-size: 14px;
}

.status-icon {
  font-weight: bold;
  font-size: 16px;
}

.status-icon.success { color: var(--color-success); }
.status-icon.failure, .status-icon.cancelled { color: var(--color-failure); }
.status-icon.running { 
  color: var(--color-running);
  display: inline-block;
  animation: spin 2s linear infinite;
}
.status-icon.pending { color: var(--text-secondary); }
.status-icon.skipped { color: var(--skipped); }

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
