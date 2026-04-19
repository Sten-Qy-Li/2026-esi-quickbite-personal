<template>
  <section class="restaurant-detail">
    <div v-if="loading" class="muted" role="status">Loading restaurant…</div>

    <div v-else-if="loadError" class="error-banner">
      {{ loadError }}
      <router-link to="/restaurants" class="btn-link">Back to list</router-link>
    </div>

    <template v-else-if="restaurant">
      <header class="detail-header">
        <div>
          <h1>{{ restaurant.name }}</h1>
          <p class="muted">
            {{ formatAddress() || 'No address on file' }}
          </p>
        </div>
        <span :class="['badge', restaurant.isOpen ? 'open' : 'closed']">
          {{ restaurant.isOpen ? 'Open' : 'Closed' }}
        </span>
      </header>

      <div class="nav-actions">
        <router-link :to="`/restaurants/${restaurant.restaurantId}/menu`" class="btn">
          View menu
        </router-link>
        <button
          v-if="canManage"
          type="button"
          class="btn-secondary"
          :disabled="toggling"
          @click="toggleStatus"
        >
          {{ toggling ? 'Updating…' : restaurant.isOpen ? 'Mark as closed' : 'Mark as open' }}
        </button>
      </div>

      <section v-if="canManage" class="edit-panel">
        <h2>Edit details</h2>
        <p class="muted">
          Sends <code>PUT /api/restaurants/{{ restaurant.restaurantId }}</code>.
        </p>

        <form @submit.prevent="onSave" novalidate>
          <label for="name">Name</label>
          <input id="name" v-model.trim="form.name" maxlength="255" required />
          <p v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</p>

          <label for="address">Address</label>
          <input id="address" v-model.trim="form.address" maxlength="255" />

          <label for="city">City</label>
          <input id="city" v-model.trim="form.city" maxlength="120" />

          <div class="row">
            <div>
              <label for="latitude">Latitude</label>
              <input
                id="latitude"
                v-model.number="form.latitude"
                type="number"
                step="0.000001"
                min="-90"
                max="90"
                required
              />
              <p v-if="fieldErrors.latitude" class="field-error">{{ fieldErrors.latitude }}</p>
            </div>
            <div>
              <label for="longitude">Longitude</label>
              <input
                id="longitude"
                v-model.number="form.longitude"
                type="number"
                step="0.000001"
                min="-180"
                max="180"
                required
              />
              <p v-if="fieldErrors.longitude" class="field-error">{{ fieldErrors.longitude }}</p>
            </div>
          </div>

          <label for="operatingHours">Operating hours</label>
          <input
            id="operatingHours"
            v-model.trim="form.operatingHours"
            placeholder="HH:MM-HH:MM"
            pattern="^\d{2}:\d{2}-\d{2}:\d{2}$"
          />
          <p v-if="fieldErrors.operatingHours" class="field-error">{{ fieldErrors.operatingHours }}</p>

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
        <dl>
          <dt>Hours</dt>
          <dd>{{ restaurant.operatingHours || '—' }}</dd>
          <dt>Coordinates</dt>
          <dd>{{ restaurant.latitude }}, {{ restaurant.longitude }}</dd>
        </dl>
      </section>
    </template>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { canManageRestaurants } from '../auth/token.js';

const HOURS_PATTERN = /^\d{2}:\d{2}-\d{2}:\d{2}$/;

export default {
  name: 'RestaurantDetailView',
  props: { id: { type: String, required: true } },
  data() {
    return {
      restaurant: null,
      form: { name: '', address: '', city: '', latitude: null, longitude: null, operatingHours: '' },
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
        const data = await api.get(`/api/restaurants/${this.id}`);
        this.restaurant = data;
        this.reset();
      } catch (err) {
        this.loadError = err instanceof ApiError ? err.message : 'Could not load restaurant.';
        this.restaurant = null;
      } finally {
        this.loading = false;
      }
    },
    reset() {
      if (!this.restaurant) return;
      this.form = {
        name: this.restaurant.name || '',
        address: this.restaurant.address || '',
        city: this.restaurant.city || '',
        latitude: this.restaurant.latitude ?? null,
        longitude: this.restaurant.longitude ?? null,
        operatingHours: this.restaurant.operatingHours || ''
      };
      this.fieldErrors = {};
      this.saveError = '';
      this.savedAt = 0;
    },
    validate() {
      const errors = {};
      if (!this.form.name) errors.name = 'Name is required.';
      if (this.form.latitude === null || this.form.latitude === '') {
        errors.latitude = 'Latitude is required.';
      } else if (this.form.latitude < -90 || this.form.latitude > 90) {
        errors.latitude = 'Latitude must be between -90 and 90.';
      }
      if (this.form.longitude === null || this.form.longitude === '') {
        errors.longitude = 'Longitude is required.';
      } else if (this.form.longitude < -180 || this.form.longitude > 180) {
        errors.longitude = 'Longitude must be between -180 and 180.';
      }
      if (this.form.operatingHours && !HOURS_PATTERN.test(this.form.operatingHours)) {
        errors.operatingHours = 'Use the HH:MM-HH:MM format.';
      }
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
          address: this.form.address || null,
          city: this.form.city || null,
          latitude: Number(this.form.latitude),
          longitude: Number(this.form.longitude),
          operatingHours: this.form.operatingHours || null
        };
        const updated = await api.put(`/api/restaurants/${this.id}`, payload);
        this.restaurant = updated;
        this.savedAt = Date.now();
      } catch (err) {
        this.saveError = err instanceof ApiError ? err.message : 'Could not save changes.';
      } finally {
        this.saving = false;
      }
    },
    async toggleStatus() {
      this.toggling = true;
      this.saveError = '';
      try {
        const next = !this.restaurant.isOpen;
        const updated = await api.patch(`/api/restaurants/${this.id}/status`, { isOpen: next });
        this.restaurant = updated;
      } catch (err) {
        this.saveError = err instanceof ApiError ? err.message : 'Could not update status.';
      } finally {
        this.toggling = false;
      }
    },
    formatAddress() {
      if (!this.restaurant) return '';
      return [this.restaurant.address, this.restaurant.city].filter(Boolean).join(', ');
    }
  }
};
</script>

<style scoped>
.restaurant-detail { display: flex; flex-direction: column; gap: 1.25rem; }

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

.badge.open { background: #dcfce7; color: #166534; }
.badge.closed { background: #fee2e2; color: #991b1b; }

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
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}

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

dl { margin: 0; display: grid; grid-template-columns: 160px 1fr; gap: 0.4rem 1rem; }
dt { font-weight: 600; color: var(--qb-muted); }
dd { margin: 0; }
</style>
