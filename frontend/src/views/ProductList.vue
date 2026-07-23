<template>
  <section class="page">
    <h2>商品列表</h2>
    <div class="toolbar">
      <input v-model="keyword" placeholder="搜索商品" />
      <button @click="loadProducts">搜索</button>
    </div>
    <div class="placeholder">TODO: 展示商品分页、分类筛选、商品卡片和进入详情操作。</div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { getProducts } from '@/api'

const keyword = ref('')

async function loadProducts() {
  await getProducts({ keyword: keyword.value })
}
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.toolbar { display: flex; gap: 8px; }
.placeholder { padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 6px; }
</style>
