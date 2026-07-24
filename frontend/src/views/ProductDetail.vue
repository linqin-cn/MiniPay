<template>
  <section class="detail-page">
    <button class="text-btn" @click="router.back()">返回商品列表</button>

    <div v-if="loading" class="state-panel">正在加载商品...</div>
    <div v-else-if="!product" class="state-panel">商品不存在</div>

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
              @click="selectedSku = sku"
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
            <input v-model.number="quantity" type="number" min="1" />
            <button @click="increase">+</button>
          </div>
        </div>

        <div class="summary-line">
          <span>小计</span>
          <strong>¥{{ money(subtotal) }}</strong>
        </div>

        <div class="actions">
          <button class="secondary" @click="handleAddCart">加入购物车</button>
          <button class="primary" @click="buyNow">立即购买</button>
        </div>

        <div v-if="notice" class="notice">{{ notice }}</div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { addCartItem, getProduct, getProductSkus } from '@/api'
import { findDemoProduct, getDemoSkus, money } from '@/data/demoCatalog'

const route = useRoute()
const router = useRouter()
const product = ref(null)
const skus = ref([])
const selectedSku = ref(null)
const quantity = ref(1)
const loading = ref(false)
const notice = ref('')

const subtotal = computed(() => Number(selectedSku.value?.price || 0) * Number(quantity.value || 1))

function normalizeList(data) {
  return Array.isArray(data) ? data : []
}

async function loadDetail() {
  loading.value = true
  const id = route.params.id
  try {
    const [productRes, skuRes] = await Promise.all([getProduct(id), getProductSkus(id)])
    product.value = productRes.data?.data || findDemoProduct(id)
    skus.value = normalizeList(skuRes.data?.data)
    if (!skus.value.length) skus.value = getDemoSkus(Number(id))
  } catch (error) {
    product.value = findDemoProduct(id)
    skus.value = getDemoSkus(Number(id))
  } finally {
    selectedSku.value = skus.value[0] || null
    loading.value = false
  }
}

function decrease() {
  quantity.value = Math.max(1, Number(quantity.value || 1) - 1)
}

function increase() {
  quantity.value = Number(quantity.value || 1) + 1
}

function buildCheckoutItem() {
  return {
    product: product.value,
    sku: selectedSku.value,
    quantity: Number(quantity.value || 1)
  }
}

async function handleAddCart() {
  if (!selectedSku.value) return
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
  if (!selectedSku.value) return
  localStorage.setItem('checkoutItems', JSON.stringify([buildCheckoutItem()]))
  router.push('/checkout')
}

onMounted(loadDetail)
</script>

<style scoped>
.detail-page { display: grid; gap: 14px; }
.text-btn { justify-self: start; border: 0; background: transparent; color: #14532d; font-weight: 700; cursor: pointer; padding: 0; }
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
.stepper { display: grid; grid-template-columns: 38px 72px 38px; }
.stepper button, .stepper input { height: 38px; border: 1px solid #d7dde8; background: #fff; text-align: center; }
.stepper input { border-left: 0; border-right: 0; outline: none; }
.actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.actions button { height: 44px; border: 0; border-radius: 6px; font-weight: 800; cursor: pointer; }
.actions .primary { background: #14532d; color: #fff; }
.actions .secondary { background: #e7f2eb; color: #14532d; }
.notice { padding: 10px 12px; background: #fff7d6; color: #7a4b00; border-radius: 6px; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } }
</style>
