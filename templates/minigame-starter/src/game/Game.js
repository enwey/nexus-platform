export class Game {
  constructor(canvas, hooks) {
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.hooks = hooks;

    this.running = false;
    this.score = 0;
    this.lastTime = 0;
    this.speed = 290;
    this.nextSpawn = 0;
    this.runnerX = canvas.width / 2;
    this.runnerY = canvas.height - 70;
    this.blockers = [];

    this.boundTick = this.tick.bind(this);
    this.bindInput();
  }

  bindInput() {
    window.addEventListener("keydown", (e) => {
      if (!this.running) return;
      if (e.key === "ArrowLeft") this.runnerX -= 28;
      if (e.key === "ArrowRight") this.runnerX += 28;
      this.clampRunner();
    });

    this.canvas.addEventListener("touchstart", (event) => {
      if (!this.running) return;
      const touch = event.touches[0];
      if (!touch) return;
      const rect = this.canvas.getBoundingClientRect();
      const x = touch.clientX - rect.left;
      this.runnerX = x;
      this.clampRunner();
    }, { passive: true });
  }

  start() {
    if (this.running) return;
    this.running = true;
    this.hooks.onStatus("Running");
    requestAnimationFrame(this.boundTick);
  }

  restart() {
    this.running = false;
    this.score = 0;
    this.lastTime = 0;
    this.nextSpawn = 0;
    this.blockers = [];
    this.runnerX = this.canvas.width / 2;
    this.runnerY = this.canvas.height - 70;
    this.hooks.onScore(this.score);
    this.hooks.onStatus("Ready");
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
    if (ts >= this.nextSpawn) {
      this.nextSpawn = ts + 620;
      this.spawnBlocker();
      this.score += 1;
      this.hooks.onScore(this.score);
    }

    for (const blocker of this.blockers) {
      blocker.y += (this.speed * dt) / 1000;
    }
    this.blockers = this.blockers.filter((b) => b.y < this.canvas.height + 32);

    if (this.hit()) {
      this.running = false;
      this.hooks.onGameOver(this.score);
    }
  }

  hit() {
    const w = 36;
    const h = 36;
    const rx = this.runnerX - w / 2;
    const ry = this.runnerY - h / 2;
    return this.blockers.some((b) => {
      return rx < b.x + b.w && rx + w > b.x && ry < b.y + b.h && ry + h > b.y;
    });
  }

  spawnBlocker() {
    const width = 40;
    const lane = Math.floor(Math.random() * 6);
    this.blockers.push({
      x: 18 + lane * 66,
      y: -48,
      w: width,
      h: width
    });
  }

  clampRunner() {
    this.runnerX = Math.max(20, Math.min(this.canvas.width - 20, this.runnerX));
  }

  render() {
    const { ctx, canvas } = this;
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const g = ctx.createLinearGradient(0, 0, 0, canvas.height);
    g.addColorStop(0, "#081128");
    g.addColorStop(1, "#02060f");
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = "#1c2f60";
    for (let i = 1; i < 6; i += 1) {
      const x = i * 70;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, canvas.height);
      ctx.stroke();
    }

    ctx.fillStyle = "#ff697f";
    for (const b of this.blockers) {
      ctx.fillRect(b.x, b.y, b.w, b.h);
    }

    ctx.fillStyle = "#68d7ff";
    ctx.fillRect(this.runnerX - 18, this.runnerY - 18, 36, 36);
  }
}

