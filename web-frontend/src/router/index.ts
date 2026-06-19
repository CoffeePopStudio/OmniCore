import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/dashboard',
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
      },
      {
        path: 'query',
        name: 'Query',
        component: () => import('../views/QueryView.vue'),
      },
      {
        path: 'rollback',
        name: 'Rollback',
        component: () => import('../views/RollbackView.vue'),
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('../views/LogViewer.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach(async (to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!token) {
    const handled = await useAuthStore().checkAutoLogin()
    if (handled) {
      next()
      return
    }
  }
  const hasToken = !!localStorage.getItem('token')
  if (to.meta.requiresAuth && !hasToken) {
    next({ name: 'Login' })
  } else if ((to.name === 'Login' || to.name === 'Register') && hasToken) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
