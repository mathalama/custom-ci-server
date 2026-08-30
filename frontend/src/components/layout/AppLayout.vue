<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="logo">
          <div class="logo-mark">
            <span>R</span>
          </div>
          <div class="logo-typography">
            <span class="logo-title">Rabotyaga</span>
            <span class="logo-edition">CI</span>
          </div>
        </router-link>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="active" exact>
          <Icon name="dashboard" :size="15" />
          <span>Дашборд</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="active">
          <Icon name="folder" :size="15" />
          <span>Проекты</span>
        </router-link>
        <router-link to="/runners" class="nav-item" active-class="active">
          <Icon name="package" :size="15" />
          <span>Раннеры</span>
        </router-link>
        <router-link to="/settings" class="nav-item" active-class="active">
          <Icon name="gear" :size="15" />
          <span>Настройки</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="engine-status">
          <div class="status-indicator"></div>
          <span>Система активна</span>
        </div>
        <div class="version-label">v1.0.0-rc</div>
      </div>
    </aside>

    <!-- Main content -->
    <main class="main-content">
      <div class="content-wrapper">
        <slot />
      </div>
    </main>

    <!-- Global Toast Notifications -->
    <ToastContainer />
  </div>
</template>

<script setup lang="ts">
import Icon from '@/components/common/Icon.vue'
import ToastContainer from '@/components/common/ToastContainer.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-main);
  color: var(--text-primary);
}

.sidebar {
  width: 230px;
  background: var(--bg-sidebar);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  z-index: 50;
}

.sidebar-header {
  padding: 20px 20px 18px 20px;
  border-bottom: 1px solid var(--border-muted);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  text-decoration: none;
  color: inherit;
}

.logo-mark {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: #f4f4f3;
  color: #0c0d0e;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 14px;
  letter-spacing: -0.03em;
}

.logo-typography {
  display: flex;
  align-items: center;
  gap: 6px;
}

.logo-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.logo-edition {
  font-size: 10px;
  font-weight: 600;
  padding: 1px 5px;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.sidebar-nav {
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid transparent;
}

.nav-item:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
}

.nav-item.active {
  color: var(--text-primary);
  background: var(--bg-card);
  border-color: var(--border-color);
  font-weight: 600;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.engine-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-secondary);
}

.status-indicator {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-green);
  box-shadow: 0 0 6px rgba(52, 211, 153, 0.4);
}

.version-label {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.main-content {
  flex: 1;
  margin-left: 230px;
  min-height: 100vh;
  background: var(--bg-main);
}

.content-wrapper {
  max-width: 1240px;
  margin: 0 auto;
  padding: 32px 40px;
}
</style>
