<template>
  <router-link :to="`/builds/${build.id}`" class="build-card card">
    <div class="build-main">
      <div class="build-title-row">
        <StatusBadge :status="build.status" />
        <span class="build-id">#{{ build.id }}</span>
        <span class="branch-tag">
          <Icon name="git-branch" :size="13" />
          <span>{{ build.branch }}</span>
        </span>
        <code class="commit-tag">
          <Icon name="git-commit" :size="13" />
          <span>{{ build.commitSha ? build.commitSha.slice(0, 7) : 'head' }}</span>
        </code>
      </div>

      <div class="build-meta-row">
        <span class="meta-item">
          <Icon :name="triggerIcon" :size="13" />
          <span>{{ build.triggerType }}</span>
        </span>
        <span class="dot-separator">•</span>
        <span v-if="duration" class="meta-item">
          <Icon name="clock" :size="13" />
          <span>{{ duration }}</span>
        </span>
        <span v-if="duration" class="dot-separator">•</span>
        <span class="meta-item">{{ formatTime(build.createdAt) }}</span>
      </div>
    </div>

    <div class="build-actions">
      <Icon name="arrow-right" :size="14" color="var(--text-muted)" />
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { BuildResponse } from '@/types'
import StatusBadge from '@/components/common/StatusBadge.vue'
import Icon from '@/components/common/Icon.vue'

const props = defineProps<{
  build: BuildResponse
}>()

const triggerIcon = computed(() => {
  const icons: Record<string, string> = {
    WEBHOOK: 'webhook',
    MANUAL: 'user',
    SCHEDULE: 'schedule',
  }
  return icons[props.build.triggerType] || 'gear'
})

const duration = computed(() => {
  if (!props.build.startedAt || !props.build.finishedAt) return null
  const ms = new Date(props.build.finishedAt).getTime() - new Date(props.build.startedAt).getTime()
  if (ms < 1000) return `${ms}ms`
  const s = Math.floor(ms / 1000)
  if (s < 60) return `${s}с`
  const m = Math.floor(s / 60)
  return `${m}м ${s % 60}с`
})

const formatTime = (iso: string) => {
  if (!iso) return ''
  return new Date(iso).toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<style scoped>
.build-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  text-decoration: none;
  color: inherit;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 6px;
  padding: 12px 16px;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.build-card:hover {
  border-color: var(--border-color-hover);
  background: var(--bg-card-hover);
  text-decoration: none;
}

.build-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.build-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.build-id {
  font-weight: 600;
  font-size: 13px;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

.build-meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dot-separator {
  color: var(--border-color);
}
</style>
