<template>
  <TopBar
    :warehouses="warehouses"
    :activeId="activeId"
    :loading="loading"
    @switch="onSwitchWarehouse"
    @settings="showSettings = true"
    @rescan="onRescan"
  />
  <FilterBar
    :statusFilter="statusFilter"
    :sortBy="sortBy"
    :sortOrder="sortOrder"
    :total="total"
    :selected="selected"
    :rejected="rejected"
    :unmarked="unmarked"
    @change-filter="onChangeFilter"
    @change-sort="onChangeSort"
  />
  <PhotoGrid
    :photos="displayPhotos"
    :loading="loading"
    @open="onOpenLightbox"
  />
  <StatusBar
    :total="total"
    :selected="selected"
    :rejected="rejected"
    :unmarked="unmarked"
  />
  <Lightbox
    v-if="showLightbox"
    :photo="lightboxPhoto"
    :exif="lightboxExif"
    :hasPrev="lightboxIndex > 0"
    :hasNext="lightboxIndex < displayPhotos.length - 1"
    @prev="onLightboxPrev"
    @next="onLightboxNext"
    @mark="onMark"
    @close="showLightbox = false"
  />
  <SettingsModal
    v-if="showSettings"
    :warehouses="warehouses"
    :activeId="activeId"
    @switch="onSwitchWarehouse"
    @add="onAddWarehouse"
    @remove="onRemoveWarehouse"
    @close="showSettings = false"
  />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as api from './api/index.js'
import TopBar from './components/TopBar.vue'
import FilterBar from './components/FilterBar.vue'
import PhotoGrid from './components/PhotoGrid.vue'
import StatusBar from './components/StatusBar.vue'
import Lightbox from './components/Lightbox.vue'
import SettingsModal from './components/SettingsModal.vue'

const warehouses = ref([])
const activeId = ref('')
const photos = ref([])
const total = ref(0)
const selected = ref(0)
const rejected = ref(0)
const unmarked = ref(0)
const loading = ref(false)
const showSettings = ref(false)
const showLightbox = ref(false)
const lightboxPhoto = ref(null)
const lightboxExif = ref(null)
const lightboxIndex = ref(-1)
const statusFilter = ref('all')
const sortBy = ref('name')
const sortOrder = ref('asc')

function getColumnCount() {
  const w = window.innerWidth
  if (w > 1600) return 6
  if (w > 1200) return 5
  if (w > 900) return 4
  if (w > 600) return 3
  return 2
}

function reorderForColumns(arr, cols) {
  if (cols <= 1 || arr.length <= cols) return arr
  const rows = Math.ceil(arr.length / cols)
  const total = cols * rows
  const result = new Array(total)
  for (let c = 0; c < cols; c++) {
    for (let r = 0; r < rows; r++) {
      const oldIdx = r * cols + c
      if (oldIdx < arr.length) {
        result[c * rows + r] = arr[oldIdx]
      }
    }
  }
  return result.filter(item => item !== undefined)
}

const columnCount = ref(getColumnCount())

function onResize() {
  columnCount.value = getColumnCount()
}

onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const displayPhotos = computed(() =>
  reorderForColumns(photos.value, columnCount.value)
)

async function fetchWarehouses() {
  warehouses.value = await api.getWarehouses()
  const active = warehouses.value.find(w => w.active)
  activeId.value = active ? active.id : ''
  if (activeId.value) {
    await loadPhotos()
  }
}

async function onSwitchWarehouse(id) {
  await api.switchWarehouse(id)
  await fetchWarehouses()
}

async function onAddWarehouse(name, path) {
  await api.addWarehouse(name, path)
  await fetchWarehouses()
}

async function onRemoveWarehouse(id) {
  await api.removeWarehouse(id)
  await fetchWarehouses()
}

async function onRescan() {
  loading.value = true
  try {
    await api.rescan()
    await loadPhotos()
  } finally {
    loading.value = false
  }
}

async function loadPhotos() {
  try {
    const data = await api.getPhotos({
      status: statusFilter.value,
      sort: sortBy.value,
      order: sortOrder.value,
      page: 1,
      size: 60
    })
    photos.value = data.rows || []
    total.value = data.total || 0
    selected.value = data.selected || 0
    rejected.value = data.rejected || 0
    unmarked.value = data.unmarked || 0
  } catch (e) {
    console.error('Failed to load photos', e)
  }
}

function onChangeFilter(status) {
  statusFilter.value = status
  loadPhotos()
}

function onChangeSort(sort) {
  if (sortBy.value === sort) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = sort
    sortOrder.value = 'asc'
  }
  loadPhotos()
}

async function onOpenLightbox(photo, index) {
  lightboxIndex.value = index
  lightboxPhoto.value = photo
  lightboxExif.value = null
  showLightbox.value = true
  try {
    lightboxExif.value = await api.getPhoto(photo.id)
  } catch (e) {
    console.error('Failed to load EXIF', e)
  }
}

function onLightboxPrev() {
  if (lightboxIndex.value > 0) {
    onOpenLightbox(displayPhotos.value[lightboxIndex.value - 1], lightboxIndex.value - 1)
  }
}

function onLightboxNext() {
  if (lightboxIndex.value < displayPhotos.value.length - 1) {
    onOpenLightbox(displayPhotos.value[lightboxIndex.value + 1], lightboxIndex.value + 1)
  }
}

async function onMark(photo, status) {
  try {
    await api.markPhoto(photo.id, status)
    photo.status = status
    // 当前有状态过滤且照片新状态与过滤条件不符 → 从列表移除
    if (statusFilter.value !== 'all' && photo.status !== statusFilter.value) {
      photos.value = photos.value.filter(p => p.id !== photo.id)
      showLightbox.value = false
    }
    const stats = await api.getStatistics()
    selected.value = stats.selected || 0
    rejected.value = stats.rejected || 0
    unmarked.value = stats.unmarked || 0
  } catch (e) {
    console.error('Failed to mark photo', e)
  }
}

onMounted(fetchWarehouses)
</script>
