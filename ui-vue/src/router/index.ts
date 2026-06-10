import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { initSession, isAuthenticated } from '@/core/state/session'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'landing',
    component: () => import('@/modules/auth/pages/LandingPage.vue')
  },
  {
    path: '/app/:projectUuid?',
    name: 'todos',
    component: () => import('@/modules/todos/pages/TodoAppPage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

export const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach(async (to) => {
  await initSession()

  if (to.meta.requiresAuth && !isAuthenticated.value) {
    return { name: 'landing' }
  }

  if (to.name === 'landing' && isAuthenticated.value) {
    return { name: 'todos' }
  }

  return true
})
