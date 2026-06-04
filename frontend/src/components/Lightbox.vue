<template>
  <div class="lightbox-overlay" @click.self="$emit('close')" @mouseup="onMouseUp">
    <div class="lightbox-container">
      <div class="lightbox-header">
        <span class="lightbox-title">{{ photo?.fileName }}</span>
        <button class="btn btn-icon lightbox-close" @click="$emit('close')">&times;</button>
      </div>
      <div class="lightbox-body">
        <button class="lightbox-nav lightbox-prev" :disabled="!hasPrev" @click="resetZoom(); $emit('prev')">&#8249;</button>
        <div class="lightbox-image-wrapper"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseleave="onMouseUp">
          <img ref="imgRef"
            :src="`/api/photos/${photo?.id}/file`"
            class="lightbox-image"
            :class="{ zoomed: isZoomed, dragging: isDragging }"
            :style="imageStyle"
            @click="onImageClick"
            draggable="false">
          <div v-if="isZoomed" class="zoom-hint">拖拽平移</div>
        </div>
        <button class="lightbox-nav lightbox-next" :disabled="!hasNext" @click="resetZoom(); $emit('next')">&#8250;</button>
      </div>
      <div class="lightbox-footer">
        <div class="lightbox-actions">
          <button class="btn btn-selected" :class="{ active: photo?.status === 'selected' }"
            @click="$emit('mark', photo, 'selected')">&#9733; 精选</button>
          <button class="btn btn-rejected" :class="{ active: photo?.status === 'rejected' }"
            @click="$emit('mark', photo, 'rejected')">&#10005; 落选</button>
          <button class="btn btn-unmark" @click="$emit('mark', photo, 'unmarked')">&#8634; 重置</button>
          <button class="btn btn-zoom" @click="toggleZoom()">
            {{ zoomLabel }}
          </button>
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
import { ref, computed, watch } from 'vue'

const props = defineProps({ photo: Object, exif: Object, hasPrev: Boolean, hasNext: Boolean })
const emit = defineEmits(['prev', 'next', 'mark', 'close'])

/** 缩放档位：原图 → 2x → 4x → 8x，循环 */
const ZOOM_LEVELS = [1, 2, 4, 8]

const zoomLevel = ref(0)          // ZOOM_LEVELS 的索引，0 = 原图
const isDragging = ref(false)
const wasDragged = ref(false)     // 区分点击和拖拽
const dragStart = ref({ x: 0, y: 0 })
const offset = ref({ x: 0, y: 0 })
const lastOffset = ref({ x: 0, y: 0 })

// 切换照片时重置缩放
watch(() => props.photo, () => resetZoom())

const isZoomed = computed(() => zoomLevel.value > 0)

function onImageClick() {
  // 拖拽后不触发放大/缩小
  if (wasDragged.value) {
    wasDragged.value = false
    return
  }
  // 循环下一档
  zoomLevel.value = (zoomLevel.value + 1) % ZOOM_LEVELS.length
  if (!isZoomed.value) {
    offset.value = { x: 0, y: 0 }
    lastOffset.value = { x: 0, y: 0 }
  }
}

function toggleZoom() {
  if (isZoomed.value) {
    zoomLevel.value = 0
    offset.value = { x: 0, y: 0 }
    lastOffset.value = { x: 0, y: 0 }
  } else {
    zoomLevel.value = 1
  }
}

function resetZoom() {
  zoomLevel.value = 0
  isDragging.value = false
  wasDragged.value = false
  offset.value = { x: 0, y: 0 }
  lastOffset.value = { x: 0, y: 0 }
}

function onMouseDown(e) {
  if (!isZoomed.value) return
  isDragging.value = true
  wasDragged.value = false
  dragStart.value = { x: e.clientX, y: e.clientY }
  lastOffset.value = { ...offset.value }
}

function onMouseMove(e) {
  if (!isDragging.value) return
  // 移动超过 3px 才算拖拽（防手抖）
  const dx = e.clientX - dragStart.value.x
  const dy = e.clientY - dragStart.value.y
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
    wasDragged.value = true
  }
  offset.value = {
    x: lastOffset.value.x + dx,
    y: lastOffset.value.y + dy
  }
}

function onMouseUp() {
  isDragging.value = false
}

const zoomScale = computed(() => ZOOM_LEVELS[zoomLevel.value])

const zoomLabel = computed(() => {
  if (!isZoomed.value) return '+ 放大'
  return `×${zoomScale.value} ${zoomLevel.value === ZOOM_LEVELS.length - 1 ? '缩小' : '放大'}`
})

const imageStyle = computed(() => {
  if (!isZoomed.value) return {}
  return {
    transform: `translate(${offset.value.x}px, ${offset.value.y}px) scale(${zoomScale.value})`,
    cursor: isDragging.value ? 'grabbing' : 'grab'
  }
})

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>
