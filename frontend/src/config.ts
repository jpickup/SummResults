import type { AppConfig } from './types';

/**
 * Read and validate the backend API base URL.
 *
 * Source priority:
 *  1. <meta name="api-base-url"> in index.html — set at container startup
 *     by the frontend Dockerfile's envsubst step (Docker / production).
 *  2. import.meta.env.VITE_API_BASE_URL — set in .env for local `npm run dev`.
 *
 * Throws a descriptive Error if neither source yields a usable URL.
 */
function loadAppConfig(): AppConfig {
  // Read the meta tag value set by envsubst at container startup.
  const metaTag = document.querySelector<HTMLMetaElement>('meta[name="api-base-url"]');
  const metaValue = metaTag?.content ?? '';

  // If the placeholder was never substituted it will still be the literal
  // token we put in index.html. Treat that as "not configured".
  const isPlaceholder = metaValue === '' || metaValue === '__API_BASE_URL__';

  const apiBaseUrl = isPlaceholder
    ? (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? ''
    : metaValue;

  if (!apiBaseUrl || apiBaseUrl.trim() === '') {
    throw new Error(
      'apiBaseUrl is not configured. ' +
      'For Docker: ensure API_BASE_URL is set in your environment. ' +
      'For local dev: set VITE_API_BASE_URL in .env.'
    );
  }

  return { apiBaseUrl: apiBaseUrl.trim() };
}

export const appConfig: AppConfig = loadAppConfig();
