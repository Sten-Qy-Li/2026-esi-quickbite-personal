<template>
  <section class="not-found">
    <h1>404</h1>
    <p class="muted">No route matches <code>{{ $route.fullPath }}</code>.</p>
    <router-link to="/" class="btn">Back to home</router-link>
  </section>
</template>

<script>
export default { name: 'NotFoundView' };
</script>

<style scoped>
.not-found { text-align: center; padding-top: 3rem; }
.not-found h1 { font-size: 4rem; margin-bottom: 0.5rem; }
</style>
