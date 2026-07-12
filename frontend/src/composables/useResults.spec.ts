import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { useResults } from './useResults';
import type { TeamResult } from '../types';

/**
 * Unit tests for the useResults composable.
 */

const API_BASE = 'http://localhost:8080';
const EVENT_ID = 'SUMM-2026';

const SAMPLE_RESULT: TeamResult = {
  teamName: 'Smith & Jones',
  members: ['Smith John', 'Jones Alice'],
  day1NetScore: 100,
  day2NetScore: 80,
  totalScore: 180,
};

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('useResults', () => {
  it('loading is true during fetch and false after', async () => {
    let resolveFetch!: (value: Response) => void;
    const pendingPromise = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });

    vi.stubGlobal('fetch', vi.fn(() => pendingPromise));

    const { loading } = useResults(API_BASE, EVENT_ID);

    expect(loading.value).toBe(true);

    resolveFetch({
      ok: true,
      status: 200,
      json: () => Promise.resolve([]),
    } as unknown as Response);

    await flushPromises();

    expect(loading.value).toBe(false);
  });

  it('error is set on non-2xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 404,
        } as unknown as Response),
      ),
    );

    const { error, results } = useResults(API_BASE, EVENT_ID);

    await flushPromises();

    expect(error.value).toContain('404');
    expect(results.value).toEqual([]);
  });

  it('results are populated on 200 response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve([SAMPLE_RESULT]),
        } as unknown as Response),
      ),
    );

    const { results, error } = useResults(API_BASE, EVENT_ID);

    await flushPromises();

    expect(results.value).toHaveLength(1);
    expect(results.value[0].teamName).toBe('Smith & Jones');
    expect(results.value[0].totalScore).toBe(180);
    expect(error.value).toBeNull();
  });

  it('builds the correct URL with encoded eventId', async () => {
    const mockFetch = vi.fn(() =>
      Promise.resolve({
        ok: true,
        status: 200,
        json: () => Promise.resolve([]),
      } as unknown as Response),
    );
    vi.stubGlobal('fetch', mockFetch);

    useResults(API_BASE, EVENT_ID);
    await flushPromises();

    expect(mockFetch).toHaveBeenCalledWith(
      `${API_BASE}/api/results?eventId=${encodeURIComponent(EVENT_ID)}`,
    );
  });

  it('refresh is disabled while a fetch is in flight', async () => {
    let resolveFetch!: (value: Response) => void;
    const pendingPromise = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });

    const mockFetch = vi.fn(() => pendingPromise);
    vi.stubGlobal('fetch', mockFetch);

    const { refresh, loading } = useResults(API_BASE, EVENT_ID);

    expect(loading.value).toBe(true);
    expect(mockFetch).toHaveBeenCalledTimes(1);

    refresh();
    refresh();

    expect(mockFetch).toHaveBeenCalledTimes(1);

    resolveFetch({
      ok: true,
      status: 200,
      json: () => Promise.resolve([]),
    } as unknown as Response);

    await flushPromises();
  });
});
