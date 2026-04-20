<template>
  <section class="login">
    <h1>Sign in</h1>
    <p class="muted">Authenticates against <code>POST /api/auth/login</code> on the gateway.</p>

    <form @submit.prevent="onSubmit">
      <label for="email">Email</label>
      <input id="email" v-model="email" type="email" autocomplete="username" required />

      <label for="password">Password</label>
      <input id="password" v-model="password" type="password" autocomplete="current-password" required />

      <div v-if="error" class="error-banner">{{ error }}</div>

      <div class="actions">
        <button type="submit" :disabled="submitting">
          {{ submitting ? 'Signing in…' : 'Sign in' }}
        </button>
        <router-link to="/signup" class="muted">No account? Sign up</router-link>
      </div>
    </form>
  </section>
</template>

<script>
import { api, ApiError } from '../api/client.js';
import { setToken } from '../auth/token.js';

export default {
  name: 'LoginView',
  data() {
    return {
      email: '',
      password: '',
      submitting: false,
      error: ''
    };
  },
  methods: {
    async onSubmit() {
      this.error = '';
      this.submitting = true;
      try {
        const result = await api.post('/api/auth/login', {
          email: this.email,
          password: this.password
        });
        const token = result && (result.token || result.accessToken || result.jwt);
        if (!token) throw new ApiError('Login response did not include a token.');
        setToken(token);
        const next = typeof this.$route.query.next === 'string' ? this.$route.query.next : '/';
        this.$router.push(next);
      } catch (err) {
        this.error = err instanceof ApiError ? err.message : 'Sign-in failed.';
      } finally {
        this.submitting = false;
      }
    }
  }
};
</script>

<style scoped>
.login { max-width: 420px; }
.actions { display: flex; align-items: center; gap: 1rem; margin-top: 1rem; }
.actions a { color: var(--qb-muted); }
</style>
