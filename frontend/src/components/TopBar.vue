<template>
  <header class="top-bar">
    <div class="top-bar-inner">
      <h1 class="logo">摄影选图</h1>
      <div class="top-bar-controls">
        <select class="warehouse-select" :value="activeId" @change="onSwitch">
          <option value="" disabled>选择仓库</option>
          <option v-for="w in warehouses" :key="w.id" :value="w.id">
            {{ w.name }} ({{ w.path }})
          </option>
        </select>
        <button class="btn btn-icon" title="仓库设置" @click="$emit('settings')">&#9881;</button>
        <button class="btn btn-primary" :disabled="loading" @click="$emit('rescan')">
          {{ loading ? '扫描中...' : '刷新' }}
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
defineProps({ warehouses: Array, activeId: String, loading: Boolean })
const emit = defineEmits(['switch', 'settings', 'rescan'])
function onSwitch(e) { emit('switch', e.target.value) }
</script>
