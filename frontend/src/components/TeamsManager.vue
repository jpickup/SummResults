<script setup lang="ts">
import { ref } from 'vue';
import { useTeams } from '../composables/useTeams';
import type { Team } from '../types';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';

const props = defineProps<{ apiBaseUrl: string }>();
const emit = defineEmits<{ (e: 'back'): void }>();

const { teams, loading, error, createTeam, updateTeam, deleteTeam } = useTeams(props.apiBaseUrl);

// ── form state ──────────────────────────────────────────────────────────────
const editingId = ref<string | null>(null);   // null = creating new
const formTeamName = ref('');
const formMember1 = ref('');
const formMember2 = ref('');
const formError = ref<string | null>(null);
const formVisible = ref(false);

function openCreate() {
  editingId.value = null;
  formTeamName.value = '';
  formMember1.value = '';
  formMember2.value = '';
  formError.value = null;
  formVisible.value = true;
}

function openEdit(team: Team) {
  editingId.value = team.id;
  formTeamName.value = team.teamName;
  formMember1.value = team.member1;
  formMember2.value = team.member2 ?? '';
  formError.value = null;
  formVisible.value = true;
}

function cancelForm() {
  formVisible.value = false;
}

async function submitForm() {
  formError.value = null;
  const req = {
    teamName: formTeamName.value.trim(),
    member1: formMember1.value.trim(),
    member2: formMember2.value.trim() || undefined,
  };
  try {
    if (editingId.value === null) {
      await createTeam(req);
    } else {
      await updateTeam(editingId.value, req);
    }
    formVisible.value = false;
  } catch (err) {
    formError.value = err instanceof Error ? err.message : String(err);
  }
}

async function confirmDelete(team: Team) {
  if (!confirm(`Delete team "${team.teamName}"?`)) return;
  try {
    await deleteTeam(team.id);
  } catch (err) {
    alert(`Delete failed: ${err instanceof Error ? err.message : String(err)}`);
  }
}
</script>

<template>
  <div class="teams-manager">
    <div class="manager-header">
      <button class="back-button" @click="emit('back')">← Back</button>
      <h1>Manage Teams</h1>
      <button class="add-button" @click="openCreate">+ Add Team</button>
    </div>

    <LoadingSpinner v-if="loading" />
    <ErrorMessage v-else-if="error" :message="error" />

    <template v-else>
      <!-- Team form (inline, shown on add/edit) -->
      <div v-if="formVisible" class="team-form">
        <h2>{{ editingId === null ? 'New Team' : 'Edit Team' }}</h2>
        <ErrorMessage v-if="formError" :message="formError" />
        <div class="field">
          <label for="teamName">Team name</label>
          <input id="teamName" v-model="formTeamName" type="text" placeholder="e.g. Smith &amp; Jones" />
        </div>
        <div class="field">
          <label for="member1">Competitor 1 <span class="required">*</span></label>
          <input id="member1" v-model="formMember1" type="text" placeholder="Exact MapRun name" />
        </div>
        <div class="field">
          <label for="member2">Competitor 2 <span class="optional">(optional)</span></label>
          <input id="member2" v-model="formMember2" type="text" placeholder="Exact MapRun name" />
        </div>
        <div class="form-actions">
          <button class="save-button" @click="submitForm">Save</button>
          <button class="cancel-button" @click="cancelForm">Cancel</button>
        </div>
      </div>

      <!-- Team list -->
      <p v-if="teams.length === 0 && !formVisible" class="empty-state">
        No teams defined yet. Click "+ Add Team" to create one.
      </p>
      <table v-if="teams.length > 0">
        <thead>
          <tr>
            <th>Team Name</th>
            <th>Competitor 1</th>
            <th>Competitor 2</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in teams" :key="team.id">
            <td>{{ team.teamName }}</td>
            <td>{{ team.member1 }}</td>
            <td>{{ team.member2 ?? '—' }}</td>
            <td class="actions">
              <button class="edit-button" @click="openEdit(team)">Edit</button>
              <button class="delete-button" @click="confirmDelete(team)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
    </template>
  </div>
</template>

<style scoped>
.teams-manager {
  padding: 1rem;
  max-width: 800px;
  margin: 0 auto;
}

.manager-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.manager-header h1 {
  flex: 1;
  margin: 0;
}

.back-button,
.cancel-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.back-button:hover,
.cancel-button:hover {
  background: #e8e8e8;
}

.add-button,
.save-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #3a7bd5;
  color: #fff;
  border: none;
  border-radius: 4px;
}

.add-button:hover,
.save-button:hover {
  background: #2e62b0;
}

.team-form {
  background: #fafafa;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1rem 1.25rem;
  margin-bottom: 1.5rem;
}

.team-form h2 {
  margin: 0 0 1rem;
  font-size: 1rem;
}

.field {
  display: flex;
  flex-direction: column;
  margin-bottom: 0.75rem;
}

.field label {
  font-size: 0.85rem;
  margin-bottom: 0.25rem;
  font-weight: 600;
}

.field input {
  padding: 0.4rem 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
  max-width: 360px;
}

.required { color: #c00; }
.optional { font-weight: normal; color: #666; }

.form-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 1rem;
}

table {
  border-collapse: collapse;
  width: 100%;
  font-size: 0.9rem;
}

th, td {
  padding: 6px 10px;
  border: 1px solid #ddd;
  text-align: left;
}

th {
  background: #f0f0f0;
}

.actions {
  white-space: nowrap;
  display: flex;
  gap: 0.4rem;
}

.edit-button {
  padding: 0.25rem 0.6rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.8rem;
}

.edit-button:hover { background: #e8e8e8; }

.delete-button {
  padding: 0.25rem 0.6rem;
  cursor: pointer;
  background: #fff0f0;
  border: 1px solid #f5c0c0;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #c00;
}

.delete-button:hover { background: #ffe0e0; }

.empty-state { color: #666; }
</style>
