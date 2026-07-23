<template>
  <section class="page">
    <h2>商家订单管理</h2>
    <div class="placeholder">TODO: 展示商家订单、发货、查看物流和售后入口。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { getMerchantOrders } from '@/api'

onMounted(async () => {
  await getMerchantOrders()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
