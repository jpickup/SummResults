<script setup lang="ts">
import { ref } from 'vue'
import type { NamedEvent } from './types'
import { appConfig } from './config'
import { useEvents } from './composables/useEvents'
import EventsList from './components/EventsList.vue'
import ResultsPage from './components/ResultsPage.vue'
import TeamsManager from './components/TeamsManager.vue'

// config.ts reads window.__APP_CONFIG__ synchronously — no async needed.
// If apiBaseUrl is missing it throws at module load, which Vite will surface
// as a JS error in the console rather than a blank page.

type View = 'events' | 'results' | 'teams'

const backendBaseUrl = appConfig.apiBaseUrl

const { events, loading, error } = useEvents(backendBaseUrl)

const view = ref<View>('events')
const selectedEvent = ref<NamedEvent | null>(null)

function selectEvent(eventId: string) {
  selectedEvent.value = events.value.find(e => e.id === eventId) ?? null
  if (selectedEvent.value) view.value = 'results'
}

function goToEvents() {
  view.value = 'events'
  selectedEvent.value = null
}

function goToTeams() {
  view.value = 'teams'
}

function backFromTeams() {
  view.value = selectedEvent.value ? 'results' : 'events'
}
</script>

<template>
  <TeamsManager
    v-if="view === 'teams'"
    :apiBaseUrl="backendBaseUrl"
    :eventId="selectedEvent?.id ?? ''"
    :eventName="selectedEvent?.name ?? ''"
    @back="backFromTeams"
  />
  <ResultsPage
    v-else-if="view === 'results' && selectedEvent"
    :apiBaseUrl="backendBaseUrl"
    :eventId="selectedEvent.id"
    :eventName="selectedEvent.name"
    @back="goToEvents"
    @manageTeams="goToTeams"
  />
  <EventsList
    v-else
    :events="events"
    :loading="loading"
    :error="error"
    @select="selectEvent"
  />
</template>
