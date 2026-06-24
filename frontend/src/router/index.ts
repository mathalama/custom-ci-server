import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/pages/DashboardPage.vue'),
    },
    {
      path: '/projects',
      name: 'projects',
      component: () => import('@/pages/ProjectsPage.vue'),
    },
    {
      path: '/projects/new',
      name: 'create-project',
      component: () => import('@/pages/CreateProjectPage.vue'),
    },
    {
      path: '/projects/:id',
      name: 'project-detail',
      component: () => import('@/pages/ProjectDetailPage.vue'),
      props: true,
    },
    {
      path: '/builds/:id',
      name: 'build-detail',
      component: () => import('@/pages/BuildDetailPage.vue'),
      props: true,
    },
  ],
})

export default router
