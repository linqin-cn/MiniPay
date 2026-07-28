<template>
  <section class="detail-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">order detail</p>
        <h2>订单详情</h2>
      </div>
      <button class="text-btn" @click="router.push(backPath)"><span class="back-icon" aria-hidden="true">←</span>返回订单列表</button>
    </header>

    <div v-if="loading" class="state-panel">正在加载订单...</div>
    <div v-else-if="!order" class="state-panel">订单不存在</div>

    <div v-else class="detail-layout">
      <main class="main-panel">
        <section class="panel">
          <div class="panel-title">
            <h3>订单状态</h3>
            <span :class="['status', normalizeOrderStatus(order.status)]">{{ statusText(order.status) }}</span>
          </div>
          <div class="info-grid">
            <span>订单号</span><strong>{{ order.orderNo || order.orderId }}</strong>
            <span>创建时间</span><strong>{{ formatTime(order.createdAt) }}</strong>
            <span>备注</span><strong>{{ order.remark || order.description || '-' }}</strong>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title"><h3>商品明细</h3></div>
          <div v-if="detailItems.length" class="order-items">
            <article v-for="item in detailItems" :key="item.key" class="order-item">
              <img :src="item.image" :alt="item.title" />
              <div>
                <h4>{{ item.title }}</h4>
                <p>{{ item.skuName }}</p>
              </div>
              <div class="item-price">
                <strong>¥{{ money(item.price) }}</strong>
                <span>x {{ item.quantity }}</span>
              </div>
            </article>
          </div>
          <div v-else class="empty-line">旧版订单暂无商品明细</div>
        </section>

        <section class="panel">
          <div class="panel-title"><h3>收货信息</h3></div>
          <div class="address-box">
            <strong>{{ order.address?.receiverName || order.receiverName || '-' }}</strong>
            <span>{{ order.address?.receiverPhone || order.receiverPhone || '-' }}</span>
            <p>{{ order.address?.fullAddress || order.receiverAddress || '-' }}</p>
          </div>
        </section>
      </main>

      <aside class="summary-panel">
        <h3>金额与支付</h3>
        <div class="summary-row"><span>商品金额</span><strong>¥{{ money(order.totalAmount || order.amount || 0) }}</strong></div>
        <div class="summary-row"><span>优惠金额</span><strong>-¥{{ money(order.discountAmount || 0) }}</strong></div>
        <div class="summary-row"><span>运费</span><strong>¥{{ money(order.freightAmount || 0) }}</strong></div>
        <div class="summary-total"><span>实付</span><strong>¥{{ money(order.payAmount || order.amount || 0) }}</strong></div>
        <div v-if="payment" class="payment-box">
          <span>支付状态：{{ payment.status || '-' }}</span>
          <span>支付单号：{{ payment.paymentNo || payment.paymentId || '-' }}</span>
        </div>
        <button v-if="canPay" class="primary" @click="router.push(`/pay/${order.orderNo || order.orderId}`)">去支付</button>
        <button v-if="canCancel" class="danger-btn" :disabled="canceling" @click="handleCancelOrder">
          {{ canceling ? '取消中...' : '取消支付' }}
        </button>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { cancelOrder, getLogisticsByOrderNo, getPaymentOrderByOrderNo, getTradeOrder } from '@/api'
import { money } from '@/data/demoCatalog'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const payment = ref(null)
const logistics = ref(null)
const loading = ref(false)
const canceling = ref(false)
const notice = ref('')
const isMerchant = localStorage.getItem('userRole') === 'MERCHANT'
const backPath = isMerchant ? '/merchant/orders' : '/orders'

const canPay = computed(() => !isMerchant && normalizeOrderStatus(order.value?.status) === 'CREATED')
const canCancel = computed(() => !isMerchant && ['CREATED', 'PAYING'].includes(normalizeOrderStatus(order.value?.status)))
const detailItems = computed(() => normalizeItems(order.value?.items || []))

function normalizeOrderStatus(status) {
  if (status === 'PENDING') return 'CREATED'
  if (status === 'SUCCESS') return 'PAID'
  if (status === 'FAILED') return 'CLOSED'
  return status
}

function statusText(status) {
  const map = { CREATED: '待支付', PAYING: '支付中', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', CLOSED: '已关闭' }
  const normalized = normalizeOrderStatus(status)
  return map[normalized] || normalized || '-'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function normalizeItems(items) {
  if (!Array.isArray(items)) return []
  return items.map((item, index) => ({
    key: item.id || item.skuId || item.sku?.id || index,
    title: item.productTitle || item.product?.title || item.Title || '订单商品',
    skuName: item.skuName || item.sku?.skuName || '默认规格',
    image: item.productImage || item.product?.mainImage || item.image || '',
    price: item.unitPrice || item.sku?.price || item.price || 0,
    quantity: item.quantity || 1
  }))
}

function currentUserId() {
  return localStorage.getItem('userId') || 'anonymous'
}

function orderStorageKey(orderNo) {
  return `order:${currentUserId()}:${orderNo}`
}

async function loadDetail() {
  loading.value = true
  const orderNo = route.params.orderNo
  const localOrder = localStorage.getItem(orderStorageKey(orderNo)) || localStorage.getItem(`order:${orderNo}`)
  const localPayment = localStorage.getItem(`payment:${currentUserId()}:${orderNo}`) || localStorage.getItem(`payment:${orderNo}`)
  if (localOrder) order.value = JSON.parse(localOrder)
  if (localPayment) payment.value = JSON.parse(localPayment)

  try {
    const res = await getTradeOrder(orderNo)
    if (res.data?.data) order.value = { ...order.value, ...res.data.data }
  } catch (error) {
    // local fallback
  }

  try {
    const res = await getPaymentOrderByOrderNo(orderNo)
    if (res.data?.data) payment.value = { ...payment.value, ...res.data.data }
  } catch (error) {
    // local fallback
  }

  try {
    const res = await getLogisticsByOrderNo(orderNo)
    if (res.data?.data) logistics.value = res.data.data
  } catch (error) {
    // optional
  }

  loading.value = false
}

async function handleCancelOrder() {
  if (!order.value || !canCancel.value) return
  const orderNo = order.value.orderNo || order.value.orderId || route.params.orderNo
  if (!orderNo) {
    notice.value = '订单号为空，无法取消订单'
    return
  }
  canceling.value = true
  notice.value = ''
  try {
    const res = await cancelOrder(orderNo)
    order.value = { ...order.value, ...(res.data?.data || {}), status: 'CANCELLED' }
    localStorage.setItem(orderStorageKey(orderNo), JSON.stringify({ ...order.value, userId: currentUserId() }))
    notice.value = '订单已取消'
  } catch (error) {
    notice.value = error.response?.data?.message || '取消订单失败，请稍后重试'
  } finally {
    canceling.value = false
  }
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { display: grid; gap: 18px; }
.page-head, .panel-title, .summary-row, .summary-total { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2, h3, h4, p { margin: 0; }
h2 { font-size: 28px; color: #111827; }
.text-btn { display: inline-flex; align-items: center; gap: 6px; border: 0; background: transparent; color: #14532d; font-weight: 800; cursor: pointer; }
.back-icon { font-size: 18px; line-height: 1; transform: translateY(-1px); }
.detail-layout { display: grid; grid-template-columns: 1fr 340px; gap: 18px; align-items: start; }
.main-panel { display: grid; gap: 14px; }
.panel, .summary-panel, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; }
.panel, .summary-panel { padding: 18px; display: grid; gap: 14px; }
.status { padding: 4px 9px; border-radius: 999px; background: #eef2f7; color: #475569; font-size: 13px; }
.status.PAID, .status.COMPLETED { background: #dcfce7; color: #166534; }
.status.CREATED, .status.PAYING { background: #fff4cc; color: #8a5a00; }
.status.CLOSED, .status.CANCELLED { background: #fee2e2; color: #991b1b; }
.info-grid { display: grid; grid-template-columns: 86px 1fr; gap: 12px; color: #64748b; }
.info-grid strong { color: #111827; overflow-wrap: anywhere; }
.order-items { display: grid; gap: 12px; }
.order-item { display: grid; grid-template-columns: 74px 1fr auto; gap: 12px; align-items: center; }
.order-item img { width: 74px; height: 74px; object-fit: cover; border-radius: 6px; }
.order-item img[src=""] { background: #eef2f7; }
.order-item p, .item-price span, .empty-line { color: #64748b; font-size: 13px; }
.item-price { display: grid; gap: 4px; text-align: right; }
.item-price strong { color: #b42318; }
.address-box, .payment-box { display: grid; gap: 6px; padding: 14px; background: #f8fafc; border-radius: 6px; color: #475569; }
.summary-panel { position: sticky; top: 16px; }
.summary-total { border-top: 1px solid #e3e8ef; padding-top: 14px; }
.summary-total strong { font-size: 28px; color: #b42318; }
.primary { height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 800; cursor: pointer; }
.danger-btn { height: 44px; border: 0; border-radius: 6px; background: #fff1f0; color: #b42318; font-weight: 800; cursor: pointer; }
.danger-btn:disabled { opacity: .65; cursor: not-allowed; }
.notice { padding: 10px 12px; border-radius: 6px; background: #fff4cc; color: #8a5a00; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } .summary-panel { position: static; } }
</style>
