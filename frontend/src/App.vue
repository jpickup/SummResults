<script setup lang="ts">
import { ref } from 'vue'
import type { AppConfig } from './types'
import { useResults } from './composables/useResults'
import ResultsTable from './components/ResultsTable.vue'
import LoadingSpinner from './components/LoadingSpinner.vue'
import ErrorMessage from './components/ErrorMessage.vue'
import EmptyState from './components/EmptyState.vue'

const configError = ref<string | null>(null)
let appConfigVal: AppConfig | null = null

try {
  const m = await import('./config')
  appConfigVal = m.appConfig
} catch (e) {
  configError.value = e instanceof Error ? e.message : String(e)
}

const { results, loading, error, isRefreshing, refresh } = appConfigVal
  ? useResults(appConfigVal)
  : { results: ref([]), loading: ref(false), error: ref(null), isRefreshing: ref(false), refresh: () => {} }
</script>

<template>
  <div id="app">
    <div v-if="configError" class="config-error">
      Configuration error: {{ configError }}
    </div>
    <template v-else>
      <h1>MapRun Results</h1>
      <button @click="refresh" :disabled="isRefreshing">Refresh</button>
      <LoadingSpinner v-if="loading" />
      <ErrorMessage v-else-if="error" :message="error" />
      <EmptyState v-else-if="results.length === 0" />
      <ResultsTable v-else :results="results" />
    </template>
  </div>
</template>
