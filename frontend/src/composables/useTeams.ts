import { ref } from 'vue';
import type { Ref } from 'vue';
import type { Team, TeamRequest } from '../types';

/**
 * Composable that provides reactive team state and CRUD operations
 * against GET/POST/PUT/DELETE /api/teams.
 *
 * Auto-fetches on creation. All mutating operations (create, update, delete)
 * refresh the list from the server after a successful response.
 */
export function useTeams(apiBaseUrl: string): {
  teams: Ref<Team[]>;
  loading: Ref<boolean>;
  error: Ref<string | null>;
  refresh: () => Promise<void>;
  createTeam: (req: TeamRequest) => Promise<void>;
  updateTeam: (id: string, req: TeamRequest) => Promise<void>;
  deleteTeam: (id: string) => Promise<void>;
} {
  const teams = ref<Team[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function refresh(): Promise<void> {
    loading.value = true;
    error.value = null;
    try {
      const res = await fetch(`${apiBaseUrl}/api/teams`);
      if (!res.ok) {
        error.value = `Failed to load teams: HTTP ${res.status}`;
        teams.value = [];
        return;
      }
      teams.value = (await res.json()) as Team[];
    } catch (err) {
      error.value = err instanceof Error ? err.message : String(err);
      teams.value = [];
    } finally {
      loading.value = false;
    }
  }

  async function createTeam(req: TeamRequest): Promise<void> {
    const res = await fetch(`${apiBaseUrl}/api/teams`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error ?? `HTTP ${res.status}`);
    }
    await refresh();
  }

  async function updateTeam(id: string, req: TeamRequest): Promise<void> {
    const res = await fetch(`${apiBaseUrl}/api/teams/${encodeURIComponent(id)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.error ?? `HTTP ${res.status}`);
    }
    await refresh();
  }

  async function deleteTeam(id: string): Promise<void> {
    const res = await fetch(`${apiBaseUrl}/api/teams/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    await refresh();
  }

  // Auto-fetch on composable creation.
  refresh();

  return { teams, loading, error, refresh, createTeam, updateTeam, deleteTeam };
}
