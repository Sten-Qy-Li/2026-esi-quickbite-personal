<template>
  <section class="home">
    <h1>QuickBite</h1>
    <p class="muted">Order food from local restaurants. Browse restaurants, view menus, and (for owners) manage them.</p>

    <div class="cards">
      <router-link to="/restaurants" class="card">
        <h3>Browse restaurants</h3>
        <p>See open spots near you and explore menus.</p>
      </router-link>
      <router-link v-if="canManage" to="/restaurants/new" class="card">
        <h3>Add a restaurant</h3>
        <p>Create a new restaurant profile.</p>
      </router-link>
      <router-link v-if="authed" to="/orders" class="card">
        <h3>Track your orders</h3>
        <p>Live status from kitchen to courier.</p>
      </router-link>
      <router-link v-else to="/login" class="card">
        <h3>Sign in</h3>
        <p>Log in to place orders and track delivery.</p>
      </router-link>
    </div>

    <p class="muted base-url">API base: <code>{{ apiBase }}</code></p>
  </section>
</template>

<script>
import { isAuthenticated, canManageRestaurants } from '../auth/token.js';
import { baseUrl } from '../api/client.js';

export default {
  name: 'HomeView',
  computed: {
    authed() { return isAuthenticated(); },
    canManage() { return canManageRestaurants(); },
    apiBase() { return baseUrl; }
  }
};
</script>

<style scoped>
.home h1 { margin-top: 0; }

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 1rem;
  margin: 1.5rem 0;
}

.card {
  display: block;
  padding: 1rem;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  background: #fff;
  color: inherit;
  text-decoration: none;
}

.card h3 { margin: 0 0 0.4rem; color: var(--qb-accent); }

.base-url { font-size: 0.85rem; }
</style>
