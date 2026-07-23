<template>
  <section class="page">
    <h2>商家商品管理</h2>
    <div class="placeholder">TODO: 商品发布、编辑、SKU 管理、上下架操作。</div>
  </section>
</template>

<script setup>
import { getProducts } from '@/api'

async function loadProducts() {
  await getProducts()
}
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
