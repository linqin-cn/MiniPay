<template>
  <section class="page">
    <h2>地址管理</h2>
    <div class="placeholder">TODO: 展示收货地址列表、新增、编辑、删除和设置默认地址。</div>
  </section>
</template>

<script setup>
import { onMounted } from 'vue'
import { getAddresses } from '@/api'

onMounted(async () => {
  await getAddresses()
})
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
