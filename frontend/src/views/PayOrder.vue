<template>
  <section class="pay-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">cashier</p>
        <h2>收银台</h2>
      </div>
      <button class="text-btn" @click="router.push('/orders')">查看订单</button>
    </header>

    <div v-if="loading" class="state-panel">正在加载订单...</div>
    <div v-else-if="!order" class="state-panel">订单不存在或已过期</div>

    <div v-else class="pay-layout">
      <main class="panel order-panel">
        <div class="panel-title">
          <h3>订单信息</h3>
          <span :class="['status', order.status]">{{ statusText }}</span>
        </div>
        <div class="info-grid">
          <span>订单号</span><strong>{{ order.orderNo || order.orderId }}</strong>
          <span>创建时间</span><strong>{{ formatTime(order.createdAt) }}</strong>
          <span>应付金额</span><strong class="amount">¥{{ money(order.payAmount || order.amount) }}</strong>
        </div>

        <div v-if="payItems.length" class="mini-items">
          <article v-for="item in payItems" :key="item.key">
            <img :src="item.image" :alt="item.title" />
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.skuName }} x {{ item.quantity }}</span>
            </div>
          </article>
        </div>
      </main>

      <aside class="panel cashier-panel">
        <h3>支付方式</h3>
        <div class="pay-methods">
          <button :class="{ active: payChannel === 'ALIPAY_MOCK' }" @click="payChannel = 'ALIPAY_MOCK'">支付宝模拟</button>
          <button :class="{ active: payChannel === 'WECHAT_MOCK' }" @click="payChannel = 'WECHAT_MOCK'">微信模拟</button>
          <button :class="{ active: payChannel === 'BALANCE' }" @click="payChannel = 'BALANCE'">余额支付</button>
        </div>

        <div class="pay-total">
          <span>需支付</span>
          <strong>¥{{ money(order.payAmount || order.amount) }}</strong>
        </div>

        <button class="pay-btn" :disabled="paying || paid" @click="handlePay">
          {{ paid ? '已支付' : paying ? '支付中...' : '确认支付' }}
        </button>
        <button class="secondary-btn" @click="router.push(`/orders/${order.orderNo || order.orderId}`)">查看订单详情</button>

        <div v-if="paymentResult" :class="['result-box', paymentResult.status]">
          <strong>{{ paymentResult.status === 'SUCCESS' ? '支付成功' : '支付处理中' }}</strong>
          <span v-if="paymentResult.paymentNo || paymentResult.paymentId">支付单号：{{ paymentResult.paymentNo || paymentResult.paymentId }}</span>
        </div>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPaymentOrder, getOrder, getTradeOrder, mockPaymentCallback } from '@/api'
import { money } from '@/data/demoCatalog'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const paying = ref(false)
const payChannel = ref('ALIPAY_MOCK')
const paymentResult = ref(null)
const notice = ref('')

const paid = computed(() => ['PAID', 'SUCCESS', 'COMPLETED'].includes(order.value?.status))
const payItems = computed(() => normalizeItems(order.value?.items || []))
const statusText = computed(() => {
  const map = { CREATED: '待支付', PENDING: '待支付', PAID: '已支付', SUCCESS: '已支付', FAILED: '支付失败' }
  return map[order.value?.status] || order.value?.status || '-'
})

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

function currentUserId() {
  return localStorage.getItem('userId') || 'anonymous'
}

function orderStorageKey(orderNo) {
  return `order:${currentUserId()}:${orderNo}`
}

function normalizeItems(items) {
  if (!Array.isArray(items)) return []
  return items.map((item, index) => ({
    key: item.id || item.skuId || item.sku?.id || index,
    title: item.productTitle || item.product?.title || item.Title || '订单商品',
    skuName: item.skuName || item.sku?.skuName || '默认规格',
    image: item.productImage || item.product?.mainImage || item.image || '',
    quantity: item.quantity || 1
  }))
}

async function loadOrder() {
  loading.value = true
  const orderNo = route.params.orderNo
  const localOrder = localStorage.getItem(orderStorageKey(orderNo)) || localStorage.getItem(`order:${orderNo}`)
  if (localOrder) order.value = JSON.parse(localOrder)
  try {
    const res = await getTradeOrder(orderNo)
    if (res.data?.data) order.value = { ...order.value, ...res.data.data }
  } catch (error) {
    try {
      const res = await getOrder(orderNo)
      if (res.data?.data) order.value = { ...order.value, ...res.data.data, orderNo }
    } catch (legacyError) {
      // local fallback only
    }
  } finally {
    loading.value = false
  }
}

function buildPaymentReq() {
  if (!order.value) return null
  return {
    orderNo: order.value.orderNo || order.value.orderId,
    payAmount: Number(order.value.payAmount || order.value.amount || 0),
    payChannel: payChannel.value
  }
}

async function handlePay() {
  if (!order.value) return
  const paymentReq = buildPaymentReq()
  if (!paymentReq || !paymentReq.orderNo || paymentReq.payAmount <= 0) {
    notice.value = '订单信息不完整，无法发起支付'
    return
  }
  paying.value = true
  notice.value = ''
  try {
    const res = await createPaymentOrder(paymentReq)
    paymentResult.value = res.data?.data || null
    const callbackRes = await mockPaymentCallback(paymentReq)
    finishLocalPayment(callbackRes.data?.data || paymentResult.value)
  } catch (error) {
    finishLocalPayment({ status: 'SUCCESS', paymentNo: `PAY${Date.now()}` })
    notice.value = '后端支付接口暂未实现，已完成本地模拟支付'
  } finally {
    paying.value = false
  }
}

function finishLocalPayment(result) {
  const orderNo = order.value.orderNo || order.value.orderId
  paymentResult.value = { status: 'SUCCESS', ...(result || {}) }
  order.value.status = 'PAID'
  localStorage.setItem(orderStorageKey(orderNo), JSON.stringify({ ...order.value, userId: currentUserId() }))
  localStorage.setItem(`payment:${currentUserId()}:${orderNo}`, JSON.stringify(paymentResult.value))
}

onMounted(loadOrder)
</script>

<style scoped>
.pay-page { display: grid; gap: 18px; }
.page-head, .panel-title, .pay-total { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2, h3 { margin: 0; }
h2 { font-size: 28px; color: #111827; }
.text-btn { border: 0; background: transparent; color: #14532d; font-weight: 800; cursor: pointer; }
.pay-layout { display: grid; grid-template-columns: 1fr 360px; gap: 18px; align-items: start; }
.panel, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; padding: 18px; }
.order-panel, .cashier-panel { display: grid; gap: 16px; }
.status { padding: 4px 9px; border-radius: 999px; background: #eef2f7; color: #475569; font-size: 13px; }
.status.PAID, .status.SUCCESS { background: #dcfce7; color: #166534; }
.status.CREATED, .status.PENDING { background: #fff4cc; color: #8a5a00; }
.info-grid { display: grid; grid-template-columns: 86px 1fr; gap: 12px; color: #64748b; }
.info-grid strong { color: #111827; overflow-wrap: anywhere; }
.info-grid .amount { color: #b42318; font-size: 26px; }
.mini-items { display: grid; gap: 10px; border-top: 1px solid #e3e8ef; padding-top: 14px; }
.mini-items article { display: grid; grid-template-columns: 56px 1fr; gap: 10px; align-items: center; }
.mini-items img { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; }
.mini-items img[src=""] { background: #eef2f7; }
.mini-items div { display: grid; gap: 4px; }
.mini-items span { color: #64748b; font-size: 13px; }
.pay-methods { display: grid; gap: 8px; }
.pay-methods button { height: 42px; border: 1px solid #d7dde8; background: #fff; border-radius: 6px; cursor: pointer; font-weight: 700; }
.pay-methods button.active { border-color: #14532d; background: #f0f8f3; color: #14532d; }
.pay-total { padding: 14px; background: #f8fafc; border-radius: 6px; }
.pay-total strong { color: #b42318; font-size: 28px; }
.pay-btn, .secondary-btn { height: 44px; border: 0; border-radius: 6px; font-weight: 800; cursor: pointer; }
.pay-btn { background: #14532d; color: #fff; }
.pay-btn:disabled { opacity: .65; cursor: not-allowed; }
.secondary-btn { background: #e7f2eb; color: #14532d; }
.result-box, .notice { display: grid; gap: 4px; padding: 12px; border-radius: 6px; }
.result-box { background: #dcfce7; color: #166534; }
.notice { background: #fff4cc; color: #8a5a00; }
.state-panel { color: #64748b; }
@media (max-width: 900px) { .pay-layout { grid-template-columns: 1fr; } }
</style>
