export type TaskStatus = 'TODO' | 'DOING' | 'DONE'

export interface TodoProject {
  uuid: string
  name: string
  dateCreated: string
  lastUpdated: string
}

export interface TodoTask {
  uuid: string
  projectUuid: string
  title: string
  status: TaskStatus
  sortOrder: number
  dateCreated: string
  lastUpdated: string
}

export interface ProjectRequest {
  name: string
}

export interface TaskRequest {
  title?: string
  status?: TaskStatus
  sortOrder?: number
}
