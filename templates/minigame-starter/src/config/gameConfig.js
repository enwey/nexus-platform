export const GAME_CONFIG = {
  canvas: {
    width: 420,
    height: 640
  },
  player: {
    width: 34,
    height: 42,
    baseY: 570,
    moveSmoothing: 0.18
  },
  obstacle: {
    width: 40,
    height: 40,
    baseSpeed: 245,
    maxSpeed: 430
  },
  lanes: {
    count: 6,
    sidePadding: 24
  },
  difficulty: {
    minSpawnInterval: 260,
    maxSpawnInterval: 760,
    speedRampPerScore: 1.9
  }
};
