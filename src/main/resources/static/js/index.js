const API_BASE = '/api';

const app = Vue.createApp({
    data() {
        return {
            warehouses: [],
            activeWarehouse: null,
            photos: [],
            total: 0,
            selected: 0,
            rejected: 0,
            unmarked: 0,
            page: 1,
            pageSize: 60,
            statusFilter: 'all',
            sortBy: 'name',
            sortOrder: 'asc',
            showLightbox: false,
            showSettings: false,
            lightboxIndex: 0,
            lightboxPhoto: null,
            lightboxExif: null,
            loading: false,
            newWarehouseName: '',
            newWarehousePath: '',
        };
    },
    computed: {
        lightboxPhotos() {
            return this.photos;
        },
    },
    methods: {
        async fetchWarehouses() {
            try {
                const res = await fetch(`${API_BASE}/warehouse`);
                this.warehouses = await res.json();
                this.activeWarehouse = this.warehouses.find(w => w.active) || null;
                if (this.activeWarehouse) {
                    await this.rescan();
                }
            } catch (e) {
                console.error('Failed to load warehouses', e);
            }
        },
        async switchWarehouse(id) {
            try {
                await fetch(`${API_BASE}/warehouse/${id}/active`, { method: 'PUT' });
                await this.fetchWarehouses();
            } catch (e) {
                console.error('Failed to switch warehouse', e);
            }
        },
        async addWarehouse() {
            if (!this.newWarehouseName || !this.newWarehousePath) return;
            try {
                await fetch(`${API_BASE}/warehouse`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name: this.newWarehouseName, path: this.newWarehousePath }),
                });
                this.newWarehouseName = '';
                this.newWarehousePath = '';
                await this.fetchWarehouses();
            } catch (e) {
                console.error('Failed to add warehouse', e);
            }
        },
        async removeWarehouse(id) {
            try {
                await fetch(`${API_BASE}/warehouse/${id}`, { method: 'DELETE' });
                await this.fetchWarehouses();
            } catch (e) {
                console.error('Failed to remove warehouse', e);
            }
        },
        async rescan() {
            this.loading = true;
            try {
                await fetch(`${API_BASE}/photos/rescan`, { method: 'POST' });
                await this.loadPhotos();
            } catch (e) {
                console.error('Failed to rescan', e);
            } finally {
                this.loading = false;
            }
        },
        async loadPhotos() {
            try {
                const params = new URLSearchParams({
                    status: this.statusFilter,
                    sort: this.sortBy,
                    order: this.sortOrder,
                    page: this.page,
                    size: this.pageSize,
                });
                const res = await fetch(`${API_BASE}/photos?${params}`);
                const data = await res.json();
                this.photos = data.rows || [];
                this.total = data.total || 0;
                this.selected = data.selected || 0;
                this.rejected = data.rejected || 0;
                this.unmarked = data.unmarked || 0;
            } catch (e) {
                console.error('Failed to load photos', e);
            }
        },
        async markPhoto(photo, status) {
            try {
                await fetch(`${API_BASE}/photos/${photo.id}/mark`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ status }),
                });
                photo.status = status;
                if (status === 'selected') this.selected++;
                else if (status === 'rejected') this.rejected++;
                if (photo.status !== 'selected' && this.lightboxPhoto && this.lightboxPhoto.id === photo.id) {
                }
                await this.updateCounts();
            } catch (e) {
                console.error('Failed to mark photo', e);
            }
        },
        async updateCounts() {
            try {
                const res = await fetch(`${API_BASE}/photos/statistics`);
                const stats = await res.json();
                this.selected = stats.selected || 0;
                this.rejected = stats.rejected || 0;
                this.unmarked = stats.unmarked || 0;
            } catch (e) {
                console.error('Failed to get stats', e);
            }
        },
        openLightbox(photo) {
            const idx = this.photos.findIndex(p => p.id === photo.id);
            if (idx !== -1) {
                this.lightboxIndex = idx;
                this.lightboxPhoto = photo;
                this.loadExif(photo);
                this.showLightbox = true;
            }
        },
        closeLightbox() {
            this.showLightbox = false;
            this.lightboxPhoto = null;
            this.lightboxExif = null;
        },
        lightboxPrev() {
            if (this.lightboxIndex > 0) {
                this.lightboxIndex--;
                this.lightboxPhoto = this.photos[this.lightboxIndex];
                this.loadExif(this.lightboxPhoto);
            }
        },
        lightboxNext() {
            if (this.lightboxIndex < this.photos.length - 1) {
                this.lightboxIndex++;
                this.lightboxPhoto = this.photos[this.lightboxIndex];
                this.loadExif(this.lightboxPhoto);
            }
        },
        async loadExif(photo) {
            try {
                const res = await fetch(`${API_BASE}/photos/${photo.id}`);
                if (res.ok) {
                    this.lightboxExif = await res.json();
                }
            } catch (e) {
                console.error('Failed to load exif', e);
            }
        },
        changeFilter(status) {
            this.statusFilter = status;
            this.page = 1;
            this.loadPhotos();
        },
        changeSort(sort) {
            if (this.sortBy === sort) {
                this.sortOrder = this.sortOrder === 'asc' ? 'desc' : 'asc';
            } else {
                this.sortBy = sort;
                this.sortOrder = 'asc';
            }
            this.page = 1;
            this.loadPhotos();
        },
        toggleSettings() {
            this.showSettings = !this.showSettings;
        },
        loadMore() {
            if (this.photos.length < this.total) {
                this.page++;
                this.loadPhotos();
            }
        },
        formatFileSize(bytes) {
            if (!bytes) return '-';
            if (bytes < 1024) return bytes + ' B';
            if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
            return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
        },
    },
    mounted() {
        this.fetchWarehouses();
    },
});

app.component('top-bar', {
    template: `
        <header class="top-bar">
            <div class="top-bar-inner">
                <h1 class="logo">📷 摄影选图</h1>
                <div class="top-bar-controls">
                    <select class="warehouse-select" v-model="$parent.activeWarehouse?.id"
                            @change="$parent.switchWarehouse($event.target.value)">
                        <option value="" disabled>选择仓库</option>
                        <option v-for="w in $parent.warehouses" :key="w.id" :value="w.id">
                            {{ w.name }} ({{ w.path }})
                        </option>
                    </select>
                    <button class="btn btn-icon" @click="$parent.toggleSettings()" title="仓库设置">⚙</button>
                    <button class="btn btn-primary" @click="$parent.rescan()" :disabled="$parent.loading">
                        {{ $parent.loading ? '扫描中...' : '🔄 扫描' }}
                    </button>
                </div>
            </div>
        </header>
    `,
});

app.component('filter-bar', {
    template: `
        <div class="filter-bar">
            <div class="filter-tabs">
                <button v-for="tab in tabs" :key="tab.key"
                        class="filter-tab"
                        :class="{ active: $parent.statusFilter === tab.key }"
                        @click="$parent.changeFilter(tab.key)">
                    {{ tab.label }}
                    <span class="filter-count">{{ getCount(tab.key) }}</span>
                </button>
            </div>
            <div class="filter-sorts">
                <button v-for="s in sorts" :key="s.key"
                        class="sort-btn"
                        :class="{ active: $parent.sortBy === s.key }"
                        @click="$parent.changeSort(s.key)">
                    {{ s.label }}
                    <span v-if="$parent.sortBy === s.key" class="sort-arrow">
                        {{ $parent.sortOrder === 'asc' ? '↑' : '↓' }}
                    </span>
                </button>
            </div>
        </div>
    `,
    data() {
        return {
            tabs: [
                { key: 'all', label: '全部' },
                { key: 'selected', label: '精选' },
                { key: 'unmarked', label: '未筛' },
                { key: 'rejected', label: '落选' },
            ],
            sorts: [
                { key: 'name', label: '文件名' },
                { key: 'date', label: '拍摄日期' },
                { key: 'status', label: '状态' },
            ],
        };
    },
    methods: {
        getCount(key) {
            const p = this.$parent;
            if (key === 'all') return p.total;
            if (key === 'selected') return p.selected;
            if (key === 'unmarked') return p.unmarked;
            if (key === 'rejected') return p.rejected;
            return 0;
        },
    },
});

app.component('photo-grid', {
    template: `
        <div class="photo-grid-wrapper">
            <div v-if="$parent.loading" class="loading-indicator">正在扫描仓库...</div>
            <div v-else-if="$parent.photos.length === 0" class="empty-state">
                <p>暂无照片，请先选择仓库并扫描</p>
            </div>
            <div v-else class="photo-grid">
                <div v-for="photo in $parent.photos" :key="photo.id"
                     class="photo-card"
                     @click="$parent.openLightbox(photo)">
                    <div class="photo-card-img-wrapper">
                        <img :src="photo.thumbnailUrl"
                             :alt="photo.fileName"
                             class="photo-card-img"
                             loading="lazy"
                             @error="onImgError">
                        <div v-if="photo.status === 'selected'" class="photo-badge selected-badge">★</div>
                        <div v-else-if="photo.status === 'rejected'" class="photo-badge rejected-badge">✕</div>
                    </div>
                    <div class="photo-card-info">
                        <span class="photo-card-name" :title="photo.fileName">{{ photo.fileName }}</span>
                        <span v-if="photo.dateTaken" class="photo-card-date">{{ photo.dateTaken }}</span>
                    </div>
                </div>
            </div>
        </div>
    `,
    methods: {
        onImgError(e) {
            e.target.src = 'data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect fill="%23eee" width="200" height="200"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="%23999" font-size="14">加载失败</text></svg>';
        },
    },
});

app.component('status-bar', {
    template: `
        <footer class="status-bar" v-if="$parent.total > 0">
            <span>共 <strong>{{ $parent.total }}</strong> 张</span>
            <span class="status-sep">·</span>
            <span class="status-selected">已选 <strong>{{ $parent.selected }}</strong> 张</span>
            <span class="status-sep">·</span>
            <span class="status-rejected">落选 <strong>{{ $parent.rejected }}</strong> 张</span>
            <span class="status-sep">·</span>
            <span class="status-unmarked">待筛 <strong>{{ $parent.unmarked }}</strong> 张</span>
        </footer>
    `,
});

app.component('lightbox', {
    template: `
        <div class="lightbox-overlay" @click.self="$parent.closeLightbox()">
            <div class="lightbox-container">
                <div class="lightbox-header">
                    <span class="lightbox-title">{{ $parent.lightboxPhoto?.fileName }}</span>
                    <button class="btn btn-icon lightbox-close" @click="$parent.closeLightbox()">✕</button>
                </div>
                <div class="lightbox-body">
                    <button class="lightbox-nav lightbox-prev" @click="$parent.lightboxPrev()"
                            :disabled="$parent.lightboxIndex === 0">‹</button>
                    <div class="lightbox-image-wrapper">
                        <img :src="$parent.lightboxPhoto?.thumbnailUrl" class="lightbox-image">
                    </div>
                    <button class="lightbox-nav lightbox-next" @click="$parent.lightboxNext()"
                            :disabled="$parent.lightboxIndex === $parent.photos.length - 1">›</button>
                </div>
                <div class="lightbox-footer">
                    <div class="lightbox-actions">
                        <button class="btn btn-selected"
                                :class="{ active: $parent.lightboxPhoto?.status === 'selected' }"
                                @click="$parent.markPhoto($parent.lightboxPhoto, 'selected')">★ 精选</button>
                        <button class="btn btn-rejected"
                                :class="{ active: $parent.lightboxPhoto?.status === 'rejected' }"
                                @click="$parent.markPhoto($parent.lightboxPhoto, 'rejected')">✕ 落选</button>
                        <button class="btn btn-unmark"
                                @click="$parent.markPhoto($parent.lightboxPhoto, 'unmarked')">↺ 重置</button>
                    </div>
                    <div class="lightbox-exif" v-if="$parent.lightboxExif">
                        <table>
                            <tr v-if="$parent.lightboxExif.cameraModel"><td>相机</td><td>{{ $parent.lightboxExif.cameraModel }}</td></tr>
                            <tr v-if="$parent.lightboxExif.fNumber"><td>光圈</td><td>{{ $parent.lightboxExif.fNumber }}</td></tr>
                            <tr v-if="$parent.lightboxExif.shutterSpeed"><td>快门</td><td>{{ $parent.lightboxExif.shutterSpeed }}</td></tr>
                            <tr v-if="$parent.lightboxExif.isoSpeed"><td>ISO</td><td>{{ $parent.lightboxExif.isoSpeed }}</td></tr>
                            <tr v-if="$parent.lightboxExif.focalLength"><td>焦距</td><td>{{ $parent.lightboxExif.focalLength }}</td></tr>
                            <tr v-if="$parent.lightboxExif.flash"><td>闪光灯</td><td>{{ $parent.lightboxExif.flash }}</td></tr>
                            <tr v-if="$parent.lightboxExif.lenInfo"><td>镜头</td><td>{{ $parent.lightboxExif.lenInfo }}</td></tr>
                            <tr v-if="$parent.lightboxExif.dateTaken"><td>拍摄时间</td><td>{{ $parent.lightboxExif.dateTaken }}</td></tr>
                            <tr v-if="$parent.lightboxPhoto?.fileSize"><td>文件大小</td><td>{{ $parent.formatFileSize($parent.lightboxPhoto.fileSize) }}</td></tr>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    `,
});

app.component('settings-modal', {
    template: `
        <div class="modal-overlay" @click.self="$parent.toggleSettings()">
            <div class="modal-container">
                <div class="modal-header">
                    <h2>仓库设置</h2>
                    <button class="btn btn-icon" @click="$parent.toggleSettings()">✕</button>
                </div>
                <div class="modal-body">
                    <div class="warehouse-list">
                        <h3>已配置仓库</h3>
                        <div v-if="$parent.warehouses.length === 0" class="empty-hint">暂无仓库，请添加</div>
                        <div v-for="w in $parent.warehouses" :key="w.id" class="warehouse-item">
                            <div class="warehouse-item-info">
                                <span class="warehouse-name">{{ w.name }}</span>
                                <span class="warehouse-path">{{ w.path }}</span>
                            </div>
                            <div class="warehouse-item-actions">
                                <span v-if="w.active" class="badge-active">当前</span>
                                <button v-else class="btn btn-sm" @click="$parent.switchWarehouse(w.id)">切换</button>
                                <button class="btn btn-sm btn-danger" @click="$parent.removeWarehouse(w.id)">删除</button>
                            </div>
                        </div>
                    </div>
                    <div class="warehouse-add">
                        <h3>添加仓库</h3>
                        <div class="form-row">
                            <input class="input" v-model="$parent.newWarehouseName" placeholder="仓库名称（如：2024年婚纱）">
                        </div>
                        <div class="form-row">
                            <input class="input" v-model="$parent.newWarehousePath" placeholder="仓库路径（如：D:\\PhotoBank）">
                        </div>
                        <button class="btn btn-primary" @click="$parent.addWarehouse()">添加</button>
                    </div>
                </div>
            </div>
        </div>
    `,
});

app.mount('#app');
