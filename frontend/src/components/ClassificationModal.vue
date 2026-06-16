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
  background: #33312e;
  border-radius: 12px;
  padding: 32px 40px;
  width: 400px;
  max-width: 90vw;
  text-align: center;
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(69, 67, 64, 0.5);
}

.classify-header h2 {
  font-family: 'Crimson Pro', 'Georgia', 'Songti SC', 'STSong', serif;
  font-size: 20px;
  font-weight: 500;
  color: #b8956a;
  margin-bottom: 20px;
}

.classify-body p {
  color: #8c8882;
  font-size: 14px;
  font-family: 'DM Sans', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  margin-bottom: 16px;
}

.progress-bar-track {
  width: 100%;
  height: 6px;
  background: #3d3b38;
  border-radius: 3px;
  overflow: hidden;
  margin-bottom: 10px;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #b8956a, #c9a87c);
  border-radius: 3px;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-text {
  font-size: 13px;
  font-family: 'DM Sans', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #b8956a;
  font-weight: 500;
}
</style>
