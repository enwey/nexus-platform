import { createSeededRandom } from "../algorithms/random.js";
import { createSpawnScheduler } from "../algorithms/spawnScheduler.js";
import { GAME_CONFIG } from "../config/gameConfig.js";
import { createLaneSystem } from "../systems/lanes.js";

export class RunnerScene {
  constructor(canvas, hooks) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.hooks = hooks;

    this.random = createSeededRandom();
    this.scheduler = createSpawnScheduler({
      minInterval: GAME_CONFIG.difficulty.minSpawnInterval,
      maxInterval: GAME_CONFIG.difficulty.maxSpawnInterval,
      rng: this.random
    });
    this.lanes = createLaneSystem({
      width: canvas.width,
      laneCount: GAME_CONFIG.lanes.count,
      sidePadding: GAME_CONFIG.lanes.sidePadding
    });

    this.running = false;
    this.score = 0;
    this.lastTime = 0;
    this.nextSpawnAt = 0;
    this.playerLane = Math.floor(this.lanes.count / 2);
    this.playerX = this.lanes.toCenter(this.playerLane);
    this.obstacles = [];

    this.boundTick = this.tick.bind(this);
    this.bindInput();
  }

  bindInput() {
    window.addEventListener("keydown", (event) => {
      if (!this.running) return;
      if (event.key === "ArrowLeft" || event.key.toLowerCase() === "a") {
        this.playerLane -= 1;
      } else if (event.key === "ArrowRight" || event.key.toLowerCase() === "d") {
        this.playerLane += 1;
      } else {
        return;
      }
      this.playerLane = Math.max(0, Math.min(this.lanes.count - 1, this.playerLane));
    });

    const moveToTouch = (clientX) => {
      const rect = this.canvas.getBoundingClientRect();
      const x = ((clientX - rect.left) / rect.width) * this.canvas.width;
      this.playerLane = this.lanes.toLaneIndex(x);
    };

    this.canvas.addEventListener("touchstart", (event) => {
      if (!this.running) return;
      if (event.touches[0]) moveToTouch(event.touches[0].clientX);
    }, { passive: true });

    this.canvas.addEventListener("touchmove", (event) => {
      if (!this.running) return;
      if (event.touches[0]) moveToTouch(event.touches[0].clientX);
    }, { passive: true });
  }

  start() {
    if (this.running) return;
    this.running = true;
    this.hooks.onStatus("status.running");
    requestAnimationFrame(this.boundTick);
  }

  restart() {
    this.running = false;
    this.score = 0;
    this.lastTime = 0;
    this.nextSpawnAt = 0;
    this.playerLane = Math.floor(this.lanes.count / 2);
    this.playerX = this.lanes.toCenter(this.playerLane);
    this.obstacles = [];
    this.hooks.onScore(0);
    this.hooks.onStatus("status.ready");
    this.render();
  }

  tick(ts) {
    if (!this.running) return;
    if (!this.lastTime) this.lastTime = ts;
    const dt = ts - this.lastTime;
    this.lastTime = ts;

    this.update(dt, ts);
    this.render();
    requestAnimationFrame(this.boundTick);
  }

  update(dt, ts) {
    if (ts >= this.nextSpawnAt) {
      this.spawnObstacle();
      this.nextSpawnAt = ts + this.scheduler.next(this.score);
      this.score += 1;
      this.hooks.onScore(this.score);
    }

    const targetX = this.lanes.toCenter(this.playerLane);
    this.playerX += (targetX - this.playerX) * GAME_CONFIG.player.moveSmoothing;

    const speed = Math.min(
      GAME_CONFIG.obstacle.baseSpeed + this.score * GAME_CONFIG.difficulty.speedRampPerScore,
      GAME_CONFIG.obstacle.maxSpeed
    );
    for (const obstacle of this.obstacles) {
      obstacle.y += (speed * dt) / 1000;
    }
    this.obstacles = this.obstacles.filter((item) => item.y < this.canvas.height + 64);

    if (this.hit()) {
      this.running = false;
      this.hooks.onGameOver(this.score);
    }
  }

  spawnObstacle() {
    const lane = Math.floor(this.random() * this.lanes.count);
    const width = GAME_CONFIG.obstacle.width;
    const height = GAME_CONFIG.obstacle.height;
    this.obstacles.push({
      x: this.lanes.toCenter(lane) - width / 2,
      y: -height - 16,
      w: width,
      h: height,
      hue: 190 + Math.round(this.random() * 130)
    });
  }

  hit() {
    const player = {
      x: this.playerX - GAME_CONFIG.player.width / 2,
      y: GAME_CONFIG.player.baseY - GAME_CONFIG.player.height / 2,
      w: GAME_CONFIG.player.width,
      h: GAME_CONFIG.player.height
    };
    return this.obstacles.some((obstacle) => {
      return player.x < obstacle.x + obstacle.w &&
        player.x + player.w > obstacle.x &&
        player.y < obstacle.y + obstacle.h &&
        player.y + player.h > obstacle.y;
    });
  }

  render() {
    const { ctx, canvas } = this;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const background = ctx.createLinearGradient(0, 0, 0, canvas.height);
    background.addColorStop(0, "#07172d");
    background.addColorStop(1, "#030814");
    ctx.fillStyle = background;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    this.drawLaneGrid();
    this.drawPlayer();
    this.drawObstacles();
  }

  drawLaneGrid() {
    const { ctx, canvas } = this;
    ctx.strokeStyle = "rgba(61, 116, 205, 0.35)";
    ctx.lineWidth = 2;
    for (let index = 1; index < this.lanes.count; index += 1) {
      const x = GAME_CONFIG.lanes.sidePadding + this.lanes.laneWidth * index;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }
  }

  drawPlayer() {
    const { ctx } = this;
    ctx.save();
    ctx.translate(this.playerX, GAME_CONFIG.player.baseY);
    ctx.fillStyle = "#68d7ff";
    ctx.beginPath();
    ctx.moveTo(0, -26);
    ctx.lineTo(18, 18);
    ctx.lineTo(0, 12);
    ctx.lineTo(-18, 18);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = "rgba(255,255,255,0.7)";
    ctx.fillRect(-4, -10, 8, 18);
    ctx.restore();
  }

  drawObstacles() {
    const { ctx } = this;
    for (const obstacle of this.obstacles) {
      const fill = ctx.createLinearGradient(obstacle.x, obstacle.y, obstacle.x, obstacle.y + obstacle.h);
      fill.addColorStop(0, `hsl(${obstacle.hue} 95% 68%)`);
      fill.addColorStop(1, "#ff657d");
      ctx.fillStyle = fill;
      ctx.fillRect(obstacle.x, obstacle.y, obstacle.w, obstacle.h);
    }
  }
}
