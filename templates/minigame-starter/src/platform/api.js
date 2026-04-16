import { bridge } from "./bridge.js";

const SCORE_KEY = "nexus_minigame_best_score";

export const platformApi = {
  async ensureLogin(i18n) {
    try {
      await bridge.login();
      await bridge.showToast({ title: i18n.t("toast.login"), icon: "none" });
    } catch (error) {
      console.warn("login failed", error);
    }
  },

  async reportScore(score, i18n) {
    const current = await this.getBestScore();
    const best = Math.max(score, current);
    await bridge.setStorage({ key: SCORE_KEY, data: best });
    await bridge.showToast({ title: i18n.t("toast.best", { best }), icon: "none" });
    return best;
  },

  async getBestScore() {
    try {
      const result = await bridge.getStorage({ key: SCORE_KEY });
      return Number(result?.data || 0);
    } catch {
      return 0;
    }
  }
};
