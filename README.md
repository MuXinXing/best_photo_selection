# 📷 摄影选图工具 (Best Photo Selection)

> 面向摄影的选图小工具 —— 快速筛片、标记、EXIF 速览，告别手动管理 RAW/JPG 的流程。

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.4-4fc08d.svg)](https://vuejs.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 🎯 项目简介

**摄影选图工具** 是一款专为人像/Cosplay 摄影设计的选片应用。它可以：

- 🔍 扫描本地目录中的所有 JPG 照片
- ⚡ 生成缩略图瀑布流，快速浏览海量照片
- ✅ 标记**精选** / ❌ **落选**，状态持久化保存
- 📋 显示完整的 EXIF 信息（机身、光圈、快门、ISO、焦距、镜头等）
- 📂 自动归类 JPG 和 RAW 文件到对应子目录
- 💾 精选照片自动复制到 `选片/` 目录

所有数据均存储在本地，无需网络，无需数据库。

---

## ✨ 核心功能

| 功能 | 说明 |
|---|---|
| **多片仓管理** | 支持配置多个仓库目录，随时切换，配置持久化 |
| **瀑布流浏览** | 响应式 CSS 瀑布流网格，适配不同屏幕尺寸 |
| **缩略图缓存** | 首次添加仓库自动生成缩略图并缓存到 `.thumbnails/` 目录 |
| **精选/落选标记** | 单选标记照片，标记状态存入 `.selection_status.json` |
| **大图预览** | 点击照片进入照片视图，支持缩放与拖拽平移 |
| **EXIF 详情** | 在照片视图中查看完整拍摄参数 |
| **文件归类** | 自动将 RAW 和 JPG 文件分类到 `JPG/` 和 `RAW/` 子目录 |
| **自动拷贝精选** | 标记精选时自动将 JPG 及同名 RAW 拷贝到 `选片/JPG/` 和 `选片/RAW/` |
| **排序筛选** | 支持按文件名、拍摄日期、状态排序；按精选/落选/未筛过滤 |
| **异步分类** | 文件归类操作异步执行 |

---

## 🛠 技术栈

### 后端
- **Java 21**
- **Spring Boot 4.0.4**
- **metadata-extractor 2.19.0** — EXIF 信息读取
- **thumbnailator 0.4.20** — 生成略缩图
- **Maven** — 构建管理

### 前端
- **Vue 3**
- **纯 CSS** — 暗色主题，无第三方 UI 框架

---

## 📁 项目结构

```
best_photo_selection/
├── pom.xml                          # Maven项目配置
├── src/
│   ├── main/
│   │   ├── java/com/mx/
│   │   │   ├── SelectionApplicationContextMain.java  # 启动类
│   │   │   ├── config/
│   │   │   │   └── WebMvcConfig.java                 # CORS配置
│   │   │   ├── controller/
│   │   │   │   ├── PhotoController.java              # 照片API (/api/photos)
│   │   │   │   └── WarehouseController.java          # 仓库API (/api/warehouse)
│   │   │   ├── entity/
│   │   │   │   ├── PhotoItem.java                    # 照片实体
│   │   │   │   └── PhotoExif.java                    # EXIF实体
│   │   │   ├── exception/
│   │   │   │   └── PhotoSelectionException.java      # 自定义异常
│   │   │   ├── service/
│   │   │   │   ├── PhotoService.java                 # 照片核心服务
│   │   │   │   ├── WarehouseService.java             # 仓库管理服务
│   │   │   │   ├── ClassificationService.java        # 文件分类服务
│   │   │   │   └── ThumbnailService.java             # 缩略图服务
│   │   │   └── util/
│   │   │       ├── ExifTool.java                     # EXIF 读取工具
│   │   │       └── FolderTool.java                   # 文件夹工具
│   │   └── resources/
│   │       ├── application.yml                       # 主配置
│   │       └── application-dev.yml                   # 开发环境配置
│   └── test/java/com/mx/
│       └── ExifReaderTest.java                       # EXIF读取测试
├── frontend/
│   ├── index.html                                    # HTML入口
│   ├── package.json                                  # 前端依赖
│   ├── vite.config.js                                # Vite 配置
│   └── src/
│       ├── main.js                                   # Vue入口
│       ├── App.vue                                   # 根组件
│       ├── api/
│       │   └── index.js                              # API封装
│       ├── assets/
│       │   └── main.css                              # 全局样式
│       └── components/
│           ├── TopBar.vue                            # 顶部栏
│           ├── FilterBar.vue                         # 筛选栏
│           ├── PhotoGrid.vue                         # 照片网格
│           ├── StatusBar.vue                         # 状态栏
│           ├── Lightbox.vue                          # 大图预览
│           ├── SettingsModal.vue                     # 仓库设置
│           └── ClassificationModal.vue               # 分类进度
└── .gitignore
```

---

## 🚀 快速开始

### 环境要求

- **JDK 21+**
- **Maven 3.6+**
- **Node.js 18+**
- **Windows / macOS / Linux**

### 1. 克隆项目

```bash
git clone <>
cd best_photo_selection
```

### 2. 启动后端

```bash
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`，开发模式使用 `dev` profile 时运行在 `7990` 端口：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动前端 (开发模式)

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`，API 请求自动代理到后端。

### 4. 生产构建

```bash
cd frontend
npm run build          # 构建产物输出到 ../src/main/resources/static/
cd ..
mvn clean package      # 打包为 JAR
java -jar target/selection-1.0.0.jar
```

构建后前后端合并为一个 JAR，直接运行即可。

---

## ⚙️ 配置说明

### 应用配置 (`application.yml`)

```yaml
server:
  port: 8080

spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8

app:
  config-dir: ~/.photo-selection  # 仓库配置文件存储目录

logging:
  level:
    com.mx: DEBUG
```

### 运行时数据文件

| 文件 | 位置 | 说明 |
|---|---|---|
| `warehouses.json` | `~/.photo-selection/` | 仓库列表配置 |
| `.selection_status.json` | 片仓根目录 | 照片标记状态 |
| `.thumbnails/` | 片仓根目录 | 缩略图缓存 |

---

## 📡 API 文档

### 片仓管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/warehouse` | 获取所有片仓 |
| `POST` | `/api/warehouse` | 添加片仓 |
| `PUT` | `/api/warehouse/{id}/active` | 切换活动片仓 |
| `DELETE` | `/api/warehouse/{id}` | 删除片仓 |

### 照片管理

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/api/photos` | 照片列表 (分页/排序/筛选) |
| `GET` | `/api/photos/{id}` | 照片详情 + EXIF |
| `GET` | `/api/photos/{id}/file` | 获取原图文件 |
| `GET` | `/api/photos/{id}/thumbnail?w=` | 获取指定宽度缩略图 |
| `POST` | `/api/photos/{id}/mark` | 标记单张照片状态 |
| `POST` | `/api/photos/batch-mark` | 批量标记 |
| `GET` | `/api/photos/statistics` | 统计数据 |
| `POST` | `/api/photos/rescan` | 重新扫描片仓 |

### 文件归类

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/api/photos/classify` | 触发异步归类 |
| `GET` | `/api/photos/classify/progress` | 查询归类进度 |

### 查询参数说明

**GET `/api/photos` 支持：**

| 参数 | 类型 | 说明 |
|---|---|---|
| `status` | string | 筛选状态：`selected`, `rejected`, `unmarked` |
| `sort` | string | 排序字段：`name`, `date`, `status` |
| `order` | string | 排序方向：`asc`, `desc` |
| `page` | int | 页码 (从0开始) |
| `size` | int | 每页数量 (0=不分页) |

---

## 🖥 使用流程

1. **添加片仓** — 点击齿轮图标，添加你的照片目录
2. **自动归类** — 首次添加时会提示归类，将 JPG/RAW 分到子目录
3. **浏览选片** — 在瀑布流中浏览，点击照片进入大图模式
4. **标记状态** — 使用按钮标记「精选」或「落选」（支持键盘快捷键）
5. **筛选排序** — 按状态筛选、按日期/名称排序
6. **导出精选** — 标记为精选的照片自动复制到 `选片/` 目录

---

## 🗺 技术路线图

- [x] 键盘快捷键（← → 翻页）
- [x] 多视图（单列 / 双列 / 自适应列数）
- [ ] 对比模式（并排查看两张照片）
- [ ] 色彩标签（红/黄/绿/蓝/紫五色标记）
- [ ] 拖放导入新仓库
- [ ] 远程协作、生成公网链接提供给模特，模特也可以选择

