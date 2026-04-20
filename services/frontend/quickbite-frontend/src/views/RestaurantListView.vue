<template>
  <section class="restaurant-list">
    <header class="list-header">
      <h1>Restaurants</h1>
      <router-link v-if="canManage" to="/restaurants/new" class="btn">
        Add restaurant
      </router-link>
    </header>

    <form class="filters" @submit.prevent="load">
      <div class="field">
        <label for="filter-city">City</label>
        <input id="filter-city" v-model.trim="city" placeholder="e.g. Tartu" />
      </div>
      <div class="field">
        <label for="filter-status">Status</label>
        <select id="filter-status" v-model="statusFilter">
          <option value="">Any</option>
          <option value="open">Open now</option>
          <option value="closed">Closed</option>
        </select>
      </div>
      <div class="field actions">
        <button type="submit" :disabled="loading">Apply filters</button>
        <button type="button" class="btn-link" @click="reset" :disabled="loading">Reset</button>
      </div>
    </form>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <div v-if="loading" class="muted" role="status">Loading restaurants…</div>

    <div v-else-if="restaurants.length === 0" class="empty">
      No restaurants match the current filters.
    </div>

    <ul v-else class="cards">
      <li v-for="r in restaurants" :key="r.restaurantId" class="card">
        <div class="card-head">
          <h3>{{ r.name }}</h3>
          <span :class="['badge', r.isOpen ? 'open' : 'closed']">
            {{ r.isOpen ? 'Open' : 'Closed' }}
          </span>
        </div>
        <p class="muted">
          {{ formatAddress(r) || 'No address on file' }}
        </p>
        <p v-if="r.operatingHours" class="muted hours">Hours: {{ r.operatingHours }}</p>
        <div class="card-actions">
          <router-link :to="`/restaurants/${r.restaurantId}`" class="btn-link">View details</router-link>
          <router-link :to="`/restaurants/${r.restaurantId}/menu`" class="btn-link">Menu</router-link>
        </div>
      </li>
    </ul>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { canManageRestaurants } from '../auth/token.js';

export default {
  name: 'RestaurantListView',
  data() {
    return {
      restaurants: [],
      loading: false,
      error: '',
      city: '',
      statusFilter: ''
    };
  },
  computed: {
    canManage() {
      return canManageRestaurants();
    }
  },
  mounted() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      this.error = '';
      try {
        const params = new URLSearchParams();
        if (this.city) params.set('city', this.city);
        if (this.statusFilter === 'open') params.set('isOpen', 'true');
        if (this.statusFilter === 'closed') params.set('isOpen', 'false');
        const query = params.toString();
        const path = query ? `/api/restaurants?${query}` : '/api/restaurants';
        const data = await api.get(path);
        if (data && Array.isArray(data.content)) {
          this.restaurants = data.content;
        } else if (Array.isArray(data)) {
          this.restaurants = data;
        } else {
          this.restaurants = [];
        }
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not load restaurants.';
        this.restaurants = [];
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.city = '';
      this.statusFilter = '';
      this.load();
    },
    formatAddress(r) {
      return [r.address, r.city].filter(Boolean).join(', ');
    }
  }
};
</script>

<style scoped>
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.list-header h1 { margin: 0; }

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 0.75rem;
  align-items: end;
  padding: 0.75rem;
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  margin-bottom: 1rem;
}

.filters .field { display: flex; flex-direction: column; }
.filters .actions { flex-direction: row; gap: 0.5rem; align-items: center; }

.empty {
  border: 1px dashed var(--qb-border);
  padding: 1.25rem;
  border-radius: 6px;
  background: #fff;
  color: var(--qb-muted);
  text-align: center;
}

.cards {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1rem;
}

.card {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 1rem;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.4rem;
}

.card-head h3 { margin: 0; color: var(--qb-accent); }

.badge {
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-weight: 600;
}

.badge.open { background: #dcfce7; color: #166534; }
.badge.closed { background: #fee2e2; color: #991b1b; }

.hours { font-size: 0.85rem; }

.card-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0;
  text-decoration: none;
  font-size: 0.9rem;
  border: 0;
  cursor: pointer;
}
</style>
