package com.maprun.results.config;

import com.maprun.results.teams.TeamsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the {@code app.events} list from {@code application.properties} (or environment
 * overrides) into a typed configuration object.
 *
 * <p>Each entry maps a user-friendly event name and stable ID to the two underlying
 * MapRun event IDs required by the MapRun API.
 *
 * <p>Example property block:
 * <pre>
 * app.events[0].id=SUMM-2026
 * app.events[0].name=The SUMM, 2026
 * app.events[0].day1EventId=SUMM Day 1 v2 ScoreP420
 * app.events[0].day2EventId=SUMM Day 2 v2 ScoreP360
 * app.events[0].teamsFile=SUMM-Day2
 * </pre>
 */
@ConfigurationProperties(prefix = "app")
public class EventsConfig {
    private static final Logger logger = LoggerFactory.getLogger(EventsConfig.class);

    private List<EventEntry> events = new ArrayList<>();

    public List<EventEntry> getEvents() {
        return events;
    }

    public void setEvents(List<EventEntry> events) {
        logger.info("Events: {}", events);
        this.events = events;
    }

    public EventEntry getEvent(String eventId) {
        logger.info("Looking for event with Id {}", eventId);
        return events.stream().filter(e -> e.id.equals(eventId)).findFirst().orElseThrow();
    }

    /**
     * A single named event entry.
     */
    public static class EventEntry {

        /** Stable identifier used as the API key, e.g. {@code "SUMM-2026"}. */
        private String id;

        /** Human-readable display name, e.g. {@code "The SUMM, 2026"}. */
        private String name;

        /** MapRun event ID for Day 1. */
        private String day1EventId;

        /** MapRun event ID for Day 2. */
        private String day2EventId;

        private boolean uniqueControls;

        private String teamsFilename;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDay1EventId() { return day1EventId; }
        public void setDay1EventId(String day1EventId) { this.day1EventId = day1EventId; }

        public String getDay2EventId() { return day2EventId; }
        public void setDay2EventId(String day2EventId) { this.day2EventId = day2EventId; }

        public boolean isUniqueControls() {
            return uniqueControls;
        }

        public void setUniqueControls(boolean uniqueControls) {
            this.uniqueControls = uniqueControls;
        }

        public void setTeamsFilename(String teamsFilename) {
            this.teamsFilename = teamsFilename;
        }

        public String getTeamsFilename() {
            return teamsFilename;
        }

        @Override
        public String toString() {
            return "EventEntry{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", day1EventId='" + day1EventId + '\'' +
                    ", day2EventId='" + day2EventId + '\'' +
                    ", uniqueControls=" + uniqueControls +
                    ", teamsFilename='" + teamsFilename + '\'' +
                    '}';
        }
    }
}
