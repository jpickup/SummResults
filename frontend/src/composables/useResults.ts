import { ref } from 'vue';
import type { Ref } from 'vue';
import type { AppConfig, ParticipantResult } from '../types';

/**
 * Composable that fetches processed results from the backend API.
 *
 * - Immediately triggers a fetch on creation (auto-fetch).
 * - Exposes loading/error/results state as reactive refs.
 * - Guards against stacked requests: refresh() is a no-op while a fetch
 *   is already in flight.
 *
 * Validates: Requirements 5.4, 5.6, 5.7
 */
export function useResults(config: AppConfig): {
  results: Ref<ParticipantResult[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
  isRefreshing: Ref<boolean>;
  refresh: () => void;
} {
  const results = ref<ParticipantResult[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Alias — both refer to the same reactive ref so consumers can use either name.
  const isRefreshing = loading;

  async function refresh(): Promise<void> {
    // Guard: don't stack requests.
    if (loading.value) {
      return;
    }

    loading.value = true;
    error.value = null;

    const url =
      `${config.apiBaseUrl}/api/results` +
      `?day1EventId=${encodeURIComponent(config.day1EventId)}` +
      `&day2EventId=${encodeURIComponent(config.day2EventId)}`;

    try {
      const response = await fetch(url);

      if (!response.ok) {
        error.value = `Failed to load results: HTTP ${response.status}`;
        results.value = [];
        return;
      }

      results.value = (await response.json()) as ParticipantResult[];
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(err);
      results.value = [];
    } finally {
      loading.value = false;
    }
  }

  // Auto-fetch on composable creation (Requirement 5.4).
  refresh();

  return { results, loading, error, isRefreshing, refresh };
}
