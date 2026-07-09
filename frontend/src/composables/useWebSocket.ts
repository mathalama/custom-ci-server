import { ref, onMounted, onUnmounted } from 'vue';
import { Client } from '@stomp/stompjs';

export interface WsStepPayload {
  stepId: number;
  status: string;
  exitCode?: number;
}

export interface WsLogPayload {
  stepId: number;
  stream: string;
  content: string;
  lineNumber: number;
  timestamp: string;
}

export interface WsBuildPayload {
  buildId: number;
  status: string;
}

export function useWebSocket(buildId: number) {
  const isConnected = ref(false);
  const stepUpdates = ref<WsStepPayload[]>([]);
  const logChunks = ref<WsLogPayload[]>([]);
  const buildUpdate = ref<WsBuildPayload | null>(null);

  const client = new Client({
    brokerURL: `ws://${window.location.hostname}:8080/api/ws`,
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    isConnected.value = true;
    
    // Subscribe to overall build status
    client.subscribe(`/topic/builds/${buildId}`, (message) => {
      buildUpdate.value = JSON.parse(message.body);
    });

    // Subscribe to step updates
    client.subscribe(`/topic/builds/${buildId}/steps`, (message) => {
      stepUpdates.value.push(JSON.parse(message.body));
    });

    // Subscribe to logs
    client.subscribe(`/topic/builds/${buildId}/logs`, (message) => {
      logChunks.value.push(JSON.parse(message.body));
    });
  };

  client.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
  };

  client.onWebSocketClose = () => {
    isConnected.value = false;
  };

  onMounted(() => {
    client.activate();
  });

  onUnmounted(() => {
    client.deactivate();
  });

  return {
    isConnected,
    stepUpdates,
    logChunks,
    buildUpdate
  };
}
