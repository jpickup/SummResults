import type { AppConfig } from './types';

/**
 * Augment the global Window interface to declare the runtime config slot
 * injected by the <script> block in index.html.
 */
declare global {
  interface Window {
    __APP_CONFIG__?: Partial<AppConfig>;
  }
}

/**
 * Read and validate the runtime configuration object injected via
 * window.__APP_CONFIG__ in index.html.
 *
 * Throws a descriptive Error if any required field is absent or empty,
 * so that App.vue can catch it and render an error state without
 * attempting any backend API calls.
 */
function loadAppConfig(): AppConfig {
  const raw = window.__APP_CONFIG__ ?? {};

  if (!raw.apiBaseUrl || raw.apiBaseUrl.trim() === '') {
    throw new Error('APP_CONFIG: apiBaseUrl is not configured');
  }

  return {
    apiBaseUrl: raw.apiBaseUrl.trim(),
    day1EventId: raw.day1EventId ?? '',
    day2EventId: raw.day2EventId ?? '',
  };
}

/**
 * The validated application configuration.
 * Exported for use by useResults.ts and any other module that needs
 * the backend base URL or event IDs.
 */
export const appConfig: AppConfig = loadAppConfig();
