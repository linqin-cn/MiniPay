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
  { path: '/', name: 'ProductList', component: ProductList, meta: { roles: ['BUYER'] } },
  { path: '/products', name: 'Products', component: ProductList, meta: { roles: ['BUYER'] } },
  { path: '/products/:id', name: 'ProductDetail', component: ProductDetail, meta: { roles: ['BUYER'] } },
  { path: '/cart', name: 'Cart', component: Cart, meta: { roles: ['BUYER'] } },
  { path: '/checkout', name: 'Checkout', component: Checkout, meta: { roles: ['BUYER'] } },
  { path: '/pay/result', name: 'PayResult', component: PayResult },
  { path: '/pay/:orderNo', name: 'PayOrder', component: PayOrder, meta: { roles: ['BUYER'] } },
  { path: '/orders', name: 'OrderList', component: OrderList, meta: { roles: ['BUYER'] } },
  { path: '/orders/:orderNo', name: 'OrderDetail', component: OrderDetail, meta: { roles: ['BUYER', 'MERCHANT'] } },
  { path: '/addresses', name: 'Addresses', component: Addresses, meta: { roles: ['BUYER'] } },
  { path: '/merchant', name: 'MerchantDashboard', component: MerchantDashboard, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/products', name: 'MerchantProducts', component: MerchantProducts, meta: { roles: ['MERCHANT'] } },
  { path: '/merchant/orders', name: 'MerchantOrders', component: MerchantOrders, meta: { roles: ['MERCHANT'] } },
  { path: '/create', name: 'CreateOrder', component: CreateOrder, meta: { roles: ['BUYER'] } },
  { path: '/query', name: 'QueryResult', component: QueryResult, meta: { roles: ['BUYER', 'MERCHANT'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('userRole') || 'BUYER'
  if (to.path === '/' && role === 'MERCHANT') {
    next('/merchant')
    return
  }
  if (to.name !== 'Login' && !token) {
    next('/login')
  } else if (to.meta?.roles && !to.meta.roles.includes(role)) {
    next(role === 'MERCHANT' ? '/merchant' : '/products')
  } else {
    next()
  }
})

export default router
