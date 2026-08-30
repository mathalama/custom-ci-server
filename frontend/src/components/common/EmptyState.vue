<template>
  <div class="zovik-empty-state">
    <!-- 1. No Pipelines Illustration -->
    <svg v-if="type === 'no-pipelines'" width="200" height="150" viewBox="0 0 200 150" fill="none" class="empty-illustration">
      <rect x="20" y="20" width="160" height="110" rx="16" fill="#18181B" stroke="#27272A" stroke-width="1.5" />
      <path d="M20 55 H180 M20 95 H180 M60 20 V130 M100 20 V130 M140 20 V130" stroke="#27272A" stroke-width="0.75" stroke-dasharray="3 3" />
      
      <circle cx="50" cy="75" r="14" fill="#09090B" stroke="#7C3AED" stroke-width="2" />
      <circle cx="50" cy="75" r="5" fill="#7C3AED" />
      
      <circle cx="100" cy="50" r="12" fill="#09090B" stroke="#3F3F46" stroke-width="1.5" stroke-dasharray="3 2" />
      <circle cx="100" cy="100" r="12" fill="#09090B" stroke="#3F3F46" stroke-width="1.5" stroke-dasharray="3 2" />
      <circle cx="150" cy="75" r="12" fill="#09090B" stroke="#3F3F46" stroke-width="1.5" stroke-dasharray="3 2" />

      <path d="M64 75 C80 75, 80 50, 88 50" stroke="#7C3AED" stroke-width="1.5" stroke-dasharray="4 3" opacity="0.6" />
      <path d="M64 75 C80 75, 80 100, 88 100" stroke="#7C3AED" stroke-width="1.5" stroke-dasharray="4 3" opacity="0.6" />
      <path d="M112 50 C125 50, 130 75, 138 75" stroke="#3F3F46" stroke-width="1.5" stroke-dasharray="4 3" />
      <path d="M112 100 C125 100, 130 75, 138 75" stroke="#3F3F46" stroke-width="1.5" stroke-dasharray="4 3" />
    </svg>

    <!-- 2. No Runners Illustration -->
    <svg v-else-if="type === 'no-runners'" width="200" height="150" viewBox="0 0 200 150" fill="none" class="empty-illustration">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="#27272A" stroke-width="1.5" />
      
      <rect x="45" y="45" width="110" height="24" rx="6" fill="#09090B" stroke="#3F3F46" stroke-width="1.5" />
      <circle cx="60" cy="57" r="3" fill="#EF4444" />
      <line x1="75" y1="57" x2="140" y2="57" stroke="#27272A" stroke-width="2" stroke-linecap="round" />

      <rect x="45" y="80" width="110" height="24" rx="6" fill="#09090B" stroke="#7C3AED" stroke-width="1.5" stroke-dasharray="4 3" />
      <circle cx="60" cy="92" r="3" fill="#EAB308" />
      <line x1="75" y1="92" x2="120" y2="92" stroke="rgba(124, 58, 237, 0.4)" stroke-width="2" stroke-linecap="round" />
      
      <path d="M100 20 C85 20, 75 30, 75 35" stroke="#7C3AED" stroke-width="2" stroke-linecap="round" stroke-dasharray="3 3" opacity="0.6" />
      <path d="M100 15 C75 15, 60 30, 60 40" stroke="#7C3AED" stroke-width="2" stroke-linecap="round" stroke-dasharray="3 3" opacity="0.3" />
    </svg>

    <!-- 3. Build Failed Illustration -->
    <svg v-else-if="type === 'build-failed'" width="200" height="150" viewBox="0 0 200 150" fill="none" class="empty-illustration">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="rgba(239, 68, 68, 0.3)" stroke-width="1.5" />
      <circle cx="100" cy="70" r="28" fill="rgba(239, 68, 68, 0.08)" stroke="#EF4444" stroke-width="2" />
      <line x1="88" y1="58" x2="112" y2="82" stroke="#EF4444" stroke-width="3" stroke-linecap="round" />
      <line x1="112" y1="58" x2="88" y2="82" stroke="#EF4444" stroke-width="3" stroke-linecap="round" />
      <rect x="50" y="108" width="100" height="4" rx="2" fill="#27272A" />
      <rect x="65" y="116" width="70" height="4" rx="2" fill="#EF4444" opacity="0.6" />
    </svg>

    <!-- 4. Disconnected Illustration -->
    <svg v-else-if="type === 'disconnected'" width="200" height="150" viewBox="0 0 200 150" fill="none" class="empty-illustration">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="#27272A" stroke-width="1.5" />
      <circle cx="100" cy="75" r="24" fill="#09090B" stroke="#52525B" stroke-width="2" stroke-dasharray="4 3" />
      <line x1="60" y1="35" x2="140" y2="115" stroke="#EF4444" stroke-width="2.5" stroke-linecap="round" />
      <circle cx="100" cy="75" r="5" fill="#71717A" />
    </svg>

    <!-- Default Generic Illustration -->
    <svg v-else width="200" height="150" viewBox="0 0 200 150" fill="none" class="empty-illustration">
      <rect x="25" y="25" width="150" height="100" rx="14" fill="#18181B" stroke="#27272A" stroke-width="1.5" />
      <circle cx="100" cy="75" r="20" fill="#09090B" stroke="#7C3AED" stroke-width="2" />
      <circle cx="100" cy="75" r="6" fill="#7C3AED" />
    </svg>

    <h3 class="empty-title">{{ title }}</h3>
    <p class="empty-desc">{{ description }}</p>

    <div v-if="$slots.action || actionText" class="empty-action">
      <slot name="action">
        <button v-if="actionText" class="btn btn-primary" @click="$emit('action')">
          <span>{{ actionText }}</span>
        </button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    type?: 'no-pipelines' | 'no-runners' | 'build-failed' | 'disconnected' | 'generic'
    title: string
    description?: string
    actionText?: string
  }>(),
  {
    type: 'no-pipelines',
    description: '',
    actionText: '',
  }
)

defineEmits<{
  (e: 'action'): void
}>()
</script>

<style scoped>
.zovik-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  width: 100%;
}

.empty-illustration {
  margin-bottom: 20px;
  filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.4));
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  letter-spacing: -0.02em;
}

.empty-desc {
  font-size: 13.5px;
  color: var(--text-secondary);
  line-height: 1.55;
  max-width: 440px;
  margin: 0 0 20px 0;
}

.empty-action {
  margin-top: 4px;
}
</style>
