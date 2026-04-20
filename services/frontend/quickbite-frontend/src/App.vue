<template>
  <div id="app">
    <AppNav />
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script>
import AppNav from './components/AppNav.vue';

export default {
  name: 'App',
  components: { AppNav }
};
</script>

<style>
:root {
  --qb-bg: #fafafa;
  --qb-fg: #1f2933;
  --qb-accent: #d2691e;
  --qb-border: #e1e4e8;
  --qb-muted: #6b7280;
}

* { box-sizing: border-box; }

body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
  background: var(--qb-bg);
  color: var(--qb-fg);
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  max-width: 960px;
  width: 100%;
  margin: 0 auto;
  padding: 1.5rem 1rem;
}

button,
.btn {
  background: var(--qb-accent);
  color: #fff;
  border: 0;
  border-radius: 4px;
  padding: 0.5rem 0.9rem;
  font-size: 0.95rem;
  cursor: pointer;
}

button:disabled { opacity: 0.6; cursor: not-allowed; }

input, select, textarea {
  font: inherit;
  padding: 0.45rem 0.6rem;
  border: 1px solid var(--qb-border);
  border-radius: 4px;
  width: 100%;
}

label { display: block; font-weight: 600; margin: 0.75rem 0 0.25rem; }

.error-banner {
  background: #fde8e8;
  color: #9b1c1c;
  border: 1px solid #f8b4b4;
  padding: 0.6rem 0.8rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.muted { color: var(--qb-muted); }
</style>
