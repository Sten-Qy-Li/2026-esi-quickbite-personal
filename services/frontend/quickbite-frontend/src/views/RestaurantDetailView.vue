<template>
  <section>
    <h1>Restaurant detail</h1>
    <p class="muted">Phase 13 will show details for <code>{{ id }}</code>.</p>
    <router-link :to="`/restaurants/${id}/menu`" class="btn">View menu</router-link>
  </section>
</template>

<script>
export default {
  name: 'RestaurantDetailView',
  props: { id: { type: String, required: true } }
};
</script>
