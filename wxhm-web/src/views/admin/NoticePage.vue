<template>
  <div class="page">
    <nut-navbar title="公众号模板" left-show @click-back="$router.push('/admin')" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="logout">退出</span>
      </template>
    </nut-navbar>

    <div class="card">
      <nut-button size="small" @click="loadLatest">读取最新配置</nut-button>
    </div>

    <div class="card">
      <h4>配置列表</h4>
      <nut-cell
        v-for="c in configs"
        :key="c.id"
        :title="c.name"
        :desc="c.template_id"
        is-link
        @click="edit(c)"
      />
      <p v-if="!configs.length" class="empty">暂无配置</p>
    </div>

    <div class="card">
      <h4>{{ form.id ? '编辑' : '新建' }}</h4>
      <nut-input v-model="form.name" placeholder="配置名称" />
      <nut-input v-model="form.appid" placeholder="AppID" />
      <nut-input v-model="form.secret" type="password" placeholder="Secret" />
      <nut-input v-model="form.touser" placeholder="用户 openid" />
      <nut-input v-model="form.template_id" placeholder="模板 ID" />
      <nut-textarea v-model="form.template_data" rows="5" placeholder="模板 data JSON" />
      <nut-input v-model="form.url" placeholder="跳转 URL（可选）" />
      <nut-button type="primary" block class="mt" @click="save">保存</nut-button>
      <nut-button v-if="form.id" type="warning" block class="mt" @click="send">发送测试</nut-button>
      <nut-button v-if="form.id" type="danger" block plain class="mt" @click="remove">删除</nut-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast } from '@nutui/nutui'
import { apiJson, setAdminToken } from '@/api/client'

const route = useRoute()
const router = useRouter()
const configs = ref([])
const form = ref({
  id: null,
  name: '',
  appid: '',
  secret: '',
  touser: '',
  template_id: '',
  template_data: '',
  url: '',
})

async function loadList() {
  const { ok, data } = await apiJson('/api/admin/notice/configs')
  if (ok && Array.isArray(data)) configs.value = data
}

onMounted(async () => {
  await loadList()
  const editId = route.query.edit
  if (editId) {
    const { ok, data } = await apiJson('/api/admin/notice/configs/' + editId)
    if (ok && data?.id) applyForm(data)
  }
})

function applyForm(c) {
  form.value = {
    id: c.id,
    name: c.name || '',
    appid: c.appid || '',
    secret: c.secret || '',
    touser: c.touser || '',
    template_id: c.template_id || '',
    template_data: c.template_data || '',
    url: c.url || '',
  }
}

function edit(c) {
  applyForm(c)
}

async function loadLatest() {
  const { ok, data } = await apiJson('/api/admin/notice/action', {
    method: 'POST',
    body: JSON.stringify({ action: 'load' }),
  })
  if (ok && data?.ok && data.id) {
    applyForm(data)
    showToast.success(data.message || '已加载')
  } else {
    showToast.text(data?.message || '失败')
  }
}

async function save() {
  const body = {
    action: 'save',
    config_id: form.value.id,
    name: form.value.name,
    appid: form.value.appid,
    secret: form.value.secret,
    touser: form.value.touser,
    template_id: form.value.template_id,
    template_data: form.value.template_data,
    url: form.value.url,
  }
  const { ok, data } = await apiJson('/api/admin/notice/action', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (ok && data?.ok) {
    showToast.success(data.message || '成功')
    if (data.id) form.value.id = data.id
    await loadList()
  } else {
    showToast.text(data?.message || '失败')
  }
}

async function send() {
  if (!form.value.id) return
  const { ok, data } = await apiJson('/api/admin/notice/action', {
    method: 'POST',
    body: JSON.stringify({ action: 'send', config_id: form.value.id }),
  })
  if (ok && data?.ok) {
    showToast.success(data.message || '已发送')
  } else {
    showToast.text(data?.message || '失败')
  }
}

async function remove() {
  if (!form.value.id) return
  const { ok, data } = await apiJson('/api/admin/notice/configs/' + form.value.id, { method: 'DELETE' })
  if (ok && data?.ok) {
    showToast.success('已删除')
    form.value = { id: null, name: '', appid: '', secret: '', touser: '', template_id: '', template_data: '', url: '' }
    await loadList()
  } else {
    showToast.text(data?.message || '失败')
  }
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
.card { margin: 12px; padding: 12px; background: #fff; border-radius: 12px; }
.card h4 { margin: 0 0 10px; }
.nut-input, .nut-textarea { margin-bottom: 10px; }
.mt { margin-top: 10px; }
.empty { color: #999; font-size: 13px; text-align: center; padding: 12px; }
</style>
