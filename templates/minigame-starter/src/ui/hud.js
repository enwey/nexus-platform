export function createHud({ scoreEl, statusEl }) {
  return {
    setScore(value) {
      scoreEl.textContent = `Score: ${value}`;
    },
    setStatus(text) {
      statusEl.textContent = `Status: ${text}`;
    }
  };
}

