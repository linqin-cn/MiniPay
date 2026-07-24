<template>
  <div id="app">
    <nav class="navbar" v-if="$route.name !== 'Login'">
      <div class="navbar-brand">
        <h1>MiniPay</h1>
      </div>
      <div class="navbar-nav">
        <router-link to="/products" :class="{ active: ['ProductList', 'Products', 'ProductDetail'].includes($route.name) }">商品</router-link>
        <router-link to="/cart" :class="{ active: $route.name === 'Cart' }">购物车</router-link>
        <router-link to="/orders" :class="{ active: ['OrderList', 'OrderDetail'].includes($route.name) }">订单</router-link>
        <router-link to="/addresses" :class="{ active: $route.name === 'Addresses' }">地址</router-link>
        <router-link to="/merchant" :class="{ active: String($route.name).startsWith('Merchant') }">商家后台</router-link>
        <router-link to="/query" :class="{ active: $route.name === 'QueryResult' }">旧版查询</router-link>
        <a class="logout-btn" @click="logout">退出登录</a>
      </div>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const router = useRouter()

    function logout() {
      localStorage.removeItem('token')
      router.push('/login')
    }

    return { logout }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f5f5f5;
  color: #333;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.navbar-brand h1 {
  font-size: 1.5rem;
  font-weight: 600;
}

.navbar-nav {
  display: flex;
  gap: 2rem;
}

.navbar-nav a {
  color: white;
  text-decoration: none;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background 0.3s;
}

.navbar-nav a:hover,
.navbar-nav a.active {
  background: rgba(255,255,255,0.2);
}

.logout-btn {
  color: white;
  text-decoration: none;
  font-weight: 500;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background 0.3s;
  cursor: pointer;
}

.logout-btn:hover {
  background: rgba(255,255,255,0.2);
}

.main-content {
  padding: 2rem;
  max-width: 1180px;
  margin: 0 auto;
}

/* Order pages use global safeguards so cached scoped CSS cannot break layout. */
.orders-page, .detail-page {
  display: grid;
  gap: 18px;
}

.orders-page .page-head,
.orders-page .order-top,
.orders-page .order-bottom,
.orders-page .actions,
.orders-page .amount-box,
.detail-page .page-head,
.detail-page .panel-title,
.detail-page .summary-row,
.detail-page .summary-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.orders-page .eyebrow, .detail-page .eyebrow {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: .08em;
}

.orders-page h2, .detail-page h2 {
  margin: 0;
  font-size: 28px;
  color: #111827;
}

.orders-page .text-btn, .detail-page .text-btn {
  border: 0;
  background: transparent;
  color: #14532d;
  font-weight: 800;
  cursor: pointer;
}

.orders-page .tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.orders-page .tabs button {
  height: 34px;
  padding: 0 14px;
  border: 1px solid #d7dde8;
  background: #fff;
  border-radius: 999px;
  cursor: pointer;
}

.orders-page .tabs button.active {
  background: #14532d;
  border-color: #14532d;
  color: #fff;
}

.orders-page .order-list {
  display: grid;
  gap: 12px;
}

.orders-page .order-card,
.orders-page .state-panel,
.detail-page .panel,
.detail-page .summary-panel,
.detail-page .state-panel {
  background: #fff;
  border: 1px solid #e3e8ef;
  border-radius: 8px;
}

.orders-page .order-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  overflow: hidden;
}

.orders-page .order-top strong {
  display: block;
  color: #111827;
  overflow-wrap: anywhere;
}

.orders-page .order-top span {
  color: #64748b;
  font-size: 13px;
}

.orders-page .status, .detail-page .status {
  padding: 4px 9px;
  border-radius: 999px;
  background: #eef2f7;
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
}

.orders-page .status.PAID, .orders-page .status.SUCCESS, .orders-page .status.COMPLETED,
.detail-page .status.PAID, .detail-page .status.SUCCESS, .detail-page .status.COMPLETED {
  background: #dcfce7;
  color: #166534;
}

.orders-page .status.CREATED, .orders-page .status.PENDING,
.detail-page .status.CREATED, .detail-page .status.PENDING {
  background: #fff4cc;
  color: #8a5a00;
}

.orders-page .item-strip {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 6px;
  color: #475569;
  overflow: hidden;
}

.orders-page .item-strip img {
  width: 48px !important;
  height: 48px !important;
  max-width: 48px !important;
  max-height: 48px !important;
  object-fit: cover;
  border-radius: 6px;
  display: block;
}

.orders-page .amount-box {
  justify-content: flex-start;
  color: #64748b;
}

.orders-page .amount-box strong {
  color: #b42318;
  font-size: 24px;
}

.orders-page .actions button, .orders-page .state-panel button {
  height: 36px;
  border: 0;
  border-radius: 6px;
  padding: 0 14px;
  font-weight: 800;
  cursor: pointer;
}

.orders-page .actions .primary, .orders-page .state-panel button {
  background: #14532d;
  color: #fff;
}

.orders-page .actions .secondary {
  background: #e7f2eb;
  color: #14532d;
}

@media (max-width: 720px) {
  .orders-page .order-bottom, .orders-page .order-top {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
