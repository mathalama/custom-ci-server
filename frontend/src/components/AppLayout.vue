<template>
  <div class="app-layout">
    <!-- Sidebar -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo">
          <span class="logo-icon">⚙️</span>
          <span class="logo-text">RabotyagaCI</span>
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link to="/" class="nav-item" active-class="active">
          <span class="nav-icon">📊</span>
          <span>Dashboard</span>
        </router-link>
        <router-link to="/projects" class="nav-item" active-class="active">
          <span class="nav-icon">📁</span>
          <span>Проекты</span>
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

<style scoped>
.app-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
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
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--accent), var(--accent-secondary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.2s;
  font-size: 14px;
}

.nav-item:hover {
  background: var(--surface-hover);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent-bg);
  color: var(--accent);
  font-weight: 600;
}

.nav-icon {
  font-size: 18px;
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
  margin-left: 240px;
  padding: 32px;
  background: var(--surface);
  min-height: 100vh;
}
</style>
