<template>
  <div class="filter-bar">
    <div class="filter-tabs">
      <button v-for="tab in tabs" :key="tab.key"
        class="filter-tab" :class="{ active: statusFilter === tab.key }"
        @click="$emit('changeFilter', tab.key)">
        {{ tab.label }}
        <span class="filter-count">{{ counts[tab.key] }}</span>
      </button>
    </div>
    <div class="filter-sorts">
      <button v-for="s in sorts" :key="s.key"
        class="sort-btn" :class="{ active: sortBy === s.key }"
        @click="$emit('changeSort', s.key)">
        {{ s.label }}
        <span v-if="sortBy === s.key" class="sort-arrow">{{ sortOrder === 'asc' ? '↑' : '↓' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  statusFilter: String, sortBy: String, sortOrder: String,
  total: Number, selected: Number, rejected: Number, unmarked: Number
})
defineEmits(['changeFilter', 'changeSort'])
const tabs = [
  { key: 'all', label: '全部' },
  { key: 'selected', label: '精选' },
  { key: 'unmarked', label: '未筛' },
  { key: 'rejected', label: '落选' }
]
const sorts = [
  { key: 'name', label: '文件名' },
  { key: 'date', label: '拍摄日期' },
  { key: 'status', label: '状态' }
]
const counts = computed(() => ({
  all: props.total, selected: props.selected,
  unmarked: props.unmarked, rejected: props.rejected
}))
</script>
