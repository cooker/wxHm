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
      <nut-button
        v-if="surveyUrl"
        block
        plain
        type="success"
        class="survey-btn"
        @click="openSurvey"
      >
        {{ surveyButtonText }}
      </nut-button>
    </div>
    <div class="wave-wrap" aria-hidden="true">
      <svg class="wave wave-back" viewBox="0 0 1440 120" preserveAspectRatio="none">
        <path d="M0,64L48,69.3C96,75,192,85,288,85.3C384,85,480,75,576,69.3C672,64,768,64,864,64C960,64,1056,64,1152,69.3C1248,75,1344,85,1392,90.7L1440,96L1440,120L1392,120C1344,120,1248,120,1152,120C1056,120,960,120,864,120C768,120,672,120,576,120C480,120,384,120,288,120C192,120,96,120,48,120L0,120Z" />
      </svg>
      <svg class="wave wave-front" viewBox="0 0 1440 120" preserveAspectRatio="none">
        <path d="M0,96L40,90.7C80,85,160,75,240,64C320,53,400,43,480,42.7C560,43,640,53,720,58.7C800,64,880,64,960,64C1040,64,1120,64,1200,69.3C1280,75,1360,85,1400,90.7L1440,96L1440,120L1400,120C1360,120,1280,120,1200,120C1120,120,1040,120,960,120C880,120,800,120,720,120C640,120,560,120,480,120C400,120,320,120,240,120C160,120,80,120,40,120L0,120Z" />
      </svg>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { apiJson } from '@/api/client'

const route = useRoute()
const routeName = computed(() => route.params.groupName || '')
const groupName = ref('')
const title = computed(() => (groupName.value ? `${groupName.value} · 进群` : '进群通道'))

const groupExists = ref(false)
const qrFile = ref('')
const wsrvUrl = ref('')
const todayVisitCount = ref(0)
const imgSrc = ref('')
const fallbackUrl = ref('')
const wechat = ref(false)
const surveyUrl = ref('')
const surveyButtonText = ref('填写问卷')

onMounted(async () => {
  const ua = navigator.userAgent.toLowerCase()
  wechat.value = ua.includes('micromessenger')

  const { ok, data } = await apiJson('/api/public/group/' + encodeURIComponent(routeName.value))
  if (!ok || !data) return
  groupName.value = data.groupName || routeName.value
  groupExists.value = !!data.groupExists
  qrFile.value = data.qrFile || ''
  wsrvUrl.value = data.wsrvUrl || ''
  todayVisitCount.value = data.todayVisitCount ?? 0
  imgSrc.value = data.wsrvUrl || ''
  surveyUrl.value = data.surveyUrl || ''
  surveyButtonText.value = data.surveyButtonText || '填写问卷'
  const origin = window.location.origin
  fallbackUrl.value = `${origin}/uploads/${encodeURIComponent(groupName.value || routeName.value)}/${encodeURIComponent(data.qrFile || '')}`
})

function onImgError() {
  if (fallbackUrl.value && imgSrc.value !== fallbackUrl.value) {
    imgSrc.value = fallbackUrl.value
  }
}

function saveQr() {
  if (fallbackUrl.value) window.open(fallbackUrl.value, '_blank')
}

function openSurvey() {
  if (!surveyUrl.value) return
  const clickUrl = `/api/public/group/${encodeURIComponent(routeName.value)}/survey-click`
  fetch(clickUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
    keepalive: true,
    credentials: 'include',
  }).catch(() => {})
  window.open(surveyUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<style scoped>
.page {
  position: relative;
  min-height: 100vh;
  padding-bottom: 72px;
  background: linear-gradient(180deg, #f7fafc 0%, #eef6ff 100%);
  overflow: hidden;
}
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
.survey-btn { margin: 0 0 8px; }
.save-btn { margin-top: 8px; }
.wave-wrap {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -2px;
  height: 120px;
  pointer-events: none;
}
.wave {
  position: absolute;
  left: 0;
  width: 200%;
  height: 100%;
}
.wave path { fill: rgba(153, 208, 255, 0.42); }
.wave-back {
  bottom: 0;
  opacity: 0.65;
  animation: waveMove 18s linear infinite;
}
.wave-front {
  bottom: -6px;
  opacity: 0.95;
  animation: waveMoveReverse 12s linear infinite;
}
@keyframes waveMove {
  0% { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}
@keyframes waveMoveReverse {
  0% { transform: translateX(-50%); }
  100% { transform: translateX(0); }
}
</style>
