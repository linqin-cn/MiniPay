import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/login.vue'
import ProductList from '@/views/ProductList.vue'
import ProductDetail from '@/views/ProductDetail.vue'
import Cart from '@/views/Cart.vue'
import Checkout from '@/views/Checkout.vue'
import PayOrder from '@/views/PayOrder.vue'
import PayResult from '@/views/PayResult.vue'
import OrderList from '@/views/OrderList.vue'
import OrderDetail from '@/views/OrderDetail.vue'
import Addresses from '@/views/Addresses.vue'
import MerchantDashboard from '@/views/merchant/MerchantDashboard.vue'
import MerchantProducts from '@/views/merchant/MerchantProducts.vue'
import MerchantOrders from '@/views/merchant/MerchantOrders.vue'
import CreateOrder from '@/views/CreateOrder.vue'
import QueryResult from '@/views/QueryResult.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/', name: 'ProductList', component: ProductList },
  { path: '/products', name: 'Products', component: ProductList },
  { path: '/products/:id', name: 'ProductDetail', component: ProductDetail },
  { path: '/cart', name: 'Cart', component: Cart },
  { path: '/checkout', name: 'Checkout', component: Checkout },
  { path: '/pay/result', name: 'PayResult', component: PayResult },
  { path: '/pay/:orderNo', name: 'PayOrder', component: PayOrder },
  { path: '/orders', name: 'OrderList', component: OrderList },
  { path: '/orders/:orderNo', name: 'OrderDetail', component: OrderDetail },
  { path: '/addresses', name: 'Addresses', component: Addresses },
  { path: '/merchant', name: 'MerchantDashboard', component: MerchantDashboard },
  { path: '/merchant/products', name: 'MerchantProducts', component: MerchantProducts },
  { path: '/merchant/orders', name: 'MerchantOrders', component: MerchantOrders },
  { path: '/create', name: 'CreateOrder', component: CreateOrder },
  { path: '/query', name: 'QueryResult', component: QueryResult }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'Login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
