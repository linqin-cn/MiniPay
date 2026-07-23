<template>
  <section class="page">
    <h2>购物车</h2>
    <div class="placeholder">TODO: 展示购物车商品、勾选、修改数量、删除和结算。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { getCart } from '@/api'

onMounted(async () => {
  await getCart()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
