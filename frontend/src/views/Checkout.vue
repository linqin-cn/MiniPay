<template>
  <section class="page">
    <h2>确认订单</h2>
    <div class="placeholder">TODO: 展示地址、商品明细、优惠、运费、应付金额和提交订单。</div>
  </section>
</template>

<script setup>
import { confirmOrder, createTradeOrder } from '@/api'

async function previewOrder(data) {
  await confirmOrder(data)
}

async function submitOrder(data) {
  await createTradeOrder(data)
}
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
