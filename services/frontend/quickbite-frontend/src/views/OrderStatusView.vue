<template>
  <section>
    <h1>Orders</h1>
    <p v-if="id" class="muted">Order status for <code>{{ id }}</code> will live here.</p>
    <p v-else class="muted">Your order history will appear here once Order Service ships.</p>
  </section>
</template>

<script>
export default {
  name: 'OrderStatusView',
  props: { id: { type: String, default: '' } }
};
</script>
