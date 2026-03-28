import { bridge } from "./bridge.js";

const SCORE_KEY = "nexus_minigame_best_score";

export const platformApi = {
  async ensureLogin() {
    try {
      await bridge.login();
      await bridge.showToast({ title: "Login ok", icon: "none" });
    } catch (error) {
      console.warn("login failed", error);
    }
  },

  async reportScore(score) {
    const current = await this.getBestScore();
    const best = Math.max(score, current);
    await bridge.setStorage({ key: SCORE_KEY, data: best });
    await bridge.showToast({ title: `Best: ${best}`, icon: "none" });
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

