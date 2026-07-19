import axios from 'axios'
import type {
  ApiResponse,
  PagedResponse,
  ProjectResponse,
  BuildResponse,
  BuildArtifact,
  CreateProjectRequest,
  TriggerBuildRequest,
  SecretResponse,
  CreateSecretRequest,
} from '@/types'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// ===== Projects =====
export const getProjects = (page = 0, size = 20) =>
  api.get<ApiResponse<PagedResponse<ProjectResponse>>>('/projects', {
    params: { page, size },
  })

export const getProject = (id: number) =>
  api.get<ApiResponse<ProjectResponse>>(`/projects/${id}`)

export const createProject = (data: CreateProjectRequest) =>
  api.post<ApiResponse<ProjectResponse>>('/projects', data)

export const updateProject = (id: number, data: UpdateProjectRequest) =>
  api.put<ApiResponse<ProjectResponse>>(`/projects/${id}`, data)

export const deleteProject = (id: number) =>
  api.delete(`/projects/${id}`)

// ===== Secrets =====
export const getSecrets = (projectId: number) =>
  api.get<ApiResponse<SecretResponse[]>>(`/projects/${projectId}/secrets`)

export const addSecret = (projectId: number, data: CreateSecretRequest) =>
  api.post<ApiResponse<SecretResponse>>(`/projects/${projectId}/secrets`, data)

export const deleteSecret = (projectId: number, secretId: number) =>
  api.delete(`/projects/${projectId}/secrets/${secretId}`)

// ===== Builds =====
export const getBuilds = (projectId: number, page = 0, size = 20) =>
  api.get<ApiResponse<PagedResponse<BuildResponse>>>('/builds', {
    params: { projectId, page, size },
  })

export const getRecentBuilds = (limit: number = 10) =>
  api.get<ApiResponse<BuildResponse[]>>('/builds/recent', {
    params: { limit },
  })

export const getBuild = (id: number) =>
  api.get<ApiResponse<BuildResponse>>(`/builds/${id}`)

export const triggerBuild = (projectId: number, data: TriggerBuildRequest) =>
  api.post<ApiResponse<BuildResponse>>(`/projects/${projectId}/trigger`, data)

export const cancelBuild = (id: number) =>
  api.post<ApiResponse<void>>(`/builds/${id}/cancel`)

// ===== Artifacts =====
export const getArtifacts = (buildId: number) =>
  api.get<ApiResponse<BuildArtifact[]>>(`/builds/${buildId}/artifacts`)

export const getArtifactDownloadUrl = (buildId: number, artifactId: number) =>
  `/api/v1/builds/${buildId}/artifacts/${artifactId}/download`

// ===== Logs =====
export const getHistoricalLogs = (buildId: number, stepId: number, size = 1000) =>
  api.get<ApiResponse<PagedResponse<any>>>(`/builds/${buildId}/steps/${stepId}/logs`, {
    params: { size, sort: 'lineNumber,asc' }
  })

// ===== Logs SSE =====
export const createLogStream = (
  buildId: number,
  stepId: number,
  onMessage: (data: { stream: string; content: string; lineNumber: number; timestamp: string }) => void,
  onError?: (err: Event) => void
): EventSource => {
  const url = `/api/v1/builds/${buildId}/steps/${stepId}/logs/stream`
  const eventSource = new EventSource(url)

  eventSource.addEventListener('log-chunk', (event) => {
    const data = JSON.parse(event.data)
    onMessage(data)
  })

  eventSource.onerror = (err) => {
    if (onError) onError(err)
    eventSource.close()
  }

  return eventSource
}

// ===== OAuth =====
export const getGithubOauthConfig = () =>
  api.get<ApiResponse<{ clientId: string }>>('/auth/github/config')

export const exchangeGithubCode = (code: string) =>
  api.post<ApiResponse<{ status: string }>>('/auth/github/callback', { code })

export const getGithubOauthStatus = () =>
  api.get<ApiResponse<{ connected: boolean }>>('/auth/github/status')

export const disconnectGithubOauth = () =>
  api.delete<ApiResponse<void>>('/auth/github/disconnect')

export const getGithubProfile = () =>
  api.get<ApiResponse<any>>('/auth/github/profile')

export const getGithubUserRepos = () =>
  api.get<ApiResponse<any>>('/auth/github/repos')

export const getGithubRepoBranches = (owner: string, repo: string) =>
  api.get<ApiResponse<any>>(`/auth/github/repos/${owner}/${repo}/branches`)

export const getGithubRepoContents = (owner: string, repo: string, ref?: string) =>
  api.get<ApiResponse<any>>(`/auth/github/repos/${owner}/${repo}/contents${ref ? '?ref=' + ref : ''}`)

// ===== System Info =====
export const getSystemInfo = () =>
  api.get<ApiResponse<{ version: string; status: string }>>('/system/info')

// ===== Runners =====
export const getRunners = () =>
  api.get<ApiResponse<any[]>>('/runners')

export const getRunner = (id: number) =>
  api.get<ApiResponse<any>>(`/runners/${id}`)

export const createRunner = (data: any) =>
  api.post<ApiResponse<any>>('/runners', data)

export const deleteRunner = (id: number) =>
  api.delete(`/runners/${id}`)

