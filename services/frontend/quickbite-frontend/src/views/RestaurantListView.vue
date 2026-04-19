<template>
  <section>
    <h1>Restaurants</h1>
    <p class="muted">Phase 13 wires this list to <code>GET /api/restaurants</code>.</p>
    <div class="placeholder">Restaurant list placeholder.</div>
  </section>
</template>

<script>
export default { name: 'RestaurantListView' };
</script>

<style scoped>
.placeholder {
  border: 1px dashed var(--qb-border);
  padding: 1.25rem;
  border-radius: 6px;
  background: #fff;
  color: var(--qb-muted);
}
</style>
