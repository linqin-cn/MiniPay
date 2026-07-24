<template>
  <section class="detail-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">order detail</p>
        <h2>订单详情</h2>
      </div>
      <button class="text-btn" @click="router.push('/orders')">返回订单列表</button>
    </header>

    <div v-if="loading" class="state-panel">正在加载订单...</div>
    <div v-else-if="!order" class="state-panel">订单不存在</div>

    <div v-else class="detail-layout">
      <main class="main-panel">
        <section class="panel">
          <div class="panel-title">
            <h3>订单状态</h3>
            <span :class="['status', order.status]">{{ statusText(order.status) }}</span>
          </div>
          <div class="info-grid">
            <span>订单号</span><strong>{{ order.orderNo || order.orderId }}</strong>
            <span>创建时间</span><strong>{{ formatTime(order.createdAt) }}</strong>
            <span>备注</span><strong>{{ order.remark || order.description || '-' }}</strong>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title"><h3>商品明细</h3></div>
          <div v-if="order.items?.length" class="order-items">
            <article v-for="item in order.items" :key="item.sku.id" class="order-item">
              <img :src="item.product.mainImage" :alt="item.product.title" />
              <div>
                <h4>{{ item.product.title }}</h4>
                <p>{{ item.sku.skuName }}</p>
              </div>
              <div class="item-price">
                <strong>¥{{ money(item.sku.price) }}</strong>
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
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLogisticsByOrderNo, getPaymentOrderByOrderNo, getTradeOrder } from '@/api'
import { money } from '@/data/demoCatalog'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const payment = ref(null)
const logistics = ref(null)
const loading = ref(false)

const canPay = computed(() => ['CREATED', 'PENDING'].includes(order.value?.status))

function statusText(status) {
  const map = { CREATED: '待支付', PENDING: '待支付', PAID: '已支付', SUCCESS: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消', FAILED: '支付失败' }
  return map[status] || status || '-'
}

function formatTime(time) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

async function loadDetail() {
  loading.value = true
  const orderNo = route.params.orderNo
  const localOrder = localStorage.getItem(`order:${orderNo}`)
  const localPayment = localStorage.getItem(`payment:${orderNo}`)
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

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { display: grid; gap: 18px; }
.page-head, .panel-title, .summary-row, .summary-total { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2, h3, h4, p { margin: 0; }
h2 { font-size: 28px; color: #111827; }
.text-btn { border: 0; background: transparent; color: #14532d; font-weight: 800; cursor: pointer; }
.detail-layout { display: grid; grid-template-columns: 1fr 340px; gap: 18px; align-items: start; }
.main-panel { display: grid; gap: 14px; }
.panel, .summary-panel, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; }
.panel, .summary-panel { padding: 18px; display: grid; gap: 14px; }
.status { padding: 4px 9px; border-radius: 999px; background: #eef2f7; color: #475569; font-size: 13px; }
.status.PAID, .status.SUCCESS, .status.COMPLETED { background: #dcfce7; color: #166534; }
.status.CREATED, .status.PENDING { background: #fff4cc; color: #8a5a00; }
.info-grid { display: grid; grid-template-columns: 86px 1fr; gap: 12px; color: #64748b; }
.info-grid strong { color: #111827; overflow-wrap: anywhere; }
.order-items { display: grid; gap: 12px; }
.order-item { display: grid; grid-template-columns: 74px 1fr auto; gap: 12px; align-items: center; }
.order-item img { width: 74px; height: 74px; object-fit: cover; border-radius: 6px; }
.order-item p, .item-price span, .empty-line { color: #64748b; font-size: 13px; }
.item-price { display: grid; gap: 4px; text-align: right; }
.item-price strong { color: #b42318; }
.address-box, .payment-box { display: grid; gap: 6px; padding: 14px; background: #f8fafc; border-radius: 6px; color: #475569; }
.summary-panel { position: sticky; top: 16px; }
.summary-total { border-top: 1px solid #e3e8ef; padding-top: 14px; }
.summary-total strong { font-size: 28px; color: #b42318; }
.primary { height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 800; cursor: pointer; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } .summary-panel { position: static; } }
</style>
