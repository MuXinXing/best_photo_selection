<template>
  <div class="modal-overlay">
    <div class="classify-modal">
      <div class="classify-header">
        <h2>正在整理文件</h2>
      </div>
      <div class="classify-body">
        <p>{{ phase }}</p>
        <div class="progress-bar-track">
          <div class="progress-bar-fill" :style="{ width: progress + '%' }"></div>
        </div>
        <span class="progress-text">{{ progress }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ progress: Number, phase: String })
</script>

<style scoped>
.classify-modal {
  background: #1e1e1e;
  border-radius: 12px;
  padding: 32px 40px;
  width: 400px;
  max-width: 90vw;
  text-align: center;
}

.classify-header h2 {
  font-size: 18px;
  color: #fff;
  margin-bottom: 20px;
}

.classify-body p {
  color: #999;
  font-size: 14px;
  margin-bottom: 16px;
}

.progress-bar-track {
  width: 100%;
  height: 8px;
  background: #2a2a2a;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar-fill {
  height: 100%;
  background: #f0a500;
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 14px;
  color: #f0a500;
  font-weight: 500;
}
</style>
