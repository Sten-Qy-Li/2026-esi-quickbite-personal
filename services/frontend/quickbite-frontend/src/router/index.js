import { createRouter, createWebHistory } from 'vue-router';
import { isAuthenticated } from '../auth/token.js';

import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import SignupView from '../views/SignupView.vue';
import RestaurantListView from '../views/RestaurantListView.vue';
import RestaurantDetailView from '../views/RestaurantDetailView.vue';
import MenuView from '../views/MenuView.vue';
import CartView from '../views/CartView.vue';
import OrderStatusView from '../views/OrderStatusView.vue';
import NotFoundView from '../views/NotFoundView.vue';

/**
 * `meta.requiresAuth` flips on protected routes. The router-level
 * guard below redirects anonymous visitors to /login with a `next`
 * param so the post-login flow can return them.
 */
const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/login', name: 'login', component: LoginView, meta: { hideWhenAuthed: true } },
  { path: '/signup', name: 'signup', component: SignupView, meta: { hideWhenAuthed: true } },
  {
    path: '/restaurants',
    name: 'restaurants',
    component: RestaurantListView
  },
  {
    path: '/restaurants/:id',
    name: 'restaurant-detail',
    component: RestaurantDetailView,
    props: true
  },
  {
    path: '/restaurants/:id/menu',
    name: 'restaurant-menu',
    component: MenuView,
    props: true
  },
  {
    path: '/cart',
    name: 'cart',
    component: CartView,
    meta: { requiresAuth: true }
  },
  {
    path: '/orders/:id?',
    name: 'orders',
    component: OrderStatusView,
    props: true,
    meta: { requiresAuth: true }
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
];

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL || '/'),
  routes
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isAuthenticated()) {
    return {
      name: 'login',
      query: { next: to.fullPath }
    };
  }
  if (to.meta.hideWhenAuthed && isAuthenticated()) {
    return { name: 'home' };
  }
  return true;
});

export default router;
