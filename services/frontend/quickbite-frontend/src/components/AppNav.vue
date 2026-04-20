<template>
  <nav class="app-nav">
    <div class="brand">
      <router-link to="/">QuickBite</router-link>
    </div>
    <ul class="links">
      <li><router-link to="/">Home</router-link></li>
      <li><router-link to="/restaurants">Restaurants</router-link></li>
      <li v-if="authed"><router-link to="/cart">Cart</router-link></li>
      <li v-if="authed"><router-link to="/orders">Orders</router-link></li>
    </ul>
    <div class="auth">
      <template v-if="authed">
        <span class="muted">Signed in as {{ displayName }}</span>
        <button class="btn-link" @click="onLogout">Logout</button>
      </template>
      <template v-else>
        <router-link to="/login">Login</router-link>
        <router-link to="/signup">Sign up</router-link>
      </template>
    </div>
  </nav>
</template>

<script>
import { isAuthenticated, clearToken, readClaims } from '../auth/token.js';

export default {
  name: 'AppNav',
  data() {
    return { authVersion: 0 };
  },
  computed: {
    authed() {
      // Touch authVersion so the computed re-evaluates after login/logout.
      // eslint-disable-next-line no-unused-expressions
      this.authVersion;
      return isAuthenticated();
    },
    displayName() {
      const claims = readClaims();
      if (!claims) return 'user';
      return claims.sub || claims.userId || 'user';
    }
  },
  watch: {
    $route() {
      this.authVersion += 1;
    }
  },
  mounted() {
    window.addEventListener('storage', this.onStorage);
  },
  beforeUnmount() {
    window.removeEventListener('storage', this.onStorage);
  },
  methods: {
    onStorage(event) {
      if (event.key === 'quickbite.jwt') this.authVersion += 1;
    },
    onLogout() {
      clearToken();
      this.authVersion += 1;
      this.$router.push({ name: 'login' });
    }
  }
};
</script>

<style scoped>
.app-nav {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1.25rem;
  background: #ffffff;
  border-bottom: 1px solid var(--qb-border);
}

.brand a {
  font-weight: 700;
  font-size: 1.15rem;
  color: var(--qb-accent);
  text-decoration: none;
}

.links {
  display: flex;
  gap: 1rem;
  list-style: none;
  margin: 0;
  padding: 0;
}

.links a, .auth a {
  color: var(--qb-fg);
  text-decoration: none;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.links a.router-link-active,
.auth a.router-link-active {
  background: rgba(210, 105, 30, 0.1);
  color: var(--qb-accent);
}

.auth {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0.5rem;
}
</style>
