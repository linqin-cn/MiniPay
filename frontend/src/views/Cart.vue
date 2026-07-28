<template>
  <section class="cart-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">cart</p>
        <h2>购物车</h2>
      </div>
      <button class="text-btn" @click="router.push('/products')">继续选购</button>
    </header>

    <div v-if="loading" class="state-panel">正在加载购物车...</div>

    <div v-else-if="cartItems.length === 0" class="state-panel empty">
      <strong>购物车还是空的</strong>
      <span>先去商品列表选择想买的商品。</span>
      <button @click="router.push('/products')">去选商品</button>
    </div>

    <div v-else class="cart-layout">
      <main class="cart-list">
        <div class="toolbar">
          <label class="check-line">
            <input type="checkbox" :checked="allSelected" @change="toggleAll($event.target.checked)" />
            <span>全选</span>
          </label>
          <button class="ghost-btn" @click="removeSelected">删除已选</button>
        </div>

        <article v-for="item in cartItems" :key="item.id || item.skuId" class="cart-card">
          <input type="checkbox" :checked="item.selected" @change="toggleSelected(item, $event.target.checked)" />
          <img :src="item.product.mainImage" :alt="item.product.title" />
          <div class="item-info">
            <h3>{{ item.product.title }}</h3>
            <p>{{ item.sku.skuName || '默认规格' }}</p>
            <small>库存 {{ item.stock ?? '-' }}</small>
            <span>{{ item.product.merchantName || 'MiniPay 店铺' }}</span>
          </div>
          <strong class="price">¥{{ money(item.sku.price) }}</strong>
          <div class="stepper">
            <button @click="changeQuantity(item, item.quantity - 1)">-</button>
            <input :value="item.quantity" type="number" min="1" :max="item.stock || 1" @change="changeQuantity(item, Number($event.target.value))" />
            <button :disabled="Number(item.quantity || 1) >= Number(item.stock || 0)" @click="changeQuantity(item, item.quantity + 1)">+</button>
          </div>
          <strong class="subtotal">¥{{ money(lineAmount(item)) }}</strong>
          <button class="delete-btn" @click="removeItem(item)">删除</button>
        </article>
      </main>

      <aside class="summary-panel">
        <h3>结算清单</h3>
        <div class="summary-row"><span>已选商品</span><strong>{{ selectedItems.length }} 件</strong></div>
        <div class="summary-row"><span>商品金额</span><strong>¥{{ money(selectedAmount) }}</strong></div>
        <div class="summary-row"><span>预计优惠</span><strong>-¥{{ money(discountAmount) }}</strong></div>
        <div class="summary-total"><span>预计应付</span><strong>¥{{ money(payAmount) }}</strong></div>
        <button class="submit-btn" :disabled="selectedItems.length === 0" @click="checkout">去结算</button>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { deleteCartItem, deleteSelectedCartItems, getCart, getInventory, getProduct, getProductSkus, updateCartItem, updateCartItemSelected } from '@/api'
import { demoProducts, findDemoProduct, getDemoSkus, money } from '@/data/demoCatalog'

const router = useRouter()
const loading = ref(false)
const notice = ref('')
const cartItems = ref([])

const selectedItems = computed(() => cartItems.value.filter(item => item.selected))
const allSelected = computed(() => cartItems.value.length > 0 && cartItems.value.every(item => item.selected))
const selectedAmount = computed(() => selectedItems.value.reduce((sum, item) => sum + lineAmount(item), 0))
const discountAmount = computed(() => selectedAmount.value >= 500 ? 30 : 0)
const payAmount = computed(() => Math.max(0, selectedAmount.value - discountAmount.value))

function lineAmount(item) {
  return Number(item.sku.price || 0) * Number(item.quantity || 1)
}

function localCart() {
  const checkoutItems = JSON.parse(localStorage.getItem('checkoutItems') || '[]')
  if (checkoutItems.length) {
    return checkoutItems.map((item, index) => ({
      id: `local-${index}`,
      productId: item.product?.id,
      skuId: item.sku?.id,
      quantity: Number(item.quantity || 1),
      selected: true,
      product: item.product,
      sku: item.sku
    }))
  }
  const product = demoProducts[0]
  const sku = getDemoSkus(product.id)[0]
  return [{ id: 'demo-1', productId: product.id, skuId: sku.id, quantity: 1, selected: true, product, sku }]
}

async function enrichItem(item) {
  if (item.product && item.sku) return item
  let product = findDemoProduct(item.productId)
  let sku = getDemoSkus(item.productId).find(skuItem => String(skuItem.id) === String(item.skuId))
  try {
    const productRes = await getProduct(item.productId)
    product = productRes.data?.data || product
    const skuRes = await getProductSkus(item.productId)
    const skus = skuRes.data?.data || []
    sku = skus.find(skuItem => String(skuItem.id) === String(item.skuId)) || sku
  } catch (error) {
    // 使用演示商品补齐购物车展示信息
  }
  return {
    ...item,
    selected: item.selected !== false,
    quantity: Number(item.quantity || 1),
    product: product || { id: item.productId, title: `商品 ${item.productId}`, mainImage: '', merchantName: 'MiniPay 店铺' },
    sku: sku || { id: item.skuId, productId: item.productId, skuName: `SKU ${item.skuId}`, price: 0 },
    stock: await getSkuStock(item.skuId)
  }
}

async function loadCart() {
  loading.value = true
  notice.value = ''
  try {
    const res = await getCart()
    const list = res.data?.data || []
    cartItems.value = await Promise.all(list.map(enrichItem))
  } catch (error) {
    cartItems.value = []
    notice.value = '购物车服务暂不可用，请确认 cart-service 和 gateway 已启动'
  } finally {
    loading.value = false
  }
}

async function changeQuantity(item, quantity) {
  const stock = Number(item.stock ?? 0)
  let next = Math.max(1, Number(quantity || 1))
  if (stock <= 0) {
    notice.value = '该商品库存不足，无法增加数量'
    return
  }
  if (next > stock) {
    next = stock
    notice.value = `库存不足，最多只能购买 ${stock} 件`
  }
  item.quantity = next
  if (typeof item.id === 'number') {
    try { await updateCartItem(item.id, { productId: item.productId, skuId: item.skuId, quantity: next, selected: item.selected }) } catch (error) {}
  }
}

async function toggleSelected(item, selected) {
  item.selected = selected
  if (typeof item.id === 'number') {
    try { await updateCartItemSelected(item.id, selected) } catch (error) {}
  }
}

async function toggleAll(selected) {
  await Promise.all(cartItems.value.map(item => toggleSelected(item, selected)))
}

async function removeItem(item) {
  cartItems.value = cartItems.value.filter(current => current !== item)
  if (typeof item.id === 'number') {
    try { await deleteCartItem(item.id) } catch (error) {}
  }
}

async function removeSelected() {
  cartItems.value = cartItems.value.filter(item => !item.selected)
  try { await deleteSelectedCartItems() } catch (error) {}
}

function checkout() {
  const invalidItem = selectedItems.value.find(item => Number(item.quantity || 1) > Number(item.stock ?? 0))
  if (invalidItem) {
    notice.value = `${invalidItem.product.title} 库存不足，最多只能购买 ${invalidItem.stock || 0} 件`
    return
  }
  const checkoutItems = selectedItems.value.map(item => ({ product: item.product, sku: item.sku, quantity: item.quantity }))
  localStorage.setItem('checkoutItems', JSON.stringify(checkoutItems))
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

onMounted(loadCart)
</script>

<style scoped>
.cart-page { display: grid; gap: 20px; }
.page-head, .toolbar, .summary-row, .summary-total { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2, h3, p { margin: 0; }
h2 { font-size: 28px; line-height: 1.2; color: #111827; }
button { font-family: inherit; }
.text-btn, .ghost-btn, .delete-btn { min-height: 36px; border: 0; border-radius: 6px; background: #e7f2eb; color: #14532d; padding: 0 12px; font-weight: 800; cursor: pointer; }
.delete-btn { background: #fff1f0; color: #b42318; }
.cart-layout { display: grid; grid-template-columns: minmax(0, 1fr) 332px; gap: 18px; align-items: start; }
.cart-list { display: grid; gap: 12px; min-width: 0; }
.toolbar, .cart-card, .summary-panel, .state-panel { background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.toolbar { min-height: 52px; padding: 12px 16px; }
.check-line { display: flex; align-items: center; gap: 8px; color: #475569; font-weight: 800; }
.check-line input, .cart-card > input { width: 16px; height: 16px; accent-color: #14532d; }
.cart-card { display: grid; grid-template-columns: 22px 76px minmax(180px, 1fr) 88px 116px 100px 58px; gap: 14px; align-items: center; padding: 16px; overflow: hidden; }
.cart-card img { width: 76px; height: 76px; border-radius: 8px; object-fit: cover; background: #eef2f7; display: block; }
.item-info { min-width: 0; display: grid; gap: 5px; }
.item-info h3 { color: #111827; font-size: 16px; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.item-info p, .item-info span, .item-info small { color: #64748b; font-size: 13px; line-height: 1.4; }
.price, .subtotal { color: #b42318; font-size: 16px; white-space: nowrap; }
.subtotal { justify-self: end; }
.stepper { display: grid; grid-template-columns: 34px 48px 34px; align-items: center; }
.stepper button, .stepper input { height: 34px; border: 1px solid #d7dde8; background: #fff; text-align: center; color: #111827; }
.stepper button { cursor: pointer; font-weight: 900; }
.stepper button:disabled { opacity: .55; cursor: not-allowed; }
.stepper input { border-left: 0; border-right: 0; outline: none; min-width: 0; }
.summary-panel { position: sticky; top: 86px; display: grid; gap: 14px; padding: 18px; }
.summary-panel h3 { color: #111827; font-size: 18px; }
.summary-row { color: #475569; font-size: 14px; }
.summary-total { border-top: 1px solid #e3e8ef; padding-top: 14px; }
.summary-total span { color: #111827; font-weight: 800; }
.summary-total strong { font-size: 28px; color: #b42318; }
.submit-btn, .state-panel button { min-height: 44px; border: 0; border-radius: 6px; background: #14532d; color: #fff; font-weight: 900; cursor: pointer; }
.submit-btn:disabled { opacity: .55; cursor: not-allowed; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px 12px; border-radius: 6px; line-height: 1.5; }
.state-panel { padding: 28px; color: #64748b; }
.state-panel.empty { display: grid; gap: 10px; justify-items: start; }
.state-panel strong { color: #111827; }
@media (max-width: 1080px) { .cart-layout { grid-template-columns: 1fr; } .summary-panel { position: static; } }
@media (max-width: 780px) { .page-head { align-items: flex-start; flex-direction: column; } .cart-card { grid-template-columns: 22px 72px minmax(0, 1fr); } .price, .stepper, .subtotal, .delete-btn { grid-column: 3; justify-self: start; } .subtotal { font-size: 18px; } }
</style>
