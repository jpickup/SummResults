<template>
  <div style="overflow-x: auto">
    <table>
      <thead>
        <tr>
          <th rowspan="2">#</th>
          <th rowspan="2">Team</th>
          <th rowspan="2">Members</th>
          <th rowspan="2" class="group-total">Score</th>
          <th rowspan="2" class="handicap-group">Handicap</th>
          <th colspan="2" class="group-header">Day 1</th>
          <th colspan="2" class="group-header">Day 2</th>
        </tr>
        <tr>
          <th>Controls</th>
          <th>Net</th>
          <th>Controls</th>
          <th>Net</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(result, index) in results" :key="result.teamName">
          <td class="rank">{{ index + 1 }}</td>
          <td class="team-name">{{ result.teamName }}</td>
          <td class="members">{{ result.members.join(', ') }}</td>
          <td class="score total">{{ result.totalScore }}</td>
          <td class="score handicap-col" :class="{ 'has-handicap': result.handicapPct > 0 }">
            {{ result.handicapPct > 0 ? result.handicapScore : result.totalScore }}
          </td>
          <td class="controls">{{ result.day1Controls }}</td>
          <td class="score">{{ result.day1NetScore }}</td>
          <td class="controls">{{ result.day2Controls }}</td>
          <td class="score">{{ result.day2NetScore }}</td>
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
th, td { padding: 5px 10px; border: 1px solid #ccc; }
th { background: #f0f0f0; text-align: center; white-space: nowrap; }
td { white-space: nowrap; }

.group-header { text-align: center; background: #e8e8e8; font-size: 0.78rem; letter-spacing: 0.03em; }
.group-total { background: #ffe3e3ff; }
.handicap-group { background: #e8f0fb; }

.rank    { text-align: center; color: #666; width: 2.5rem; }
.team-name { font-weight: 600; }
.members { color: #555; font-size: 0.82rem; white-space: normal; }
.score   { text-align: right; }
.controls { font-size: 0.6rem; white-space: wrap;}
.total   { font-weight: 700; }

.handicap-col { background: #f3f7ff; font-weight: 600; }
.has-handicap { background: #ddeeff; font-weight: 600; color: #1a4a8a; }
</style>
