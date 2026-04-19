<template>
  <section class="menu-item-detail">
    <div v-if="loading" class="muted" role="status">Loading menu item…</div>

    <div v-else-if="loadError" class="error-banner">
      {{ loadError }}
      <router-link to="/restaurants" class="btn-link">Back</router-link>
    </div>

    <template v-else-if="item">
      <header class="detail-header">
        <div>
          <h1>{{ item.name }}</h1>
          <p class="muted">{{ item.category }} — {{ formatPrice() }}</p>
          <p class="muted">
            Restaurant:
            <router-link :to="{ name: 'restaurant-menu', params: { id: item.restaurantId } }">
              back to menu
            </router-link>
          </p>
        </div>
        <span :class="['badge', item.isAvailable ? 'available' : 'unavailable']">
          {{ item.isAvailable ? 'Available' : 'Unavailable' }}
        </span>
      </header>

      <div v-if="canManage" class="nav-actions">
        <button type="button" class="btn-secondary" :disabled="toggling" @click="toggleAvailability">
          {{ toggling
            ? 'Updating…'
            : item.isAvailable ? 'Mark as unavailable' : 'Mark as available' }}
        </button>
      </div>

      <section v-if="canManage" class="edit-panel">
        <h2>Edit item</h2>
        <p class="muted">Sends <code>PUT /api/menu-items/{{ item.menuItemId }}</code>.</p>

        <form @submit.prevent="onSave" novalidate>
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

          <div v-if="saveError" class="error-banner">{{ saveError }}</div>
          <div v-if="savedAt" class="success-banner">Saved.</div>

          <div class="actions">
            <button type="submit" :disabled="saving">
              {{ saving ? 'Saving…' : 'Save changes' }}
            </button>
            <button type="button" class="btn-link" :disabled="saving" @click="reset">
              Discard
            </button>
          </div>
        </form>
      </section>

      <section v-else class="info-panel">
        <p v-if="item.description">{{ item.description }}</p>
        <p v-else class="muted">No description provided.</p>
      </section>
    </template>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { canManageRestaurants } from '../auth/token.js';

export default {
  name: 'MenuItemDetailView',
  props: { id: { type: String, required: true } },
  data() {
    return {
      item: null,
      form: {
        name: '',
        description: '',
        priceAmount: null,
        priceCurrency: 'EUR',
        category: '',
        isAvailable: true
      },
      loading: false,
      loadError: '',
      saving: false,
      saveError: '',
      savedAt: 0,
      toggling: false,
      fieldErrors: {}
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
      handler() { this.load(); }
    }
  },
  methods: {
    async load() {
      this.loading = true;
      this.loadError = '';
      try {
        const data = await api.get(`/api/menu-items/${this.id}`);
        this.item = data;
        this.reset();
      } catch (err) {
        this.loadError = err instanceof ApiError ? err.message : 'Could not load menu item.';
        this.item = null;
      } finally {
        this.loading = false;
      }
    },
    reset() {
      if (!this.item) return;
      this.form = {
        name: this.item.name || '',
        description: this.item.description || '',
        priceAmount: this.item.priceAmount != null ? Number(this.item.priceAmount) : null,
        priceCurrency: this.item.priceCurrency || 'EUR',
        category: this.item.category || '',
        isAvailable: this.item.isAvailable
      };
      this.fieldErrors = {};
      this.saveError = '';
      this.savedAt = 0;
    },
    validate() {
      const errors = {};
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
    async onSave() {
      this.saveError = '';
      this.savedAt = 0;
      if (!this.validate()) return;
      this.saving = true;
      try {
        const payload = {
          name: this.form.name,
          description: this.form.description || null,
          priceAmount: Number(this.form.priceAmount),
          priceCurrency: this.form.priceCurrency,
          category: this.form.category,
          isAvailable: this.form.isAvailable
        };
        const updated = await api.put(`/api/menu-items/${this.id}`, payload);
        this.item = updated;
        this.savedAt = Date.now();
      } catch (err) {
        this.saveError = err instanceof ApiError ? err.message : 'Could not save changes.';
      } finally {
        this.saving = false;
      }
    },
    async toggleAvailability() {
      if (!this.item) return;
      this.toggling = true;
      this.saveError = '';
      try {
        const payload = {
          name: this.item.name,
          description: this.item.description,
          priceAmount: Number(this.item.priceAmount),
          priceCurrency: this.item.priceCurrency,
          category: this.item.category,
          isAvailable: !this.item.isAvailable
        };
        const updated = await api.put(`/api/menu-items/${this.id}`, payload);
        this.item = updated;
        this.reset();
      } catch (err) {
        this.saveError = err instanceof ApiError ? err.message : 'Could not update availability.';
      } finally {
        this.toggling = false;
      }
    },
    formatPrice() {
      if (!this.item) return '';
      if (this.item.priceAmount === null || this.item.priceAmount === undefined) return '—';
      const amount = Number(this.item.priceAmount);
      return `${amount.toFixed(2)} ${this.item.priceCurrency || ''}`.trim();
    }
  }
};
</script>

<style scoped>
.menu-item-detail { display: flex; flex-direction: column; gap: 1.25rem; }

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
}

.detail-header h1 { margin: 0 0 0.25rem; }

.badge {
  font-size: 0.8rem;
  padding: 0.25rem 0.6rem;
  border-radius: 999px;
  font-weight: 600;
}

.badge.available { background: #dcfce7; color: #166534; }
.badge.unavailable { background: #fee2e2; color: #991b1b; }

.nav-actions {
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.btn-secondary {
  background: transparent;
  color: var(--qb-accent);
  border: 1px solid var(--qb-accent);
}

.edit-panel, .info-panel {
  background: #fff;
  border: 1px solid var(--qb-border);
  border-radius: 6px;
  padding: 1rem 1.25rem;
}

.edit-panel h2 { margin-top: 0; }

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

.success-banner {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
  padding: 0.5rem 0.8rem;
  border-radius: 4px;
  margin: 0.75rem 0 0;
}

.actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-link {
  background: transparent;
  color: var(--qb-accent);
  padding: 0.25rem 0.5rem;
  border: 0;
  cursor: pointer;
}

textarea { resize: vertical; }
</style>
