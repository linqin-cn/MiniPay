<template>
  <section class="page">
    <h2>订单详情</h2>
    <div class="placeholder">TODO: 展示订单状态、商品明细、收货地址、支付信息和物流信息。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getTradeOrder, getPaymentOrderByOrderNo, getLogisticsByOrderNo } from '@/api'

const route = useRoute()

onMounted(async () => {
  await getTradeOrder(route.params.orderNo)
  await getPaymentOrderByOrderNo(route.params.orderNo)
  await getLogisticsByOrderNo(route.params.orderNo)
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
