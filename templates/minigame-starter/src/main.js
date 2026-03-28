import { createHud } from "./ui/hud.js";
import { Game } from "./game/Game.js";
import { platformApi } from "./platform/api.js";

const canvas = document.getElementById("gameCanvas");
const startBtn = document.getElementById("startBtn");
const restartBtn = document.getElementById("restartBtn");

const hud = createHud({
  scoreEl: document.getElementById("scoreText"),
  statusEl: document.getElementById("statusText")
});

const game = new Game(canvas, {
  onScore: (value) => hud.setScore(value),
  onStatus: (text) => hud.setStatus(text),
  onGameOver: async (score) => {
    hud.setStatus("Game Over");
    await platformApi.reportScore(score);
  }
});

startBtn.addEventListener("click", async () => {
  await platformApi.ensureLogin();
  game.start();
});

restartBtn.addEventListener("click", () => {
  game.restart();
});

game.render();

