import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import LayoutView from '@/views/LayoutView.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: LayoutView,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { requiresAuth: true, title: '统计概览' },
      },
      {
        path: 'checkin',
        name: 'Checkin',
        component: () => import('@/views/CheckinView.vue'),
        meta: { requiresAuth: true, title: '包裹入库' },
      },
      {
        path: 'packages',
        name: 'PackageList',
        component: () => import('@/views/PackageListView.vue'),
        meta: { requiresAuth: true, title: '包裹列表' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
