/**
 * Tests for config.ts
 *
 * Because config.ts executes loadAppConfig() at module import time, each test
 * must reset the module registry and re-import the module dynamically so the
 * validation logic runs fresh against the window.__APP_CONFIG__ value set for
 * that test.
 */

import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { AppConfig } from './types';

// Augment Window for TypeScript
declare global {
  interface Window {
    __APP_CONFIG__?: Partial<AppConfig>;
  }
}

describe('config.ts', () => {
  beforeEach(() => {
    // Reset module registry so config.ts re-executes on next dynamic import
    vi.resetModules();
  });

  it('throws when apiBaseUrl is absent', async () => {
    window.__APP_CONFIG__ = {};

    await expect(import('./config')).rejects.toThrow('apiBaseUrl is not configured');
  });

  it('throws when apiBaseUrl is empty string', async () => {
    window.__APP_CONFIG__ = { apiBaseUrl: '' };

    await expect(import('./config')).rejects.toThrow('apiBaseUrl is not configured');
  });

  it('returns correct config when apiBaseUrl is provided', async () => {
    window.__APP_CONFIG__ = {
      apiBaseUrl: 'http://localhost:8080',
    };

    const { appConfig } = await import('./config');

    expect(appConfig.apiBaseUrl).toBe('http://localhost:8080');
  });

  it('trims whitespace from apiBaseUrl', async () => {
    window.__APP_CONFIG__ = { apiBaseUrl: '  http://localhost:8080  ' };

    const { appConfig } = await import('./config');

    expect(appConfig.apiBaseUrl).toBe('http://localhost:8080');
  });
});
