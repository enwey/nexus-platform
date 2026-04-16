export function createHud({ scoreEl, statusEl, i18n }) {
  const bestEl = document.createElement("span");
  statusEl.parentNode.insertBefore(bestEl, statusEl);

  return {
    setScore(value) {
      scoreEl.textContent = `${i18n.t("hud.score")}: ${value}`;
    },
    setBest(value) {
      bestEl.textContent = `${i18n.t("hud.best")}: ${value}`;
    },
    setStatusKey(key, vars) {
      statusEl.textContent = i18n.t(key, vars);
    }
  };
}
