import { messages } from "./messages.js";

export function resolveLocale(rawLocale) {
  const raw = String(rawLocale || "en").toLowerCase();
  if (raw.startsWith("zh-tw") || raw.startsWith("zh-hk") || raw.startsWith("zh-mo") || raw.includes("hant")) {
    return "zh-TW";
  }
  if (raw.startsWith("zh")) {
    return "zh-CN";
  }
  return "en";
}

export function detectLocale() {
  return resolveLocale(navigator.language || "en");
}

export function createI18n(locale) {
  let resolved = messages[resolveLocale(locale)] ? resolveLocale(locale) : "en";
  return {
    get locale() {
      return resolved;
    },
    setLocale(nextLocale) {
      resolved = messages[resolveLocale(nextLocale)] ? resolveLocale(nextLocale) : "en";
      return resolved;
    },
    t(key, vars = {}) {
      const table = messages[resolved] || messages.en;
      const template = table[key] || messages.en[key] || key;
      return Object.entries(vars).reduce((output, [name, value]) => {
        return output.replaceAll(`{${name}}`, String(value));
      }, template);
    }
  };
}
