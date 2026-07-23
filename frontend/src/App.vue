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
</style>
