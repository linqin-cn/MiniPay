<template>
  <section class="merchant-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">merchant console</p>
        <h2>商家后台</h2>
      </div>
      <div class="head-actions">
        <button @click="router.push('/merchant/products')">管理商品</button>
        <button class="primary" @click="router.push('/merchant/orders')">处理订单</button>
      </div>
    </header>

    <div class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-title">
          <h3>待处理订单</h3>
          <button @click="router.push('/merchant/orders')">查看全部</button>
        </div>
        <div v-if="pendingOrders.length === 0" class="state-line">暂无待发货订单</div>
        <article v-for="order in pendingOrders" :key="order.orderNo || order.orderId" class="order-row">
          <div>
            <strong>{{ order.orderNo || order.orderId }}</strong>
            <span>{{ statusText(order.status) }}</span>
          </div>
          <em>¥{{ money(order.payAmount || order.amount) }}</em>
        </article>
      </section>

      <section class="panel">
        <div class="panel-title">
          <h3>商品状态</h3>
          <button @click="router.push('/merchant/products')">维护商品</button>
        </div>
        <div v-if="products.length === 0" class="state-line">暂无商品</div>
        <article v-for="product in products.slice(0, 5)" :key="product.id" class="product-row">
          <img :src="product.mainImage" :alt="product.title" />
          <div>
            <strong>{{ product.title }}</strong>
            <span>{{ product.status || 'ON_SALE' }}</span>
          </div>
        </article>
      </section>
    </div>

    <p v-if="notice" class="notice">{{ notice }}</p>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentMerchant, getMerchantOrders, getProducts } from '@/api'
import { money } from '@/data/demoCatalog'

const router = useRouter()
const products = ref([])
const orders = ref([])
const notice = ref('')
const currentMerchantId = ref(null)

const pendingOrders = computed(() => orders.value.filter(order => normalizeOrderStatus(order.status) === 'PAID').slice(0, 4))
const metrics = computed(() => [
  { label: '在售商品', value: products.value.filter(item => (item.status || 'ON_SALE') === 'ON_SALE').length, hint: '可被用户购买' },
  { label: '待发货', value: pendingOrders.value.length, hint: '支付完成待处理' },
  { label: '订单总数', value: orders.value.length, hint: '当前可见订单' },
  { label: '成交金额', value: `¥${money(orders.value.reduce((sum, order) => sum + Number(order.payAmount || order.amount || 0), 0))}`, hint: '按已加载订单统计' }
])

function normalizeProducts(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function normalizeOrders(payload) {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.data?.data)) return data.data.data
  return []
}

function normalizeOrderStatus(status) {
  if (status === 'PENDING') return 'CREATED'
  if (status === 'SUCCESS') return 'PAID'
  return status
}

function statusText(status) {
  const map = { CREATED: '待支付', PAYING: '支付中', PAID: '待发货', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', CLOSED: '已关闭' }
  return map[normalizeOrderStatus(status)] || status || '未知'
}

async function ensureCurrentMerchant() {
  if (currentMerchantId.value) return currentMerchantId.value
  const res = await getCurrentMerchant()
  currentMerchantId.value = res.data?.data?.id || null
  if (!currentMerchantId.value) throw new Error('当前商家信息不存在')
  return currentMerchantId.value
}

async function loadData() {
  notice.value = ''
  try {
    await ensureCurrentMerchant()
    const productRes = await getProducts({ merchantId: currentMerchantId.value })
    products.value = normalizeProducts(productRes.data?.data)
  } catch (error) {
    products.value = []
    notice.value = '商品服务暂不可用或当前商家身份获取失败'
  }

  try {
    const orderRes = await getMerchantOrders()
    orders.value = normalizeOrders(orderRes.data)
  } catch (error) {
    orders.value = []
  }
}

onMounted(loadData)
</script>

<style scoped>
.merchant-page { display: grid; gap: 20px; }
.page-head, .panel-title, .head-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2, h3 { margin: 0; }
h2 { font-size: 28px; line-height: 1.2; color: #111827; }
button { min-height: 36px; border: 0; border-radius: 6px; padding: 0 14px; background: #e7f2eb; color: #14532d; font-weight: 900; cursor: pointer; font-family: inherit; }
button.primary { background: #14532d; color: #fff; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.metric-card, .panel { background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.metric-card { display: grid; gap: 6px; min-height: 118px; padding: 18px; border-top: 3px solid #14532d; }
.metric-card span, .metric-card small { color: #64748b; line-height: 1.45; }
.metric-card strong { font-size: 28px; line-height: 1.1; color: #111827; overflow-wrap: anywhere; }
.dashboard-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 18px; }
.panel { display: grid; gap: 12px; padding: 18px; align-content: start; }
.panel-title h3 { color: #111827; font-size: 18px; }
.order-row, .product-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 6px; }
.order-row div, .product-row div { display: grid; gap: 4px; min-width: 0; }
.order-row strong, .product-row strong { color: #111827; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.order-row span, .product-row span, .state-line { color: #64748b; line-height: 1.5; }
.order-row em { font-style: normal; color: #b42318; font-weight: 900; white-space: nowrap; }
.product-row { justify-content: flex-start; }
.product-row img { width: 54px; height: 54px; border-radius: 8px; object-fit: cover; background: #eef2f7; flex: 0 0 auto; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px 12px; border-radius: 6px; line-height: 1.5; }
@media (max-width: 980px) { .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .dashboard-grid { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .metric-grid { grid-template-columns: 1fr; } .page-head { align-items: stretch; flex-direction: column; } .head-actions { justify-content: flex-start; flex-wrap: wrap; } }
</style>
