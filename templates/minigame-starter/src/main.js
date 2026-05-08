import { createHud } from "./ui/hud.js";
import { Game } from "./game/Game.js";
import { platformApi } from "./platform/api.js";
import { createI18n, detectLocale } from "./i18n/index.js";
import { loadGameManifest, resolveIconPath, resolveManifestLocale } from "./platform/manifest.js";

const canvas = document.getElementById("gameCanvas");
const startBtn = document.getElementById("startBtn");
const restartBtn = document.getElementById("restartBtn");
const titleEl = document.getElementById("gameTitle");
const descriptionEl = document.getElementById("gameDescription");
const hintEl = document.getElementById("hintText");
const logoEl = document.getElementById("gameLogo");
const logoFallbackEl = document.getElementById("logoFallback");

const i18n = createI18n(detectLocale());
const manifest = await loadGameManifest();

const iconPath = resolveIconPath(manifest);
if (iconPath) {
  logoEl.src = iconPath;
  logoEl.hidden = false;
  logoEl.addEventListener("load", () => {
    logoFallbackEl.hidden = true;
  }, { once: true });
  logoEl.addEventListener("error", () => {
    logoEl.hidden = true;
    logoFallbackEl.hidden = false;
  }, { once: true });
}

const hud = createHud({
  scoreEl: document.getElementById("scoreText"),
  statusEl: document.getElementById("statusText"),
  i18n
});

const game = new Game(canvas, {
  i18n,
  onScore: (value) => hud.setScore(value),
  onStatus: (key, vars) => hud.setStatusKey(key, vars),
  onBestScore: (value) => hud.setBest(value),
  onGameOver: async (score) => {
    hud.setStatusKey("status.gameOver");
    const best = await platformApi.reportScore(score, i18n);
    hud.setBest(best);
  }
});

function applyLocalizedPresentation(rawLocale) {
  const resolvedLocale = i18n.setLocale(rawLocale);
  const manifestLocale = resolveManifestLocale(manifest, resolvedLocale);

  document.documentElement.lang = manifestLocale.tag;
  document.title = manifestLocale.name;
  titleEl.textContent = manifestLocale.name;
  descriptionEl.textContent = manifestLocale.description;
  hintEl.textContent = i18n.t("hint.move");
  startBtn.textContent = i18n.t("action.start");
  restartBtn.textContent = i18n.t("action.restart");
  logoFallbackEl.textContent = manifestLocale.name.slice(0, 1).toUpperCase();
  hud.refresh();
}

startBtn.addEventListener("click", async () => {
  await platformApi.ensureLogin(i18n);
  game.start();
});

restartBtn.addEventListener("click", () => {
  game.restart();
});

window.addEventListener("nexuslanguagechange", (event) => {
  applyLocalizedPresentation(event?.detail?.language || detectLocale());
});

applyLocalizedPresentation(detectLocale());
hud.setBest(await platformApi.getBestScore());
game.render();
