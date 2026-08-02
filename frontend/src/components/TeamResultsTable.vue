<template>
  <div style="overflow-x: auto">
    <table>
      <thead>
        <tr>
          <th rowspan="2" class="sortable" @click="setSort('rank')" :aria-sort="ariaSort('rank')">
            # <span class="sort-icon">{{ sortIcon('rank') }}</span>
          </th>
          <th rowspan="2" class="sortable" @click="setSort('teamName')" :aria-sort="ariaSort('teamName')">
            Team <span class="sort-icon">{{ sortIcon('teamName') }}</span>
          </th>
          <th rowspan="2">Members</th>
          <th rowspan="2" class="group-total sortable" @click="setSort('totalScore')" :aria-sort="ariaSort('totalScore')">
            Score <span class="sort-icon">{{ sortIcon('totalScore') }}</span>
          </th>
          <th rowspan="2" class="handicap-group sortable" @click="setSort('handicapScore')" :aria-sort="ariaSort('handicapScore')">
            Handicap <span class="sort-icon">{{ sortIcon('handicapScore') }}</span>
          </th>
          <template v-if="showDetails">
            <th colspan="2" class="group-header">Day 1</th>
            <th colspan="2" class="group-header">Day 2</th>
          </template>
        </tr>
        <tr>
          <template v-if="showDetails">
            <th class="sortable" @click="setSort('day1Controls')" :aria-sort="ariaSort('day1Controls')">
              Controls <span class="sort-icon">{{ sortIcon('day1Controls') }}</span>
            </th>
            <th class="sortable" @click="setSort('day1NetScore')" :aria-sort="ariaSort('day1NetScore')">
              Net <span class="sort-icon">{{ sortIcon('day1NetScore') }}</span>
            </th>
            <th class="sortable" @click="setSort('day2Controls')" :aria-sort="ariaSort('day2Controls')">
              Controls <span class="sort-icon">{{ sortIcon('day2Controls') }}</span>
            </th>
            <th class="sortable" @click="setSort('day2NetScore')" :aria-sort="ariaSort('day2NetScore')">
              Net <span class="sort-icon">{{ sortIcon('day2NetScore') }}</span>
            </th>
          </template>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(result, index) in sortedResults" :key="result.teamName">
          <td class="rank">{{ index + 1 }}</td>
          <td class="team-name">{{ result.teamName }}</td>
          <td class="members">{{ result.members.join(', ') }}</td>
          <td class="score total">{{ result.totalScore }}</td>
          <td class="score handicap-col" :class="{ 'has-handicap': result.handicapPct > 0 }">
            {{ result.handicapPct > 0 ? result.handicapScore : result.totalScore }}
          </td>
          <template v-if="showDetails">
            <td class="controls">{{ result.day1Controls }}</td>
            <td class="score">{{ result.day1NetScore }}</td>
            <td class="controls">{{ result.day2Controls }}</td>
            <td class="score">{{ result.day2NetScore }}</td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type { TeamResult } from '../types';

const props = defineProps<{
  results: TeamResult[];
  showDetails: boolean;
}>();

type SortKey = 'rank' | 'teamName' | 'totalScore' | 'handicapScore' | 'day1Controls' | 'day1NetScore' | 'day2Controls' | 'day2NetScore';

// 'rank' means "use the original server order"
const sortKey = ref<SortKey>('rank');
const sortAsc = ref(false); // default: descending for numeric columns

function setSort(key: SortKey) {
  if (sortKey.value === key) {
    sortAsc.value = !sortAsc.value;
  } else {
    sortKey.value = key;
    // Text columns default ascending; numeric columns default descending
    sortAsc.value = key === 'teamName' || key === 'day1Controls' || key === 'day2Controls';
  }
}

function sortIcon(key: SortKey): string {
  if (sortKey.value !== key) return '⇅';
  return sortAsc.value ? '▲' : '▼';
}

function ariaSort(key: SortKey): 'none' | 'ascending' | 'descending' {
  if (sortKey.value !== key) return 'none';
  return sortAsc.value ? 'ascending' : 'descending';
}

const sortedResults = computed<TeamResult[]>(() => {
  if (sortKey.value === 'rank') return props.results;

  const key = sortKey.value;
  const asc = sortAsc.value;

  return [...props.results].sort((a, b) => {
    const av = a[key as keyof TeamResult];
    const bv = b[key as keyof TeamResult];

    let cmp: number;
    if (typeof av === 'string' && typeof bv === 'string') {
      cmp = av.localeCompare(bv);
    } else {
      cmp = (av as number) - (bv as number);
    }
    return asc ? cmp : -cmp;
  });
});
</script>

<style scoped>
table { border-collapse: collapse; min-width: 100%; font-size: 0.9rem; }
th, td { padding: 5px 10px; border: 1px solid #ccc; }
th { background: #f0f0f0; text-align: center; white-space: nowrap; }
td { white-space: nowrap; }

.sortable { cursor: pointer; user-select: none; }
.sortable:hover { background: #e0e0e0; }
.sort-icon { font-size: 0.7rem; opacity: 0.6; margin-left: 2px; }

.group-header { text-align: center; background: #e8e8e8; font-size: 0.78rem; letter-spacing: 0.03em; }
.group-total { background: #ffe3e3ff; }
.handicap-group { background: #e8f0fb; }

.rank    { text-align: center; color: #666; width: 2.5rem; }
.team-name { font-weight: 600; }
.members { color: #555; font-size: 0.82rem; white-space: normal; min-width: 8rem; }
.score   { text-align: right; }
.controls { font-size: 0.82rem; white-space: normal; min-width: 5rem; word-break: break-word; }
.total   { font-weight: 700; }

.handicap-col { background: #f3f7ff; font-weight: 600; }
.has-handicap { background: #ddeeff; font-weight: 600; color: #1a4a8a; }
</style>
