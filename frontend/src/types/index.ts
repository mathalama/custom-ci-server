// Зеркало backend DTO
export type BuildStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'CANCELLED'
export type StepStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILURE' | 'SKIPPED'
export type TriggerType = 'WEBHOOK' | 'MANUAL' | 'SCHEDULE'
export type GitProvider = 'GITHUB' | 'GITLAB' | 'GITEA'

export interface ApiResponse<T> {
  success: boolean
  data: T
  error: string | null
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ProjectResponse {
  id: number
  name: string
  repoUrl: string
  gitProvider: GitProvider
  defaultBranch: string
  webhookSecret: string
  pipelineConfigPath: string
  isActive: boolean
  createdAt: string
  updatedAt: string
}

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

export interface CreateProjectRequest {
  name: string
  repoUrl: string
  gitProvider: GitProvider
  defaultBranch?: string
  pipelineConfigPath?: string
}

export interface TriggerBuildRequest {
  branch: string
  commitSha: string
}
