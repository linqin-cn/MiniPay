<template>
  <section class="catalog-page">
    <header class="catalog-header">
      <div>
        <p class="eyebrow">MiniPay marketplace</p>
        <h2>商品列表</h2>
      </div>
      <div class="search-box">
        <input v-model.trim="keyword" placeholder="搜索商品、分类或店铺" @keyup.enter="loadProducts" />
        <button @click="loadProducts">搜索</button>
      </div>
    </header>

    <div class="status-row">
      <span>共 {{ filteredProducts.length }} 件商品</span>
      <span v-if="usingDemo" class="demo-badge">演示数据</span>
    </div>

    <div v-if="loading" class="state-panel">正在加载商品...</div>
    <div v-else-if="filteredProducts.length === 0" class="state-panel">没有找到匹配商品</div>

    <div v-else class="product-grid">
      <article v-for="product in filteredProducts" :key="product.id" class="product-card" @click="goDetail(product.id)">
        <div class="image-wrap">
          <img :src="product.mainImage" :alt="product.title" />
        </div>
        <div class="product-body">
          <div class="meta-line">
            <span>{{ product.categoryName || '精选商品' }}</span>
            <span>{{ product.status || 'ON_SALE' }}</span>
          </div>
          <h3>{{ product.title }}</h3>
          <p>{{ product.description }}</p>
          <div class="tag-row">
            <span v-for="tag in product.tags || []" :key="tag">{{ tag }}</span>
          </div>
          <div class="card-footer">
            <strong>¥{{ money(getLowestPrice(product.id)) }}</strong>
            <button @click.stop="goDetail(product.id)">查看详情</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProducts } from '@/api'
import { demoProducts, getDemoSkus, money } from '@/data/demoCatalog'

const router = useRouter()
const keyword = ref('')
const products = ref([])
const loading = ref(false)
const usingDemo = ref(false)

const filteredProducts = computed(() => {
  const text = keyword.value.toLowerCase()
  if (!text) return products.value
  return products.value.filter(item => {
    return [item.title, item.description, item.categoryName, item.merchantName]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(text))
  })
})

function normalizeProducts(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.list)) return data.list
  return []
}

async function loadProducts() {
  loading.value = true
  try {
    const res = await getProducts({ keyword: keyword.value })
    const list = normalizeProducts(res.data?.data)
    products.value = list.length ? list : demoProducts
    usingDemo.value = list.length === 0
  } catch (error) {
    products.value = demoProducts
    usingDemo.value = true
  } finally {
    loading.value = false
  }
}

function getLowestPrice(productId) {
  const skus = getDemoSkus(productId)
  if (!skus.length) return 0
  return Math.min(...skus.map(item => Number(item.price || 0)))
}

function goDetail(id) {
  router.push(`/products/${id}`)
}

onMounted(loadProducts)
</script>

<style scoped>
.catalog-page { display: grid; gap: 18px; }
.catalog-header { display: flex; align-items: end; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2 { margin: 0; font-size: 28px; color: #111827; }
.search-box { display: flex; width: min(460px, 100%); border: 1px solid #d7dde8; border-radius: 6px; overflow: hidden; background: #fff; }
.search-box input { flex: 1; border: 0; padding: 12px 14px; font-size: 14px; outline: none; }
.search-box button, .card-footer button { border: 0; background: #14532d; color: #fff; padding: 0 18px; font-weight: 600; cursor: pointer; }
.status-row { display: flex; justify-content: space-between; color: #64748b; font-size: 14px; }
.demo-badge { color: #8a5a00; background: #fff4cc; padding: 3px 8px; border-radius: 999px; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.product-card { background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; overflow: hidden; cursor: pointer; transition: border-color .2s, transform .2s, box-shadow .2s; }
.product-card:hover { transform: translateY(-2px); border-color: #9bb8a8; box-shadow: 0 10px 24px rgba(15, 23, 42, .08); }
.image-wrap { aspect-ratio: 4 / 3; background: #eef2f7; overflow: hidden; }
.image-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.product-body { display: grid; gap: 10px; padding: 14px; }
.meta-line, .card-footer { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.meta-line { color: #64748b; font-size: 12px; }
h3 { margin: 0; color: #111827; font-size: 17px; line-height: 1.35; }
p { margin: 0; color: #64748b; line-height: 1.5; font-size: 13px; min-height: 40px; }
.tag-row { display: flex; flex-wrap: wrap; gap: 6px; min-height: 24px; }
.tag-row span { background: #edf6f0; color: #166534; font-size: 12px; padding: 3px 7px; border-radius: 999px; }
.card-footer strong { color: #b42318; font-size: 20px; }
.card-footer button { height: 34px; border-radius: 6px; }
.state-panel { padding: 28px; background: #fff; border: 1px solid #e3e8ef; border-radius: 8px; color: #64748b; }
@media (max-width: 760px) { .catalog-header { align-items: stretch; flex-direction: column; } }
</style>
