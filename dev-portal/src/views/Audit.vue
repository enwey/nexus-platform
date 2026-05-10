<template>
  <div class="legacy-page">
    <el-card class="legacy-card">
      <div class="legacy-badge">{{ lt('历史页面', '歷史頁面', 'Legacy Page') }}</div>
      <h1>{{ lt('旧版审核页已停用', '舊版審核頁已停用', 'Legacy Review Page Retired') }}</h1>
      <p>
        {{
          lt(
            '开发者后台不再提供旧版“通过 / 拒绝”审核入口，当前审核相关流程已经迁移到版本发布、申诉记录和资质页。',
            '開發者後台不再提供舊版「通過 / 拒絕」審核入口，目前審核相關流程已遷移到版本發布、申訴記錄與資質頁。',
            'The developer portal no longer exposes the old approve/reject review entry. Review workflows now live in release management, appeal records, and certification pages.'
          )
        }}
      </p>

      <el-alert
        :title="lt('这样可以避免误用旧的游戏级审核接口，影响到错误版本。', '這樣可以避免誤用舊的遊戲級審核介面，影響到錯誤版本。', 'This avoids accidentally using legacy game-level review endpoints against the wrong version.')"
        type="warning"
        :closable="false"
        show-icon
        class="legacy-alert"
      />

      <div class="legacy-actions">
        <el-button type="primary" @click="router.push('/releases/list')">
          {{ lt('前往版本发布', '前往版本發布', 'Open Release Center') }}
        </el-button>
        <el-button @click="router.push('/reviews/appeals')">
          {{ lt('查看申诉记录', '查看申訴記錄', 'View Appeals') }}
        </el-button>
        <el-button @click="router.push('/reviews/certification')">
          {{ lt('查看资质与审核', '查看資質與審核', 'Open Reviews & Certification') }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useI18nLite } from '../i18n'

const router = useRouter()
const { lt } = useI18nLite()
</script>

<style scoped>
.legacy-page {
  padding: 24px;
}

.legacy-card {
  max-width: 840px;
  border-radius: 24px;
}

.legacy-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #2859c5;
  font-size: 12px;
  font-weight: 700;
}

.legacy-card h1 {
  margin: 16px 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: #101828;
}

.legacy-card p {
  margin: 0;
  color: #475467;
  line-height: 1.7;
}

.legacy-alert {
  margin-top: 18px;
}

.legacy-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 22px;
}

@media (max-width: 768px) {
  .legacy-page {
    padding: 16px;
  }

  .legacy-card h1 {
    font-size: 22px;
  }

  .legacy-actions {
    flex-direction: column;
  }

  .legacy-actions :deep(.el-button) {
    margin-left: 0;
  }
}
</style>
