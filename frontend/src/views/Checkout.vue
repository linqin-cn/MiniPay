<template>
  <section class="checkout-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">checkout</p>
        <h2>确认订单</h2>
      </div>
      <button class="text-btn" @click="router.push('/products')">继续选购</button>
    </header>

    <div v-if="items.length === 0" class="state-panel">
      还没有待结算商品
      <button @click="router.push('/products')">去选商品</button>
    </div>

    <div v-else class="checkout-layout">
      <main class="main-panel">
        <section class="panel">
          <div class="panel-title">
            <h3>收货地址</h3>
            <span v-if="usingDemoAddress">默认演示地址</span>
          </div>
          <div class="address-box">
            <strong>{{ address.receiverName }}</strong>
            <span>{{ address.receiverPhone }}</span>
            <p>{{ address.fullAddress }}</p>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">
            <h3>商品明细</h3>
            <span>{{ items.length }} 件商品</span>
          </div>
          <div class="order-items">
            <article v-for="item in items" :key="item.sku.id" class="order-item">
              <img :src="item.product.mainImage" :alt="item.product.title" />
              <div>
                <h4>{{ item.product.title }}</h4>
                <p>{{ item.sku.skuName }}</p>
                <small>库存 {{ item.stock ?? '-' }}</small>
                <span>{{ item.product.merchantName || 'MiniPay 店铺' }}</span>
              </div>
              <div class="item-price">
                <strong>¥{{ money(item.sku.price) }}</strong>
                <span>x {{ item.quantity }}</span>
              </div>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title"><h3>订单备注</h3></div>
          <textarea v-model="remark" placeholder="给商家留言，选填"></textarea>
        </section>
      </main>

      <aside class="summary-panel">
        <h3>金额确认</h3>
        <div class="summary-row"><span>商品金额</span><strong>¥{{ money(totalAmount) }}</strong></div>
        <div class="summary-row"><span>优惠金额</span><strong>-¥{{ money(discountAmount) }}</strong></div>
        <div class="summary-row"><span>运费</span><strong>¥{{ money(freightAmount) }}</strong></div>
        <div class="summary-total"><span>应付</span><strong>¥{{ money(payAmount) }}</strong></div>
        <button class="submit-btn" :disabled="submitting" @click="submitOrder">
          {{ submitting ? '提交中...' : '提交订单' }}
        </button>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { confirmOrder, createTradeOrder, getAddresses, getInventory } from '@/api'
import { money } from '@/data/demoCatalog'

const router = useRouter()
const items = ref([])
const remark = ref('')
const submitting = ref(false)
const notice = ref('')
const usingDemoAddress = ref(false)
const address = ref({
  id: 1,
  receiverName: '林同学',
  receiverPhone: '13000000000',
  fullAddress: '上海市 浦东新区 MiniPay 路 100 号'
})

const totalAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.sku.price || 0) * Number(item.quantity || 1), 0))
const discountAmount = computed(() => totalAmount.value >= 500 ? 30 : 0)
const freightAmount = computed(() => totalAmount.value >= 99 ? 0 : 12)
const payAmount = computed(() => Math.max(0, totalAmount.value - discountAmount.value + freightAmount.value))

async function loadItems() {
  const rows = JSON.parse(localStorage.getItem('checkoutItems') || '[]')
  items.value = await Promise.all(rows.map(async item => ({ ...item, stock: await getSkuStock(item.sku?.id) })))
}

async function loadAddress() {
  try {
    const res = await getAddresses()
    const list = res.data?.data || []
    if (Array.isArray(list) && list.length) {
      const selected = list.find(item => item.isDefault) || list[0]
      address.value = {
        id: selected.id,
        receiverName: selected.receiverName,
        receiverPhone: selected.receiverPhone,
        fullAddress: [selected.province, selected.city, selected.district, selected.detailAddress].filter(Boolean).join(' ')
      }
      usingDemoAddress.value = false
      return
    }
  } catch (error) {
    // backend TODO fallback
  }
  usingDemoAddress.value = true
}

function buildOrderReq() {
  return {
    addressId: address.value.id,
    couponId: null,
    remark: remark.value,
    items: items.value.map(item => ({ skuId: item.sku.id, quantity: item.quantity }))
  }
}

function buildLocalOrder() {
  const orderNo = `DEMO${Date.now()}`
  return {
    orderNo,
    userId: currentUserId(),
    status: 'CREATED',
    totalAmount: totalAmount.value,
    discountAmount: discountAmount.value,
    freightAmount: freightAmount.value,
    payAmount: payAmount.value,
    address: address.value,
    items: items.value,
    remark: remark.value,
    createdAt: new Date().toISOString()
  }
}

async function previewOrder() {
  try {
    await confirmOrder(buildOrderReq())
  } catch (error) {
    // backend TODO fallback
  }
}

function currentUserId() {
  return localStorage.getItem('userId') || 'anonymous'
}

function orderStorageKey(orderNo) {
  return `order:${currentUserId()}:${orderNo}`
}

async function getSkuStock(skuId) {
  try {
    const res = await getInventory(skuId)
    return Number(res.data?.data?.availableStock ?? res.data?.data?.totalStock ?? 0)
  } catch (error) {
    return 0
  }
}

async function validateCheckoutStock() {
  for (const item of items.value) {
    const stock = await getSkuStock(item.sku?.id)
    item.stock = stock
    if (Number(item.quantity || 1) > stock) {
      notice.value = `${item.product.title} 库存不足，最多只能购买 ${stock} 件`
      return false
    }
  }
  return true
}

async function submitOrder() {
  submitting.value = true
  notice.value = ''
  try {
    if (!(await validateCheckoutStock())) return
    const res = await createTradeOrder(buildOrderReq())
    const data = res.data?.data
    if (data?.orderNo || data?.orderId) {
      const orderNo = data.orderNo || data.orderId
      localStorage.setItem(orderStorageKey(orderNo), JSON.stringify({ ...buildLocalOrder(), ...data, orderNo, userId: currentUserId() }))
      router.push(`/pay/${orderNo}`)
      return
    }
    throw new Error('empty order')
  } catch (error) {
    notice.value = error.response?.data?.message || error.message || '订单创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadItems()
  await loadAddress()
  await previewOrder()
})
</script>

<style scoped>
.checkout-page { display: grid; gap: 18px; }
.page-head, .panel-title, .summary-row, .summary-total { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2, h3, h4, p { margin: 0; }
h2 { font-size: 28px; color: #111827; }
.text-btn { border: 0; background: transparent; color: #14532d; font-weight: 800; cursor: pointer; }
.checkout-layout { display: grid; grid-template-columns: 1fr 340px; gap: 18px; align-items: start; }
.main-panel { display: grid; gap: 14px; }
.panel, .summary-panel, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; }
.panel { padding: 18px; display: grid; gap: 14px; }
.panel-title span { color: #64748b; font-size: 13px; }
.address-box { display: grid; grid-template-columns: auto 1fr; gap: 6px 12px; padding: 14px; background: #f8fafc; border-radius: 6px; }
.address-box p { grid-column: 1 / -1; color: #475569; }
.order-items { display: grid; gap: 12px; }
.order-item { display: grid; grid-template-columns: 74px 1fr auto; gap: 12px; align-items: center; }
.order-item img { width: 74px; height: 74px; object-fit: cover; border-radius: 6px; background: #eef2f7; }
.order-item h4 { color: #111827; }
.order-item p, .order-item span, .order-item small { color: #64748b; font-size: 13px; }
.item-price { display: grid; gap: 4px; text-align: right; }
.item-price strong { color: #b42318; }
textarea { width: 100%; min-height: 84px; resize: vertical; border: 1px solid #d7dde8; border-radius: 6px; padding: 12px; outline: none; }
.summary-panel { position: sticky; top: 16px; display: grid; gap: 14px; padding: 18px; }
.summary-row { color: #475569; }
.summary-total { border-top: 1px solid #e3e8ef; padding-top: 14px; }
.summary-total strong { font-size: 28px; color: #b42318; }
.submit-btn, .state-panel button { height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 800; cursor: pointer; }
.submit-btn:disabled { opacity: .65; cursor: not-allowed; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px; border-radius: 6px; }
.state-panel { display: grid; gap: 12px; padding: 28px; color: #64748b; }
@media (max-width: 900px) { .checkout-layout { grid-template-columns: 1fr; } .summary-panel { position: static; } }
</style>
