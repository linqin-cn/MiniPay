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

    <section class="promo-carousel" :style="{ background: activeBanner.bg }">
      <button class="slide-hit" type="button" @click="goDetail(activeBanner.productId)" aria-label="查看轮播商品"></button>
      <div class="banner-copy">
        <span>{{ activeBanner.kicker }}</span>
        <h1>{{ activeBanner.title }}</h1>
        <p>{{ activeBanner.subtitle }}</p>
      </div>
      <div class="banner-media">
        <img :src="activeBanner.image" :alt="activeBanner.title" />
      </div>
      <div class="carousel-dots" aria-label="轮播图切换">
        <button
          v-for="(banner, index) in banners"
          :key="banner.title"
          type="button"
          :class="{ active: index === activeBannerIndex }"
          :aria-label="`切换到第 ${index + 1} 张轮播图`"
          @click="setBanner(index)"
        ></button>
      </div>
      <button class="prev-btn" type="button" aria-label="上一张轮播图" @click="prevBanner">‹</button>
      <button class="next-btn" type="button" aria-label="下一张轮播图" @click="nextBanner">›</button>
    </section>

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
            <strong>¥{{ money(getLowestPrice(product)) }}</strong>
            <button @click.stop="goDetail(product.id)">查看详情</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getProducts, getProductSkus } from '@/api'
import { demoProducts, getDemoSkus, money } from '@/data/demoCatalog'

const router = useRouter()
const keyword = ref('')
const products = ref([])
const loading = ref(false)
const usingDemo = ref(false)
const skuPriceMap = ref({})
const activeBannerIndex = ref(0)
let carouselTimer = null

const banners = [
  {
    productId: 1001,
    kicker: '限时直降',
    title: 'MiniPods Pro\n好物立刻抢购',
    subtitle: '通勤降噪耳机，到手价更香',
    image: 'https://images.unsplash.com/photo-1606220588913-b3aacb4d2f46?auto=format&fit=crop&w=700&q=80',
    bg: 'linear-gradient(135deg, #f8003b 0%, #f70435 54%, #c40028 100%)'
  },
  {
    productId: 1002,
    kicker: '办公焕新',
    title: '机械键盘\n效率手感一起升级',
    subtitle: '三模连接，桌面主力装备',
    image: 'https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?auto=format&fit=crop&w=700&q=80',
    bg: 'linear-gradient(135deg, #123524 0%, #166534 58%, #0f2f24 100%)'
  },
  {
    productId: 1003,
    kicker: '智能穿戴',
    title: 'Pulse Watch\n运动健康随时看',
    subtitle: '消息提醒、防水运动、长续航',
    image: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=700&q=80',
    bg: 'linear-gradient(135deg, #0f172a 0%, #2563eb 58%, #111827 100%)'
  }
]

const activeBanner = computed(() => banners[activeBannerIndex.value])

const filteredProducts = computed(() => {
  const text = keyword.value.toLowerCase()
  const onSaleProducts = products.value.filter(item => (item.status || 'ON_SALE') === 'ON_SALE')
  if (!text) return onSaleProducts
  return onSaleProducts.filter(item => {
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
    const res = await getProducts({ keyword: keyword.value, status: 'ON_SALE' })
    const list = normalizeProducts(res.data?.data)
    products.value = list.length ? list : demoProducts
    usingDemo.value = list.length === 0
    skuPriceMap.value = list.length ? await loadSkuPrices(list) : {}
  } catch (error) {
    products.value = demoProducts
    skuPriceMap.value = {}
    usingDemo.value = true
  } finally {
    loading.value = false
  }
}

async function loadSkuPrices(list) {
  const entries = await Promise.all(list.map(async product => {
    try {
      const res = await getProductSkus(product.id)
      const skus = Array.isArray(res.data?.data) ? res.data.data : []
      const prices = skus.map(item => Number(item.price || 0)).filter(price => price > 0)
      return [product.id, prices.length ? Math.min(...prices) : null]
    } catch (error) {
      return [product.id, null]
    }
  }))
  return Object.fromEntries(entries.filter(([, price]) => price != null))
}

function getLowestPrice(product) {
  if (skuPriceMap.value[product.id] != null) return skuPriceMap.value[product.id]
  if (product.price != null) return product.price
  const skus = getDemoSkus(product.id)
  if (!skus.length) return 0
  return Math.min(...skus.map(item => Number(item.price || 0)))
}

function goDetail(id) {
  router.push(`/products/${id}`)
}

function setBanner(index) {
  activeBannerIndex.value = index
  restartCarousel()
}

function nextBanner() {
  activeBannerIndex.value = (activeBannerIndex.value + 1) % banners.length
  restartCarousel()
}

function prevBanner() {
  activeBannerIndex.value = (activeBannerIndex.value - 1 + banners.length) % banners.length
  restartCarousel()
}

function startCarousel() {
  carouselTimer = window.setInterval(() => {
    activeBannerIndex.value = (activeBannerIndex.value + 1) % banners.length
  }, 4500)
}

function restartCarousel() {
  if (carouselTimer) window.clearInterval(carouselTimer)
  startCarousel()
}

onMounted(() => {
  loadProducts()
  startCarousel()
})

onBeforeUnmount(() => {
  if (carouselTimer) window.clearInterval(carouselTimer)
})
</script>

<style scoped>
.catalog-page { display: grid; gap: 18px; }
.catalog-header { display: flex; align-items: end; justify-content: space-between; gap: 20px; }
.eyebrow { margin: 0 0 4px; color: #64748b; font-size: 12px; text-transform: uppercase; letter-spacing: .08em; }
h2 { margin: 0; font-size: 28px; color: #111827; }
.promo-carousel { position: relative; min-height: 240px; border-radius: 10px; overflow: hidden; display: grid; grid-template-columns: minmax(0, 1fr) 320px; align-items: center; padding: 28px 70px 28px 28px; color: #fff; box-shadow: 0 16px 34px rgba(17, 24, 39, .12); }
.slide-hit { position: absolute; inset: 0; border: 0; background: transparent; cursor: pointer; z-index: 1; }
.banner-copy { position: relative; z-index: 2; display: grid; gap: 8px; align-content: center; max-width: 460px; }
.banner-copy span { width: fit-content; padding: 4px 9px; border-radius: 999px; background: rgba(255, 255, 255, .18); font-size: 13px; font-weight: 900; }
.banner-copy h1 { margin: 0; white-space: pre-line; font-size: 34px; line-height: 1.18; font-weight: 950; color: #fff; }
.banner-copy p { margin: 0; min-height: 0; color: rgba(255, 255, 255, .9); font-size: 20px; line-height: 1.35; font-weight: 800; }
.banner-media { position: relative; z-index: 2; justify-self: center; width: 210px; height: 190px; display: grid; place-items: center; }
.banner-media::before { content: ''; position: absolute; width: 180px; height: 180px; border-radius: 999px; background: rgba(255, 255, 255, .2); filter: blur(1px); }
.banner-media img { position: relative; width: 180px; height: 180px; object-fit: contain; border-radius: 8px; filter: drop-shadow(0 18px 26px rgba(0, 0, 0, .18)); }
.carousel-dots { position: absolute; left: 24px; bottom: 20px; z-index: 3; display: flex; gap: 9px; align-items: center; }
.carousel-dots button { width: 13px; height: 13px; padding: 0; border: 0; border-radius: 999px; background: rgba(255, 255, 255, .58); cursor: pointer; transition: width .2s, background .2s; }
.carousel-dots button.active { width: 28px; background: #fff; }
.prev-btn, .next-btn { position: absolute; top: 50%; z-index: 3; width: 44px; height: 50px; transform: translateY(-50%); border: 0; background: rgba(0, 0, 0, .24); color: #fff; font-size: 34px; line-height: 1; cursor: pointer; display: grid; place-items: center; transition: background .2s; }
.prev-btn:hover, .next-btn:hover { background: rgba(0, 0, 0, .34); }
.prev-btn { left: 0; border-radius: 0 999px 999px 0; }
.next-btn { right: 0; border-radius: 999px 0 0 999px; }
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
@media (max-width: 760px) {
  .catalog-header { align-items: stretch; flex-direction: column; }
  .promo-carousel { min-height: 280px; grid-template-columns: 1fr; padding: 22px 54px 50px 20px; align-items: start; }
  .banner-copy h1 { font-size: 28px; }
  .banner-copy p { font-size: 16px; }
  .banner-media { position: absolute; right: 22px; bottom: 36px; width: 140px; height: 120px; opacity: .95; }
  .banner-media img { width: 130px; height: 120px; }
  .carousel-dots { left: 20px; bottom: 18px; }
  .prev-btn, .next-btn { width: 38px; height: 46px; font-size: 30px; }
}
@media (prefers-reduced-motion: reduce) { .carousel-dots button { transition: none; } }
</style>
