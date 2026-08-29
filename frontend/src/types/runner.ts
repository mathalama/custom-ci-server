export type RunnerStatus = 'ONLINE' | 'OFFLINE' | 'BUSY' | 'PROVISIONING' | 'ERROR'

export interface RunnerResponse {
  id: number
  name: string
  host?: string
  port?: number
  username?: string
  status: RunnerStatus
  os?: string
  cpuCores?: number
  memoryBytes?: number
  lastSeenAt: string | null
  createdAt: string
  installLog?: string
}

export interface RegistrationTokenResponse {
  runnerId: number
  registrationToken: string
  expiresAt: string
}

export interface AddRunnerRequest {
  name: string
  host?: string
  port?: number
  sshUser?: string
  sshKey?: string
  sshPassword?: string
}
