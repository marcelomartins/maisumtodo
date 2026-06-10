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
</script>

<template>
  <main class="min-h-screen overflow-hidden bg-slate-950 text-white">
    <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(99,102,241,0.45),transparent_34%),radial-gradient(circle_at_bottom_right,rgba(34,211,238,0.24),transparent_32%)]" />

    <section class="relative mx-auto grid min-h-screen w-full max-w-6xl grid-cols-1 gap-8 px-5 py-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:px-8">
      <div class="space-y-8">
        <UBadge color="secondary" variant="soft" size="lg">
          Simples, direto, focado
        </UBadge>

        <div class="space-y-5">
          <h1 class="max-w-3xl text-5xl font-black tracking-tight text-white sm:text-6xl lg:text-7xl">
            Mais Um Todo para organizar projetos sem virar outro projeto.
          </h1>
          <p class="max-w-2xl text-lg leading-8 text-slate-300">
            Crie projetos, acompanhe tarefas em lista ou kanban e mantenha o fluxo pequeno o suficiente para ser usado todos os dias.
          </p>
        </div>

        <div class="grid gap-3 text-sm text-slate-300 sm:grid-cols-3">
          <div class="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
            <p class="text-2xl font-bold text-white">01</p>
            <p>Projetos na lateral.</p>
          </div>
          <div class="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
            <p class="text-2xl font-bold text-white">02</p>
            <p>Lista para execução rápida.</p>
          </div>
          <div class="rounded-2xl border border-white/10 bg-white/5 p-4 backdrop-blur">
            <p class="text-2xl font-bold text-white">03</p>
            <p>Kanban para visualizar status.</p>
          </div>
        </div>
      </div>

      <UCard class="mx-auto w-full max-w-md border-white/10 bg-white/95 text-slate-950 shadow-2xl shadow-indigo-950/40 dark:bg-slate-100">
        <template #header>
          <div class="space-y-1">
            <h2 class="text-2xl font-bold text-slate-950">
              {{ title }}
            </h2>
            <p class="text-sm text-slate-500">
              {{ subtitle }}
            </p>
          </div>
        </template>

        <form class="space-y-4" @submit.prevent="submit">
          <label class="block space-y-1.5">
            <span class="text-sm font-medium text-slate-700">Email</span>
            <UInput v-model="form.email" type="email" placeholder="voce@email.com" size="lg" class="w-full" autocomplete="email" />
          </label>

          <label class="block space-y-1.5">
            <span class="text-sm font-medium text-slate-700">Senha</span>
            <UInput v-model="form.password" type="password" placeholder="Sua senha" size="lg" class="w-full" autocomplete="current-password" />
          </label>

          <p v-if="sessionState.error" class="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700 ring-1 ring-red-200">
            {{ sessionState.error }}
          </p>

          <UButton type="submit" size="lg" block :loading="sessionState.loading">
            {{ mode === 'login' ? 'Entrar agora' : 'Criar conta' }}
          </UButton>
        </form>

        <template #footer>
          <div class="flex items-center justify-between gap-3 text-sm">
            <span class="text-slate-500">
              {{ mode === 'login' ? 'Ainda nao tem conta?' : 'Ja tem conta?' }}
            </span>
            <UButton variant="link" color="primary" @click="toggleMode">
              {{ mode === 'login' ? 'Cadastrar' : 'Entrar' }}
            </UButton>
          </div>
        </template>
      </UCard>
    </section>
  </main>
</template>
