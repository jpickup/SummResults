<template>
  <div style="overflow-x: auto">
    <table>
      <thead>
        <tr>
          <th>#</th>
          <th>Team</th>
          <th>Members</th>
          <th>D1 Net</th>
          <th>D2 Net</th>
          <th>Total</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(result, index) in results" :key="result.teamName">
          <td class="rank">{{ index + 1 }}</td>
          <td class="team-name">{{ result.teamName }}</td>
          <td class="members">{{ result.members.join(', ') }}</td>
          <td>{{ result.day1NetScore }}</td>
          <td>{{ result.day2NetScore }}</td>
          <td class="total">{{ result.totalScore }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { TeamResult } from '../types';

defineProps<{
  results: TeamResult[];
}>();
</script>

<style scoped>
table { border-collapse: collapse; min-width: 100%; font-size: 0.9rem; }
th, td { padding: 6px 10px; border: 1px solid #ccc; }
th { background: #f0f0f0; text-align: center; white-space: nowrap; }
td { white-space: nowrap; }
.rank { text-align: center; color: #666; width: 2.5rem; }
.team-name { font-weight: 600; }
.members { color: #555; font-size: 0.82rem; white-space: normal; }
.total { font-weight: 700; text-align: right; }
</style>
