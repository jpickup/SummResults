<script setup lang="ts">
import type { NamedEvent } from '../types';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';

defineProps<{
  events: NamedEvent[];
  loading: boolean;
  error: string | null;
}>();

const emit = defineEmits<{
  (e: 'select', eventId: string): void;
}>();
</script>

<template>
  <div class="events-list">
    <h1>Events</h1>
    <LoadingSpinner v-if="loading" />
    <ErrorMessage v-else-if="error" :message="error" />
    <p v-else-if="events.length === 0" class="empty-state">No events available.</p>
    <ul v-else>
      <li v-for="event in events" :key="event.id">
        <button class="event-button" @click="emit('select', event.id)">
          {{ event.name }}
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.events-list {
  max-width: 480px;
  margin: 2rem auto;
  padding: 0 1rem;
}

ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

li {
  margin-bottom: 0.75rem;
}

.event-button {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  text-align: left;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ccc;
  border-radius: 4px;
}

.event-button:hover {
  background: #e8e8e8;
}

.empty-state {
  color: #666;
}
</style>
