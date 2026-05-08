export function createHud({ scoreEl, statusEl, i18n }) {
  const bestEl = document.createElement("span");
  statusEl.parentNode.insertBefore(bestEl, statusEl);
  let lastScore = 0;
  let lastBest = 0;
  let lastStatusKey = "status.ready";
  let lastStatusVars = {};

  return {
    setScore(value) {
      lastScore = value;
      scoreEl.textContent = `${i18n.t("hud.score")}: ${value}`;
    },
    setBest(value) {
      lastBest = value;
      bestEl.textContent = `${i18n.t("hud.best")}: ${value}`;
    },
    setStatusKey(key, vars) {
      lastStatusKey = key;
      lastStatusVars = vars || {};
      statusEl.textContent = i18n.t(key, lastStatusVars);
    },
    refresh() {
      scoreEl.textContent = `${i18n.t("hud.score")}: ${lastScore}`;
      bestEl.textContent = `${i18n.t("hud.best")}: ${lastBest}`;
      statusEl.textContent = i18n.t(lastStatusKey, lastStatusVars);
    }
  };
}
