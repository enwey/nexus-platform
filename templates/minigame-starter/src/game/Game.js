import { RunnerScene } from "../scenes/RunnerScene.js";

export class Game {
  constructor(canvas, hooks) {
    this.scene = new RunnerScene(canvas, hooks);
  }

  start() {
    this.scene.start();
  }

  restart() {
    this.scene.restart();
  }

  render() {
    this.scene.render();
  }
}
