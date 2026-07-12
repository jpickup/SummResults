<script setup lang="ts">
import { useResults } from '../composables/useResults';
import TeamResultsTable from './TeamResultsTable.vue';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';
import EmptyState from './EmptyState.vue';

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
</script>

<template>
  <div class="results-page">
    <div class="results-header">
      <button class="back-button" @click="emit('back')">← Back</button>
      <h1>{{ eventName }}</h1>
      <div class="header-actions">
        <button class="manage-teams-button" @click="emit('manageTeams')">Manage Teams</button>
        <button @click="refresh" :disabled="isRefreshing">Refresh</button>
      </div>
    </div>
    <LoadingSpinner v-if="loading" />
    <ErrorMessage v-else-if="error" :message="error" />
    <EmptyState v-else-if="results.length === 0" />
    <TeamResultsTable v-else :results="results" />
  </div>
</template>

<style scoped>
.results-page {
  padding: 1rem;
}

.results-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1rem;
}

.results-header h1 {
  flex: 1;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 0.5rem;
}

.back-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.back-button:hover {
  background: #e8e8e8;
}

.manage-teams-button {
  padding: 0.4rem 0.75rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.manage-teams-button:hover {
  background: #e8e8e8;
}
</style>
