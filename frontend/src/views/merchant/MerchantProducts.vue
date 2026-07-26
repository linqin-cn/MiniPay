<template>
  <section class="merchant-products">
    <header class="page-head">
      <div>
        <p class="eyebrow">products</p>
        <h2>商家商品管理</h2>
      </div>
      <button class="text-btn" @click="router.push('/merchant')">返回概览</button>
    </header>

    <div class="product-layout">
      <main class="product-table">
        <div class="table-head">
          <span>商品</span><span>状态</span><span>操作</span>
        </div>
        <div v-if="loading" class="state-panel">正在加载商品...</div>
        <article v-for="product in products" :key="product.id" class="product-row">
          <div class="product-cell">
            <img :src="product.mainImage" :alt="product.title" />
            <div>
              <strong>{{ product.title }}</strong>
              <span>{{ product.description }}</span>
              <b>¥{{ money(product.price || 0) }}</b>
            </div>
          </div>
          <em :class="['status', product.status || 'ON_SALE']">{{ product.status || 'ON_SALE' }}</em>
          <div class="row-actions">
            <button @click="editProduct(product)">编辑</button>
            <button v-if="(product.status || 'ON_SALE') !== 'ON_SALE'" @click="changeSale(product, true)">上架</button>
            <button v-else class="danger" @click="changeSale(product, false)">下架</button>
          </div>
        </article>
      </main>

      <aside class="form-panel">
        <h3>{{ form.id ? '编辑商品' : '发布商品' }}</h3>
        <label>商品名称<input v-model.trim="form.title" placeholder="商品标题" /></label>
        <label>商品图片<input v-model.trim="form.mainImage" placeholder="图片 URL" /></label>
        <label>SKU 名称<input v-model.trim="form.skuName" placeholder="默认规格" /></label>
        <label>销售价格<input v-model.number="form.price" type="number" min="0" step="0.01" placeholder="0.00" /></label>
        <label>商品原价<input v-model.number="form.originalPrice" type="number" min="0" step="0.01" placeholder="0.00" /></label>
        <label>分类 ID<input v-model.number="form.categoryId" type="number" min="1" /></label>
        <label>商品描述<textarea v-model.trim="form.description" placeholder="商品卖点和说明" /></label>
        <button class="submit-btn" :disabled="saving" @click="saveProduct">{{ saving ? '保存中...' : '保存商品' }}</button>
        <button class="ghost-btn" @click="resetForm">清空表单</button>
        <p v-if="notice" class="notice">{{ notice }}</p>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProduct, getProducts, getProductSkus, offSaleProduct, onSaleProduct, updateProduct } from '@/api'
import { demoProducts } from '@/data/demoCatalog'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const notice = ref('')
const products = ref([])
const form = reactive(emptyForm())

function emptyForm() {
  return { id: null, merchantId: 1, categoryId: 1, title: '', description: '', mainImage: '', skuName: '默认规格', price: 0, originalPrice: 0 }
}

function normalizeProducts(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProducts()
    const rows = normalizeProducts(res.data?.data)
    products.value = rows.length ? await hydrateSkuInfo(rows) : demoProducts
  } catch (error) {
    products.value = demoProducts
    notice.value = '商品服务暂不可用，当前使用演示商品数据'
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function editProduct(product) {
  Object.assign(form, {
    id: product.id,
    merchantId: product.merchantId || 1,
    categoryId: product.categoryId || 1,
    title: product.title || '',
    description: product.description || '',
    mainImage: product.mainImage || '',
    skuName: product.skuName || '默认规格',
    price: Number(product.price || 0),
    originalPrice: Number(product.originalPrice || product.price || 0)
  })
}

function validateForm() {
  if (!form.title || !form.mainImage) {
    notice.value = '请填写商品名称和图片 URL'
    return false
  }
  if (Number(form.price) <= 0) {
    notice.value = '请填写大于 0 的销售价格'
    return false
  }
  return true
}

async function saveProduct() {
  if (!validateForm()) return
  saving.value = true
  const payload = { ...form }
  delete payload.id
  payload.originalPrice = Number(payload.originalPrice || payload.price)
  try {
    if (form.id) {
      await updateProduct(form.id, payload)
      notice.value = '商品已更新'
    } else {
      await createProduct(payload)
      notice.value = '商品已发布'
    }
    resetForm()
    await loadProducts()
  } catch (error) {
    if (form.id) {
      const index = products.value.findIndex(item => item.id === form.id)
      if (index >= 0) products.value[index] = { ...products.value[index], ...payload }
    } else {
      products.value.unshift({ ...payload, id: Date.now(), status: 'ON_SALE' })
    }
    notice.value = '后端暂不可用，已在当前页面临时保存'
    resetForm()
  } finally {
    saving.value = false
  }
}

async function changeSale(product, onSale) {
  product.status = onSale ? 'ON_SALE' : 'OFF_SALE'
  try {
    if (onSale) await onSaleProduct(product.id)
    else await offSaleProduct(product.id)
  } catch (error) {}
}

async function hydrateSkuInfo(rows) {
  return Promise.all(rows.map(async product => {
    try {
      const skuRes = await getProductSkus(product.id)
      const sku = Array.isArray(skuRes.data?.data) ? skuRes.data.data[0] : null
      return sku ? { ...product, skuName: sku.skuName, price: sku.price, originalPrice: sku.originalPrice } : product
    } catch (error) {
      return product
    }
  }))
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

onMounted(loadProducts)
</script>

<style scoped>
.merchant-products { display: grid; gap: 20px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2, h3 { margin: 0; }
h2 { font-size: 28px; line-height: 1.2; color: #111827; }
button { font-family: inherit; }
.text-btn, .ghost-btn { min-height: 36px; border: 0; border-radius: 6px; background: #e7f2eb; color: #14532d; padding: 0 12px; font-weight: 900; cursor: pointer; }
.ghost-btn { background: transparent; justify-self: center; }
.product-layout { display: grid; grid-template-columns: minmax(0, 1fr) 372px; gap: 18px; align-items: start; }
.product-table, .form-panel, .state-panel { background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; box-shadow: 0 12px 28px rgba(17, 24, 39, .04); }
.product-table { display: grid; overflow: hidden; align-content: start; }
.table-head, .product-row { display: grid; grid-template-columns: minmax(260px, 1fr) 104px 176px; gap: 12px; align-items: center; padding: 13px 16px; }
.table-head { background: #f8fafc; color: #64748b; font-weight: 900; font-size: 13px; }
.product-row { border-top: 1px solid #e3e8ef; min-width: 0; }
.product-row:hover { background: #fbfdfc; }
.product-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }
.product-cell img { width: 60px; height: 60px; object-fit: cover; border-radius: 8px; background: #eef2f7; flex: 0 0 auto; }
.product-cell div { display: grid; gap: 5px; min-width: 0; }
.product-cell strong { color: #111827; line-height: 1.35; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-cell span { color: #64748b; line-height: 1.45; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-cell b { color: #b42318; font-size: 15px; }
.status { justify-self: start; font-style: normal; color: #475569; background: #eef2f7; padding: 4px 9px; border-radius: 999px; font-size: 12px; font-weight: 900; white-space: nowrap; }
.status.ON_SALE { color: #166534; background: #dcfce7; }
.row-actions { display: flex; gap: 8px; justify-content: flex-end; flex-wrap: wrap; }
.row-actions button, .submit-btn { min-height: 36px; border: 0; border-radius: 6px; padding: 0 12px; background: #e7f2eb; color: #14532d; font-weight: 900; cursor: pointer; }
.row-actions .danger { background: #fff1f0; color: #b42318; }
.form-panel { position: sticky; top: 86px; display: grid; gap: 13px; padding: 18px; }
.form-panel h3 { color: #111827; font-size: 18px; }
.form-panel label { display: grid; gap: 7px; color: #475569; font-size: 14px; font-weight: 800; }
.form-panel input, .form-panel textarea { width: 100%; border: 1px solid #d7dde8; border-radius: 6px; padding: 11px 12px; outline: none; color: #111827; transition: border-color .2s, box-shadow .2s; }
.form-panel input:focus, .form-panel textarea:focus { border-color: #14532d; box-shadow: 0 0 0 3px rgba(20, 83, 45, .1); }
.form-panel textarea { min-height: 98px; resize: vertical; }
.submit-btn { min-height: 44px; background: #14532d; color: #fff; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px 12px; border-radius: 6px; line-height: 1.5; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 1060px) { .product-layout { grid-template-columns: 1fr; } .form-panel { position: static; } }
@media (max-width: 760px) { .page-head { align-items: flex-start; flex-direction: column; } .table-head { display: none; } .product-row { grid-template-columns: 1fr; } .row-actions { justify-content: flex-start; } }
</style>
