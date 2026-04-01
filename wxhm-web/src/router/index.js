import { createRouter, createWebHistory } from 'vue-router'
import { apiFetch, setAdminToken } from '@/api/client'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/Home.vue') },
  { path: '/group/:groupName', name: 'group', component: () => import('@/views/GroupPage.vue') },
  { path: '/admin/login', name: 'admin-login', component: () => import('@/views/admin/Login.vue') },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/AdminHome.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/stats',
    name: 'admin-stats',
    component: () => import('@/views/admin/StatsPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/upload',
    name: 'admin-upload',
    component: () => import('@/views/admin/UploadPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/notice',
    name: 'admin-notice',
    component: () => import('@/views/admin/NoticePage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/missing-groups',
    name: 'admin-missing',
    component: () => import('@/views/admin/MissingGroupsPage.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin/login-monitor',
    name: 'admin-login-monitor',
    component: () => import('@/views/admin/LoginMonitorPage.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory('/app/'),
  routes,
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) return true
  const res = await apiFetch('/api/admin/session')
  const data = await res.json().catch(() => ({}))
  if (!data.loggedIn) {
    setAdminToken(null)
    const fp = to.fullPath || '/admin'
    const redir = fp.startsWith('/app') ? fp : '/app' + (fp.startsWith('/') ? fp : '/' + fp)
    return { name: 'admin-login', query: { redirect: redir } }
  }
  if (data.token) setAdminToken(data.token)
  return true
})

export default router
