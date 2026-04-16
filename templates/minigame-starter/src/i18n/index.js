import { messages } from "./messages.js";

export function detectLocale() {
  const raw = (navigator.language || "en").toLowerCase();
  if (raw.startsWith("zh-tw") || raw.startsWith("zh-hk") || raw.startsWith("zh-mo") || raw.includes("hant")) {
    return "zh-TW";
  }
  if (raw.startsWith("zh")) {
    return "zh-CN";
  }
  return "en";
}

export function createI18n(locale) {
  const resolved = messages[locale] ? locale : "en";
  return {
    locale: resolved,
    t(key, vars = {}) {
      const table = messages[resolved] || messages.en;
      const template = table[key] || messages.en[key] || key;
      return Object.entries(vars).reduce((output, [name, value]) => {
        return output.replaceAll(`{${name}}`, String(value));
      }, template);
    }
  };
}
