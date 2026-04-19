<template>
  <section class="menu-view">
    <header class="list-header">
      <div>
        <h1>Menu</h1>
        <p class="muted">
          <span v-if="restaurant">{{ restaurant.name }}</span>
          <span v-else>Restaurant <code>{{ id }}</code></span>
        </p>
      </div>
      <router-link
        v-if="canManage"
        :to="{ name: 'menu-item-new', query: { restaurantId: id } }"
        class="btn"
      >
        Add menu item
      </router-link>
    </header>

    <form class="filters" @submit.prevent="load">
      <div class="field">
        <label for="filter-category">Category</label>
        <input id="filter-category" v-model.trim="category" placeholder="e.g. Main" />
      </div>
      <div class="field">
        <label for="filter-availability">Availability</label>
        <select id="filter-availability" v-model="availabilityFilter">
          <option value="">Any</option>
          <option value="available">Available</option>
          <option value="unavailable">Unavailable</option>
        </select>
      </div>
      <div class="field actions">
        <button type="submit" :disabled="loading">Apply filters</button>
        <button type="button" class="btn-link" :disabled="loading" @click="reset">Reset</button>
      </div>
    </form>

    <div v-if="error" class="error-banner">{{ error }}</div>

    <div v-if="loading" class="muted" role="status">Loading menu…</div>

    <div v-else-if="items.length === 0" class="empty">
      No menu items yet for this restaurant.
    </div>

    <ul v-else class="cards">
      <li v-for="item in items" :key="item.menuItemId" class="card">
        <div class="card-head">
          <h3>{{ item.name }}</h3>
          <span :class="['badge', item.isAvailable ? 'available' : 'unavailable']">
            {{ item.isAvailable ? 'Available' : 'Unavailable' }}
          </span>
        </div>
        <p class="price">{{ formatPrice(item) }}</p>
        <p class="muted category">{{ item.category }}</p>
        <p v-if="item.description" class="description">{{ item.description }}</p>
        <div class="card-actions">
          <router-link :to="{ name: 'menu-item-detail', params: { id: item.menuItemId } }" class="btn-link">
            {{ canManage ? 'Edit' : 'Details' }}
          </router-link>
        </div>
      </li>
    </ul>

    <p class="muted back">
      <router-link :to="{ name: 'restaurant-detail', params: { id } }">← Back to restaurant</router-link>
    </p>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { canManageRestaurants } from '../auth/token.js';

export default {
  name: 'MenuView',
  props: { id: { type: String, required: true } },
  data() {
    return {
      restaurant: null,
      items: [],
      category: '',
      availabilityFilter: '',
      loading: false,
      error: ''
    };
  },
  computed: {
    canManage() {
      return canManageRestaurants();
    }
  },
  watch: {
    id: {
      immediate: true,
      handler() {
        this.loadRestaurant();
        this.load();
      }
    }
  },
  methods: {
    async loadRestaurant() {
      try {
        this.restaurant = await api.get(`/api/restaurants/${this.id}`);
      } catch (_err) {
        this.restaurant = null;
      }
    },
    async load() {
      this.loading = true;
      this.error = '';
      try {
        const params = new URLSearchParams();
        if (this.category) params.set('category', this.category);
        if (this.availabilityFilter === 'available') params.set('available', 'true');
        if (this.availabilityFilter === 'unavailable') params.set('available', 'false');
        const query = params.toString();
        const base = `/api/restaurants/${this.id}/menu-items`;
        const data = await api.get(query ? `${base}?${query}` : base);
        this.items = Array.isArray(data) ? data : [];
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not load menu items.';
        this.items = [];
      } finally {
        this.loading = false;
      }
    },
    reset() {
      this.category = '';
      this.availabilityFilter = '';
      this.load();
    },
    formatPrice(item) {
      if (item.priceAmount === null || item.priceAmount === undefined) return '—';
      const amount = Number(item.priceAmount);
      return `${amount.toFixed(2)} ${item.priceCurrency || ''}`.trim();
    }
  }
};
</script>

<style scoped>
.list-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
}

.list-header h1 { margin: 0 0 0.25rem; }

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
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}

.card {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.card-head h3 { margin: 0; color: var(--qb-accent); }

.badge {
  font-size: 0.75rem;
  padding: 0.15rem 0.5rem;
  border-radius: 999px;
  font-weight: 600;
}

.badge.available { background: #dcfce7; color: #166534; }
.badge.unavailable { background: #fee2e2; color: #991b1b; }

.price { font-weight: 600; margin: 0.2rem 0; }

.category {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.description { margin: 0.3rem 0; font-size: 0.9rem; }

.card-actions { margin-top: auto; }

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0;
  border: 0;
  cursor: pointer;
  text-decoration: none;
  font-size: 0.9rem;
}

.back { margin-top: 1rem; }
.back a { color: var(--qb-muted); text-decoration: none; }
</style>
