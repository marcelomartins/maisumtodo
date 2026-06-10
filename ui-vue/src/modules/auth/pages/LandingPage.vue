<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, sessionState } from '@/core/state/session'

const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const form = reactive({
  email: '',
  password: ''
})

const title = computed(() => mode.value === 'login' ? 'Entrar' : 'Criar conta')
const subtitle = computed(() => mode.value === 'login'
  ? 'Acesse seus projetos e continue suas tarefas.'
  : 'Comece com email e senha, sem cadastro complicado.')

async function submit() {
  const action = mode.value === 'login' ? login : register
  await action({ email: form.email, password: form.password })
  await router.push({ name: 'todos' })
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

function startRegistration() {
  mode.value = 'register'
  document.getElementById('auth-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}
</script>

<template>
  <main class="relative isolate min-h-screen overflow-hidden bg-white text-gray-900 dark:bg-[var(--ui-bg)] dark:text-white">
    <div class="pointer-events-none absolute inset-0 -z-10 overflow-hidden">
      <div class="absolute inset-0 bg-grid-pattern text-blue-950 opacity-[0.04] dark:text-white dark:opacity-[0.03]" />
      <div class="absolute -top-[20%] -left-[10%] h-[48%] w-[48%] rounded-full bg-blue-200/60 opacity-40 blur-3xl dark:bg-blue-900/40" />
      <div class="animate-pulse-slow absolute top-[34%] -right-[14%] h-[58%] w-[58%] rounded-full bg-sky-200/60 opacity-40 blur-3xl dark:bg-sky-900/40" />
      <div class="absolute -bottom-[22%] left-[18%] h-[42%] w-[42%] rounded-full bg-blue-100/70 opacity-50 blur-3xl dark:bg-blue-900/30" />
    </div>

    <header class="relative z-10 border-b border-gray-200/80 bg-white/80 backdrop-blur-md dark:border-gray-800 dark:bg-[var(--ui-bg)]">
      <div class="mx-auto flex h-20 w-full max-w-7xl items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        <div class="flex min-w-0 items-center gap-3">
          <div class="flex size-9 shrink-0 items-center justify-center rounded-xl bg-blue-600 text-white shadow-lg shadow-blue-500/20">
            <UIcon name="i-lucide-list-checks" class="size-5" />
          </div>
          <div class="min-w-0">
            <p class="truncate text-xl font-bold tracking-tight text-gray-950 dark:text-white">
              Mais Um Todo
            </p>
            <p class="truncate text-[10px] font-bold uppercase tracking-widest text-blue-600 dark:text-blue-400">
              Projetos e tarefas
            </p>
          </div>
        </div>

        <UButton color="primary" class="rounded-full px-5 font-medium shadow-lg shadow-blue-500/15" @click="startRegistration">
          Comecar
        </UButton>
      </div>
    </header>

    <section class="relative mx-auto grid min-h-[calc(100vh-5rem)] w-full max-w-7xl grid-cols-1 gap-10 px-4 py-10 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:px-8 lg:py-16">
      <div class="space-y-8">
        <UBadge color="primary" variant="soft" size="lg" class="rounded-full px-3">
          Simples, direto, focado
        </UBadge>

        <div class="space-y-5">
          <h1 class="max-w-3xl text-5xl font-extrabold leading-tight tracking-tight text-gray-950 dark:text-white sm:text-6xl lg:text-7xl">
            Organize projetos e <span class="bg-gradient-to-r from-blue-600 to-sky-500 bg-clip-text text-transparent dark:from-blue-400 dark:to-sky-400">tarefas</span> sem complicar o dia.
          </h1>
          <p class="max-w-2xl text-lg leading-8 text-gray-600 dark:text-gray-300">
            Crie projetos, acompanhe tarefas em lista ou kanban e mantenha o fluxo pequeno o suficiente para ser usado todos os dias.
          </p>
        </div>

        <div class="grid gap-4 text-sm text-gray-600 dark:text-gray-300 sm:grid-cols-3">
          <div class="rounded-3xl border border-gray-100 bg-white/80 p-5 shadow-lg shadow-blue-500/5 backdrop-blur dark:border-gray-800 dark:bg-gray-900/50">
            <div class="mb-4 flex size-11 items-center justify-center rounded-2xl bg-blue-50 text-blue-600 dark:bg-blue-500/10 dark:text-blue-400">
              <UIcon name="i-lucide-panel-left" class="size-5" />
            </div>
            <p class="mb-1 font-bold text-gray-950 dark:text-white">Projetos</p>
            <p>Organizacao pela barra lateral.</p>
          </div>
          <div class="rounded-3xl border border-gray-100 bg-white/80 p-5 shadow-lg shadow-blue-500/5 backdrop-blur dark:border-gray-800 dark:bg-gray-900/50">
            <div class="mb-4 flex size-11 items-center justify-center rounded-2xl bg-sky-50 text-sky-600 dark:bg-sky-500/10 dark:text-sky-400">
              <UIcon name="i-lucide-list-todo" class="size-5" />
            </div>
            <p class="mb-1 font-bold text-gray-950 dark:text-white">Lista</p>
            <p>Execucao rapida por tarefa.</p>
          </div>
          <div class="rounded-3xl border border-gray-100 bg-white/80 p-5 shadow-lg shadow-blue-500/5 backdrop-blur dark:border-gray-800 dark:bg-gray-900/50">
            <div class="mb-4 flex size-11 items-center justify-center rounded-2xl bg-green-50 text-green-600 dark:bg-green-500/10 dark:text-green-400">
              <UIcon name="i-lucide-columns-3" class="size-5" />
            </div>
            <p class="mb-1 font-bold text-gray-950 dark:text-white">Kanban</p>
            <p>Visualize o status do trabalho.</p>
          </div>
        </div>
      </div>

      <div id="auth-card" class="relative mx-auto w-full max-w-md scroll-mt-24">
        <div class="absolute -inset-1 rounded-[2rem] bg-gradient-to-r from-blue-500 to-sky-500 opacity-20 blur-xl" />

        <UCard
          class="relative overflow-hidden rounded-[2rem] border-gray-100 bg-white/95 shadow-2xl shadow-blue-500/10 backdrop-blur dark:border-gray-800 dark:bg-gray-900/95"
          :ui="{ header: 'p-6 sm:px-7', body: 'p-6 sm:p-7', footer: 'p-6 sm:px-7' }"
        >
          <template #header>
            <div class="space-y-2">
              <UBadge color="neutral" variant="subtle" class="rounded-full">
                Acesso
              </UBadge>
              <div>
                <h2 class="text-2xl font-bold text-gray-950 dark:text-white">
                  {{ title }}
                </h2>
                <p class="mt-1 text-sm text-gray-500 dark:text-gray-400">
                  {{ subtitle }}
                </p>
              </div>
            </div>
          </template>

          <form class="space-y-4" @submit.prevent="submit">
            <UFormField label="Email">
              <UInput v-model="form.email" type="email" placeholder="voce@email.com" size="lg" icon="i-lucide-mail" class="w-full" autocomplete="email" />
            </UFormField>

            <UFormField label="Senha">
              <UInput
                v-model="form.password"
                type="password"
                placeholder="Sua senha"
                size="lg"
                icon="i-lucide-lock"
                class="w-full"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              />
            </UFormField>

            <UAlert
              v-if="sessionState.error"
              color="error"
              variant="soft"
              icon="i-lucide-triangle-alert"
              :description="sessionState.error"
            />

            <UButton type="submit" size="lg" block :loading="sessionState.loading" class="rounded-full font-semibold shadow-xl shadow-blue-500/20">
              {{ mode === 'login' ? 'Entrar agora' : 'Criar conta' }}
            </UButton>
          </form>

          <template #footer>
            <div class="flex items-center justify-between gap-3 text-sm">
              <span class="text-gray-500 dark:text-gray-400">
                {{ mode === 'login' ? 'Ainda nao tem conta?' : 'Ja tem conta?' }}
              </span>
              <UButton variant="link" color="primary" class="font-semibold" @click="toggleMode">
                {{ mode === 'login' ? 'Cadastrar' : 'Entrar' }}
              </UButton>
            </div>
          </template>
        </UCard>
      </div>
    </section>
  </main>
</template>

<style scoped>
.animate-pulse-slow {
  animation: pulse 4s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.bg-grid-pattern {
  background-image:
    linear-gradient(to right, currentColor 1px, transparent 1px),
    linear-gradient(to bottom, currentColor 1px, transparent 1px);
  background-size: 32px 32px;
}
</style>
