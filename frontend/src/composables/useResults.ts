import { ref } from 'vue';
import type { Ref } from 'vue';
import type { TeamResult } from '../types';

/**
 * Composable that fetches processed team results from the backend API for a
 * single named event identified by its stable {@code eventId}.
 *
 * - Immediately triggers a fetch on creation (auto-fetch).
 * - Exposes loading/error/results state as reactive refs.
 * - Guards against stacked requests: refresh() is a no-op while a fetch
 *   is already in flight.
 */
export function useResults(apiBaseUrl: string, eventId: string): {
  results: Ref<TeamResult[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
  isRefreshing: Ref<boolean>;
  refresh: () => void;
} {
  const results = ref<TeamResult[]>([]);
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

    const url = `${apiBaseUrl}/api/results?eventId=${encodeURIComponent(eventId)}`;

    try {
      const response = await fetch(url);

      if (!response.ok) {
        error.value = `Failed to load results: HTTP ${response.status}`;
        results.value = [];
        return;
      }

      results.value = (await response.json()) as TeamResult[];
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(err);
      results.value = [];
    } finally {
      loading.value = false;
    }
  }

  // Auto-fetch on composable creation.
  refresh();

  return { results, loading, error, isRefreshing, refresh };
}
