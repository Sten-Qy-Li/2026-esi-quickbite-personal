<template>
  <section class="add-menu-item">
    <h1>Add a menu item</h1>
    <p class="muted">
      Posts to <code>POST /api/restaurants/{{ form.restaurantId || '{id}' }}/menu-items</code>.
    </p>

    <form @submit.prevent="onSubmit" novalidate>
      <label for="restaurant">Restaurant</label>
      <select id="restaurant" v-model="form.restaurantId" :disabled="restaurantsLoading" required>
        <option value="" disabled>{{ restaurantsLoading ? 'Loading…' : 'Choose a restaurant' }}</option>
        <option v-for="r in restaurants" :key="r.restaurantId" :value="r.restaurantId">
          {{ r.name }}<span v-if="r.city"> — {{ r.city }}</span>
        </option>
      </select>
      <p v-if="fieldErrors.restaurantId" class="field-error">{{ fieldErrors.restaurantId }}</p>

      <label for="name">Name</label>
      <input id="name" v-model.trim="form.name" maxlength="255" required />
      <p v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</p>

      <label for="description">Description</label>
      <textarea id="description" v-model.trim="form.description" maxlength="2000" rows="3" />

      <div class="row">
        <div>
          <label for="priceAmount">Price</label>
          <input
            id="priceAmount"
            v-model.number="form.priceAmount"
            type="number"
            step="0.01"
            min="0"
            required
          />
          <p v-if="fieldErrors.priceAmount" class="field-error">{{ fieldErrors.priceAmount }}</p>
        </div>
        <div>
          <label for="priceCurrency">Currency</label>
          <input
            id="priceCurrency"
            v-model="form.priceCurrency"
            maxlength="3"
            pattern="^[A-Z]{3}$"
            required
            @input="form.priceCurrency = form.priceCurrency.toUpperCase()"
          />
          <p v-if="fieldErrors.priceCurrency" class="field-error">{{ fieldErrors.priceCurrency }}</p>
        </div>
      </div>

      <label for="category">Category</label>
      <input id="category" v-model.trim="form.category" maxlength="100" required />
      <p v-if="fieldErrors.category" class="field-error">{{ fieldErrors.category }}</p>

      <label class="inline">
        <input type="checkbox" v-model="form.isAvailable" />
        Available for ordering
      </label>

      <div v-if="error" class="error-banner">{{ error }}</div>

      <div class="actions">
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Creating…' : 'Create menu item' }}
        </button>
        <router-link :to="cancelRoute" class="muted">Cancel</router-link>
      </div>
    </form>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';

export default {
  name: 'AddMenuItemView',
  data() {
    return {
      restaurants: [],
      restaurantsLoading: false,
      form: {
        restaurantId: '',
        name: '',
        description: '',
        priceAmount: null,
        priceCurrency: 'EUR',
        category: '',
        isAvailable: true
      },
      submitting: false,
      error: '',
      fieldErrors: {}
    };
  },
  computed: {
    cancelRoute() {
      if (this.form.restaurantId) {
        return { name: 'restaurant-menu', params: { id: this.form.restaurantId } };
      }
      return { name: 'restaurants' };
    }
  },
  mounted() {
    const preset = this.$route.query.restaurantId;
    if (typeof preset === 'string') this.form.restaurantId = preset;
    this.loadRestaurants();
  },
  methods: {
    async loadRestaurants() {
      this.restaurantsLoading = true;
      try {
        const data = await api.get('/api/restaurants');
        if (data && Array.isArray(data.content)) {
          this.restaurants = data.content;
        } else if (Array.isArray(data)) {
          this.restaurants = data;
        } else {
          this.restaurants = [];
        }
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not load restaurants.';
      } finally {
        this.restaurantsLoading = false;
      }
    },
    validate() {
      const errors = {};
      if (!this.form.restaurantId) errors.restaurantId = 'Pick a restaurant.';
      if (!this.form.name) errors.name = 'Name is required.';
      if (this.form.priceAmount === null || this.form.priceAmount === '') {
        errors.priceAmount = 'Price is required.';
      } else if (Number(this.form.priceAmount) < 0) {
        errors.priceAmount = 'Price must be zero or more.';
      }
      if (!/^[A-Z]{3}$/.test(this.form.priceCurrency || '')) {
        errors.priceCurrency = 'Use a 3-letter ISO code (e.g. EUR).';
      }
      if (!this.form.category) errors.category = 'Category is required.';
      this.fieldErrors = errors;
      return Object.keys(errors).length === 0;
    },
    async onSubmit() {
      this.error = '';
      if (!this.validate()) return;
      this.submitting = true;
      try {
        const payload = {
          name: this.form.name,
          description: this.form.description || null,
          priceAmount: Number(this.form.priceAmount),
          priceCurrency: this.form.priceCurrency,
          category: this.form.category,
          isAvailable: this.form.isAvailable
        };
        const created = await api.post(
          `/api/restaurants/${this.form.restaurantId}/menu-items`,
          payload
        );
        this.$router.push({ name: 'menu-item-detail', params: { id: created.menuItemId } });
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not create menu item.';
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.add-menu-item { max-width: 560px; }

.row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 0.75rem;
}

.inline {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.75rem 0;
  font-weight: 600;
}

.inline input { width: auto; }

.field-error {
  color: #9b1c1c;
  font-size: 0.85rem;
  margin: 0.2rem 0 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.25rem;
}

.actions a { color: var(--qb-muted); }

textarea { resize: vertical; }
</style>
