export async function loadGameManifest() {
  try {
    const response = await fetch("./manifest.json", { cache: "no-store" });
    if (!response.ok) {
      throw new Error(`manifest ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.warn("manifest load failed", error);
    return {};
  }
}

export function resolveManifestLocale(manifest, locale) {
  const metadata = manifest.metadata || {};
  const locales = metadata.locales || manifest.locales || {};
  const defaultLocale = metadata.defaultLocale || "en";
  const tag = pickLocaleTag(locales, locale, defaultLocale);
  const entry = locales[tag] || {};

  return {
    tag,
    name: entry.name || manifest.name || "Nexus Mini Game",
    description: entry.description || manifest.description || ""
  };
}

export function resolveIconPath(manifest) {
  const metadata = manifest.metadata || {};
  return metadata.icon || manifest.icon || "";
}

function pickLocaleTag(locales, locale, defaultLocale) {
  if (locales[locale]) return locale;
  const languageOnly = locale.split("-")[0];
  const direct = Object.keys(locales).find((key) => key.toLowerCase() === languageOnly.toLowerCase());
  if (direct) return direct;
  const prefixed = Object.keys(locales).find((key) => key.toLowerCase().startsWith(`${languageOnly.toLowerCase()}-`));
  if (prefixed) return prefixed;
  if (locales[defaultLocale]) return defaultLocale;
  return Object.keys(locales)[0] || defaultLocale;
}
