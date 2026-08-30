<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="logo">
          <div class="logo-mark">
            <svg viewBox="0 0 100 100" width="22" height="22" fill="none">
              <path d="M28 28 L72 28 L28 72 L72 72" stroke="#C4B5FD" stroke-width="9" stroke-linecap="round" stroke-linejoin="round" />
              <circle cx="28" cy="28" r="8" fill="#09090B" stroke="#A78BFA" stroke-width="3.5" />
              <circle cx="28" cy="28" r="3" fill="#A78BFA" />
              <circle cx="72" cy="28" r="8" fill="#09090B" stroke="#8B5CF6" stroke-width="3.5" />
              <circle cx="72" cy="28" r="3" fill="#8B5CF6" />
              <circle cx="28" cy="72" r="8" fill="#09090B" stroke="#7C3AED" stroke-width="3.5" />
              <circle cx="28" cy="72" r="3" fill="#7C3AED" />
              <circle cx="72" cy="72" r="8" fill="#09090B" stroke="#10B981" stroke-width="3.5" />
              <circle cx="72" cy="72" r="3" fill="#10B981" />
            </svg>
          </div>
          <div class="logo-typography">
            <span class="logo-title">Zovik</span>
            <span class="logo-edition">CI/CD</span>
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
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  background: #18181b;
  border: 1px solid #27272a;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(124, 58, 237, 0.25);
  transition: all 0.2s ease;
}

.logo:hover .logo-mark {
  border-color: rgba(124, 58, 237, 0.5);
  box-shadow: 0 4px 20px rgba(124, 58, 237, 0.4);
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
  background: rgba(124, 58, 237, 0.12);
  border: 1px solid rgba(124, 58, 237, 0.3);
  color: #a78bfa;
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
