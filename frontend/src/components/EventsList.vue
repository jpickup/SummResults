<script setup lang="ts">
import type { NamedEvent } from '../types';
import LoadingSpinner from './LoadingSpinner.vue';
import ErrorMessage from './ErrorMessage.vue';
import AppHeader from './AppHeader.vue';

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
  <div class="landing">
    <AppHeader>
      <h1 class="landing-title">SUMM Results</h1>
    </AppHeader>

    <main class="landing-main">
      <p class="landing-subtitle">Select an event to view results</p>
      <LoadingSpinner v-if="loading" />
      <ErrorMessage v-else-if="error" :message="error" />
      <p v-else-if="events.length === 0" class="empty-state">No events available.</p>
      <ul v-else class="events-list" role="list">
        <li v-for="event in events" :key="event.id">
          <button class="event-card" @click="emit('select', event.id)">
            <span class="event-name">{{ event.name }}</span>
            <span class="event-arrow" aria-hidden="true">→</span>
          </button>
        </li>
      </ul>
    </main>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.landing-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.landing-subtitle {
  font-size: 1rem;
  color: #666;
  padding: 0 1rem;
  margin-bottom: 1rem;
}

.landing-main {
  width: 100%;
  max-width: 480px;
  padding: 0 1rem;
  align-self: center;
}

.events-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.event-card {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.event-card:hover {
  border-color: #999;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.event-card:focus-visible {
  outline: 2px solid #0066cc;
  outline-offset: 2px;
}

.event-name {
  font-size: 1rem;
  font-weight: 500;
  color: #1a1a1a;
}

.event-arrow {
  font-size: 1.1rem;
  color: #999;
  flex-shrink: 0;
  margin-left: 1rem;
}

.empty-state {
  text-align: center;
  color: #888;
  padding: 2rem 0;
}
</style>
