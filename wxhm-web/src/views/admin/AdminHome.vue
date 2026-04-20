<template>
  <div class="page">
    <nut-navbar title="管理中心" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="logout">退出</span>
      </template>
    </nut-navbar>

    <div class="menu-grid">
      <div class="menu-item" @click="$router.push('/admin/stats')">
        <span class="t">数据趋势</span>
        <span class="d">7 日 PV/UV</span>
      </div>
      <div class="menu-item" @click="$router.push('/admin/missing-groups')">
        <span class="t">未创建群码</span>
        <span class="d">误链访问统计</span>
      </div>
      <div class="menu-item" @click="$router.push('/admin/upload')">
        <span class="t">自定义文件</span>
        <span class="d">上传与管理</span>
      </div>
      <div class="menu-item" @click="$router.push('/admin/notice')">
        <span class="t">公众号</span>
        <span class="d">模板消息</span>
      </div>
      <div class="menu-item" @click="$router.push('/admin/login-monitor')">
        <span class="t">登录监控</span>
        <span class="d">错误密码与风控</span>
      </div>
    </div>

    <div class="card">
      <h4>发布群码</h4>
      <nut-input v-model="groupName" placeholder="群组名" />
      <input ref="fileRef" type="file" accept="image/*" class="file" @change="onPick" />
      <nut-button type="primary" block class="mt" @click="pickFile">选择图片并上传</nut-button>
    </div>

    <div class="card">
      <h4>问卷链接配置</h4>
      <p class="tip">仅支持全局配置，链接为空表示关闭。</p>
      <nut-input v-model="globalSurveyUrl" placeholder="全局默认问卷链接（https://...）" />
      <nut-input v-model="globalSurveyText" class="mt8" placeholder="全局按钮文案（默认：填写问卷）" />
      <nut-button type="primary" plain block class="mt" @click="saveGlobalSurvey">保存全局配置</nut-button>
    </div>

    <div class="card">
      <h4>群组列表</h4>
      <div v-for="g in groups" :key="g.name" class="row">
        <div class="left">
          <nut-input v-if="editing === g.name" v-model="editName" @blur="commitRename(g.name)" @keyup.enter="commitRename(g.name)" />
          <span v-else class="name" @click="startEdit(g.name)">{{ g.name }}</span>
          <nut-tag :type="g.qrActive ? 'success' : 'danger'" size="small">{{ g.qrActive ? '有效' : '已失效' }}</nut-tag>
        </div>
        <div class="ops">
          <nut-button size="small" type="success" plain @click="preview(g.name)">预览</nut-button>
          <nut-button size="small" type="primary" plain @click="replaceQr(g.name)">换码</nut-button>
          <nut-button size="small" type="danger" plain @click="removeGroup(g.name)">删除</nut-button>
        </div>
      </div>
      <p v-if="!groups.length" class="empty">暂无群组</p>
    </div>

    <input ref="replaceRef" type="file" accept="image/*" class="hidden" @change="onReplaceFile" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from '@nutui/nutui'
import { apiJson, apiFetch, setAdminToken } from '@/api/client'

const route = useRoute()
const groups = ref([])
const groupName = ref('')
const fileRef = ref(null)
const replaceRef = ref(null)
const replaceTarget = ref('')
const editing = ref('')
const editName = ref('')
const globalSurveyUrl = ref('')
const globalSurveyText = ref('')

async function loadGroups() {
  const { ok, data } = await apiJson('/api/admin/groups')
  if (ok && Array.isArray(data)) groups.value = data
}

async function loadSurveyConfig() {
  const { ok, data } = await apiJson('/api/admin/survey/config')
  if (!ok || !data) return
  globalSurveyUrl.value = data.globalUrl || ''
  globalSurveyText.value = data.globalButtonText || ''
}

onMounted(async () => {
  const q = route.query.group_name
  if (q) groupName.value = String(q)
  await loadGroups()
  await loadSurveyConfig()
})

function pickFile() {
  fileRef.value?.click()
}

async function onPick(e) {
  const f = e.target.files?.[0]
  e.target.value = ''
  if (!f || !groupName.value.trim()) {
    showToast.text('请填写群组名并选择图片')
    return
  }
  const fd = new FormData()
  fd.append('group_name', groupName.value.trim())
  fd.append('file', f)
  const res = await apiFetch('/api/admin/groups/upload', { method: 'POST', body: fd })
  const data = await res.json().catch(() => ({}))
  if (res.ok && data.ok) {
    showToast.success(data.message || '成功')
    await loadGroups()
  } else {
    showToast.text(data.message || '上传失败')
  }
}

function preview(name) {
  window.open('/app/group/' + encodeURIComponent(name), '_blank')
}

function replaceQr(name) {
  replaceTarget.value = name
  replaceRef.value?.click()
}

async function onReplaceFile(e) {
  const f = e.target.files?.[0]
  e.target.value = ''
  const name = replaceTarget.value
  if (!f || !name) return
  const fd = new FormData()
  fd.append('group_name', name)
  fd.append('file', f)
  const res = await apiFetch('/api/admin/groups/upload', { method: 'POST', body: fd })
  const data = await res.json().catch(() => ({}))
  if (res.ok && data.ok) {
    showToast.success(data.message || '已更新')
    await loadGroups()
  } else {
    showToast.text(data.message || '失败')
  }
}

function startEdit(name) {
  editing.value = name
  editName.value = name
}

async function commitRename(oldName) {
  const newName = editName.value.trim()
  editing.value = ''
  if (!newName || newName === oldName) return
  const { ok, data } = await apiJson('/api/admin/groups/rename', {
    method: 'POST',
    body: JSON.stringify({ old_name: oldName, new_name: newName }),
  })
  if (ok && data?.ok) {
    showToast.success('已更名')
    await loadGroups()
  } else {
    showToast.text(data?.message || '更名失败')
  }
}

async function removeGroup(name) {
  const { ok, data } = await apiJson('/api/admin/groups/' + encodeURIComponent(name), { method: 'DELETE' })
  if (ok && data?.ok) {
    showToast.success('已删除')
    await loadGroups()
  } else {
    showToast.text(data?.message || '删除失败')
  }
}

async function saveGlobalSurvey() {
  const { ok, data } = await apiJson('/api/admin/survey/config/global', {
    method: 'POST',
    body: JSON.stringify({
      survey_url: globalSurveyUrl.value,
      button_text: globalSurveyText.value,
    }),
  })
  if (ok && data?.ok) {
    showToast.success(data.message || '已保存')
    await loadSurveyConfig()
  } else {
    showToast.text(data?.message || '保存失败')
  }
}

async function logout() {
  await apiJson('/api/admin/logout', { method: 'POST', body: '{}' })
  setAdminToken(null)
  window.location.href = '/app/admin/login'
}
</script>

<style scoped>
.page { padding-bottom: 24px; }
.nav-link { font-size: 14px; color: #1989fa; padding-right: 8px; }
.menu-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding: 12px;
}
.menu-item {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  box-shadow: 0 2px 8px rgba(0,0,0,.04);
  border: 1px solid #eee;
}
.menu-item .t { display: block; font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.menu-item .d { font-size: 12px; color: #999; }
.card {
  margin: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,.05);
}
.card h4 { margin: 0 0 12px; font-size: 15px; }
.tip { margin: 0 0 10px; color: #999; font-size: 12px; }
.file, .hidden { display: none; }
.mt { margin-top: 12px; }
.mt8 { margin-top: 8px; }
.row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.name { font-weight: 600; cursor: pointer; }
.ops { display: flex; gap: 6px; flex-wrap: wrap; }
.empty { color: #999; font-size: 13px; text-align: center; padding: 16px; }
</style>
