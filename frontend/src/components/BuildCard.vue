<template>
  <router-link :to="`/builds/${build.id}`" class="build-card card">
    <div class="build-card-header">
      <div class="build-info">
        <span class="build-id">#{{ build.id }}</span>
        <StatusBadge :status="build.status" />
      </div>
      <span class="build-trigger">
        <Icon :name="triggerIcon" :size="14" />
        {{ build.triggerType }}
      </span>
    </div>
    <div class="build-meta">
      <div class="meta-item">
        <Icon name="git-branch" :size="14" />
        <span>{{ build.branch }}</span>
      </div>
      <div class="meta-item">
        <Icon name="git-commit" :size="14" />
        <code class="commit-sha">{{ build.commitSha.slice(0, 7) }}</code>
      </div>
      <div class="meta-item" v-if="duration">
        <Icon name="clock" :size="14" />
        <span>{{ duration }}</span>
      </div>
    </div>
    <div class="build-time">
      {{ formatTime(build.createdAt) }}
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BuildResponse } from '@/types'
import StatusBadge from './StatusBadge.vue'
import Icon from './Icon.vue'

const props = defineProps<{ build: BuildResponse }>()

const triggerIcon = computed(() => {
  const icons: Record<string, string> = {
    WEBHOOK: 'webhook',
    MANUAL: 'user',
    SCHEDULE: 'schedule',
  }
  return icons[props.build.triggerType] || 'question'
})

const duration = computed(() => {
  if (!props.build.startedAt || !props.build.finishedAt) return null
  const ms = new Date(props.build.finishedAt).getTime() - new Date(props.build.startedAt).getTime()
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}s`
  const m = Math.floor(s / 60)
  return `${m}m ${s % 60}s`
})

const formatTime = (iso: string) => {
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.build-card {
  display: block;
  text-decoration: none;
  color: inherit;
  cursor: pointer;
}

.build-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.build-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.build-id {
  font-size: 15px;
  font-weight: 700;
}

.build-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.build-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--text-secondary);
}

.commit-sha {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: var(--surface-elevated);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--text-primary);
  border: 1px solid var(--border);
}

.build-time {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
