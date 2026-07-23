<template>
  <section class="page">
    <h2>商品详情</h2>
    <div class="placeholder">TODO: 展示商品图片、标题、价格、SKU 选择、数量、加入购物车和立即购买。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getProduct, getProductSkus } from '@/api'

const route = useRoute()

onMounted(async () => {
  await getProduct(route.params.id)
  await getProductSkus(route.params.id)
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
