<template>
  <section class="signup">
    <h1>Create your account</h1>
    <p class="muted">Posts to <code>POST /api/users</code> then sends you to the sign-in page.</p>

    <form @submit.prevent="onSubmit">
      <label for="email">Email</label>
      <input id="email" v-model="email" type="email" autocomplete="username" required />

      <label for="password">Password</label>
      <input id="password" v-model="password" type="password" autocomplete="new-password" required minlength="8" />

      <label for="role">Role</label>
      <select id="role" v-model="role">
        <option value="Customer">Customer</option>
        <option value="RestaurantOwner">Restaurant owner</option>
        <option value="Driver">Driver</option>
      </select>

      <div v-if="error" class="error-banner">{{ error }}</div>

      <div class="actions">
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Creating…' : 'Create account' }}
        </button>
        <router-link to="/login" class="muted">Already registered? Sign in</router-link>
      </div>
    </form>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';

export default {
  name: 'SignupView',
  data() {
    return {
      email: '',
      password: '',
      role: 'Customer',
      submitting: false,
      error: ''
    };
  },
  methods: {
    async onSubmit() {
      this.error = '';
      this.submitting = true;
      try {
        await api.post('/api/users', {
          email: this.email,
          password: this.password,
          role: this.role
        });
        this.$router.push({ name: 'login', query: { registered: '1' } });
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Sign-up failed.';
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.signup { max-width: 420px; }
.actions { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }
.actions a { color: var(--qb-muted); }
</style>
