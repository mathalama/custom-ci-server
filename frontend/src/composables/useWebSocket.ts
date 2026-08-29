import { ref, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import type { BuildStatus, StepStatus } from '@/types'

export interface WsStepPayload {
  stepId: number
  status: StepStatus
  exitCode?: number
  durationMs?: number
}

export interface WsLogPayload {
  stepId: number
  stream: string
  content: string
  lineNumber: number
  timestamp: string
}

export interface WsBuildPayload {
  buildId: number
  status: BuildStatus
}

export function useWebSocket(buildId: number) {
  const isConnected = ref(false)
  const stepUpdates = ref<WsStepPayload[]>([])
  const logChunks = ref<WsLogPayload[]>([])
  const buildUpdate = ref<WsBuildPayload | null>(null)

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host

  const client = new Client({
    brokerURL: `${protocol}//${host}/api/ws`,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  })

  client.onConnect = () => {
    isConnected.value = true

    // Subscribe to overall build status
    client.subscribe(`/topic/builds/${buildId}`, (message) => {
      try {
        buildUpdate.value = JSON.parse(message.body)
      } catch (err) {
        console.error('Failed to parse build status frame:', err)
      }
    })

    // Subscribe to step updates
    client.subscribe(`/topic/builds/${buildId}/steps`, (message) => {
      try {
        stepUpdates.value.push(JSON.parse(message.body))
      } catch (err) {
        console.error('Failed to parse step update frame:', err)
      }
    })

    // Subscribe to logs
    client.subscribe(`/topic/builds/${buildId}/logs`, (message) => {
      try {
        logChunks.value.push(JSON.parse(message.body))
      } catch (err) {
        console.error('Failed to parse log chunk frame:', err)
      }
    })
  }

  client.onStompError = (frame) => {
    console.error('STOMP Error:', frame.headers['message'], frame.body)
  }

  client.onWebSocketClose = () => {
    isConnected.value = false
  }

  onMounted(() => {
    client.activate()
  })

  onUnmounted(() => {
    client.deactivate()
  })

  return {
    isConnected,
    stepUpdates,
    logChunks,
    buildUpdate,
  }
}
