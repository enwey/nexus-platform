export function createSpawnScheduler({ minInterval, maxInterval, rng }) {
  return {
    next(score) {
      const pressure = Math.min(score / 80, 1);
      const span = maxInterval - minInterval;
      const interval = maxInterval - span * pressure;
      const wobble = (rng() - 0.5) * 110;
      return Math.max(minInterval, Math.round(interval + wobble));
    }
  };
}
