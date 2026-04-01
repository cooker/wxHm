<template>
  <div class="page">
    <nut-navbar title="管理登录" left-show @click-back="$router.push('/')" safe-area-inset-top />
    <div class="card">
      <nut-form>
        <nut-form-item label="管理密码">
          <nut-input v-model="password" type="password" placeholder="请输入密码" />
        </nut-form-item>
      </nut-form>
      <nut-button type="primary" block :loading="loading" @click="submit">登录</nut-button>
      <p v-if="err" class="err">{{ err }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { apiJson, setAdminToken } from '@/api/client'

const route = useRoute()
const router = useRouter()
const password = ref('')
const loading = ref(false)
const err = ref('')

function appPathToRoute(full) {
  if (!full || typeof full !== 'string') return '/admin'
  if (!full.startsWith('/app')) return '/admin'
  const p = full.slice(4)
  return p || '/'
}

async function submit() {
  err.value = ''
  if (!password.value) {
    err.value = '请输入密码'
    return
  }
  loading.value = true
  try {
    let redirect = (route.query.redirect && String(route.query.redirect)) || '/app/admin'
    if (!redirect.startsWith('/app')) {
      redirect = '/app' + (redirect.startsWith('/') ? redirect : '/' + redirect)
    }
    const { ok, data } = await apiJson('/api/admin/login', {
      method: 'POST',
      body: JSON.stringify({ password: password.value, redirect }),
    })
    if (!ok || !data?.ok) {
      err.value = data?.message || '登录失败'
      return
    }
    if (data.token) setAdminToken(data.token)
    await router.replace(appPathToRoute(data.redirect))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { padding: 16px; }
.card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-top: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}
.err { color: #ee0a24; font-size: 13px; margin-top: 12px; text-align: center; }
</style>
