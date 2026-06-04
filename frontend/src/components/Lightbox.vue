<template>
  <div class="lightbox-overlay" @click.self="$emit('close')">
    <div class="lightbox-container">
      <div class="lightbox-header">
        <span class="lightbox-title">{{ photo?.fileName }}</span>
        <button class="btn btn-icon lightbox-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="lightbox-body">
        <button class="lightbox-nav lightbox-prev" :disabled="!hasPrev" @click="$emit('prev')">&#8249;</button>
        <div class="lightbox-image-wrapper">
          <img :src="`/api/photos/${photo?.id}/file`" class="lightbox-image">
        </div>
        <button class="lightbox-nav lightbox-next" :disabled="!hasNext" @click="$emit('next')">&#8250;</button>
      </div>
      <div class="lightbox-footer">
        <div class="lightbox-actions">
          <button class="btn btn-selected" :class="{ active: photo?.status === 'selected' }"
            @click="$emit('mark', photo, 'selected')">&#9733; 精选</button>
          <button class="btn btn-rejected" :class="{ active: photo?.status === 'rejected' }"
            @click="$emit('mark', photo, 'rejected')">&#10005; 落选</button>
          <button class="btn btn-unmark" @click="$emit('mark', photo, 'unmarked')">&#8634; 重置</button>
        </div>
        <div class="lightbox-exif" v-if="exif">
          <table>
            <tr v-if="exif.cameraModel"><td>相机</td><td>{{ exif.cameraModel }}</td></tr>
            <tr v-if="exif.fNumber"><td>光圈</td><td>{{ exif.fNumber }}</td></tr>
            <tr v-if="exif.shutterSpeed"><td>快门</td><td>{{ exif.shutterSpeed }}</td></tr>
            <tr v-if="exif.isoSpeed"><td>ISO</td><td>{{ exif.isoSpeed }}</td></tr>
            <tr v-if="exif.focalLength"><td>焦距</td><td>{{ exif.focalLength }}</td></tr>
            <tr v-if="exif.flash"><td>闪光灯</td><td>{{ exif.flash }}</td></tr>
            <tr v-if="exif.lenInfo"><td>镜头</td><td>{{ exif.lenInfo }}</td></tr>
            <tr v-if="exif.dateTaken"><td>拍摄时间</td><td>{{ exif.dateTaken }}</td></tr>
            <tr v-if="photo?.fileSize"><td>文件大小</td><td>{{ formatSize(photo.fileSize) }}</td></tr>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ photo: Object, exif: Object, hasPrev: Boolean, hasNext: Boolean })
defineEmits(['prev', 'next', 'mark', 'close'])
function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>
