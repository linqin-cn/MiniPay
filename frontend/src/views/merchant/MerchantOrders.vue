<template>
  <section class="merchant-orders">
    <header class="page-head">
      <div>
        <p class="eyebrow">orders</p>
        <h2>商家订单管理</h2>
      </div>
      <button class="text-btn" @click="router.push('/merchant')"><span class="back-icon" aria-hidden="true">←</span>返回概览</button>
    </header>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab.value" :class="{ active: currentTab === tab.value }" @click="currentTab = tab.value">{{ tab.label }}</button>
    </div>

    <div v-if="loading" class="state-panel">正在加载订单...</div>
    <div v-else-if="filteredOrders.length === 0" class="state-panel">当前没有订单</div>

    <main v-else class="order-list">
      <article v-for="order in filteredOrders" :key="order.orderNo || order.orderId" class="order-card">
        <div class="order-top">
          <div>
            <strong>{{ order.orderNo || order.orderId }}</strong>
            <span>{{ formatTime(order.createdAt) }}</span>
          </div>
          <em :class="['status', normalizeOrderStatus(order.status)]">{{ statusText(order.status) }}</em>
        </div>

        <div class="item-list">
          <div v-for="item in normalizeItems(order)" :key="item.id || item.skuId || item.sku?.id" class="item-row">
            <img :src="item.productImage || item.product?.mainImage" :alt="item.productTitle || item.product?.title" />
            <div>
              <strong>{{ item.productTitle || item.product?.title || '订单商品' }}</strong>
              <span>{{ item.skuName || item.sku?.skuName }} x {{ item.quantity || 1 }}</span>
            </div>
          </div>
        </div>

        <div class="order-bottom">
          <div class="amount">实收 <strong>¥{{ money(order.payAmount || order.amount) }}</strong></div>
          <div class="actions">
            <button v-if="normalizeOrderStatus(order.status) === 'PAID'" class="primary" @click="ship(order)">发货</button>
            <button @click="router.push(`/orders/${order.orderNo || order.orderId}`)">查看详情</button>
          </div>
        </div>
      </article>
    </main>

  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { notification } from 'ant-design-vue'
import { getMerchantOrders, merchantShipOrder } from '@/api'
import { money } from '@/data/demoCatalog'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const currentTab = ref('ALL')
const tabs = [
  { label: '全部', value: 'ALL' },
  { label: '待发货', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已完成', value: 'COMPLETED' }
]

const filteredOrders = computed(() => {
  if (currentTab.value === 'ALL') return orders.value
  return orders.value.filter(order => normalizeOrderStatus(order.status) === currentTab.value)
})

function normalizeOrders(payload) {
  const data = payload?.data ?? payload
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.data)) return data.data
  if (Array.isArray(data?.data?.data)) return data.data.data
  return []
}

function normalizeItems(order) {
  if (Array.isArray(order.items) && order.items.length) return order.items
  return [{ productTitle: order.description || '订单商品', quantity: 1, productImage: '' }]
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

function formatTime(value) {
  if (!value) return '暂无时间'
  return String(value).replace('T', ' ').slice(0, 19)
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await getMerchantOrders()
    orders.value = normalizeOrders(res.data)
  } catch (error) {
    orders.value = []
    notification.error({ description: '订单服务暂不可用' })
  } finally {
    loading.value = false
  }
}

async function ship(order) {
  const orderNo = order.orderNo || order.orderId
  try {
    await merchantShipOrder(orderNo)
    order.status = 'SHIPPED'
    notification.success({ description: '订单已发货' })
  } catch (error) {
    notification.error({ description: error.response?.data?.message || '发货失败，请稍后重试' })
  }
}

onMounted(loadOrders)
</script>

<style scoped>
.merchant-orders { display: grid; gap: 20px; }
.page-head, .order-top, .order-bottom, .actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2 { margin: 0; font-size: 28px; line-height: 1.2; color: #111827; }
button { font-family: inherit; }
.text-btn { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; border: 0; border-radius: 6px; background: #e7f2eb; color: #14532d; padding: 0 12px; font-weight: 900; cursor: pointer; }
.back-icon { font-size: 18px; line-height: 1; transform: translateY(-1px); }
.tabs { display: flex; gap: 8px; flex-wrap: wrap; padding: 4px; background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; width: fit-content; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.tabs button { min-height: 34px; border: 0; border-radius: 6px; background: transparent; color: #475569; padding: 0 14px; cursor: pointer; font-weight: 900; }
.tabs button.active { background: #14532d; color: #fff; }
.order-list { display: grid; gap: 12px; }
.order-card, .state-panel { background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.order-card { display: grid; gap: 14px; padding: 18px; overflow: hidden; }
.order-top strong { display: block; color: #111827; overflow-wrap: anywhere; line-height: 1.35; }
.order-top span { color: #64748b; font-size: 13px; line-height: 1.5; }
.status { font-style: normal; color: #475569; background: #eef2f7; padding: 4px 9px; border-radius: 999px; white-space: nowrap; font-size: 12px; font-weight: 900; }
.status.PAID { color: #166534; background: #dcfce7; }
.status.SHIPPED { color: #075985; background: #e0f2fe; }
.item-list { display: grid; gap: 8px; }
.item-row { display: grid; grid-template-columns: 54px minmax(0, 1fr); gap: 10px; align-items: center; padding: 10px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 6px; }
.item-row img { width: 54px; height: 54px; object-fit: cover; border-radius: 8px; background: #eef2f7; }
.item-row div { display: grid; gap: 4px; min-width: 0; }
.item-row strong { color: #111827; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-row span { color: #64748b; line-height: 1.45; }
.amount { color: #64748b; }
.amount strong { color: #b42318; font-size: 22px; white-space: nowrap; }
.actions button { min-height: 36px; border: 0; border-radius: 6px; padding: 0 14px; background: #e7f2eb; color: #14532d; font-weight: 900; cursor: pointer; }
.actions .primary { background: #14532d; color: #fff; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 720px) { .page-head, .order-top, .order-bottom { align-items: flex-start; flex-direction: column; } .tabs { width: 100%; } .actions { flex-wrap: wrap; justify-content: flex-start; } }
</style>
