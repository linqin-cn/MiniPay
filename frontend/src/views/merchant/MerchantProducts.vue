<template>
  <section class="merchant-products">
    <header class="page-head">
      <div>
        <p class="eyebrow">products</p>
        <h2>商家商品管理</h2>
      </div>
      <div class="head-actions">
        <button class="primary-btn" @click="startCreateProduct">新增商品</button>
        <button class="text-btn" @click="router.push('/merchant')"><span class="back-icon" aria-hidden="true">←</span>返回概览</button>
      </div>
    </header>

    <div class="scope-tabs">
      <button :class="{ active: productScope === 'ACTIVE' }" @click="productScope = 'ACTIVE'">商品管理</button>
      <button :class="{ active: productScope === 'ARCHIVED' }" @click="productScope = 'ARCHIVED'">已归档</button>
    </div>

    <div class="product-layout">
      <main class="product-table">
        <div class="table-head">
          <span>商品</span><span>状态</span><span>操作</span>
        </div>
        <div v-if="loading" class="state-panel">正在加载商品...</div>
        <div v-else-if="visibleProducts.length === 0" class="state-panel">当前没有商品</div>
        <article v-for="product in visibleProducts" :key="product.id" class="product-row">
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
            <button @click="loadSkus(product)">规格</button>
            <button v-if="product.status !== 'ARCHIVED'" @click="editProduct(product)">编辑</button>
            <button v-if="product.status !== 'ARCHIVED'" @click="startCreateSku(product)">新增规格</button>
            <button v-if="product.status !== 'ARCHIVED' && (product.status || 'ON_SALE') !== 'ON_SALE'" @click="changeSale(product, true)">上架</button>
            <button v-if="product.status !== 'ARCHIVED' && (product.status || 'ON_SALE') === 'ON_SALE'" class="danger" @click="changeSale(product, false)">下架</button>
            <button v-if="product.status !== 'ARCHIVED'" @click="archive(product)">归档</button>
            <button v-if="product.status === 'ARCHIVED'" @click="restore(product)">恢复</button>
            <button v-if="product.status === 'ARCHIVED'" class="danger" @click="removeArchived(product)">删除</button>
          </div>
        </article>
      </main>

      <aside class="form-panel">
        <div v-if="formMode" class="panel-section">
          <div class="panel-head">
            <h3>{{ formTitle }}</h3>
            <button class="close-btn" aria-label="关闭表单" @click="closePanel">×</button>
          </div>
          <label v-if="formMode !== 'sku'">商品名称<input v-model.trim="form.title" placeholder="商品标题" /></label>
          <label v-if="formMode !== 'sku'">
            商品图片
            <div class="image-url-row">
              <input v-model.trim="form.mainImage" placeholder="图片 URL" />
              <button type="button" class="upload-btn" :disabled="uploadingImage" @click="openImagePicker">{{ uploadingImage ? '上传中' : '选择图片' }}</button>
            </div>
            <input ref="imageFileInput" class="file-input" type="file" accept="image/jpeg,image/png,image/webp,image/gif" @change="uploadSelectedImage" />
          </label>
          <div v-if="formMode !== 'sku' && form.mainImage" class="image-preview"><img :src="form.mainImage" alt="商品图片预览" /></div>
          <label>SKU 名称<input v-model.trim="form.skuName" placeholder="默认规格" /></label>
          <label>销售价格<input v-model.number="form.price" type="number" min="0" step="0.01" placeholder="0.00" /></label>
          <label>商品原价<input v-model.number="form.originalPrice" type="number" min="0" step="0.01" placeholder="0.00" /></label>
          <label>库存数量<input v-model.number="form.stock" type="number" min="0" step="1" placeholder="请输入库存数量" /></label>
          <label v-if="formMode !== 'sku'">分类 ID<input v-model.number="form.categoryId" type="number" min="1" /></label>
          <label v-if="formMode !== 'sku'">商品描述<textarea v-model.trim="form.description" placeholder="商品卖点和说明" /></label>
          <button class="submit-btn" :disabled="saving" @click="saveProduct">{{ saving ? '保存中...' : submitText }}</button>
          <button class="ghost-btn" @click="clearCurrentForm">清空表单</button>
        </div>

        <div v-if="!formMode && !skuProduct" class="panel-empty">请选择商品操作</div>
        <p v-if="notice" class="notice">{{ notice }}</p>

        <div v-if="skuProduct" class="sku-panel">
          <div class="sku-head">
            <strong>{{ skuProduct.title }} 的规格</strong>
            <button class="ghost-btn" @click="clearSkuPanel">收起</button>
          </div>
          <div v-if="skuLoading" class="sku-empty">正在加载规格...</div>
          <div v-else-if="skuList.length === 0" class="sku-empty">暂无规格</div>
          <div v-else class="sku-list">
            <div v-for="sku in skuList" :key="sku.id" class="sku-row">
              <template v-if="editingSkuId === sku.id">
                <div class="sku-edit-form">
                  <label>SKU 名称<input v-model.trim="skuEditForm.skuName" placeholder="请输入 SKU 名称" /></label>
                  <label>销售价格<input v-model.number="skuEditForm.price" type="number" min="0" step="0.01" placeholder="请输入销售价格" /></label>
                  <label>商品原价<input v-model.number="skuEditForm.originalPrice" type="number" min="0" step="0.01" placeholder="请输入商品原价" /></label>
                  <label>库存数量<input v-model.number="skuEditForm.stock" type="number" min="0" step="1" placeholder="请输入库存数量" /></label>
                  <div class="sku-edit-actions">
                    <button @click="saveSkuName(sku)">保存</button>
                    <button @click="cancelSkuEdit">取消</button>
                  </div>
                </div>
              </template>
              <template v-else>
                <div>
                  <strong>{{ sku.skuName }}</strong>
                  <span>¥{{ money(sku.price) }}</span>
                  <small>库存 {{ sku.stock ?? '-' }}</small>
                </div>
                <button @click="startEditSkuName(sku)">编辑名称</button>
              </template>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { archiveProduct, createProduct, createProductSku, deleteProduct, getCurrentMerchant, getInventory, getProducts, getProductSkus, offSaleProduct, onSaleProduct, restoreProduct, setInventoryStock, updateProduct, updateProductSku, uploadProductImage } from '@/api'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const uploadingImage = ref(false)
const notice = ref('')
const products = ref([])
const currentMerchantId = ref(null)
const productScope = ref('ACTIVE')
const formMode = ref(null)
const form = reactive(emptyForm())
const imageFileInput = ref(null)
const skuProduct = ref(null)
const skuList = ref([])
const skuLoading = ref(false)
const editingSkuId = ref(null)
const skuEditForm = reactive({ skuName: '', price: 0, originalPrice: 0, stock: 0 })

const visibleProducts = computed(() => {
  if (productScope.value === 'ARCHIVED') return products.value.filter(product => product.status === 'ARCHIVED')
  return products.value.filter(product => !['ARCHIVED', 'DELETED'].includes(product.status))
})

const formTitle = computed(() => {
  if (formMode.value === 'sku') return `新增规格：${form.title || '商品'}`
  return form.id ? '编辑商品' : '发布商品'
})
const submitText = computed(() => formMode.value === 'sku' ? '保存规格' : '保存商品')

function emptyForm() {
  return { id: null, merchantId: currentMerchantId.value, categoryId: 1, title: '', description: '', mainImage: '', skuName: '默认规格', price: 0, originalPrice: 0, stock: 999 }
}

function normalizeProducts(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

async function loadProducts() {
  loading.value = true
  try {
    await ensureCurrentMerchant()
    const res = await getProducts({ merchantId: currentMerchantId.value })
    const rows = normalizeProducts(res.data?.data)
    products.value = await hydrateSkuInfo(rows)
  } catch (error) {
    products.value = []
    notice.value = '商品服务暂不可用或当前商家身份获取失败'
  } finally {
    loading.value = false
  }
}

async function ensureCurrentMerchant() {
  if (currentMerchantId.value) return currentMerchantId.value
  const res = await getCurrentMerchant()
  currentMerchantId.value = res.data?.data?.id || null
  if (!currentMerchantId.value) throw new Error('当前商家信息不存在')
  return currentMerchantId.value
}

function resetForm() {
  Object.assign(form, emptyForm())
}

function closePanel() {
  resetForm()
  formMode.value = null
}

function clearCurrentForm() {
  if (formMode.value === 'sku' && form.id) {
    clearSkuInputs()
    return
  }
  resetForm()
  if (formMode.value == null) formMode.value = 'create'
}

function clearSkuPanel() {
  skuProduct.value = null
  skuList.value = []
  cancelSkuEdit()
}

function startCreateProduct() {
  resetForm()
  clearSkuPanel()
  formMode.value = 'create'
  notice.value = ''
}

function editProduct(product) {
  formMode.value = 'edit'
  clearSkuPanel()
  Object.assign(form, {
    id: product.id,
    merchantId: product.merchantId || currentMerchantId.value,
    categoryId: product.categoryId || 1,
    title: product.title || '',
    description: product.description || '',
    mainImage: product.mainImage || '',
    skuName: product.skuName || '默认规格',
    price: Number(product.price || 0),
    originalPrice: Number(product.originalPrice || product.price || 0),
    stock: Number(product.stock ?? 999)
  })
}

function startCreateSku(product) {
  if (skuProduct.value?.id !== product.id) loadSkus(product, false)
  formMode.value = 'sku'
  Object.assign(form, {
    id: product.id,
    merchantId: product.merchantId || currentMerchantId.value,
    categoryId: product.categoryId || 1,
    title: product.title || '',
    description: product.description || '',
    mainImage: product.mainImage || '',
    skuName: '',
    price: 0,
    originalPrice: 0,
    stock: 999
  })
  notice.value = ''
}

function clearSkuInputs() {
  form.skuName = ''
  form.price = 0
  form.originalPrice = 0
  form.stock = 999
}

function validateForm() {
  if (formMode.value !== 'sku' && (!form.title || !form.mainImage)) {
    notice.value = '请填写商品名称和图片 URL'
    return false
  }
  if (Number(form.price) <= 0) {
    notice.value = '请填写大于 0 的销售价格'
    return false
  }
  if (Number(form.stock) < 0) {
    notice.value = '库存数量不能小于 0'
    return false
  }
  return true
}

function openImagePicker() {
  imageFileInput.value?.click()
}

async function uploadSelectedImage(event) {
  const file = event.target.files?.[0]
  if (!file) return
  uploadingImage.value = true
  notice.value = ''
  try {
    const res = await uploadProductImage(file)
    const url = res.data?.data?.url
    if (!url) throw new Error('图片上传失败')
    form.mainImage = url
    notice.value = '图片已上传并填入商品图片 URL'
  } catch (error) {
    notice.value = error.response?.data?.message || '图片上传失败，请检查图片格式或商品服务状态'
  } finally {
    uploadingImage.value = false
    event.target.value = ''
  }
}

async function saveProduct() {
  if (!validateForm()) return
  saving.value = true
  let payload = null
  try {
    await ensureCurrentMerchant()
    payload = { ...form }
    payload.merchantId = currentMerchantId.value
    delete payload.id
    delete payload.stock
    payload.originalPrice = Number(payload.originalPrice || payload.price)
    if (formMode.value === 'sku') {
      const res = await createProductSku(form.id, payload)
      const skuId = res.data?.data?.id
      if (skuId) await setInventoryStock(skuId, Number(form.stock || 0))
      notice.value = '规格已新增'
      clearSkuInputs()
      if (skuProduct.value?.id === form.id) await loadSkus(skuProduct.value)
      await loadProducts()
      return
    } else if (form.id) {
      await updateProduct(form.id, payload)
      await saveDefaultSkuStock(form.id, Number(form.stock || 0))
      notice.value = '商品已更新'
    } else {
      const res = await createProduct(payload)
      const productId = res.data?.data?.id
      if (productId) await saveDefaultSkuStock(productId, Number(form.stock || 0))
      notice.value = '商品已发布'
    }
    closePanel()
    await loadProducts()
  } catch (error) {
    if (formMode.value === 'sku') {
      notice.value = '后端暂不可用，规格未保存'
      return
    } else if (form.id) {
      const index = products.value.findIndex(item => item.id === form.id)
      if (index >= 0 && payload) products.value[index] = { ...products.value[index], ...payload }
    } else {
      if (payload) products.value.unshift({ ...payload, id: Date.now(), status: 'ON_SALE' })
    }
    if (formMode.value !== 'sku') notice.value = '后端暂不可用，已在当前页面临时保存'
    closePanel()
  } finally {
    saving.value = false
  }
}

async function loadSkus(product, showOnly = true) {
  if (showOnly) {
    resetForm()
    formMode.value = null
  }
  skuProduct.value = product
  skuLoading.value = true
  cancelSkuEdit()
  try {
    const res = await getProductSkus(product.id)
    const rows = Array.isArray(res.data?.data) ? res.data.data : []
    skuList.value = await hydrateSkuStock(rows)
  } catch (error) {
    skuList.value = []
    notice.value = '规格加载失败'
  } finally {
    skuLoading.value = false
  }
}

async function startEditSkuName(sku) {
  editingSkuId.value = sku.id
  skuEditForm.skuName = sku.skuName || ''
  skuEditForm.price = Number(sku.price || 0)
  skuEditForm.originalPrice = Number(sku.originalPrice || sku.price || 0)
  skuEditForm.stock = Number(sku.stock ?? 0)
  try {
    const res = await getInventory(sku.id)
    skuEditForm.stock = Number(res.data?.data?.totalStock ?? skuEditForm.stock)
  } catch (error) {}
}

function cancelSkuEdit() {
  editingSkuId.value = null
  skuEditForm.skuName = ''
  skuEditForm.price = 0
  skuEditForm.originalPrice = 0
  skuEditForm.stock = 0
}

async function saveSkuName(sku) {
  if (!skuEditForm.skuName) {
    notice.value = '请填写 SKU 名称'
    return
  }
  if (Number(skuEditForm.price) <= 0) {
    notice.value = '请填写大于 0 的销售价格'
    return
  }
  if (Number(skuEditForm.stock) < 0) {
    notice.value = '库存数量不能小于 0'
    return
  }
  try {
    const payload = {
      skuName: skuEditForm.skuName,
      price: Number(skuEditForm.price),
      originalPrice: Number(skuEditForm.originalPrice || skuEditForm.price)
    }
    const res = await updateProductSku(sku.id, payload)
    Object.assign(sku, res.data?.data || payload)
  } catch (error) {
    notice.value = 'SKU 名称更新失败'
    return
  }

  try {
    const inventoryRes = await setInventoryStock(sku.id, Number(skuEditForm.stock || 0))
    sku.stock = inventoryRes.data?.data?.totalStock ?? Number(skuEditForm.stock || 0)
  } catch (error) {
    notice.value = error.response?.data?.message || '库存更新失败'
    return
  }

  notice.value = 'SKU 规格信息已更新'
  cancelSkuEdit()
  await loadProducts()
}

async function changeSale(product, onSale) {
  product.status = onSale ? 'ON_SALE' : 'OFF_SALE'
  try {
    if (onSale) await onSaleProduct(product.id)
    else await offSaleProduct(product.id)
  } catch (error) {}
}

async function archive(product) {
  if (product.status !== 'OFF_SALE') {
    notice.value = '商品必须先下架再归档'
    return
  }
  try {
    await archiveProduct(product.id)
    product.status = 'ARCHIVED'
    productScope.value = 'ACTIVE'
    notice.value = '商品已归档，并从商品管理中移除'
  } catch (error) {
    notice.value = error.response?.data?.message || '商品归档失败'
  }
}

async function removeArchived(product) {
  if (product.status !== 'ARCHIVED') {
    notice.value = '只有归档商品可以删除'
    return
  }
  try {
    await deleteProduct(product.id)
    products.value = products.value.filter(item => item.id !== product.id)
    if (skuProduct.value?.id === product.id) clearSkuPanel()
    notice.value = '归档商品已删除'
  } catch (error) {
    notice.value = error.response?.data?.message || '商品删除失败'
  }
}

async function restore(product) {
  if (product.status !== 'ARCHIVED') {
    notice.value = '只有归档商品可以恢复'
    return
  }
  try {
    const res = await restoreProduct(product.id)
    Object.assign(product, res.data?.data || { status: 'OFF_SALE' })
    productScope.value = 'ACTIVE'
    notice.value = '商品已恢复为下架状态，可检查后再上架'
  } catch (error) {
    notice.value = error.response?.data?.message || '商品恢复失败'
  }
}

async function hydrateSkuInfo(rows) {
  return Promise.all(rows.map(async product => {
    try {
      const skuRes = await getProductSkus(product.id)
      const sku = Array.isArray(skuRes.data?.data) ? skuRes.data.data[0] : null
      if (!sku) return product
      const inventory = await getSkuStock(sku.id)
      return { ...product, skuName: sku.skuName, price: sku.price, originalPrice: sku.originalPrice, stock: inventory }
    } catch (error) {
      return product
    }
  }))
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

async function getSkuStock(skuId) {
  try {
    const res = await getInventory(skuId)
    return res.data?.data?.totalStock ?? null
  } catch (error) {
    return null
  }
}

async function hydrateSkuStock(rows) {
  return Promise.all(rows.map(async sku => ({ ...sku, stock: await getSkuStock(sku.id) })))
}

async function saveDefaultSkuStock(productId, stock) {
  const res = await getProductSkus(productId)
  const sku = Array.isArray(res.data?.data) ? res.data.data[0] : null
  if (sku?.id) await setInventoryStock(sku.id, stock)
}

onMounted(loadProducts)
</script>

<style scoped>
.merchant-products { display: grid; gap: 20px; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.scope-tabs { display: flex; gap: 8px; width: fit-content; padding: 4px; background: #fff; border: 1px solid #dfe7e2; border-radius: 8px; }
.scope-tabs button { min-height: 34px; border: 0; border-radius: 6px; background: transparent; color: #475569; padding: 0 14px; font-weight: 900; cursor: pointer; }
.scope-tabs button.active { background: #14532d; color: #fff; }
.eyebrow { margin: 0 0 6px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: 0; font-weight: 800; }
h2, h3 { margin: 0; }
h2 { font-size: 28px; line-height: 1.2; color: #111827; }
button { font-family: inherit; }
.primary-btn { min-height: 36px; border: 0; border-radius: 6px; background: #14532d; color: #fff; padding: 0 14px; font-weight: 900; cursor: pointer; }
.text-btn, .ghost-btn { min-height: 36px; border: 0; border-radius: 6px; background: #e7f2eb; color: #14532d; padding: 0 12px; font-weight: 900; cursor: pointer; }
.text-btn { display: inline-flex; align-items: center; gap: 6px; }
.back-icon { font-size: 18px; line-height: 1; transform: translateY(-1px); }
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
.panel-section { display: grid; gap: 13px; }
.panel-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.close-btn { width: 32px; height: 32px; border: 0; border-radius: 999px; background: #eef2f7; color: #475569; font-size: 20px; line-height: 1; cursor: pointer; }
.panel-empty { padding: 18px; border: 1px dashed #d7dde8; border-radius: 8px; color: #64748b; text-align: center; }
.form-panel label { display: grid; gap: 7px; color: #475569; font-size: 14px; font-weight: 800; }
.form-panel input, .form-panel textarea { width: 100%; border: 1px solid #d7dde8; border-radius: 6px; padding: 11px 12px; outline: none; color: #111827; transition: border-color .2s, box-shadow .2s; }
.form-panel input:focus, .form-panel textarea:focus { border-color: #14532d; box-shadow: 0 0 0 3px rgba(20, 83, 45, .1); }
.form-panel textarea { min-height: 98px; resize: vertical; }
.image-url-row { display: grid; grid-template-columns: minmax(0, 1fr) 92px; gap: 8px; }
.upload-btn { min-height: 42px; border: 0; border-radius: 6px; background: #e7f2eb; color: #14532d; padding: 0 10px; font-weight: 900; cursor: pointer; white-space: nowrap; }
.upload-btn:disabled { cursor: not-allowed; opacity: .65; }
.file-input { display: none; }
.image-preview { width: 100%; aspect-ratio: 4 / 3; overflow: hidden; border: 1px solid #d7dde8; border-radius: 8px; background: #eef2f7; }
.image-preview img { width: 100%; height: 100%; object-fit: cover; display: block; }
.submit-btn { min-height: 44px; background: #14532d; color: #fff; }
.sku-panel { display: grid; gap: 10px; padding-top: 12px; border-top: 1px solid #e3e8ef; }
.sku-head, .sku-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.sku-head strong { color: #111827; line-height: 1.4; }
.sku-list { display: grid; gap: 8px; }
.sku-row { padding: 10px; background: #f8fafc; border: 1px solid #eef2f7; border-radius: 6px; }
.sku-row div { display: grid; gap: 4px; min-width: 0; }
.sku-row strong { color: #111827; overflow-wrap: anywhere; }
.sku-row span, .sku-row small, .sku-empty { color: #64748b; font-size: 13px; }
.sku-row input { min-width: 0; flex: 1; }
.sku-row button { min-height: 32px; border: 0; border-radius: 6px; padding: 0 10px; background: #e7f2eb; color: #14532d; font-weight: 900; cursor: pointer; }
.sku-edit-form { width: 100%; display: grid; gap: 10px; }
.sku-edit-form label { display: grid; gap: 6px; color: #475569; font-size: 13px; font-weight: 800; }
.sku-edit-form input { width: 100%; border: 1px solid #d7dde8; border-radius: 6px; padding: 10px 11px; outline: none; color: #111827; }
.sku-edit-form input:focus { border-color: #14532d; box-shadow: 0 0 0 3px rgba(20, 83, 45, .1); }
.sku-edit-actions { display: flex; justify-content: flex-end; gap: 8px; }
.notice { color: #8a5a00; background: #fff4cc; padding: 10px 12px; border-radius: 6px; line-height: 1.5; }
.state-panel { padding: 28px; color: #64748b; }
@media (max-width: 1060px) { .product-layout { grid-template-columns: 1fr; } .form-panel { position: static; } }
@media (max-width: 760px) { .page-head { align-items: stretch; flex-direction: column; } .head-actions { align-self: stretch; } .head-actions button { flex: 1; } .table-head { display: none; } .product-row { grid-template-columns: 1fr; } .row-actions { justify-content: flex-start; } }
</style>
