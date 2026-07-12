import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { flushPromises } from '@vue/test-utils';
import { useResults } from './useResults';
import type { AppConfig, ParticipantResult } from '../types';

/**
 * Unit tests for the useResults composable.
 * Validates: Requirements 5.5, 5.6, 5.7
 */

const TEST_CONFIG: AppConfig = {
  apiBaseUrl: 'http://localhost:8080',
  day1EventId: 'E1',
  day2EventId: 'E2',
};

const SAMPLE_RESULT: ParticipantResult = {
  participantName: 'Smith John',
  day1Controls: [{ controlId: 'C1', points: 10 }],
  day1GrossScore: 10,
  day1Penalty: 0,
  day1NetScore: 10,
  day2Controls: [{ controlId: 'C2', points: 20 }],
  day2GrossScore: 20,
  day2Penalty: 0,
  day2Deduction: 0,
  day2NetScore: 20,
  totalScore: 30,
};

beforeEach(() => {
  vi.restoreAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('useResults', () => {
  it('loading is true during fetch and false after', async () => {
    // Arrange: fetch resolves only when we say so
    let resolveFetch!: (value: Response) => void;
    const pendingPromise = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });

    vi.stubGlobal('fetch', vi.fn(() => pendingPromise));

    // Act: create composable (auto-fetch fires immediately)
    const { loading } = useResults(TEST_CONFIG);

    // Assert: still in-flight
    expect(loading.value).toBe(true);

    // Resolve the fetch with a successful response
    resolveFetch({
      ok: true,
      status: 200,
      json: () => Promise.resolve([]),
    } as unknown as Response);

    await flushPromises();

    // Assert: now settled
    expect(loading.value).toBe(false);
  });

  it('error is set on non-2xx response', async () => {
    // Arrange: fetch returns a 404
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve({
          ok: false,
          status: 404,
        } as unknown as Response),
      ),
    );

    const { error, results } = useResults(TEST_CONFIG);

    await flushPromises();

    // Assert: error message contains the status code
    expect(error.value).toContain('404');
    expect(results.value).toEqual([]);
  });

  it('results are populated on 200 response', async () => {
    // Arrange: fetch returns one participant
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

    const { results, error } = useResults(TEST_CONFIG);

    await flushPromises();

    // Assert: results populated, no error
    expect(results.value).toHaveLength(1);
    expect(results.value[0].participantName).toBe('Smith John');
    expect(error.value).toBeNull();
  });

  it('refresh is disabled while a fetch is in flight', async () => {
    // Arrange: fetch stays pending so loading stays true
    let resolveFetch!: (value: Response) => void;
    const pendingPromise = new Promise<Response>((resolve) => {
      resolveFetch = resolve;
    });

    const mockFetch = vi.fn(() => pendingPromise);
    vi.stubGlobal('fetch', mockFetch);

    const { refresh, loading } = useResults(TEST_CONFIG);

    // loading should be true — the auto-fetch is in flight
    expect(loading.value).toBe(true);
    expect(mockFetch).toHaveBeenCalledTimes(1);

    // Attempt a second fetch while the first is still in flight
    refresh();
    refresh();

    // Should still only have been called once
    expect(mockFetch).toHaveBeenCalledTimes(1);

    // Clean up: resolve the pending fetch
    resolveFetch({
      ok: true,
      status: 200,
      json: () => Promise.resolve([]),
    } as unknown as Response);

    await flushPromises();
  });
});
