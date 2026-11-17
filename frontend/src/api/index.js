const API_BASE = '/api'

async function request(url, options = {}) {
  const res = await fetch(`${API_BASE}${url}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options
  })
  if (!res.ok) {
    let message = `请求失败（${res.status}）`
    try {
      const body = await res.json()
      if (body && body.message) message = body.message
    } catch (e) {
      // 错误响应没有 JSON 时保留状态码
    }
    throw new Error(message)
  }
  return res
}

export function getWarehouses() {
  return request('/warehouse').then(r => r.json())
}

export function addWarehouse(name, path) {
  return request('/warehouse', {
    method: 'POST',
    body: JSON.stringify({ name, path })
  }).then(r => r.json())
}

export function switchWarehouse(id) {
  return request(`/warehouse/${id}/active`, { method: 'PUT' })
}

export function removeWarehouse(id) {
  return request(`/warehouse/${id}`, { method: 'DELETE' })
}

export function getPhotos(params = {}) {
  const qs = new URLSearchParams({
    status: params.status || 'all',
    sort: params.sort || 'name',
    order: params.order || 'asc',
    page: String(params.page > 0 ? params.page : 1),
    size: String(params.size >= 0 ? params.size : 60)
  }).toString()
  return request(`/photos?${qs}`).then(r => r.json())
}

export function getPhoto(id) {
  return request(`/photos/${id}`).then(r => r.json())
}

export function markPhoto(id, status) {
  return request(`/photos/${id}/mark`, {
    method: 'POST',
    body: JSON.stringify({ status })
  })
}

export function batchMark(marks) {
  return request('/photos/batch-mark', {
    method: 'POST',
    body: JSON.stringify(marks)
  })
}

export function getStatistics() {
  return request('/photos/statistics').then(r => r.json())
}

export function rescan() {
  return request('/photos/rescan', { method: 'POST' }).then(r => r.json())
}

export function classify() {
  return request('/photos/classify', { method: 'POST' }).then(r => r.json())
}

export function getClassifyProgress() {
  return request('/photos/classify/progress').then(r => r.json())
}
