<template>
  <div class="page">
    <nut-navbar :title="title" left-show @click-back="$router.push('/')" safe-area-inset-top />
    <div v-if="wechat" class="wechat-tip">
      <strong>建议使用浏览器打开</strong>
      <span>在微信内可点右上角「···」选择在外部浏览器打开，体验更好。</span>
    </div>
    <div class="card">
      <h3>{{ groupName }}</h3>
      <div class="visit-bar" v-if="groupExists">
        <span class="marquee">本群入口今日访问 <strong>{{ todayVisitCount }}</strong> 次</span>
      </div>
      <template v-if="qrFile">
        <div class="qr-box">
          <div class="scanner" />
          <img
            class="qr-img"
            :src="imgSrc"
            alt="群二维码"
            @error="onImgError"
          />
        </div>
        <p class="ok">● 入口已激活</p>
        <p class="hint">长按上方二维码识别并加入群聊</p>
        <nut-button type="primary" block class="save-btn" @click="saveQr">保存到本地</nut-button>
      </template>
      <p v-else class="empty">暂无有效二维码</p>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { apiJson } from '@/api/client'

const route = useRoute()
const groupName = computed(() => route.params.groupName || '')
const title = computed(() => (groupName.value ? `${groupName.value} · 进群` : '进群通道'))

const groupExists = ref(false)
const qrFile = ref('')
const wsrvUrl = ref('')
const todayVisitCount = ref(0)
const imgSrc = ref('')
const fallbackUrl = ref('')
const wechat = ref(false)

onMounted(async () => {
  const ua = navigator.userAgent.toLowerCase()
  wechat.value = ua.includes('micromessenger')

  const { ok, data } = await apiJson('/api/public/group/' + encodeURIComponent(groupName.value))
  if (!ok || !data) return
  groupExists.value = !!data.groupExists
  qrFile.value = data.qrFile || ''
  wsrvUrl.value = data.wsrvUrl || ''
  todayVisitCount.value = data.todayVisitCount ?? 0
  imgSrc.value = data.wsrvUrl || ''
  const origin = window.location.origin
  fallbackUrl.value = `${origin}/uploads/${encodeURIComponent(groupName.value)}/${encodeURIComponent(data.qrFile || '')}`
})

function onImgError() {
  if (fallbackUrl.value && imgSrc.value !== fallbackUrl.value) {
    imgSrc.value = fallbackUrl.value
  }
}

function saveQr() {
  if (fallbackUrl.value) window.open(fallbackUrl.value, '_blank')
}
</script>

<style scoped>
.page { min-height: 100vh; background: #f0f2f5; }
.wechat-tip {
  background: linear-gradient(135deg, #07c160, #06ae56);
  color: #fff;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 1.5;
}
.wechat-tip strong { display: block; margin-bottom: 4px; }
.card {
  margin: 16px;
  padding: 24px 16px;
  background: #fff;
  border-radius: 16px;
  text-align: center;
  box-shadow: 0 8px 24px rgba(0,0,0,.06);
}
h3 { margin: 0 0 12px; font-size: 18px; }
.visit-bar {
  overflow: hidden;
  height: 36px;
  line-height: 36px;
  margin-bottom: 16px;
  background: #f6fff9;
  border-radius: 8px;
  border: 1px solid #e0eee4;
}
.marquee {
  display: inline-block;
  white-space: nowrap;
  font-size: 13px;
  color: #666;
  animation: scroll 16s linear infinite;
}
.marquee strong { color: #07c160; }
@keyframes scroll {
  0% { transform: translateX(100%); }
  100% { transform: translateX(-100%); }
}
.qr-box {
  position: relative;
  width: 250px;
  height: 250px;
  margin: 0 auto 16px;
  background: #fafafa;
  border: 1px solid #eee;
  overflow: hidden;
}
.scanner {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #07c160, transparent);
  animation: scan 3s linear infinite;
  z-index: 1;
  pointer-events: none;
}
@keyframes scan {
  0% { top: 0; }
  100% { top: 100%; }
}
.qr-img { width: 100%; height: 100%; object-fit: contain; }
.ok { color: #07c160; font-weight: 600; margin: 8px 0; }
.hint { color: #888; font-size: 13px; margin: 0 0 12px; }
.empty { color: #999; padding: 40px 0; }
.save-btn { margin-top: 8px; }
</style>
