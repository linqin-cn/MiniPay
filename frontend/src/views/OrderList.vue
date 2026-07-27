<template>
  <section class="orders-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">orders</p>
        <h2>我的订单</h2>
      </div>
      <button class="text-btn" @click="loadOrders">刷新</button>
    </header>

    <div class="tabs">
      <button v-for="item in statusTabs" :key="item.value" :class="{ active: activeStatus === item.value }" @click="activeStatus = item.value">
        {{ item.label }}
      </button>
    </div>

    <div v-if="loading" class="state-panel">正在加载订单...</div>
    <div v-else-if="filteredOrders.length === 0" class="state-panel">
      暂无订单
      <button @click="router.push('/products')">去选商品</button>
    </div>

    <div v-else class="order-list">
      <article v-for="order in filteredOrders" :key="order.orderNo || order.orderId" class="order-card">
        <div class="order-top">
          <div>
            <strong>{{ order.orderNo || order.orderId }}</strong>
            <span>{{ formatTime(order.createdAt) }}</span>
          </div>
          <span :class="['status', normalizeStatus(order.status)]">{{ statusText(order.status) }}</span>
        </div>

        <div v-if="normalizeItems(order.items).length" class="item-strip">
          <img v-for="item in normalizeItems(order.items)" :key="item.key" :src="item.image" :alt="item.title" />
          <span>{{ normalizeItems(order.items)[0].title }} 等 {{ normalizeItems(order.items).length }} 件商品</span>
        </div>
        <div v-else class="item-strip empty">旧版订单：{{ order.description || '暂无商品明细' }}</div>

        <div class="order-bottom">
          <div class="amount-box">
            <span>实付</span>
            <strong>¥{{ money(order.payAmount || order.amount || 0) }}</strong>
          </div>
          <div class="actions">
            <button class="secondary" @click="router.push(`/orders/${order.orderNo || order.orderId}`)">查看详情</button>
            <button v-if="canPay(order)" class="primary" @click="router.push(`/pay/${order.orderNo || order.orderId}`)">去支付</button>
            <button v-if="canReceive(order)" class="primary" @click="receive(order)">确认收货</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, receiveOrder } from '@/api'
import { money } from '@/data/demoCatalog'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const activeStatus = ref('ALL')

const statusTabs = [
  { label: '全部', value: 'ALL' },
  { label: '待支付', value: 'CREATED' },
  { label: '已支付', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' }
]

const filteredOrders = computed(() => {
  if (activeStatus.value === 'ALL') return orders.value
  return orders.value.filter(order => normalizeStatus(order.status) === activeStatus.value)
})

function normalizeStatus(status) {
  if (status === 'PENDING') return 'CREATED'
  if (status === 'SUCCESS') return 'PAID'
  if (status === 'FAILED') return 'CLOSED'
  return status
}

function statusText(status) {
  const map = {
    CREATED: '待支付',
    PAYING: '支付中',
    PAID: '已支付',
    SHIPPED: '已发货',
    RECEIVED: '已收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    CLOSED: '已关闭'
  }
  const normalized = normalizeStatus(status)
  return map[normalized] || normalized || '-'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function loadLocalOrders() {
  const userId = currentUserId()
  return Object.keys(localStorage)
    .filter(key => key.startsWith(`order:${userId}:`) || isLegacyOrderKey(key))
    .map(key => JSON.parse(localStorage.getItem(key)))
    .filter(order => !order.userId || String(order.userId) === String(userId))
    .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
}

function normalizeRemoteOrders(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

function currentUserId() {
  return localStorage.getItem('userId') || 'anonymous'
}

function isLegacyOrderKey(key) {
  return key.startsWith('order:') && key.split(':').length === 2
}

function orderStorageKey(orderNo) {
  return `order:${currentUserId()}:${orderNo}`
}

function normalizeItems(items) {
  if (!Array.isArray(items)) return []
  return items.map((item, index) => ({
    key: item.id || item.skuId || item.sku?.id || index,
    title: item.productTitle || item.product?.title || item.Title || '订单商品',
    image: item.productImage || item.product?.mainImage || item.image || '',
    quantity: item.quantity || 1
  }))
}

function mergeOrders(remoteOrders, localOrders) {
  const map = new Map()
  ;[...localOrders, ...remoteOrders].forEach(order => {
    const key = order.orderNo || order.orderId
    if (key) map.set(key, { ...(map.get(key) || {}), ...order })
  })
  return Array.from(map.values()).sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
}

async function loadOrders() {
  loading.value = true
  const localOrders = loadLocalOrders()
  try {
    const res = await getOrderList()
    orders.value = mergeOrders(normalizeRemoteOrders(res.data?.data), localOrders)
  } catch (error) {
    orders.value = localOrders
  } finally {
    loading.value = false
  }
}

function canPay(order) {
  return normalizeStatus(order.status) === 'CREATED'
}

function canReceive(order) {
  return order.status === 'SHIPPED'
}

async function receive(order) {
  const orderNo = order.orderNo || order.orderId
  try {
    await receiveOrder(orderNo)
  } catch (error) {
    // local fallback
  }
  order.status = 'COMPLETED'
  localStorage.setItem(orderStorageKey(orderNo), JSON.stringify({ ...order, userId: currentUserId() }))
  await loadOrders()
}

onMounted(loadOrders)
</script>

<style scoped>
.orders-page { display: grid; gap: 16px; }
.page-head, .order-top, .order-bottom, .actions, .amount-box { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2 { margin: 0; font-size: 28px; color: #111827; }
.text-btn { border: 0; background: transparent; color: #14532d; font-weight: 800; cursor: pointer; }
.tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.tabs button { height: 34px; padding: 0 14px; border: 1px solid #d7dde8; background: #fff; border-radius: 999px; cursor: pointer; }
.tabs button.active { background: #14532d; border-color: #14532d; color: #fff; }
.order-list { display: grid; gap: 12px; }
.order-card, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; }
.order-card { display: grid; gap: 14px; padding: 16px; }
.order-top strong { display: block; color: #111827; overflow-wrap: anywhere; }
.order-top span { color: #64748b; font-size: 13px; }
.status { padding: 4px 9px; border-radius: 999px; background: #eef2f7; color: #475569; font-size: 13px; white-space: nowrap; }
.status.PAID, .status.COMPLETED { background: #dcfce7; color: #166534; }
.status.CREATED, .status.PAYING { background: #fff4cc; color: #8a5a00; }
.status.CLOSED, .status.CANCELLED { background: #fee2e2; color: #991b1b; }
.item-strip { display: flex; align-items: center; gap: 10px; padding: 12px; background: #f8fafc; border-radius: 6px; color: #475569; }
.item-strip img { width: 48px; height: 48px; object-fit: cover; border-radius: 6px; }
.item-strip img[src=""] { background: #eef2f7; }
.item-strip.empty { color: #64748b; }
.amount-box { justify-content: flex-start; color: #64748b; }
.amount-box strong { color: #b42318; font-size: 24px; }
.actions button, .state-panel button { height: 36px; border: 0; border-radius: 6px; padding: 0 14px; font-weight: 800; cursor: pointer; }
.actions .primary, .state-panel button { background: #14532d; color: #fff; }
.actions .secondary { background: #e7f2eb; color: #14532d; }
.state-panel { display: grid; gap: 12px; padding: 28px; color: #64748b; justify-items: start; }
@media (max-width: 720px) { .order-bottom, .order-top { align-items: flex-start; flex-direction: column; } }
</style>
