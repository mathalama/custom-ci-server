<template>
  <router-link :to="`/builds/${build.id}`" class="build-card card">
    <div class="build-card-header">
      <div class="build-info">
        <span class="build-id">#{{ build.id }}</span>
        <StatusBadge :status="build.status" />
      </div>
      <span class="build-trigger">{{ triggerIcon }} {{ build.triggerType }}</span>
    </div>
    <div class="build-meta">
      <div class="meta-item">
        <span class="meta-icon">🌿</span>
        <span>{{ build.branch }}</span>
      </div>
      <div class="meta-item">
        <span class="meta-icon">📝</span>
        <code class="commit-sha">{{ build.commitSha.slice(0, 7) }}</code>
      </div>
      <div class="meta-item" v-if="duration">
        <span class="meta-icon">⏱️</span>
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

const props = defineProps<{ build: BuildResponse }>()

const triggerIcon = computed(() => {
  const icons: Record<string, string> = {
    WEBHOOK: '🔗',
    MANUAL: '👤',
    SCHEDULE: '🕐',
  }
  return icons[props.build.triggerType] || '❓'
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
  font-size: 16px;
  font-weight: 700;
}

.build-trigger {
  font-size: 12px;
  color: var(--text-secondary);
}

.build-meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-icon {
  font-size: 14px;
}

.commit-sha {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  background: var(--surface);
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--accent);
}

.build-time {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
