<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { VueFlow, useVueFlow } from '@vue-flow/core';
import { Background } from '@vue-flow/background';
import DAGNode from '../components/DAGNode.vue';
import { useWebSocket } from '../composables/useWebSocket';
import { getBuild, getHistoricalLogs, getProject } from '../api/client';

const route = useRoute();
const buildId = Number(route.params.id) || 1; // Default to 1 for demo purposes

const { isConnected, stepUpdates, logChunks, buildUpdate } = useWebSocket(buildId);

const { onNodeClick, fitView, onPaneReady } = useVueFlow();

onNodeClick((event) => {
  selectedStepId.value = Number(event.node.id);
});

onPaneReady(() => {
  setTimeout(() => {
    fitView({ padding: 0.5 });
  }, 150);
});

const nodes = ref<any[]>([]);
const edges = ref<any[]>([]);
const isLoading = ref(true);
const selectedStepId = ref<number | null>(null);
const selectedStepName = ref<string>('');
const projectName = ref('');
const logs = ref('');
const searchQuery = ref('');
const stepStartTimes = new Map<number, number>();
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

// Simple debounced filter cache
const filteredLogsCache = ref('');
const lastSearchQuery = ref('');

const filteredLogs = computed(() => {
  if (!searchQuery.value) {
    lastSearchQuery.value = '';
    filteredLogsCache.value = logs.value;
    return logs.value;
  }
  if (searchQuery.value === lastSearchQuery.value) {
    return filteredLogsCache.value;
  }
  const query = searchQuery.value.toLowerCase();
  filteredLogsCache.value = logs.value
    .split('\n')
    .filter(line => line.toLowerCase().includes(query))
    .join('\n');
  lastSearchQuery.value = searchQuery.value;
  return filteredLogsCache.value;
});

// Debounce search query
watch(searchQuery, () => {
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    // Trigger computed re-evaluation by accessing it
    void filteredLogs.value;
  }, 300);
});

const fetchLogs = async (stepId: number) => {
  try {
    const res = await getHistoricalLogs(buildId, stepId);
    if (res.data.success && res.data.data) {
      const historical = res.data.data.content;
      // Combine lines preserving raw content
      logs.value = historical.map((logLine: any) => logLine.content).join('');
      // Reset filter cache when logs change
      lastSearchQuery.value = '';
      filteredLogsCache.value = logs.value;
    }
  } catch (e) {
    console.error("Failed to load historical logs for step", stepId, e);
    logs.value = "Failed to load logs.";
  }
};

const loadDAG = async () => {
  try {
    const res = await getBuild(buildId);
    if (res.data.success && res.data.data) {
      const build = res.data.data;
      
      // Async fetch project details to populate header subtitle
      getProject(build.projectId).then((projRes) => {
        if (projRes.data.success && projRes.data.data) {
          projectName.value = projRes.data.data.name;
        }
      }).catch((err) => {
        console.error("Failed to load project details for build", err);
      });

      const stepNodes: any[] = [];
      const stepEdges: any[] = [];
      
      // Auto-select a step: running first, otherwise first step
      let initialStepId: number | null = null;
      const runningStep = build.steps.find((s: any) => s.status === 'RUNNING');
      if (runningStep) {
        initialStepId = runningStep.id;
      } else if (build.steps.length > 0) {
        initialStepId = build.steps[0].id;
      }
      
      build.steps.forEach((step: any, index: number) => {
        stepNodes.push({
          id: step.id.toString(),
          type: 'custom',
          data: { label: step.name, status: step.status, duration: step.durationMs },
          position: { x: index * 250 + 100, y: 150 }
        });
        
        if (index > 0) {
          stepEdges.push({
            id: `e${build.steps[index - 1].id}-${step.id}`,
            source: build.steps[index - 1].id.toString(),
            target: step.id.toString(),
            animated: step.status === 'RUNNING'
          });
        }
      });
      
      nodes.value = stepNodes;
      edges.value = stepEdges;
      
      if (initialStepId) {
        selectedStepId.value = initialStepId;
      }
      
      // Auto-fit after rendering
      setTimeout(() => {
        fitView({ padding: 0.5 });
      }, 150);
    }
  } catch (e) {
    console.error("Failed to load build DAG", e);
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  loadDAG();
});

// Watch selected step to load its logs
watch(selectedStepId, async (newStepId) => {
  if (newStepId) {
    logs.value = 'Loading logs...';
    const node = nodes.value.find(n => n.id === newStepId.toString());
    selectedStepName.value = node ? node.data.label : '';
    await fetchLogs(newStepId);
  }
});

// Update node statuses when stepUpdates arrive
watch(stepUpdates, (updates) => {
  if (updates.length > 0) {
    const latest = updates[updates.length - 1];
    
    if (latest.status === 'RUNNING') {
      stepStartTimes.set(latest.stepId, Date.now());
    }
    
    let calculatedDuration: number | undefined = undefined;
    if ((latest.status === 'SUCCESS' || latest.status === 'FAILURE' || latest.status === 'CANCELLED') && stepStartTimes.has(latest.stepId)) {
      calculatedDuration = Date.now() - stepStartTimes.get(latest.stepId)!;
    }
    
    // Replace array reference to trigger Vue 3 reactivity
    nodes.value = nodes.value.map(node => {
      if (node.id === latest.stepId.toString()) {
        const newData = {
          ...node.data,
          status: latest.status
        };
        if (calculatedDuration !== undefined) {
          newData.duration = calculatedDuration;
        }
        return {
          ...node,
          data: newData
        };
      }
      return node;
    });
    
    // Update edges animations
    edges.value = edges.value.map(edge => {
      if (edge.target === latest.stepId.toString()) {
        return {
          ...edge,
          animated: latest.status === 'RUNNING'
        };
      }
      return edge;
    });

    // Auto-focus on active step logs
    if (latest.status === 'RUNNING') {
      selectedStepId.value = latest.stepId;
      setTimeout(() => {
        fitView({ padding: 0.5 });
      }, 100);
    }
  }
}, { deep: true });

// Process real-time WebSocket log chunks for selected step
watch(logChunks, (chunks) => {
  if (chunks.length > 0) {
    const latest = chunks[chunks.length - 1];
    if (latest.stepId === selectedStepId.value) {
      if (logs.value === 'Loading logs...' || logs.value === 'Waiting for logs...') {
        logs.value = '';
      }
      logs.value += latest.content;
    }
  }
}, { deep: true });



</script>

<template>
  <div class="build-view">
    <div class="page-header" style="margin-bottom: 20px;">
      <div>
        <h1 class="page-title">Сборка #{{ buildId }}</h1>
        <p class="page-subtitle" style="font-size: 13px;">Проект: {{ projectName || 'custom-ci-server' }}</p>
      </div>
      <div style="display: flex; align-items: center; gap: 12px;">
        <div class="status-badge" :class="buildUpdate?.status?.toLowerCase() || 'running'">
          {{ buildUpdate?.status || 'RUNNING' }}
        </div>
        <div class="ws-status" :class="{ connected: isConnected }">
          {{ isConnected ? 'Live' : 'Connecting...' }}
        </div>
      </div>
    </div>

    <div class="content">
      <div class="dag-container glass-panel">
        <VueFlow 
          :nodes="nodes" 
          :edges="edges"
          :pan-on-drag="[2]"
          @contextmenu.prevent
        >
          <template #node-custom="customNodeProps">
            <DAGNode 
              v-bind="customNodeProps" 
              :class="{ 'node-selected': selectedStepId === Number(customNodeProps.id) }" 
              @click="selectedStepId = Number(customNodeProps.id)"
            />
          </template>
          <Background pattern-color="#6272a4" :gap="20" />
        </VueFlow>
      </div>

      <div class="log-container glass-panel">
        <div class="log-header">
          <span>Logs: {{ selectedStepName || 'Select a step' }}</span>
          <input 
            v-model="searchQuery" 
            class="log-search-input" 
            placeholder="Поиск по логам..." 
            style="margin-left: auto; width: 180px; padding: 5px 10px; font-size: 12px; border-radius: 6px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); color: #fff; outline: none; font-family: inherit;"
          />
        </div>
        <pre class="log-content">{{ filteredLogs || 'Waiting for logs...' }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.build-view {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 64px); /* Fit within main-content padding */
  box-sizing: border-box;
  gap: 20px;
}

.status-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.status-badge.success { background: rgba(16, 185, 129, 0.1); color: var(--success); }
.status-badge.failure { background: rgba(239, 68, 68, 0.1); color: var(--failure); }
.status-badge.running { background: rgba(139, 92, 246, 0.1); color: var(--running); }

.ws-status {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
}

.ws-status::before {
  content: '';
  display: block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-failure);
}

.ws-status.connected::before {
  background: var(--color-success);
  box-shadow: 0 0 8px var(--color-success);
}

.content {
  display: flex;
  flex: 1;
  gap: 20px;
  min-height: 0; /* important for flex children to scroll */
}

.dag-container {
  flex: 1;
  min-width: 0;
  position: relative;
}

.log-container {
  flex: 1.2; /* Give log console slightly more space */
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #11131c !important; /* Premium dark slate background */
  border: 1px solid rgba(255, 255, 255, 0.06) !important;
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow-md);
}

.log-header {
  padding: 12px 20px;
  background: #181a26;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  font-weight: 600;
  color: #8c93a8;
  display: flex;
  align-items: center;
  gap: 10px;
}

.log-header::before {
  content: '';
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success);
}

.log-content {
  flex: 1;
  margin: 0;
  padding: 18px 20px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: #e2e8f0; /* Soft off-white color for high legibility */
  background: transparent;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
