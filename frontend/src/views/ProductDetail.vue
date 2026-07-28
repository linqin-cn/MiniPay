<template>
  <section class="detail-page">
    <button class="text-btn" @click="router.back()"><span class="back-icon" aria-hidden="true">←</span>返回商品列表</button>

    <div v-if="loading" class="state-panel">正在加载商品...</div>
    <div v-else-if="!product" class="state-panel">商品不存在</div>
    <div v-else-if="isUnavailable" class="state-panel">商品已下架，暂时无法购买</div>

    <div v-else class="detail-layout">
      <div class="media-panel">
        <img :src="product.mainImage" :alt="product.title" />
      </div>

      <div class="buy-panel">
        <div class="meta-line">
          <span>{{ product.categoryName || '精选商品' }}</span>
          <span>{{ product.merchantName || 'MiniPay 店铺' }}</span>
        </div>
        <h2>{{ product.title }}</h2>
        <p class="description">{{ product.description }}</p>

        <div class="price-block">
          <span>到手价</span>
          <strong>¥{{ money(selectedSku?.price) }}</strong>
          <del v-if="selectedSku?.originalPrice">¥{{ money(selectedSku.originalPrice) }}</del>
        </div>

        <div class="section-block">
          <label>规格</label>
          <div class="sku-grid">
            <button
              v-for="sku in skus"
              :key="sku.id"
              :class="['sku-btn', { active: selectedSku?.id === sku.id }]"
              @click="selectSku(sku)"
            >
              <span>{{ sku.skuName }}</span>
              <small>库存 {{ sku.stock ?? '-' }}</small>
            </button>
          </div>
        </div>

        <div class="section-block quantity-line">
          <label>数量</label>
          <div class="stepper">
            <button @click="decrease">-</button>
            <input v-model.number="quantity" type="number" min="1" :max="selectedStock || 1" @change="clampQuantity" />
            <button :disabled="isOutOfStock || quantity >= selectedStock" @click="increase">+</button>
          </div>
          <small class="stock-line">可售库存 {{ selectedStock }}</small>
        </div>

        <div class="summary-line">
          <span>小计</span>
          <strong>¥{{ money(subtotal) }}</strong>
        </div>

        <div class="actions">
          <button class="secondary" :disabled="isOutOfStock" @click="handleAddCart">加入购物车</button>
          <button class="primary" :disabled="isOutOfStock" @click="buyNow">立即购买</button>
        </div>

        <div v-if="notice" class="notice">{{ notice }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addCartItem, getInventory, getProduct, getProductSkus } from '@/api'
import { findDemoProduct, getDemoSkus, money } from '@/data/demoCatalog'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const skus = ref([])
const selectedSku = ref(null)
const quantity = ref(1)
const loading = ref(false)
const notice = ref('')
const isBuyer = localStorage.getItem('userRole') !== 'MERCHANT'

const subtotal = computed(() => Number(selectedSku.value?.price || 0) * Number(quantity.value || 1))
const selectedStock = computed(() => Number(selectedSku.value?.stock ?? 0))
const isOutOfStock = computed(() => selectedSku.value && selectedStock.value <= 0)
const isUnavailable = computed(() => isBuyer && product.value && (product.value.status || 'ON_SALE') !== 'ON_SALE')

function normalizeList(data) {
  return Array.isArray(data) ? data : []
}

async function loadDetail() {
  loading.value = true
  const id = route.params.id
  try {
    const [productRes, skuRes] = await Promise.all([getProduct(id), getProductSkus(id)])
    product.value = productRes.data?.data || findDemoProduct(id)
    skus.value = await hydrateSkuStock(normalizeList(skuRes.data?.data))
    if (!skus.value.length) skus.value = getDemoSkus(Number(id))
  } catch (error) {
    product.value = findDemoProduct(id)
    skus.value = getDemoSkus(Number(id))
  } finally {
    selectedSku.value = skus.value[0] || null
    clampQuantity()
    loading.value = false
  }
}

function decrease() {
  quantity.value = Math.max(1, Number(quantity.value || 1) - 1)
}

function selectSku(sku) {
  selectedSku.value = sku
  notice.value = ''
  clampQuantity()
}

function clampQuantity() {
  const stock = selectedStock.value
  if (stock <= 0) {
    quantity.value = 1
    return
  }
  quantity.value = Math.min(stock, Math.max(1, Number(quantity.value || 1)))
}

function increase() {
  if (Number(quantity.value || 1) >= selectedStock.value) {
    notice.value = `库存不足，最多只能购买 ${selectedStock.value} 件`
    clampQuantity()
    return
  }
  quantity.value = Number(quantity.value || 1) + 1
}

function buildCheckoutItem() {
  return {
    product: product.value,
    sku: selectedSku.value,
    quantity: Number(quantity.value || 1)
  }
}

function validateStock() {
  if (!selectedSku.value) return false
  if (selectedStock.value <= 0) {
    notice.value = '该规格库存不足，暂时无法购买'
    return false
  }
  if (Number(quantity.value || 1) > selectedStock.value) {
    notice.value = `库存不足，最多只能购买 ${selectedStock.value} 件`
    clampQuantity()
    return false
  }
  return true
}

async function handleAddCart() {
  if (!isBuyer) {
    notice.value = '商家账号不能加入购物车'
    return
  }
  if (!selectedSku.value) return
  if (!validateStock()) return
  try {
    await addCartItem({ productId: product.value.id, skuId: selectedSku.value.id, quantity: quantity.value })
    notice.value = '已加入购物车'
  } catch (error) {
    const items = JSON.parse(localStorage.getItem('checkoutItems') || '[]')
    localStorage.setItem('checkoutItems', JSON.stringify([...items, buildCheckoutItem()]))
    notice.value = '后端暂未实现，已保存到本地购物草稿'
  }
}

function buyNow() {
  if (!isBuyer) {
    notice.value = '商家账号不能购买商品'
    return
  }
  if (!selectedSku.value) return
  if (!validateStock()) return
  localStorage.setItem('checkoutItems', JSON.stringify([buildCheckoutItem()]))
  router.push('/checkout')
}

async function getSkuStock(skuId) {
  try {
    const res = await getInventory(skuId)
    return Number(res.data?.data?.availableStock ?? res.data?.data?.totalStock ?? 0)
  } catch (error) {
    return 0
  }
}

async function hydrateSkuStock(rows) {
  return Promise.all(rows.map(async sku => ({ ...sku, stock: await getSkuStock(sku.id) })))
}

watch(selectedSku, () => clampQuantity())

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { display: grid; gap: 14px; }
.text-btn { justify-self: start; display: inline-flex; align-items: center; gap: 6px; border: 0; background: transparent; color: #14532d; font-weight: 700; cursor: pointer; padding: 0; }
.back-icon { font-size: 18px; line-height: 1; transform: translateY(-1px); }
.detail-layout { display: grid; grid-template-columns: minmax(280px, 1fr) 420px; gap: 22px; align-items: start; }
.media-panel, .buy-panel, .state-panel { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; }
.media-panel { overflow: hidden; }
.media-panel img { width: 100%; aspect-ratio: 1 / 1; object-fit: cover; display: block; }
.buy-panel { display: grid; gap: 18px; padding: 22px; }
.meta-line, .summary-line { display: flex; align-items: center; justify-content: space-between; gap: 12px; color: #64748b; font-size: 13px; }
h2 { margin: 0; font-size: 28px; color: #111827; line-height: 1.25; }
.description { margin: 0; color: #475569; line-height: 1.65; }
.price-block { display: flex; align-items: baseline; gap: 10px; padding: 14px; background: #f8fafc; border-radius: 6px; }
.price-block span { color: #64748b; }
.price-block strong, .summary-line strong { color: #b42318; font-size: 28px; }
.price-block del { color: #94a3b8; }
.section-block { display: grid; gap: 10px; }
.section-block label { color: #111827; font-weight: 700; }
.sku-grid { display: grid; gap: 8px; }
.sku-btn { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 12px; border: 1px solid #d7dde8; background: #fff; border-radius: 6px; cursor: pointer; text-align: left; }
.sku-btn.active { border-color: #14532d; background: #f0f8f3; }
.sku-btn small { color: #64748b; }
.quantity-line { grid-template-columns: 72px auto; align-items: center; }
.stock-line { grid-column: 2; color: #64748b; font-size: 13px; }
.stepper { display: grid; grid-template-columns: 38px 72px 38px; }
.stepper button, .stepper input { height: 38px; border: 1px solid #d7dde8; background: #fff; text-align: center; }
.stepper input { border-left: 0; border-right: 0; outline: none; }
.actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.actions button { height: 44px; border: 0; border-radius: 6px; font-weight: 800; cursor: pointer; }
.stepper button:disabled, .actions button:disabled { opacity: .55; cursor: not-allowed; }
.actions .primary { background: #14532d; color: #fff; }
.actions .secondary { background: #e7f2eb; color: #14532d; }
.notice { padding: 10px 12px; background: #fff7d6; color: #7a4b00; border-radius: 6px; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } }
</style>
