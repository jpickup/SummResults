<script setup lang="ts">
import { ref, computed } from 'vue';
import { useTeams } from '../composables/useTeams';
import type { Team } from '../types';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';
import AppHeader from './AppHeader.vue';

const props = defineProps<{ apiBaseUrl: string; eventId: string; eventName: string }>();
const emit = defineEmits<{ (e: 'back'): void }>();

const { teams, loading, error, createTeam, updateTeam, deleteTeam } = useTeams(props.apiBaseUrl, props.eventId);

// ── form state ──────────────────────────────────────────────────────────────
const editingId    = ref<string | null>(null);
const formTeamName = ref('');
const formM1Name   = ref('');
const formM1Age    = ref<number | ''>('');
const formM1Gender = ref<'M' | 'F' | ''>('');
const formM2Name   = ref('');
const formM2Age    = ref<number | ''>('');
const formM2Gender = ref<'M' | 'F' | ''>('');
const formError    = ref<string | null>(null);
const formVisible  = ref(false);

const hasMember2 = computed(() => formM2Name.value.trim() !== '');

function openCreate() {
  editingId.value    = null;
  formTeamName.value = '';
  formM1Name.value   = '';
  formM1Age.value    = '';
  formM1Gender.value = '';
  formM2Name.value   = '';
  formM2Age.value    = '';
  formM2Gender.value = '';
  formError.value    = null;
  formVisible.value  = true;
}

function openEdit(team: Team) {
  editingId.value    = team.id;
  formTeamName.value = team.teamName;
  formM1Name.value   = team.member1;
  formM1Age.value    = team.member1Age;
  formM1Gender.value = team.member1Gender;
  formM2Name.value   = team.member2 ?? '';
  formM2Age.value    = team.member2Age ?? '';
  formM2Gender.value = team.member2Gender ?? '';
  formError.value    = null;
  formVisible.value  = true;
}

function cancelForm() {
  formVisible.value = false;
}

async function submitForm() {
  formError.value = null;

  const m2name = formM2Name.value.trim();
  const req = {
    teamName:      formTeamName.value.trim(),
    member1:       formM1Name.value.trim(),
    member1Age:    Number(formM1Age.value),
    member1Gender: formM1Gender.value as 'M' | 'F',
    ...(m2name && {
      member2:       m2name,
      member2Age:    Number(formM2Age.value),
      member2Gender: formM2Gender.value as 'M' | 'F',
    }),
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
    <AppHeader>
      <button class="back-button" @click="emit('back')">← Back</button>
      <h1 class="page-title">Manage Teams — {{ eventName }}</h1>
      <button class="add-button" @click="openCreate">+ Add Team</button>
    </AppHeader>

    <LoadingSpinner v-if="loading" />
    <ErrorMessage v-else-if="error" :message="error" />

    <template v-else>
      <div class="manager-body">
      <!-- ── inline form ── -->
      <div v-if="formVisible" class="team-form">
        <h2>{{ editingId === null ? 'New Team' : 'Edit Team' }}</h2>
        <ErrorMessage v-if="formError" :message="formError" />

        <div class="field">
          <label for="teamName">Team name <span class="required">*</span></label>
          <input id="teamName" v-model="formTeamName" type="text" placeholder="e.g. Smith &amp; Jones" />
        </div>

        <fieldset class="member-fieldset">
          <legend>Competitor 1 <span class="required">*</span></legend>
          <div class="field-row">
            <div class="field field-grow">
              <label for="m1name">Name</label>
              <input id="m1name" v-model="formM1Name" type="text" placeholder="Exact MapRun name" />
            </div>
            <div class="field field-small">
              <label for="m1age">Age</label>
              <input id="m1age" v-model.number="formM1Age" type="number" min="0" max="120" placeholder="e.g. 52" />
            </div>
            <div class="field field-small">
              <label for="m1gender">Gender</label>
              <select id="m1gender" v-model="formM1Gender">
                <option value="">—</option>
                <option value="M">M</option>
                <option value="F">F</option>
              </select>
            </div>
          </div>
        </fieldset>

        <fieldset class="member-fieldset">
          <legend>Competitor 2 <span class="optional">(optional)</span></legend>
          <div class="field-row">
            <div class="field field-grow">
              <label for="m2name">Name</label>
              <input id="m2name" v-model="formM2Name" type="text" placeholder="Exact MapRun name" />
            </div>
            <div class="field field-small">
              <label for="m2age">Age <span v-if="hasMember2" class="required">*</span></label>
              <input id="m2age" v-model.number="formM2Age" type="number" min="0" max="120"
                     placeholder="e.g. 48" :disabled="!hasMember2" />
            </div>
            <div class="field field-small">
              <label for="m2gender">Gender <span v-if="hasMember2" class="required">*</span></label>
              <select id="m2gender" v-model="formM2Gender" :disabled="!hasMember2">
                <option value="">—</option>
                <option value="M">M</option>
                <option value="F">F</option>
              </select>
            </div>
          </div>
        </fieldset>

        <div class="form-actions">
          <button class="save-button" @click="submitForm">Save</button>
          <button class="cancel-button" @click="cancelForm">Cancel</button>
        </div>
      </div>

      <!-- ── team list ── -->
      <p v-if="teams.length === 0 && !formVisible" class="empty-state">
        No teams defined yet. Click "+ Add Team" to create one.
      </p>
      <table v-if="teams.length > 0">
        <thead>
          <tr>
            <th>Team Name</th>
            <th>Competitor 1</th>
            <th>Age</th>
            <th>Gender</th>
            <th>Competitor 2</th>
            <th>Age</th>
            <th>Gender</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in teams" :key="team.id">
            <td>{{ team.teamName }}</td>
            <td>{{ team.member1 }}</td>
            <td>{{ team.member1Age }}</td>
            <td>{{ team.member1Gender }}</td>
            <td>{{ team.member2 ?? '—' }}</td>
            <td>{{ team.member2Age ?? '—' }}</td>
            <td>{{ team.member2Gender ?? '—' }}</td>
            <td class="actions">
              <button class="edit-button" @click="openEdit(team)">Edit</button>
              <button class="delete-button" @click="confirmDelete(team)">Delete</button>
            </td>
          </tr>
        </tbody>
      </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.teams-manager {
  max-width: 900px;
  margin: 0 auto;
}

.page-title { flex: 1; margin: 0; font-size: 1.2rem; }

.manager-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.manager-header h1 { flex: 1; margin: 0; }

.back-button, .cancel-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.back-button:hover, .cancel-button:hover { background: #e8e8e8; }

.add-button, .save-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #3a7bd5;
  color: #fff;
  border: none;
  border-radius: 4px;
}
.add-button:hover, .save-button:hover { background: #2e62b0; }

.team-form {
  background: #fafafa;
  border: 1px solid #ddd;
  border-radius: 6px;
  padding: 1rem 1.25rem;
  margin-bottom: 1.5rem;
}
.team-form h2 { margin: 0 0 1rem; font-size: 1rem; }

.member-fieldset {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 0.75rem 1rem;
  margin-bottom: 0.75rem;
}
.member-fieldset legend {
  padding: 0 0.4rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.field-row {
  display: flex;
  gap: 0.75rem;
  align-items: flex-end;
  flex-wrap: wrap;
}

.field { display: flex; flex-direction: column; margin-bottom: 0; }
.field-grow { flex: 1 1 200px; }
.field-small { flex: 0 0 80px; }

.field label {
  font-size: 0.8rem;
  margin-bottom: 0.2rem;
  font-weight: 600;
}

.field input, .field select {
  padding: 0.4rem 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}

.field input:disabled, .field select:disabled {
  background: #f0f0f0;
  color: #aaa;
}

.manager-body {
  padding: 0 1rem 1rem;
}
.optional { font-weight: normal; color: #666; }

.form-actions { display: flex; gap: 0.5rem; margin-top: 1rem; }

table { border-collapse: collapse; width: 100%; font-size: 0.85rem; }
th, td { padding: 5px 8px; border: 1px solid #ddd; text-align: left; }
th { background: #f0f0f0; white-space: nowrap; }

.actions { white-space: nowrap; display: flex; gap: 0.4rem; }

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
