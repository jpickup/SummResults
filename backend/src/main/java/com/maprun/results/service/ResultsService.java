package com.maprun.results.service;

import com.maprun.results.client.MapRunApiClient;
import com.maprun.results.model.DayResult;
import com.maprun.results.model.ParticipantResult;
import com.maprun.results.scoring.ScoringEngine;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates fetching raw day results from the MapRun API and delegating
 * scoring to {@link ScoringEngine}.
 *
 * <p>No caching is applied — each call fetches fresh data from the MapRun API.
 * Exceptions from {@link MapRunApiClient} propagate to the caller unchanged.
 */
@Service
public class ResultsService {

    private final MapRunApiClient mapRunApiClient;
    private final ScoringEngine scoringEngine;

    public ResultsService(MapRunApiClient mapRunApiClient, ScoringEngine scoringEngine) {
        this.mapRunApiClient = mapRunApiClient;
        this.scoringEngine = scoringEngine;
    }

    /**
     * Fetches results for both days and returns a sorted list of combined participant results.
     *
     * @param day1EventId the MapRun event ID for Day 1
     * @param day2EventId the MapRun event ID for Day 2
     * @return sorted list of {@link ParticipantResult}, one entry per unique participant
     */
    public List<ParticipantResult> getResults(String day1EventId, String day2EventId) {
        List<DayResult> day1Results = mapRunApiClient.fetchDay(day1EventId, 1);
        List<DayResult> day2Results = mapRunApiClient.fetchDay(day2EventId, 2);
        return scoringEngine.calculate(day1Results, day2Results);
    }
}
