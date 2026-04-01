<template>
  <div class="home-page">
    <nut-navbar title="wxHm" safe-area-inset-top class="nav-bar" />
    <div class="home-inner">
      <section class="hero">
        <div class="hero-content">
          <nut-tag type="success" class="tag">Open Source</nut-tag>
          <h1>wxHm 智能活码</h1>
          <p class="desc">
            微信群二维码轮转与访问统计，私有化部署。固定链接对外，后台随时更换群码图片。
          </p>
          <div class="btns">
            <nut-button v-if="githubUrl" type="default" class="btn-pc" @click="openGithub">GitHub 源码</nut-button>
            <nut-button type="primary" class="btn-pc" @click="$router.push('/admin')">管理后台</nut-button>
          </div>
        </div>
      </section>

      <section class="features" aria-labelledby="features-title">
        <h2 id="features-title" class="features-heading">能力</h2>
        <div class="feature-grid">
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">🔗</span>
            <h3>活码分发</h3>
            <p>固定链接，后台换图，访客始终访问同一入口。</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">📊</span>
            <h3>数据看板</h3>
            <p>7 日 PV/UV 趋势与设备分布，便于运营决策。</p>
          </article>
          <article class="feature-card">
            <span class="feature-icon" aria-hidden="true">📢</span>
            <h3>公众号通知</h3>
            <p>模板消息可选配置，运维与过期提醒触达管理员。</p>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiJson } from '@/api/client'

const githubUrl = ref('')

onMounted(async () => {
  const { ok, data } = await apiJson('/api/public/home')
  if (ok && data?.githubUrl) githubUrl.value = data.githubUrl
})

function openGithub() {
  if (githubUrl.value) window.open(githubUrl.value, '_blank')
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f8f9fa;
}

/* 导航在宽屏与内容区对齐 */
:deep(.nut-navbar) {
  max-width: 1120px;
  margin-left: auto;
  margin-right: auto;
}

.home-inner {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 20px 48px;
  box-sizing: border-box;
}

.hero {
  text-align: center;
  padding: 28px 0 36px;
  background: linear-gradient(165deg, #ecfdf5 0%, #f8fafc 45%, #ffffff 100%);
  border-radius: 0 0 20px 20px;
  margin: 0 -20px 32px;
  padding-left: 20px;
  padding-right: 20px;
}

.hero-content {
  max-width: 640px;
  margin: 0 auto;
}

.tag {
  margin-bottom: 14px;
}

h1 {
  margin: 0 0 14px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.desc {
  color: #64748b;
  font-size: 15px;
  margin: 0 0 24px;
  line-height: 1.65;
}

.btns {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.features-heading {
  margin: 0 0 20px;
  font-size: 17px;
  font-weight: 600;
  color: #334155;
  padding-left: 4px;
}

.feature-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 14px;
}

.feature-card {
  background: #fff;
  border-radius: 14px;
  padding: 20px 18px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.feature-card:hover {
  border-color: #bbf7d0;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.feature-icon {
  display: block;
  font-size: 28px;
  line-height: 1;
  margin-bottom: 10px;
}

.feature-card h3 {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.feature-card p {
  margin: 0;
  font-size: 14px;
  color: #64748b;
  line-height: 1.55;
}

/* —— 平板 —— */
@media (min-width: 640px) {
  .home-inner {
    padding: 0 28px 56px;
  }

  .hero {
    margin: 0 -28px 36px;
    padding: 40px 28px 40px;
    border-radius: 0 0 24px 24px;
  }

  h1 {
    font-size: 32px;
  }

  .desc {
    font-size: 16px;
  }

  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
}

/* —— PC 桌面 —— */
@media (min-width: 960px) {
  :deep(.nut-navbar) {
    max-width: 1120px;
    padding: 12px 24px;
  }

  .home-inner {
    padding: 0 32px 64px;
  }

  .hero {
    margin: 0 0 48px;
    padding: 56px 48px 56px;
    border-radius: 16px;
    text-align: left;
  }

  .hero-content {
    max-width: none;
    margin: 0;
  }

  h1 {
    font-size: 40px;
    margin-bottom: 16px;
  }

  .desc {
    font-size: 17px;
    max-width: 52ch;
    margin-bottom: 28px;
  }

  .btns {
    justify-content: flex-start;
  }

  .btn-pc {
    min-width: 132px;
    height: 42px;
    font-size: 15px;
  }

  .features-heading {
    font-size: 18px;
    margin-bottom: 24px;
  }

  .feature-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
  }

  .feature-card {
    padding: 24px 22px;
    min-height: 168px;
    display: flex;
    flex-direction: column;
  }

  .feature-icon {
    font-size: 32px;
    margin-bottom: 14px;
  }

  .feature-card h3 {
    font-size: 17px;
  }

  .feature-card p {
    font-size: 15px;
  }
}

/* 超宽屏：避免 Hero 拉满整行过于空旷 */
@media (min-width: 1280px) {
  .home-inner {
    max-width: 1200px;
  }

  :deep(.nut-navbar) {
    max-width: 1200px;
  }
}
</style>
