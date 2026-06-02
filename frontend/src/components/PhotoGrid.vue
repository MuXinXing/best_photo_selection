<template>
  <div class="photo-grid-wrapper">
    <div v-if="loading" class="loading-indicator">正在扫描仓库...</div>
    <div v-else-if="photos.length === 0" class="empty-state">
      <p>暂无照片，请先选择仓库并扫描</p>
    </div>
    <div v-else class="photo-grid">
      <div v-for="(photo, idx) in photos" :key="photo.id"
        class="photo-card" @click="$emit('open', photo, idx)">
        <div class="photo-card-img-wrapper">
          <img :src="photo.thumbnailUrl" :alt="photo.fileName"
            class="photo-card-img" loading="lazy" @error="onImgError">
          <div v-if="photo.status === 'selected'" class="photo-badge selected-badge">&#9733;</div>
          <div v-else-if="photo.status === 'rejected'" class="photo-badge rejected-badge">&#10005;</div>
        </div>
        <div class="photo-card-info">
          <span class="photo-card-name" :title="photo.fileName">{{ photo.fileName }}</span>
          <span v-if="photo.dateTaken" class="photo-card-date">{{ photo.dateTaken }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ photos: Array, loading: Boolean })
defineEmits(['open'])
function onImgError(e) {
  e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23eee" width="200" height="200"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-size="14">加载失败</text></svg>'
}
</script>
