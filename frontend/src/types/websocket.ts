import type { BuildStatus, StepStatus } from './build'

export interface StepStatusUpdate {
  buildId: number
  stepId: number
  status: StepStatus
  durationMs?: number
  exitCode?: number
}

export interface LogChunk {
  buildId: number
  stepId: number
  log: string
}

export interface BuildStatusUpdate {
  buildId: number
  status: BuildStatus
}
