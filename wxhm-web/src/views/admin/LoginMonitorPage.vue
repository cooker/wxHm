<template>
  <div class="page">
    <nut-navbar title="登录监控" left-show @click-back="$router.push('/admin')" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="logout">退出</span>
      </template>
    </nut-navbar>

    <div class="toolbar">
      <nut-button size="small" type="primary" @click="loadRows">刷新</nut-button>
    </div>

    <div class="card">
      <h4>错误密码记录（最多 200 条）</h4>
      <table v-if="rows.length" class="tbl">
        <thead>
          <tr>
            <th>时间</th>
            <th>IP</th>
            <th>设备</th>
            <th>错误密码</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in rows" :key="i">
            <td>{{ r.createdAt }}</td>
            <td>{{ r.ip }}</td>
            <td>{{ r.platform }}</td>
            <td class="pwd">{{ r.inputPassword }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无错误密码记录</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from '@nutui/nutui'
import { apiJson, setAdminToken } from '@/api/client'

const router = useRouter()
const rows = ref([])

async function loadRows() {
  const { ok, data } = await apiJson('/api/admin/login-monitor')
  if (ok && data) {
    rows.value = Array.isArray(data.rows) ? data.rows : []
    return
  }
  showToast.text(data?.message || '加载失败')
}

async function logout() {
  await apiJson('/api/admin/logout', { method: 'POST', body: '{}' })
  setAdminToken(null)
  router.push('/admin/login')
}

onMounted(loadRows)
</script>

<style scoped>
.page { padding-bottom: 24px; }
.nav-link { font-size: 14px; color: #1989fa; padding-right: 8px; }
.toolbar { padding: 12px; }
.card { margin: 0 12px; padding: 12px; background: #fff; border-radius: 12px; }
.card h4 { margin: 0 0 10px; font-size: 15px; }
.tbl { width: 100%; border-collapse: collapse; font-size: 12px; }
.tbl th, .tbl td { border-bottom: 1px solid #f0f0f0; padding: 7px 4px; text-align: left; vertical-align: top; }
.pwd { color: #cf1322; word-break: break-all; max-width: 220px; }
.empty { color: #999; text-align: center; padding: 14px 0; font-size: 13px; }
</style>
