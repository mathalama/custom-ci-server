<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <Icon name="gear" :size="20" />
          <span class="logo-text">RabotyagaCI</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="active">
          <Icon name="dashboard" :size="18" />
          <span>Dashboard</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="active">
          <Icon name="folder" :size="18" />
          <span>Проекты</span>
        </router-link>
        <router-link to="/runners" class="nav-item" active-class="active">
          <Icon name="package" :size="18" />
          <span>Раннеры</span>
        </router-link>
        <router-link to="/settings" class="nav-item" active-class="active">
          <Icon name="gear" :size="18" />
          <span>Настройки</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="version">v0.1.0</div>
      </div>
    </aside>

    <!-- Main content -->
    <main class="main-content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import Icon from './Icon.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 220px;
  background: var(--surface-elevated);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid var(--border);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.15s;
  font-size: 14px;
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent-bg);
  color: var(--text-primary);
  font-weight: 600;
}

.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid var(--border);
}

.version {
  font-size: 12px;
  color: var(--text-muted);
}

.main-content {
  flex: 1;
  margin-left: 220px;
  padding: 32px;
  background: var(--surface);
  min-height: 100vh;
}
</style>
