<template>
  <div class="page">
    <nut-navbar title="数据看板" left-show @click-back="$router.push('/admin')" safe-area-inset-top>
      <template #right>
        <span class="nav-link" @click="doLogout">退出</span>
      </template>
    </nut-navbar>
    <div class="toolbar">
      <nut-checkbox v-model="auto">自动刷新</nut-checkbox>
      <nut-button size="small" type="primary" @click="refresh">刷新</nut-button>
    </div>
    <div v-for="(item, i) in chartData" :key="item.groupName || i" class="card">
      <h4>{{ item.groupName }}</h4>
      <div class="charts">
        <div :ref="el => setTrendRef(el, i)" class="box trend" />
        <div :ref="el => setPieRef(el, i)" class="box pie" />
      </div>
    </div>
    <p v-if="!chartData.length" class="empty">暂无统计数据</p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { apiJson, setAdminToken } from '@/api/client'
import { showToast } from '@nutui/nutui'

const router = useRouter()
const chartData = ref([])
const auto = ref(false)
let timer = null
const trendRefs = []
const pieRefs = []
const trendCharts = []
const pieCharts = []

function setTrendRef(el, i) {
  if (el) trendRefs[i] = el
}
function setPieRef(el, i) {
  if (el) pieRefs[i] = el
}

function buildOption(item) {
  const trendArr = Array.isArray(item.trend) ? item.trend : []
  const pieArr = Array.isArray(item.pie) ? item.pie : []
  return {
    trend: {
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendArr.map(t => (t?.date ? String(t.date).slice(5) : '')) },
      yAxis: { type: 'value' },
      series: [
        { name: 'PV', type: 'line', smooth: true, areaStyle: { opacity: 0.1 }, data: trendArr.map(t => t?.pv ?? 0), itemStyle: { color: '#1890ff' } },
        { name: 'UV', type: 'line', smooth: true, data: trendArr.map(t => t?.uv ?? 0), itemStyle: { color: '#07c160' } },
      ],
    },
    pie: {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: pieArr.length ? pieArr.map(p => ({ name: p.name, value: p.value })) : [{ name: '无数据', value: 0 }],
      }],
    },
  }
}

function disposeAll() {
  trendCharts.forEach(c => c?.dispose())
  pieCharts.forEach(c => c?.dispose())
  trendCharts.length = 0
  pieCharts.length = 0
}

function renderCharts() {
  disposeAll()
  chartData.value.forEach((item, i) => {
    const opt = buildOption(item)
    if (trendRefs[i]) {
      const c = echarts.init(trendRefs[i])
      c.setOption(opt.trend)
      trendCharts[i] = c
    }
    if (pieRefs[i]) {
      const c = echarts.init(pieRefs[i])
      c.setOption(opt.pie)
      pieCharts[i] = c
    }
  })
}

async function loadData() {
  const { ok, data } = await apiJson('/api/admin/stats/data')
  if (!ok) {
    showToast.text('加载失败')
    return
  }
  if (Array.isArray(data)) {
    chartData.value = data.map((row, i) => ({
      groupName: row.groupName ?? `群组${i + 1}`,
      trend: row.trend,
      pie: row.pie,
    }))
    await nextTick()
    renderCharts()
  }
}

async function refresh() {
  await loadData()
  showToast.success('已更新')
}

watch(auto, (v) => {
  if (timer) clearInterval(timer)
  timer = null
  if (v) {
    timer = setInterval(loadData, 15000)
  }
})

onMounted(async () => {
  await loadData()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  disposeAll()
})

async function doLogout() {
  await apiJson('/api/admin/logout', { method: 'POST', body: '{}' })
  setAdminToken(null)
  router.push('/admin/login')
}
</script>

<style scoped>
.page { padding-bottom: 24px; }
.nav-link { font-size: 14px; color: #1989fa; padding-right: 8px; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
}
.card {
  margin: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(0,0,0,.05);
}
.card h4 { margin: 0 0 8px; padding-left: 8px; border-left: 4px solid #07c160; }
.charts { display: flex; flex-wrap: wrap; gap: 12px; }
.box { min-width: 280px; flex: 1; height: 280px; }
.empty { text-align: center; color: #999; padding: 24px; }
</style>
