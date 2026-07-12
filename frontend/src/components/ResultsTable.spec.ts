import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import ResultsTable from './ResultsTable.vue';
import type { ParticipantResult } from '../types';

function makeResult(name: string, day2Deduction = 0): ParticipantResult {
  return {
    participantName: name,
    day1Controls: [], day1GrossScore: 100, day1Penalty: 0, day1NetScore: 100,
    day2Controls: [], day2GrossScore: 50, day2Penalty: 0,
    day2Deduction, day2NetScore: 50 - day2Deduction, totalScore: 150 - day2Deduction
  };
}

describe('ResultsTable.vue', () => {
  it('renders correct number of rows', () => {
    const results = [makeResult('Alice'), makeResult('Bob'), makeResult('Carol')];
    const wrapper = mount(ResultsTable, { props: { results } });
    expect(wrapper.findAll('tbody tr').length).toBe(3);
  });

  it('deduction highlight class applied only when day2Deduction > 0', () => {
    const results = [makeResult('WithDeduction', 10), makeResult('NoDeduction', 0)];
    const wrapper = mount(ResultsTable, { props: { results } });

    const rows = wrapper.findAll('tbody tr');

    // Row 0: day2Deduction = 10 — deduction cell is the 9th td (index 8)
    const deductionCellWithHighlight = rows[0].findAll('td')[8];
    expect(deductionCellWithHighlight.classes()).toContain('deduction-highlight');

    // Row 1: day2Deduction = 0 — deduction cell should NOT have the class
    const deductionCellNoHighlight = rows[1].findAll('td')[8];
    expect(deductionCellNoHighlight.classes()).not.toContain('deduction-highlight');
  });

  it('all required column headers are present', () => {
    const wrapper = mount(ResultsTable, { props: { results: [] } });
    const headers = wrapper.findAll('thead th').map(th => th.text());
    const expected = [
      'Name', 'D1 Controls', 'D1 Gross', 'D1 Penalty', 'D1 Net',
      'D2 Controls', 'D2 Gross', 'D2 Penalty', 'D2 Deduction', 'D2 Net', 'Total'
    ];
    expect(headers).toEqual(expected);
  });
});
