<template>
  <div class="pipeline-graph-wrapper card">
    <div class="dag-container" ref="containerRef">
      <!-- SVG Connection Lines Layer -->
      <svg class="dag-svg-layer" :style="{ width: svgWidth + 'px', height: svgHeight + 'px' }">
        <defs>
          <marker
            id="dag-arrow-default"
            viewBox="0 0 10 10"
            refX="6"
            refY="5"
            markerWidth="5"
            markerHeight="5"
            orient="auto-start-reverse"
          >
            <path d="M 0 1 L 10 5 L 0 9 z" fill="rgba(255, 255, 255, 0.2)" />
          </marker>
          <marker
            id="dag-arrow-success"
            viewBox="0 0 10 10"
            refX="6"
            refY="5"
            markerWidth="5"
            markerHeight="5"
            orient="auto-start-reverse"
          >
            <path d="M 0 1 L 10 5 L 0 9 z" fill="var(--accent-green)" />
          </marker>
          <marker
            id="dag-arrow-running"
            viewBox="0 0 10 10"
            refX="6"
            refY="5"
            markerWidth="5"
            markerHeight="5"
            orient="auto-start-reverse"
          >
            <path d="M 0 1 L 10 5 L 0 9 z" fill="var(--accent-amber)" />
          </marker>
        </defs>

        <path
          v-for="edge in dagEdges"
          :key="edge.id"
          :d="edge.d"
          class="dag-edge-path"
          :class="edge.status"
          :marker-end="`url(#dag-arrow-${edge.status})`"
        />
      </svg>

      <!-- DAG Column Layers (Stages) -->
      <div class="dag-layers">
        <div
          v-for="(layer, layerIndex) in dagLayers"
          :key="layerIndex"
          class="dag-stage-column"
        >
          <div class="stage-header">
            <span class="stage-label">Этап {{ layerIndex + 1 }}</span>
          </div>

          <div class="stage-nodes">
            <div
              v-for="step in layer"
              :key="step.id"
              :ref="(el) => handleNodeRef(step.name, el)"
              class="dag-node-card"
              :class="[
                step.status.toLowerCase(),
                { active: activeStepId === step.id }
              ]"
              @click="emit('select', step.id)"
            >
              <div class="node-status-badge">
                <Icon v-if="step.status === 'SUCCESS'" name="check" :size="13" />
                <Icon v-else-if="step.status === 'FAILURE'" name="x" :size="13" />
                <Icon v-else-if="step.status === 'SKIPPED'" name="arrow-right" :size="13" />
                <div v-else-if="step.status === 'RUNNING'" class="spinner-small"></div>
                <span v-else class="pending-dot"></span>
              </div>

              <div class="node-info">
                <div class="node-title" :title="step.name">{{ step.name }}</div>
                <div class="node-meta">
                  <span class="image-tag">{{ shortImage(step.dockerImage) }}</span>
                  <span v-if="step.startedAt" class="duration-tag">
                    {{ formatDuration(step.startedAt, step.finishedAt) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import type { BuildStepResponse } from '@/types'
import Icon from '@/components/common/Icon.vue'

const props = defineProps<{
  steps: BuildStepResponse[]
  activeStepId: number | null
}>()

const emit = defineEmits<{
  (e: 'select', id: number): void
}>()

const containerRef = ref<HTMLElement | null>(null)
const nodeElements = ref<Map<string, HTMLElement>>(new Map())
const svgWidth = ref(800)
const svgHeight = ref(300)

const handleNodeRef = (name: string, el: any) => {
  if (el instanceof HTMLElement) {
    nodeElements.value.set(name, el)
  } else {
    nodeElements.value.delete(name)
  }
}

const parseStepDeps = (step: BuildStepResponse): string[] => {
  if (!step.dependsOn) return []
  try {
    const parsed = JSON.parse(step.dependsOn)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

// Compute Topological Layers (Ranks) for DAG
const dagLayers = computed(() => {
  if (!props.steps || props.steps.length === 0) return []

  const stepMap = new Map<string, BuildStepResponse>()
  props.steps.forEach(s => stepMap.set(s.name, s))

  const ranks = new Map<string, number>()

  const getRank = (stepName: string, visited = new Set<string>()): number => {
    if (ranks.has(stepName)) return ranks.get(stepName)!
    if (visited.has(stepName)) return 0

    visited.add(stepName)
    const step = stepMap.get(stepName)
    if (!step) return 0

    const deps = parseStepDeps(step)
    if (deps.length === 0) {
      ranks.set(stepName, 0)
      return 0
    }

    let maxParentRank = 0
    for (const dep of deps) {
      maxParentRank = Math.max(maxParentRank, getRank(dep, new Set(visited)) + 1)
    }

    ranks.set(stepName, maxParentRank)
    return maxParentRank
  }

  props.steps.forEach(s => getRank(s.name))

  const maxRank = Math.max(0, ...Array.from(ranks.values()))
  const layers: BuildStepResponse[][] = Array.from({ length: maxRank + 1 }, () => [])

  props.steps.forEach(s => {
    const rank = ranks.get(s.name) || 0
    layers[rank].push(s)
  })

  return layers
})

interface DagEdge {
  id: string
  d: string
  status: 'default' | 'success' | 'running'
}

const dagEdges = ref<DagEdge[]>([])

const recalculateEdges = () => {
  const container = containerRef.value
  if (!container) return

  const containerRect = container.getBoundingClientRect()
  const newWidth = Math.max(container.scrollWidth, containerRect.width)
  const newHeight = Math.max(container.scrollHeight, containerRect.height)

  if (Math.abs(svgWidth.value - newWidth) > 5) svgWidth.value = newWidth
  if (Math.abs(svgHeight.value - newHeight) > 5) svgHeight.value = newHeight

  const edges: DagEdge[] = []

  props.steps.forEach(childStep => {
    const deps = parseStepDeps(childStep)
    const childEl = nodeElements.value.get(childStep.name)
    if (!childEl) return

    const childRect = childEl.getBoundingClientRect()
    const childX = childRect.left - containerRect.left + container.scrollLeft
    const childY = childRect.top - containerRect.top + childRect.height / 2 + container.scrollTop

    deps.forEach(parentName => {
      const parentEl = nodeElements.value.get(parentName)
      if (!parentEl) return

      const parentStep = props.steps.find(s => s.name === parentName)
      const parentRect = parentEl.getBoundingClientRect()
      const parentX = parentRect.right - containerRect.left + container.scrollLeft
      const parentY = parentRect.top - containerRect.top + parentRect.height / 2 + container.scrollTop

      const dx = Math.max(40, (childX - parentX) / 2)
      const pathData = `M ${parentX} ${parentY} C ${parentX + dx} ${parentY}, ${childX - dx} ${childY}, ${childX} ${childY}`

      let edgeStatus: 'default' | 'success' | 'running' = 'default'
      if (parentStep?.status === 'SUCCESS' && childStep.status === 'RUNNING') {
        edgeStatus = 'running'
      } else if (parentStep?.status === 'SUCCESS' && childStep.status === 'SUCCESS') {
        edgeStatus = 'success'
      }

      edges.push({
        id: `${parentName}->${childStep.name}`,
        d: pathData,
        status: edgeStatus
      })
    })
  })

  dagEdges.value = edges
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(recalculateEdges)
  window.addEventListener('resize', recalculateEdges)

  if (containerRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      recalculateEdges()
    })
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', recalculateEdges)
  if (resizeObserver) {
    resizeObserver.disconnect()
  }
})

watch(
  () => props.steps,
  () => {
    nextTick(recalculateEdges)
  },
  { deep: true }
)

const shortImage = (image: string) => {
  if (!image) return ''
  const parts = image.split('/')
  return parts[parts.length - 1]
}

const formatDuration = (start: string, end: string | null) => {
  const t1 = new Date(start).getTime()
  const t2 = end ? new Date(end).getTime() : Date.now()
  const diffSec = Math.floor((t2 - t1) / 1000)
  if (diffSec < 60) return `${diffSec}с`
  return `${Math.floor(diffSec / 60)}м ${diffSec % 60}с`
}
</script>

<style scoped>
.pipeline-graph-wrapper {
  position: relative;
  overflow-x: auto;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  margin-bottom: 24px;
  min-height: 200px;
}

.dag-container {
  position: relative;
  min-width: 100%;
  display: inline-block;
}

.dag-svg-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

.dag-edge-path {
  fill: none;
  stroke: rgba(255, 255, 255, 0.12);
  stroke-width: 1.5px;
  stroke-dasharray: 4 3;
  transition: stroke 0.2s ease;
}

.dag-edge-path.success {
  stroke: var(--accent-green);
  stroke-dasharray: none;
  stroke-width: 2px;
}

.dag-edge-path.running {
  stroke: var(--accent-amber);
  stroke-dasharray: 5 3;
  stroke-width: 2px;
  animation: dash 1.2s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -16;
  }
}

.dag-layers {
  display: flex;
  gap: 56px;
  position: relative;
  z-index: 2;
}

.dag-stage-column {
  display: flex;
  flex-direction: column;
  min-width: 210px;
}

.stage-header {
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--border-muted);
}

.stage-label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
}

.stage-nodes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dag-node-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--bg-surface);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
}

.dag-node-card:hover {
  border-color: var(--border-color-hover);
  background: var(--bg-card-hover);
  transform: translateY(-1px);
}

.dag-node-card.active {
  border-color: rgba(255, 255, 255, 0.3);
  background: var(--bg-elevated);
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.15);
}

.dag-node-card.success { border-left: 2px solid var(--accent-green); }
.dag-node-card.failure { border-left: 2px solid var(--accent-red); }
.dag-node-card.running { border-left: 2px solid var(--accent-amber); }
.dag-node-card.skipped { opacity: 0.5; border-left: 2px solid var(--text-muted); }

.node-status-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.04);
  flex-shrink: 0;
  border: 1px solid var(--border-color);
}

.dag-node-card.success .node-status-badge {
  background: var(--accent-green-bg);
  border-color: var(--accent-green-border);
  color: var(--accent-green);
}

.dag-node-card.failure .node-status-badge {
  background: var(--accent-red-bg);
  border-color: var(--accent-red-border);
  color: var(--accent-red);
}

.dag-node-card.running .node-status-badge {
  background: var(--accent-amber-bg);
  border-color: var(--accent-amber-border);
  color: var(--accent-amber);
}

.pending-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--text-muted);
}

.node-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.node-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
}

.image-tag {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.duration-tag {
  font-size: 11px;
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.spinner-small {
  width: 12px;
  height: 12px;
  border: 1.5px solid rgba(251, 191, 36, 0.3);
  border-top-color: var(--accent-amber);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  100% { transform: rotate(360deg); }
}
</style>
