import { api } from '@/core/api/client'
import type { ProjectRequest, TaskRequest, TodoProject, TodoTask } from '../types'

export const todosApi = {
  listProjects() {
    return api.get<TodoProject[]>('/api/projects')
  },

  createProject(request: ProjectRequest) {
    return api.post<TodoProject>('/api/projects', request)
  },

  updateProject(projectUuid: string, request: ProjectRequest) {
    return api.put<TodoProject>(`/api/projects/${projectUuid}`, request)
  },

  deleteProject(projectUuid: string) {
    return api.delete<void>(`/api/projects/${projectUuid}`)
  },

  listTasks(projectUuid: string) {
    return api.get<TodoTask[]>(`/api/projects/${projectUuid}/tasks`)
  },

  createTask(projectUuid: string, request: TaskRequest) {
    return api.post<TodoTask>(`/api/projects/${projectUuid}/tasks`, request)
  },

  updateTask(taskUuid: string, request: TaskRequest) {
    return api.put<TodoTask>(`/api/tasks/${taskUuid}`, request)
  },

  deleteTask(taskUuid: string) {
    return api.delete<void>(`/api/tasks/${taskUuid}`)
  }
}
