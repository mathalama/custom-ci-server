<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <router-link to="/" class="logo">
          <div class="logo-icon-wrap">
            <span class="logo-terminal">>_</span>
          </div>
          <span class="logo-text">Rabotyaga<span class="highlight">CI</span></span>
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
        <div class="version-badge">v1.0.0-rc</div>
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
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-muted);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
}

.logo-icon-wrap {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  background: #161b22;
  border: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-terminal {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-blue);
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.01em;
}

.highlight {
  color: var(--accent-blue);
  font-weight: 700;
}

.sidebar-nav {
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 6px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s ease;
}

.nav-item:hover {
  color: var(--text-primary);
  background: rgba(177, 186, 196, 0.12);
  text-decoration: none;
}

.nav-item.active {
  color: var(--text-primary);
  background: #161b22;
  border: 1px solid var(--border-color);
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border-muted);
}

.version-badge {
  font-size: 11px;
  color: var(--text-muted);
  font-family: var(--font-mono);
}

.main-content {
  flex: 1;
  margin-left: 240px;
  min-height: 100vh;
  background: var(--bg-main);
}

.content-wrapper {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 32px;
}
</style>
