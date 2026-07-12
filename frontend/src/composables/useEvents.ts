import { ref } from 'vue';
import type { Ref } from 'vue';
import type { NamedEvent } from '../types';

/**
 * Composable that fetches the list of named events from GET /api/events.
 *
 * - Immediately triggers a fetch on creation (auto-fetch).
 * - Exposes loading/error/events state as reactive refs.
 */
export function useEvents(apiBaseUrl: string): {
  events: Ref<NamedEvent[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
} {
  const events = ref<NamedEvent[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchEvents(): Promise<void> {
    loading.value = true;
    error.value = null;

    try {
      const response = await fetch(`${apiBaseUrl}/api/events`);

      if (!response.ok) {
        error.value = `Failed to load events: HTTP ${response.status}`;
        events.value = [];
        return;
      }

      events.value = (await response.json()) as NamedEvent[];
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(err);
      events.value = [];
    } finally {
      loading.value = false;
    }
  }

  // Auto-fetch on composable creation.
  fetchEvents();

  return { events, loading, error };
}
