export type BuildStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'CANCELLED'
export type StepStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'SKIPPED' | 'CANCELLED'
export type TriggerType = 'WEBHOOK' | 'MANUAL' | 'SCHEDULE'

export interface BuildStepResponse {
  id: number
  name: string
  stepOrder: number
  dockerImage: string
  commands: string
  status: StepStatus
  exitCode: number | null
  startedAt: string | null
  finishedAt: string | null
  durationMs: number | null
  dependsOn?: string
  unresolvedDependenciesCount?: number
}

export interface BuildResponse {
  id: number
  projectId: number
  commitSha: string
  branch: string
  triggerType: TriggerType
  status: BuildStatus
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
  steps: BuildStepResponse[]
}

export interface BuildArtifact {
  id: number
  fileName: string
  fileSize: number
  contentType: string
  createdAt: string
}

export interface TriggerBuildRequest {
  branch: string
  commitSha: string
}
