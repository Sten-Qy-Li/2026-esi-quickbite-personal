<template>
  <section class="add-restaurant">
    <h1>Add a restaurant</h1>
    <p class="muted">
      Posts to <code>POST /api/restaurants</code>. Requires the
      <code>RestaurantOwner</code> or <code>Admin</code> role on your token.
    </p>

    <form @submit.prevent="onSubmit" novalidate>
      <label for="name">Name</label>
      <input id="name" v-model.trim="form.name" maxlength="255" required />
      <p v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</p>

      <label for="address">Address</label>
      <input id="address" v-model.trim="form.address" maxlength="255" />

      <label for="city">City</label>
      <input id="city" v-model.trim="form.city" maxlength="120" required />
      <p v-if="fieldErrors.city" class="field-error">{{ fieldErrors.city }}</p>

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
        placeholder="HH:MM-HH:MM (e.g. 09:00-22:00)"
        pattern="^\d{2}:\d{2}-\d{2}:\d{2}$"
      />
      <p v-if="fieldErrors.operatingHours" class="field-error">{{ fieldErrors.operatingHours }}</p>

      <div v-if="error" class="error-banner">{{ error }}</div>

      <div class="actions">
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Creating…' : 'Create restaurant' }}
        </button>
        <router-link to="/restaurants" class="muted">Cancel</router-link>
      </div>
    </form>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';

const HOURS_PATTERN = /^\d{2}:\d{2}-\d{2}:\d{2}$/;

export default {
  name: 'AddRestaurantView',
  data() {
    return {
      form: {
        name: '',
        address: '',
        city: '',
        latitude: null,
        longitude: null,
        operatingHours: ''
      },
      submitting: false,
      error: '',
      fieldErrors: {}
    };
  },
  methods: {
    validate() {
      const errors = {};
      if (!this.form.name) errors.name = 'Name is required.';
      if (!this.form.city) errors.city = 'City is required.';
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
    async onSubmit() {
      this.error = '';
      if (!this.validate()) return;
      this.submitting = true;
      try {
        const payload = {
          name: this.form.name,
          address: this.form.address || null,
          city: this.form.city || null,
          latitude: Number(this.form.latitude),
          longitude: Number(this.form.longitude),
          operatingHours: this.form.operatingHours || null
        };
        const created = await api.post('/api/restaurants', payload);
        this.$router.push({ name: 'restaurant-detail', params: { id: created.restaurantId } });
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Could not create restaurant.';
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.add-restaurant { max-width: 560px; }

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

.actions {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-top: 1.25rem;
}

.actions a { color: var(--qb-muted); }
</style>
