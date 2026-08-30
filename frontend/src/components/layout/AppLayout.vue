<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="logo">
          <div class="logo-mark">
            <span class="logo-r">R</span>
          </div>
          <div class="logo-typography">
            <span class="logo-title">Rabotyaga</span>
            <span class="logo-edition">CI</span>
          </div>
        </router-link>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="active" exact>
          <Icon name="dashboard" :size="16" />
          <span>Дашборд</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="active">
          <Icon name="folder" :size="16" />
          <span>Проекты</span>
        </router-link>
        <router-link to="/runners" class="nav-item" active-class="active">
          <Icon name="package" :size="16" />
          <span>Раннеры</span>
        </router-link>
        <router-link to="/settings" class="nav-item" active-class="active">
          <Icon name="gear" :size="16" />
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

    <!-- Main content (Fullscreen edge-to-edge container) -->
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
  width: 100%;
  background: var(--bg-main);
  color: var(--text-primary);
}

.sidebar {
  width: 240px;
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
  padding: 22px 24px 20px 24px;
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
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--accent-brand);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 15px;
  letter-spacing: -0.03em;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.35);
}

.logo-typography {
  display: flex;
  align-items: center;
  gap: 6px;
}

.logo-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.logo-edition {
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.25);
  color: var(--accent-brand);
  font-family: var(--font-mono);
}

.sidebar-nav {
  padding: 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13.5px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  border: 1px solid transparent;
}

.nav-item:hover {
  color: var(--text-primary);
  background: var(--bg-card-hover);
}

.nav-item.active {
  color: #ffffff;
  background: var(--bg-card);
  border-color: var(--border-color);
  box-shadow: var(--shadow-bento);
}

.sidebar-footer {
  padding: 18px 24px;
  border-top: 1px solid var(--border-muted);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.engine-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}

.status-indicator {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--accent-green);
  box-shadow: 0 0 8px var(--accent-green);
}

.version-label {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  width: calc(100% - 240px);
  background: var(--bg-main);
}

.content-wrapper {
  width: 100%;
  max-width: none;
  margin: 0;
  padding: 28px 36px;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .sidebar {
    width: 70px;
  }
  .sidebar-header .logo-typography, .nav-item span, .sidebar-footer {
    display: none;
  }
  .main-content {
    margin-left: 70px;
    width: calc(100% - 70px);
  }
  .content-wrapper {
    padding: 20px 16px;
  }
}
</style>
