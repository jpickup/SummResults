<script setup lang="ts">
import { ref } from 'vue';
import { useResults } from '../composables/useResults';
import TeamResultsTable from './TeamResultsTable.vue';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';
import EmptyState from './EmptyState.vue';
import AppHeader from './AppHeader.vue';

const props = defineProps<{
  apiBaseUrl: string;
  eventId: string;
  eventName: string;
}>();

const emit = defineEmits<{
  (e: 'back'): void;
  (e: 'manageTeams'): void;
}>();

const { results, loading, error, isRefreshing, refresh } = useResults(
  props.apiBaseUrl,
  props.eventId
);

const showDetails = ref(false);
</script>

<template>
  <div class="results-page">
    <AppHeader>
      <button class="back-button" @click="emit('back')">← Back</button>
      <h1 class="page-title">{{ eventName }}</h1>
      <div class="header-actions">
        <button class="manage-teams-button" @click="emit('manageTeams')">Manage Teams</button>
        <button @click="refresh" :disabled="isRefreshing">Refresh</button>
      </div>
    </AppHeader>
    <div class="results-toolbar">
      <label class="details-toggle">
        <input type="checkbox" v-model="showDetails" />
        Show Details
      </label>
    </div>
    <div class="results-body">
      <LoadingSpinner v-if="loading" />
      <ErrorMessage v-else-if="error" :message="error" />
      <EmptyState v-else-if="results.length === 0" />
      <TeamResultsTable v-else :results="results" :showDetails="showDetails" />
    </div>
  </div>
</template>

<style scoped>
.results-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-title {
  flex: 1;
  margin: 0;
  font-size: 1.2rem;
}

.results-toolbar {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem;
  border-bottom: 1px solid #e0e0e0;
  background: #fafafa;
}

.details-toggle {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.9rem;
  cursor: pointer;
  user-select: none;
}

.details-toggle input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}

.results-body {
  padding: 0 1rem;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.back-button,
.manage-teams-button,
button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.back-button:hover,
.manage-teams-button:hover,
button:hover {
  background: #e8e8e8;
}
</style>
