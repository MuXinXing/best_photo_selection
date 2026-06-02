<template>
  <footer class="status-bar" v-if="total > 0">
    <span>共 <strong>{{ total }}</strong> 张</span>
    <span class="status-sep">&middot;</span>
    <span class="status-selected">已选 <strong>{{ selected }}</strong> 张</span>
    <span class="status-sep">&middot;</span>
    <span class="status-rejected">落选 <strong>{{ rejected }}</strong> 张</span>
    <span class="status-sep">&middot;</span>
    <span class="status-unmarked">待筛 <strong>{{ unmarked }}</strong> 张</span>
  </footer>
</template>

<script setup>
defineProps({ total: Number, selected: Number, rejected: Number, unmarked: Number })
</script>
