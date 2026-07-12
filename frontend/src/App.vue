<script setup lang="ts">
import { ref } from 'vue'
import type { AppConfig, NamedEvent } from './types'
import { useEvents } from './composables/useEvents'
import EventsList from './components/EventsList.vue'
import ResultsPage from './components/ResultsPage.vue'
import TeamsManager from './components/TeamsManager.vue'

type View = 'events' | 'results' | 'teams'

const configError = ref<string | null>(null)
let appConfigVal: AppConfig | null = null

try {
  const m = await import('./config')
  appConfigVal = m.appConfig
} catch (e) {
  configError.value = e instanceof Error ? e.message : String(e)
}

const { events, loading, error } = appConfigVal
  ? useEvents(appConfigVal.apiBaseUrl)
  : { events: ref<NamedEvent[]>([]), loading: ref(false), error: ref(null) }

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
  // Return to results if we came from there, otherwise to the events list.
  view.value = selectedEvent.value ? 'results' : 'events'
}
</script>

<template>
  <div id="app">
    <div v-if="configError" class="config-error">
      Configuration error: {{ configError }}
    </div>
    <template v-else>
      <TeamsManager
        v-if="view === 'teams'"
        :apiBaseUrl="appConfigVal!.apiBaseUrl"
        @back="backFromTeams"
      />
      <ResultsPage
        v-else-if="view === 'results' && selectedEvent"
        :apiBaseUrl="appConfigVal!.apiBaseUrl"
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
  </div>
</template>
