<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h2>仓库设置</h2>
        <button class="btn btn-icon" @click="$emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <div class="warehouse-list">
          <h3>已配置仓库</h3>
          <div v-if="warehouses.length === 0" class="empty-hint">暂无仓库，请添加</div>
          <div v-for="w in warehouses" :key="w.id" class="warehouse-item">
            <div class="warehouse-item-info">
              <span class="warehouse-name">{{ w.name }}</span>
              <span class="warehouse-path">{{ w.path }}</span>
            </div>
            <div class="warehouse-item-actions">
              <span v-if="w.id === activeId" class="badge-active">当前</span>
              <button v-else class="btn btn-sm" @click="$emit('switch', w.id)">切换</button>
              <button class="btn btn-sm btn-danger" @click="onRemove(w.id)">删除</button>
            </div>
          </div>
        </div>
        <div class="warehouse-add">
          <h3>添加仓库</h3>
          <div class="form-row">
            <input class="input" v-model="name" placeholder="仓库名称（如：2024年婚纱）">
          </div>
          <div class="form-row">
            <input class="input" v-model="path" placeholder="仓库路径（如：D:\PhotoBank）">
          </div>
          <button class="btn btn-primary" :disabled="!name || !path" @click="onAdd">添加</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
defineProps({ warehouses: Array, activeId: String })
const emit = defineEmits(['switch', 'add', 'remove', 'close'])
const name = ref('')
const path = ref('')
function onAdd() {
  if (name.value && path.value) {
    emit('add', name.value, path.value)
    name.value = ''
    path.value = ''
  }
}
function onRemove(id) {
  if (confirm('确定删除该仓库？')) {
    emit('remove', id)
  }
}
</script>
