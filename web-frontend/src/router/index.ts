import { createRouter, createWebHashHistory } from 'vue-router'
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
        name: 'Dashboard',
        component: () => import('../views/DashboardView.vue'),
      },
      {
        path: 'query/blocks',
        name: 'QueryBlocks',
        component: () => import('../views/QueryView.vue'),
        meta: { queryType: 'blocks' },
      },
      {
        path: 'query/containers',
        name: 'QueryContainers',
        component: () => import('../views/QueryView.vue'),
        meta: { queryType: 'containers' },
      },
      {
        path: 'query/inventory',
        name: 'QueryInventory',
        component: () => import('../views/QueryView.vue'),
        meta: { queryType: 'inventory' },
      },
      {
        path: 'rollback',
        name: 'Rollback',
        component: () => import('../views/RollbackView.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next({ name: 'Login' })
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
