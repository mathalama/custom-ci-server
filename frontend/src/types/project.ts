export type GitProvider = 'GITHUB' | 'GITLAB' | 'GITEA'

export interface ProjectResponse {
  id: number
  name: string
  repoUrl: string
  gitProvider: GitProvider
  defaultBranch: string
  webhookSecret: string
  pipelineConfigPath: string
  isActive: boolean
  githubToken?: string
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  repoUrl: string
  gitProvider: GitProvider
  defaultBranch?: string
  pipelineConfigPath?: string
  githubToken?: string
}

export interface UpdateProjectRequest {
  name?: string
  repoUrl?: string
  gitProvider?: GitProvider
  defaultBranch?: string
  pipelineConfigPath?: string
  isActive?: boolean
  githubToken?: string
  clearGithubToken?: boolean
}

export interface SecretResponse {
  id: number
  name: string
  value: string
  createdAt: string
}

export interface CreateSecretRequest {
  name: string
  value: string
}
