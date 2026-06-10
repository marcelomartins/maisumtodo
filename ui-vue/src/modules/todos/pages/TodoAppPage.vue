<script setup lang="ts">
import { animations, tearDown } from '@formkit/drag-and-drop'
import { dragAndDrop } from '@formkit/drag-and-drop/vue'
import type { BadgeProps, TabsItem } from '@nuxt/ui'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, toRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { logout, normalizeError, sessionState } from '@/core/state/session'
import { todosApi } from '../api/todos.api'
import type { TaskStatus, TodoProject, TodoTask } from '../types'

type StatusMeta = {
  label: string
  color: BadgeProps['color']
  icon: string
}

type KanbanColumn = {
  key: TaskStatus
  title: string
  items: TodoTask[]
}

const route = useRoute()
const router = useRouter()

const projects = ref<TodoProject[]>([])
const tasks = ref<TodoTask[]>([])
const loading = ref(false)
const errorMessage = ref('')
const newProjectName = ref('')
const isCreatingProject = ref(false)
const projectCreateInputRef = ref<{ inputRef?: HTMLInputElement | null } | null>(null)
const projectNameDraft = ref('')
const isEditingProjectName = ref(false)
const projectNameInputRef = ref<{ inputRef?: HTMLInputElement | null } | null>(null)
const newTaskTitle = ref('')
const editingTaskUuid = ref<string | null>(null)
const editingTaskTitle = ref('')
const editingTaskInputRef = ref<{ inputRef?: HTMLInputElement | null } | null>(null)
const quickCreateColumn = ref<TaskStatus | null>(null)
const quickCreateTitle = ref('')
const quickCreateInputRef = ref<{ inputRef?: HTMLInputElement | null } | null>(null)

const statusOrder: TaskStatus[] = ['TODO', 'DOING', 'DONE']
const statusMeta: Record<TaskStatus, StatusMeta> = {
  TODO: {
    label: 'A fazer',
    color: 'neutral',
    icon: 'i-lucide-circle'
  },
  DOING: {
    label: 'Fazendo',
    color: 'info',
    icon: 'i-lucide-circle-dot'
  },
  DONE: {
    label: 'Concluido',
    color: 'success',
    icon: 'i-lucide-circle-check'
  }
}

const tabItems: TabsItem[] = [
  { label: 'Lista', value: 'list', icon: 'i-lucide-list-todo' },
  { label: 'Kanban', value: 'kanban', icon: 'i-lucide-columns-3' }
]

const activeTab = computed({
  get() {
    return route.query.tab === 'kanban' ? 'kanban' : 'list'
  },
  set(value: string | number) {
    router.replace({
      name: 'todos',
      params: route.params,
      query: value === 'kanban' ? { tab: 'kanban' } : {}
    })
  }
})

const selectedProjectUuid = computed(() => {
  const value = route.params.projectUuid
  return typeof value === 'string' ? value : ''
})

const selectedProject = computed(() => {
  return projects.value.find((project) => project.uuid === selectedProjectUuid.value) ?? null
})

const listedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => {
    const doneDiff = Number(a.status === 'DONE') - Number(b.status === 'DONE')
    if (doneDiff !== 0) return doneDiff
    return b.dateCreated.localeCompare(a.dateCreated)
  })
})

const orderedTasks = computed(() => {
  return [...tasks.value].sort((a, b) => {
    const statusDiff = statusOrder.indexOf(a.status) - statusOrder.indexOf(b.status)
    if (statusDiff !== 0) return statusDiff
    const orderDiff = Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0)
    if (orderDiff !== 0) return orderDiff
    return a.dateCreated.localeCompare(b.dateCreated)
  })
})

const kanbanColumns = computed<KanbanColumn[]>(() => {
  return statusOrder.map((status) => ({
    key: status,
    title: statusMeta[status].label,
    items: tasksByStatus(status)
  }))
})

const activeCount = computed(() => tasks.value.filter((task) => task.status !== 'DONE').length)

const containerRefs = ref<Map<TaskStatus, HTMLElement>>(new Map())
const localItems = reactive<Record<TaskStatus, TodoTask[]>>({
  TODO: [],
  DOING: [],
  DONE: []
})
const configuredParents = new Set<HTMLElement>()
const draggingTaskUuid = ref<string | null>(null)

const isBoardVisible = computed(() => activeTab.value === 'kanban' && selectedProject.value !== null)

onMounted(loadProjects)

watch(selectedProjectUuid, async (projectUuid) => {
  if (!projectUuid) {
    tasks.value = []
    return
  }

  await loadTasks(projectUuid)
})

watch(selectedProject, (project) => {
  projectNameDraft.value = project?.name ?? ''
  isEditingProjectName.value = false
}, { immediate: true })

watch(
  () => [
    activeTab.value,
    selectedProjectUuid.value,
    tasks.value.map((task) => `${task.uuid}:${task.status}:${task.sortOrder}`).join('|')
  ],
  async () => {
    if (!draggingTaskUuid.value) {
      syncLocalItems()
    }

    await nextTick()
    initSortable()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  teardownCardDrag()
})

async function loadProjects() {
  await run(async () => {
    projects.value = await todosApi.listProjects()

    const selectedStillExists = projects.value.some((project) => project.uuid === selectedProjectUuid.value)
    if (projects.value.length > 0 && !selectedStillExists) {
      const firstProject = projects.value[0]
      if (firstProject) {
        await selectProject(firstProject.uuid, true)
      }
      return
    }

    if (selectedProjectUuid.value) {
      await loadTasks(selectedProjectUuid.value)
    }
  })
}

async function loadTasks(projectUuid: string) {
  await run(async () => {
    tasks.value = await todosApi.listTasks(projectUuid)
    syncLocalItems()
  })
}

async function createProject() {
  const name = newProjectName.value.trim()
  if (!name) return

  await run(async () => {
    const project = await todosApi.createProject({ name })
    projects.value.push(project)
    newProjectName.value = ''
    isCreatingProject.value = false
    await selectProject(project.uuid)
  })
}

async function openProjectCreate() {
  isCreatingProject.value = true
  newProjectName.value = ''
  await nextTick()
  projectCreateInputRef.value?.inputRef?.focus()
}

function cancelProjectCreate() {
  isCreatingProject.value = false
  newProjectName.value = ''
}

async function startProjectNameEdit() {
  const project = selectedProject.value
  if (!project) return

  projectNameDraft.value = project.name
  isEditingProjectName.value = true
  await nextTick()
  const input = projectNameInputRef.value?.inputRef
  input?.focus()
  input?.setSelectionRange(projectNameDraft.value.length, projectNameDraft.value.length)
}

function cancelProjectNameEdit() {
  projectNameDraft.value = selectedProject.value?.name ?? ''
  isEditingProjectName.value = false
}

async function saveProjectNameEdit() {
  if (!isEditingProjectName.value || loading.value) return

  const project = selectedProject.value
  const name = projectNameDraft.value.trim()

  if (!project) {
    cancelProjectNameEdit()
    return
  }

  if (!name || name === project.name) {
    cancelProjectNameEdit()
    return
  }

  const success = await run(async () => {
    const updatedProject = await todosApi.updateProject(project.uuid, { name })
    replaceProject(updatedProject)
  })

  if (success) {
    isEditingProjectName.value = false
  }
}

async function createTask(status: TaskStatus = 'TODO', title = newTaskTitle.value) {
  const project = selectedProject.value
  const normalizedTitle = title.trim()
  if (!project || !normalizedTitle) return false

  const success = await run(async () => {
    const task = await todosApi.createTask(project.uuid, {
      title: normalizedTitle,
      status,
      sortOrder: nextSortOrder(status)
    })
    tasks.value.push(task)
    newTaskTitle.value = ''
    syncLocalItems()
  })

  return success
}

async function openQuickCreate(status: TaskStatus) {
  quickCreateColumn.value = status
  quickCreateTitle.value = ''
  await nextTick()
  quickCreateInputRef.value?.inputRef?.focus()
  requestAnimationFrame(() => quickCreateInputRef.value?.inputRef?.focus())
}

function cancelQuickCreate() {
  quickCreateColumn.value = null
  quickCreateTitle.value = ''
}

async function saveQuickCreate(status: TaskStatus) {
  const success = await createTask(status, quickCreateTitle.value)
  if (success) {
    cancelQuickCreate()
  }
}

function handleQuickCreateBlur(status: TaskStatus) {
  if (quickCreateColumn.value !== status || loading.value) return

  if (!quickCreateTitle.value.trim()) {
    cancelQuickCreate()
    return
  }

  void saveQuickCreate(status)
}

async function saveTaskTitle(task: TodoTask) {
  if (editingTaskUuid.value !== task.uuid) return

  const title = editingTaskTitle.value.trim()
  if (!title || title === task.title) {
    cancelTaskEdit()
    return
  }

  const success = await updateTask(task, { title })
  if (success && editingTaskUuid.value === task.uuid) {
    cancelTaskEdit()
  }
}

async function toggleTaskDone(task: TodoTask, value: unknown) {
  const status: TaskStatus = value === true ? 'DONE' : 'TODO'
  if (task.status === status) return

  await updateTask(task, { status, sortOrder: nextSortOrder(status) })
}

async function startTaskEdit(task: TodoTask) {
  editingTaskUuid.value = task.uuid
  editingTaskTitle.value = task.title
  await nextTick()
  editingTaskInputRef.value?.inputRef?.focus()
  editingTaskInputRef.value?.inputRef?.select()
}

function cancelTaskEdit() {
  editingTaskUuid.value = null
  editingTaskTitle.value = ''
}

async function deleteTask(task: TodoTask) {
  if (!window.confirm(`Excluir a tarefa "${task.title}"?`)) return

  await run(async () => {
    await todosApi.deleteTask(task.uuid)
    tasks.value = tasks.value.filter((item) => item.uuid !== task.uuid)
    syncLocalItems()
  })
}

async function updateTask(task: TodoTask, request: { title?: string, status?: TaskStatus, sortOrder?: number }) {
  const success = await run(async () => {
    const updatedTask = await todosApi.updateTask(task.uuid, request)
    replaceTask(updatedTask)
    syncLocalItems()
  })

  if (!success) {
    syncLocalItems()
  }

  return success
}

async function selectProject(projectUuid: string, replace = false) {
  const navigation = {
    name: 'todos',
    params: { projectUuid },
    query: activeTab.value === 'kanban' ? { tab: 'kanban' } : {}
  }

  if (replace) {
    await router.replace(navigation)
    return
  }

  await router.push(navigation)
}

async function logoutUser() {
  await logout()
  await router.push({ name: 'landing' })
}

function tasksByStatus(status: TaskStatus) {
  return tasks.value
    .filter((task) => task.status === status)
    .sort((a, b) => Number(a.sortOrder ?? 0) - Number(b.sortOrder ?? 0))
}

function replaceProject(project: TodoProject) {
  projects.value = projects.value.map((item) => item.uuid === project.uuid ? project : item)
}

function replaceTask(task: TodoTask) {
  tasks.value = tasks.value.map((item) => item.uuid === task.uuid ? task : item)
}

function isTaskStatus(value: unknown): value is TaskStatus {
  return typeof value === 'string' && statusOrder.includes(value as TaskStatus)
}

function syncLocalItems() {
  statusOrder.forEach((status) => {
    localItems[status] = tasksByStatus(status)
  })
}

function setContainerRef(el: unknown, status: TaskStatus) {
  if (el instanceof HTMLElement) {
    containerRefs.value.set(status, el)
  } else {
    containerRefs.value.delete(status)
  }
}

function teardownCardDrag() {
  configuredParents.forEach((parent) => tearDown(parent))
  configuredParents.clear()
}

function initSortable() {
  if (!isBoardVisible.value) {
    teardownCardDrag()
    return
  }

  const nextParents = new Set(containerRefs.value.values())

  configuredParents.forEach((parent) => {
    if (!nextParents.has(parent)) {
      tearDown(parent)
      configuredParents.delete(parent)
    }
  })

  if (containerRefs.value.size === 0) return

  const configs = Array.from(containerRefs.value.entries()).map(([status, el]) => ({
    parent: el,
    values: toRef(localItems, status),
    group: 'todo-kanban',
    plugins: [animations({ duration: 140 })],
    onDragstart: (event: any) => {
      draggingTaskUuid.value = event.draggedNode?.data?.value?.uuid ?? null
    },
    onDragend: (event: any) => {
      handleDragEnd(event)
    }
  }))

  dragAndDrop(configs)
  nextParents.forEach((parent) => configuredParents.add(parent))
}

function handleDragEnd(event: any) {
  const fromStatus = event.state.initialParent?.el?.dataset?.status
  const toStatus = event.parent?.el?.dataset?.status
  const draggedUuid = event.draggedNode?.data?.value?.uuid as string | undefined
  const oldIndex = event.state.initialIndex
  const newIndex = draggedUuid
    ? event.values.findIndex((value: TodoTask) => value.uuid === draggedUuid)
    : -1

  draggingTaskUuid.value = null

  if (!draggedUuid || !isTaskStatus(fromStatus) || !isTaskStatus(toStatus) || newIndex === -1) {
    syncLocalItems()
    return
  }

  if (fromStatus === toStatus && newIndex === oldIndex) {
    syncLocalItems()
    return
  }

  const task = tasks.value.find((item) => item.uuid === draggedUuid)
  if (!task) {
    syncLocalItems()
    return
  }

  const sortOrder = calculateSortOrder(event.values as TodoTask[], newIndex)
  void updateTask(task, { status: toStatus, sortOrder })
}

function calculateSortOrder(columnItems: TodoTask[], index: number) {
  const previous = columnItems[index - 1]
  const next = columnItems[index + 1]
  const previousOrder = previous ? Number(previous.sortOrder) : null
  const nextOrder = next ? Number(next.sortOrder) : null

  if (previousOrder !== null && Number.isFinite(previousOrder) && nextOrder !== null && Number.isFinite(nextOrder)) {
    return (previousOrder + nextOrder) / 2
  }

  if (previousOrder !== null && Number.isFinite(previousOrder)) {
    return previousOrder + 1
  }

  if (nextOrder !== null && Number.isFinite(nextOrder)) {
    return Math.max(0.0001, nextOrder / 2)
  }

  return 1
}

function nextSortOrder(status: TaskStatus) {
  const statusTasks = tasksByStatus(status)
  const lastTask = statusTasks[statusTasks.length - 1]
  const lastOrder = lastTask ? Number(lastTask.sortOrder) : 0
  return Number.isFinite(lastOrder) ? lastOrder + 1 : statusTasks.length + 1
}

function taskCode(task: TodoTask) {
  const index = orderedTasks.value.findIndex((item) => item.uuid === task.uuid)
  return `#${String(index + 1).padStart(3, '0')}`
}

async function run(action: () => Promise<void>) {
  loading.value = true
  errorMessage.value = ''

  try {
    await action()
    return true
  } catch (error) {
    errorMessage.value = normalizeError(error)
    return false
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="min-h-screen bg-zinc-50 text-default dark:bg-zinc-950">
    <div class="flex min-h-screen flex-col lg:flex-row">
      <aside class="border-b border-default bg-elevated/25 p-4 backdrop-blur lg:sticky lg:top-0 lg:h-screen lg:w-[18rem] lg:border-b-0 lg:border-r lg:p-5">
        <div class="space-y-4">
          <div class="flex min-w-0 items-center gap-3 rounded-2xl border border-default bg-default/70 p-3 shadow-sm">
            <div class="flex size-11 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white shadow-lg shadow-blue-500/20">
              <UIcon name="i-lucide-list-checks" class="size-5.5" />
            </div>
            <div class="min-w-0">
              <p class="truncate text-base font-bold leading-5 text-highlighted">Mais Um Todo</p>
              <p class="truncate text-xs text-muted">Organize suas tarefas</p>
            </div>
          </div>

          <div class="flex items-center justify-between gap-3 px-1">
            <div class="min-w-0">
              <p class="text-xs font-bold uppercase tracking-widest text-primary">Projetos</p>
            </div>
            <div class="flex shrink-0 items-center gap-1.5">
            <UButton
              type="button"
              color="primary"
              variant="soft"
              size="sm"
              icon="i-lucide-plus"
              square
              :disabled="loading"
              aria-label="Adicionar projeto"
              class="rounded-full shadow-sm shadow-blue-500/10"
              @click="openProjectCreate"
            />
            <UButton color="neutral" variant="ghost" size="sm" icon="i-lucide-log-out" class="rounded-full" @click="logoutUser">
              Sair
            </UButton>
            </div>
          </div>
        </div>

        <div class="mt-5 space-y-2 lg:max-h-[calc(100vh-10.5rem)] lg:overflow-y-auto lg:pr-1">
          <button
            v-for="project in projects"
            :key="project.uuid"
            type="button"
            class="group flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2.5 text-left text-sm transition-colors"
            :class="project.uuid === selectedProjectUuid ? 'bg-primary/10 text-primary shadow-sm shadow-blue-500/5 ring-1 ring-primary/20' : 'text-default hover:bg-elevated/60'"
            @click="selectProject(project.uuid)"
          >
            <span class="flex min-w-0 items-center gap-2">
              <UIcon name="i-lucide-folder" class="size-4 shrink-0" />
              <span class="truncate font-medium">{{ project.name }}</span>
            </span>
            <UBadge v-if="project.uuid === selectedProjectUuid" color="neutral" size="xs" variant="subtle">
              {{ activeCount }}
            </UBadge>
          </button>

          <form
            v-if="isCreatingProject"
            class="flex items-center gap-2 rounded-xl border border-primary/20 bg-default p-2 shadow-sm shadow-blue-500/5 ring-1 ring-primary/10"
            @submit.prevent="createProject"
          >
            <UInput
              ref="projectCreateInputRef"
              v-model="newProjectName"
              autofocus
              placeholder="Nome do projeto"
              class="min-w-0 flex-1"
              :disabled="loading"
              @keydown.esc.prevent="cancelProjectCreate"
            />
            <UButton
              type="submit"
              :loading="loading"
              icon="i-lucide-check"
              square
              aria-label="Salvar projeto"
              class="rounded-full"
            />
            <UButton
              type="button"
              color="neutral"
              variant="ghost"
              icon="i-lucide-x"
              square
              aria-label="Cancelar projeto"
              class="rounded-full"
              @click="cancelProjectCreate"
            />
          </form>
        </div>

        <UEmpty
          v-if="projects.length === 0 && !loading && !isCreatingProject"
          class="mt-6"
          variant="subtle"
          icon="i-lucide-folder-plus"
          title="Nenhum projeto"
          description="Crie o primeiro projeto para organizar suas tarefas."
        />
      </aside>

      <section class="min-w-0 flex-1 p-4 sm:p-6 lg:p-8">
        <div class="mx-auto flex h-full max-w-7xl flex-col gap-6">
          <div class="min-w-0">
            <form v-if="selectedProject && isEditingProjectName" class="max-w-2xl" @submit.prevent="saveProjectNameEdit">
              <UInput
                ref="projectNameInputRef"
                v-model="projectNameDraft"
                color="neutral"
                variant="outline"
                size="xl"
                class="w-full max-w-2xl"
                :disabled="loading"
                :ui="{ base: 'px-2 py-1 text-3xl font-bold tracking-tight text-highlighted sm:text-4xl' }"
                @blur="saveProjectNameEdit"
                @keydown.esc.prevent="cancelProjectNameEdit"
              />
            </form>

            <button
              v-else-if="selectedProject"
              type="button"
              class="-mx-2 -my-1 max-w-full rounded-lg px-2 py-1 text-left text-3xl font-bold tracking-tight text-highlighted transition hover:bg-elevated/60 hover:ring-1 hover:ring-default sm:text-4xl"
              title="Clique para alterar o nome do projeto"
              @click="startProjectNameEdit"
            >
              <span class="block truncate">{{ selectedProject.name }}</span>
            </button>

            <h2 v-else class="text-3xl font-bold tracking-tight text-highlighted sm:text-4xl">
              Escolha um projeto
            </h2>
          </div>

          <UAlert
            v-if="errorMessage"
            color="error"
            variant="soft"
            icon="i-lucide-triangle-alert"
            :description="errorMessage"
          />

          <UEmpty
            v-if="!selectedProject"
            variant="subtle"
            icon="i-lucide-list-plus"
            title="Crie ou selecione um projeto"
            description="A lista e o kanban aparecem aqui quando um projeto estiver selecionado."
            class="flex-1"
          />

          <template v-else>
            <UTabs
              v-model="activeTab"
              :content="false"
              :items="tabItems"
              color="primary"
              variant="link"
              size="xl"
              class="w-full"
              :ui="{ list: 'bg-transparent p-0 border-b border-default/70 pb-1 gap-2', trigger: 'px-4 py-2 text-[0.95rem] font-semibold cursor-pointer', indicator: 'h-0.5 bottom-0' }"
            />

            <div v-if="loading && tasks.length === 0" class="flex items-center justify-center py-12">
              <UIcon name="i-lucide-loader-2" class="size-8 animate-spin text-primary" />
            </div>

            <template v-else-if="activeTab === 'list'">
              <form class="flex flex-col gap-3 rounded-2xl border border-default bg-default p-3 shadow-sm sm:flex-row" @submit.prevent="createTask()">
                <UInput
                  v-model="newTaskTitle"
                  placeholder="Digite uma nova tarefa"
                  icon="i-lucide-plus"
                  class="min-w-0 flex-1"
                />
                <UButton type="submit" :loading="loading" icon="i-lucide-plus" class="rounded-full shadow-sm shadow-blue-500/10">
                  Adicionar
                </UButton>
              </form>

              <UEmpty
                v-if="listedTasks.length === 0"
                variant="subtle"
                icon="i-lucide-list-todo"
                title="Sem tarefas ainda"
                description="Adicione a primeira tarefa desse projeto."
              />

              <div v-else class="overflow-hidden rounded-2xl border border-default bg-default shadow-sm">
                <div class="flex items-center justify-between gap-3 border-b border-default bg-elevated/50 px-4 py-3.5 text-sm">
                  <div class="flex items-center gap-2">
                    <span class="font-semibold text-highlighted">Tarefas</span>
                    <UBadge color="neutral" variant="subtle">{{ listedTasks.length }}</UBadge>
                  </div>
                  <span class="text-xs text-muted">Marque como concluida ou use as acoes no fim da linha.</span>
                </div>

                <div class="flex flex-col">
                  <div
                    v-for="task in listedTasks"
                    :key="task.uuid"
                    class="border-b border-default last:border-b-0"
                  >
                    <div class="group flex cursor-default flex-col gap-3 px-3 py-3 text-sm transition-colors hover:bg-elevated/30 sm:px-4 sm:py-[14px] lg:flex-row lg:items-center lg:justify-between">
                      <div class="flex min-w-0 flex-1 items-center gap-1 sm:gap-2.5">
                        <div class="flex shrink-0 items-center justify-center p-1 sm:p-0" @click.stop>
                          <UCheckbox
                            :model-value="task.status === 'DONE'"
                            :disabled="loading"
                            aria-label="Concluir tarefa"
                            @update:model-value="toggleTaskDone(task, $event)"
                          />
                        </div>

                        <div class="shrink-0 sm:w-[80px]">
                          <span
                            class="font-mono text-xs font-semibold sm:text-sm"
                            :class="task.status === 'DONE' ? 'text-muted line-through' : 'text-highlighted'"
                          >
                            {{ taskCode(task) }}
                          </span>
                        </div>

                        <form
                          v-if="editingTaskUuid === task.uuid"
                          class="min-w-0 flex-1"
                          @submit.prevent="saveTaskTitle(task)"
                          @click.stop
                        >
                          <UInput
                            ref="editingTaskInputRef"
                            v-model="editingTaskTitle"
                            autofocus
                            placeholder="Titulo da tarefa"
                            class="w-full"
                            :ui="{ base: 'font-medium' }"
                            @blur="saveTaskTitle(task)"
                          />
                        </form>
                        <button
                          v-else
                          type="button"
                          class="flex min-w-0 flex-1 items-center gap-2 text-left transition-colors"
                          :class="task.status === 'DONE' ? 'text-muted line-through' : 'text-highlighted'"
                          @click="startTaskEdit(task)"
                        >
                          <span class="min-w-0 truncate font-medium">
                            {{ task.title }}
                          </span>
                        </button>
                      </div>

                      <div class="flex shrink-0 flex-wrap items-center gap-2 pl-9 sm:pl-[100px] lg:pl-0 lg:justify-end">
                        <UBadge :color="statusMeta[task.status].color" variant="subtle">
                          {{ statusMeta[task.status].label }}
                        </UBadge>

                        <UButton
                          icon="i-lucide-trash-2"
                          color="error"
                          variant="ghost"
                          size="xs"
                          class="rounded-full"
                          aria-label="Excluir tarefa"
                          @click="deleteTask(task)"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </template>

            <template v-else>
              <UEmpty
                v-if="tasks.length === 0"
                variant="subtle"
                icon="i-lucide-columns-3"
                title="Quadro vazio"
                description="Adicione tarefas para visualizar o fluxo no kanban."
              />

              <div v-else class="min-h-[32rem] min-w-0 flex-1 overflow-x-auto overflow-y-hidden pb-2">
                <div class="flex h-full min-w-max gap-4">
                  <section
                    v-for="column in kanbanColumns"
                    :key="column.key"
                    class="kanban-column flex h-full min-h-[32rem] w-72 min-w-72 flex-shrink-0 flex-col gap-3 rounded-2xl border border-default bg-elevated/40 p-3 shadow-sm"
                  >
                    <div class="flex flex-shrink-0 items-center justify-between gap-3">
                      <div class="min-w-0">
                        <div class="flex items-center gap-1.5">
                          <UIcon :name="statusMeta[column.key].icon" class="size-4 text-muted" />
                          <span class="truncate text-sm font-semibold text-highlighted">{{ column.title }}</span>
                        </div>
                      </div>
                      <div class="flex items-center gap-1.5">
                        <UButton
                          type="button"
                          color="neutral"
                          variant="ghost"
                          size="xs"
                          square
                          icon="i-lucide-plus"
                          :disabled="loading"
                          class="rounded-full"
                          :aria-label="`Adicionar tarefa em ${column.title}`"
                          @click="openQuickCreate(column.key)"
                        />
                        <UBadge color="neutral" variant="subtle">{{ column.items.length }}</UBadge>
                      </div>
                    </div>

                    <div
                      v-if="quickCreateColumn === column.key"
                      class="flex-shrink-0 rounded-lg border border-primary/40 bg-default p-3 shadow-sm ring-1 ring-primary/10"
                      @click.stop
                      @pointerdown.stop
                    >
                      <UInput
                        ref="quickCreateInputRef"
                        v-model="quickCreateTitle"
                        autofocus
                        maxlength="200"
                        placeholder="Titulo da tarefa"
                        :loading="loading"
                        :disabled="loading"
                        class="w-full"
                        :ui="{ base: 'text-sm font-medium' }"
                        @blur="handleQuickCreateBlur(column.key)"
                        @keydown.enter.prevent.stop="saveQuickCreate(column.key)"
                        @keydown.esc.prevent.stop="cancelQuickCreate"
                        @keydown.stop
                      />
                    </div>

                    <div
                      :ref="(el) => setContainerRef(el, column.key)"
                      :data-status="column.key"
                      class="flex min-h-16 flex-1 flex-col gap-3 overflow-y-auto pr-1 -mr-1"
                    >
                      <div
                        v-for="task in (localItems[column.key] ?? [])"
                        :key="task.uuid"
                        class="drag-handle-target group flex-shrink-0 cursor-grab select-none rounded-xl border border-default bg-default p-3 transition-colors duration-200 hover:border-primary/40 hover:shadow-sm active:cursor-grabbing"
                      >
                        <div class="flex items-start gap-2">
                          <div class="min-w-0 flex-1 text-left text-sm font-medium leading-snug">
                            <code class="mr-1.5 inline-block rounded bg-muted px-1.5 py-0.5 align-baseline font-mono text-[12px] font-semibold text-highlighted">
                              {{ taskCode(task) }}
                            </code>
                            <span :class="task.status === 'DONE' ? 'text-muted line-through' : 'text-highlighted'">
                              {{ task.title }}
                            </span>
                          </div>

                          <UButton
                            color="error"
                            variant="ghost"
                            size="xs"
                            square
                            icon="i-lucide-trash-2"
                            class="-mt-1 shrink-0 rounded-full opacity-100 transition-opacity sm:opacity-0 sm:group-hover:opacity-100"
                            aria-label="Excluir tarefa"
                            @click.stop="deleteTask(task)"
                            @pointerdown.stop
                          />
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              </div>
            </template>
          </template>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.formkit-drag-and-drop-active {
  cursor: grabbing;
  opacity: 1;
  background-color: rgb(var(--ui-bg-elevated));
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -2px rgb(0 0 0 / 0.05);
}

.overflow-y-auto {
  scroll-behavior: smooth;
}
</style>
