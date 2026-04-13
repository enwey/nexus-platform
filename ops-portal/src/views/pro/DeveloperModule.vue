<template>
  <div class="pro-page">
    <el-card>
      <template #header>
        <div class="head-row">
          <span>{{ lt('開發者模塊', '開發者模塊', 'Developer Module') }}</span>
          <el-button type="primary" @click="loadDevelopers">{{ lt('刷新', '刷新', 'Refresh') }}</el-button>
        </div>
      </template>
      <div class="filters">
        <el-input v-model.trim="keyword" clearable style="width: 260px" :placeholder="lt('開發者 ID / 遊戲名搜索', '開發者 ID / 遊戲名搜索', 'Search developer ID / game name')" />
      </div>
      <el-table :data="filteredRows" v-loading="loading" style="width: 100%; margin-top: 12px">
        <el-table-column prop="developerId" :label="lt('開發者ID', '開發者ID', 'Developer ID')" min-width="220" />
        <el-table-column prop="gameCount" :label="lt('遊戲數', '遊戲數', 'Games')" width="90" />
        <el-table-column prop="approved" :label="lt('通過', '通過', 'Approved')" width="90" />
        <el-table-column prop="pending" :label="lt('待審核', '待審核', 'Pending')" width="90" />
        <el-table-column prop="rejected" :label="lt('拒絕', '拒絕', 'Rejected')" width="90" />
        <el-table-column :label="lt('遊戲列表', '遊戲列表', 'Games')" min-width="320">
          <template #default="{ row }">
            <el-tag v-for="name in row.games" :key="name" size="small" class="mr-6 mb-6">{{ name }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getGameList } from '../../api'
import { useI18nLite } from '../../i18n'

const { lt } = useI18nLite()
const loading = ref(false)
const keyword = ref('')
const rows = ref([])

const loadDevelopers = async () => {
  loading.value = true
  try {
    const res = await getGameList()
    const games = res.data || []
    const grouped = new Map()
    games.forEach((g) => {
      const dev = g.developerId || 'unknown'
      if (!grouped.has(dev)) {
        grouped.set(dev, {
          developerId: dev,
          gameCount: 0,
          approved: 0,
          pending: 0,
          rejected: 0,
          games: []
        })
      }
      const item = grouped.get(dev)
      item.gameCount += 1
      item.games.push(g.name || '')
      if (g.status === 'APPROVED') item.approved += 1
      if (g.status === 'PENDING') item.pending += 1
      if (g.status === 'REJECTED') item.rejected += 1
    })
    rows.value = Array.from(grouped.values()).sort((a, b) => b.gameCount - a.gameCount)
  } catch (error) {
    ElMessage.error(error.message || lt('加載開發者模塊失敗', '加載開發者模塊失敗', 'Failed to load developer module'))
  } finally {
    loading.value = false
  }
}

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key) return rows.value
  return rows.value.filter((row) => {
    const idHit = (row.developerId || '').toLowerCase().includes(key)
    const gameHit = row.games.some((name) => (name || '').toLowerCase().includes(key))
    return idHit || gameHit
  })
})

onMounted(loadDevelopers)
</script>

<style scoped>
.pro-page { display: flex; flex-direction: column; gap: 16px; }
.head-row { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; gap: 8px; align-items: center; }
.mr-6 { margin-right: 6px; }
.mb-6 { margin-bottom: 6px; }
</style>
