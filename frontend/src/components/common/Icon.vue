<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    :style="{ color }"
  >
    <use :href="`/icons.svg#icon-${name}`" />
  </svg>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    name: string
    size?: number
    color?: string
  }>(),
  {
    size: 18,
    color: 'currentColor',
  }
)
</script>

<style scoped>
.icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
