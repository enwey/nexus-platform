export function createLaneSystem({ width, laneCount, sidePadding }) {
  const laneWidth = (width - sidePadding * 2) / laneCount;
  const centers = Array.from({ length: laneCount }, (_, index) => {
    return sidePadding + laneWidth * index + laneWidth / 2;
  });

  return {
    count: laneCount,
    laneWidth,
    centers,
    toLaneIndex(x) {
      const raw = Math.round((x - sidePadding - laneWidth / 2) / laneWidth);
      return Math.max(0, Math.min(laneCount - 1, raw))
    },
    toCenter(index) {
      return centers[Math.max(0, Math.min(laneCount - 1, index))]
    }
  };
}
