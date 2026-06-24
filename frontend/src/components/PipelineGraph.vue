<template>
  <div class="pipeline-graph">
    <div v-for="(step, index) in steps" :key="step.id" class="step-node-container">
      <div 
        class="step-node" 
        :class="[step.status.toLowerCase(), { active: activeStepId === step.id }]"
        @click="$emit('select', step.id)"
      >
        <div class="step-icon">
          <Icon v-if="step.status === 'SUCCESS'" name="check" :size="16" />
          <Icon v-else-if="step.status === 'FAILED'" name="x" :size="16" />
          <div v-else-if="step.status === 'RUNNING'" class="spinner spinner-small"></div>
          <Icon v-else name="circle" :size="16" />
        </div>
        <div class="step-name">{{ step.name }}</div>
        <div class="step-duration" v-if="step.startedAt">
          {{ formatDuration(step.startedAt, step.finishedAt) }}
        </div>
      </div>
      <div v-if="index < steps.length - 1" class="step-connector" :class="getConnectorClass(step)">
        <Icon name="arrow-right" :size="14" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Icon from './Icon.vue'

defineProps<{
  steps: any[]
  activeStepId: number | null
}>()

defineEmits(['select'])

const formatDuration = (start: string, end: string | null) => {
  const t1 = new Date(start).getTime()
  const t2 = end ? new Date(end).getTime() : Date.now()
  const diffSec = Math.floor((t2 - t1) / 1000)
  if (diffSec < 60) return `${diffSec}s`
  return `${Math.floor(diffSec / 60)}m ${diffSec % 60}s`
}

const getConnectorClass = (step: any) => {
  if (step.status === 'SUCCESS') return 'success'
  if (step.status === 'FAILED') return 'failed'
  return 'pending'
}
</script>

<style scoped>
.pipeline-graph {
  display: flex;
  align-items: center;
  overflow-x: auto;
  padding: 16px;
  background: var(--surface-default);
  border-radius: 8px;
  border: 1px solid var(--border);
  margin-bottom: 24px;
}

.step-node-container {
  display: flex;
  align-items: center;
}

.step-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  padding: 12px;
  border-radius: 6px;
  background: var(--background);
  border: 2px solid var(--border);
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.step-node:hover {
  transform: translateY(-2px);
  border-color: var(--primary);
}

.step-node.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.step-node.success { border-color: #10b981; }
.step-node.failed { border-color: #ef4444; }
.step-node.running { border-color: #f59e0b; }

.step-icon {
  margin-bottom: 8px;
  color: var(--text-secondary);
}

.step-node.success .step-icon { color: #10b981; }
.step-node.failed .step-icon { color: #ef4444; }
.step-node.running .step-icon { color: #f59e0b; }

.step-name {
  font-weight: 500;
  font-size: 14px;
  text-align: center;
  margin-bottom: 4px;
}

.step-duration {
  font-size: 11px;
  color: var(--text-muted);
}

.step-connector {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 2px;
  background: var(--border);
  position: relative;
  margin: 0 8px;
}

.step-connector.success {
  background: #10b981;
}

.step-connector svg {
  position: absolute;
  color: var(--border);
  background: var(--surface-default);
}

.step-connector.success svg {
  color: #10b981;
}

.spinner-small {
  width: 16px;
  height: 16px;
  border-width: 2px;
}
</style>
