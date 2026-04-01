<template>
  <div class="page">
    <nut-navbar title="未创建群码访问" left-show @click-back="$router.push('/admin')" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="logout">退出</span>
      </template>
    </nut-navbar>

    <div class="card">
      <h4>汇总（近 3 天）</h4>
      <table v-if="summaryRows.length" class="tbl">
        <thead>
          <tr><th>群名</th><th>PV</th><th>UV</th><th>最近</th></tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in summaryRows" :key="i">
            <td>
              <span class="gname">{{ r.groupName }}</span>
              <nut-button size="mini" type="primary" plain @click="quickCreate(r.groupName)">快速建码</nut-button>
            </td>
            <td>{{ r.pv }}</td>
            <td>{{ r.uv }}</td>
            <td>{{ r.lastVisit }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty">暂无记录</p>
    </div>

    <div class="card">
      <h4>最近明细（最多 200 条）</h4>
      <div v-for="(r, i) in recentRows" :key="i" class="line">
        <span>{{ r.createdAt }}</span>
        <strong>{{ r.groupName }}</strong>
        <span class="muted">{{ r.ip }} · {{ r.platform }}</span>
      </div>
      <p v-if="!recentRows.length" class="empty">暂无明细</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from '@nutui/nutui'
import { apiJson, setAdminToken } from '@/api/client'

const router = useRouter()
const summaryRows = ref([])
const recentRows = ref([])

onMounted(async () => {
  const { ok, data } = await apiJson('/api/admin/missing-groups')
  if (ok && data) {
    summaryRows.value = data.summaryRows || []
    recentRows.value = data.recentRows || []
  }
})

function quickCreate(name) {
  if (!name) return
  router.push({ path: '/admin', query: { group_name: name } })
  showToast.text('已填入群名，请上传图片')
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
.card h4 { margin: 0 0 10px; font-size: 15px; }
.tbl { width: 100%; font-size: 12px; border-collapse: collapse; }
.tbl th, .tbl td { border-bottom: 1px solid #f0f0f0; padding: 6px 4px; text-align: left; }
.line { font-size: 12px; padding: 8px 0; border-bottom: 1px solid #f5f5f5; display: flex; flex-direction: column; gap: 4px; }
.muted { color: #999; }
.empty { color: #999; font-size: 13px; text-align: center; padding: 12px; }
.gname { display: block; margin-bottom: 6px; word-break: break-all; }
</style>
