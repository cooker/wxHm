<template>
  <div class="page">
    <nut-navbar title="自定义文件" left-show @click-back="$router.push('/admin')" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="logout">退出</span>
      </template>
    </nut-navbar>

    <div class="card">
      <h4>上传文件</h4>
      <input ref="upRef" type="file" class="hidden" @change="onUpload" />
      <nut-button type="primary" block @click="upRef?.click()">选择文件上传</nut-button>
    </div>

    <div class="card">
      <h4>粘贴批量创建</h4>
      <nut-textarea v-model="paste" rows="6" placeholder="支持 ##文本内容 ... ## 区间，每行：文件名 内容" />
      <nut-button type="success" block class="mt" @click="doPaste">创建</nut-button>
    </div>

    <div class="card">
      <h4>已上传</h4>
      <nut-cell v-for="f in files" :key="f" :title="f">
        <template #link>
          <nut-button size="small" type="primary" plain class="op-btn" @click="previewFile(f)">预览</nut-button>
          <nut-button size="small" type="danger" plain @click="delFile(f)">删除</nut-button>
        </template>
      </nut-cell>
      <p v-if="!files.length" class="empty">暂无文件</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from '@nutui/nutui'
import { apiJson, apiFetch, setAdminToken } from '@/api/client'

const router = useRouter()
const files = ref([])
const paste = ref('')
const upRef = ref(null)

async function load() {
  const { ok, data } = await apiJson('/api/admin/files')
  if (ok && Array.isArray(data)) files.value = data
}

onMounted(load)

async function onUpload(e) {
  const f = e.target.files?.[0]
  e.target.value = ''
  if (!f) return
  const fd = new FormData()
  fd.append('file', f)
  const res = await apiFetch('/api/admin/files/upload', { method: 'POST', body: fd })
  const data = await res.json().catch(() => ({}))
  if (res.ok && data.ok) {
    showToast.success(data.message || '成功')
    await load()
  } else {
    showToast.text(data.message || '失败')
  }
}

async function doPaste() {
  const { ok, data } = await apiJson('/api/admin/files/paste', {
    method: 'POST',
    body: JSON.stringify({ paste_content: paste.value }),
  })
  if (ok && data?.ok) {
    showToast.success(data.message || '成功')
    paste.value = ''
    await load()
  } else {
    showToast.text(data?.message || '失败')
  }
}

async function delFile(filename) {
  const { ok, data } = await apiJson('/api/admin/files/delete', {
    method: 'POST',
    body: JSON.stringify({ filename }),
  })
  if (ok && data?.ok) {
    showToast.success(data.message || '已删除')
    await load()
  } else {
    showToast.text(data?.message || '失败')
  }
}

function previewFile(filename) {
  if (!filename) return
  const url = '/' + encodeURIComponent(filename)
  window.open(url, '_blank')
}

async function logout() {
  await apiJson('/api/admin/logout', { method: 'POST', body: '{}' })
  setAdminToken(null)
  router.push('/admin/login')
}
</script>

<style scoped>
.page { padding-bottom: 24px; }
.nav-link { font-size: 14px; color: #1989fa; padding-right: 8px; }
.card { margin: 12px; padding: 16px; background: #fff; border-radius: 12px; }
.card h4 { margin: 0 0 12px; }
.hidden { display: none; }
.mt { margin-top: 12px; }
.op-btn { margin-right: 6px; }
.empty { color: #999; font-size: 13px; text-align: center; padding: 12px; }
</style>
