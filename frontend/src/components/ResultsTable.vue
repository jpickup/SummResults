<template>
  <div style="overflow-x: auto">
    <table>
      <thead>
        <tr>
          <th>Name</th>
          <th>D1 Controls</th>
          <th>D1 Gross</th>
          <th>D1 Penalty</th>
          <th>D1 Net</th>
          <th>D2 Controls</th>
          <th>D2 Gross</th>
          <th>D2 Penalty</th>
          <th>D2 Deduction</th>
          <th>D2 Net</th>
          <th>Total</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="result in results" :key="result.participantName">
          <td>{{ result.participantName }}</td>
          <td>{{ result.day1Controls.length }}</td>
          <td>{{ result.day1GrossScore }}</td>
          <td>{{ result.day1Penalty }}</td>
          <td>{{ result.day1NetScore }}</td>
          <td>{{ result.day2Controls.length }}</td>
          <td>{{ result.day2GrossScore }}</td>
          <td>{{ result.day2Penalty }}</td>
          <td :class="{ 'deduction-highlight': result.day2Deduction > 0 }">{{ result.day2Deduction }}</td>
          <td>{{ result.day2NetScore }}</td>
          <td>{{ result.totalScore }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { ParticipantResult } from '../types';

defineProps<{
  results: ParticipantResult[];
}>();
</script>

<style scoped>
table { border-collapse: collapse; min-width: 100%; font-size: 0.85rem; }
th, td { padding: 4px 8px; border: 1px solid #ccc; white-space: nowrap; }
th { background: #f0f0f0; text-align: center; }
.deduction-highlight { background-color: #ffe0e0; color: #c00; }
</style>
