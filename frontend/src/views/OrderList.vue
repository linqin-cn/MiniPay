<template>
  <section class="page">
    <h2>我的订单</h2>
    <div class="placeholder">TODO: 展示订单列表、状态筛选、取消订单、确认收货等操作。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { getOrderList } from '@/api'

onMounted(async () => {
  await getOrderList()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
