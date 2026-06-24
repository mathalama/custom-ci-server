<template>
  <svg
    class="icon"
    :width="size"
    :height="size"
    :style="{ color: color }"
  >
    <use :href="`/icons.svg#icon-${name}`" />
  </svg>
</template>

<script setup lang="ts">
defineProps<{
  name: string
  size?: number
  color?: string
}>()
</script>

<style scoped>
.icon {
  display: inline-block;
  vertical-align: middle;
  flex-shrink: 0;
}
</style>
