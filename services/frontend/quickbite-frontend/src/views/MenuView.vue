<template>
  <section>
    <h1>Menu</h1>
    <p class="muted">Phase 13 will list menu items for <code>{{ id }}</code> via <code>GET /api/restaurants/{id}/menu-items</code>.</p>
  </section>
</template>

<script>
export default {
  name: 'MenuView',
  props: { id: { type: String, required: true } }
};
</script>
