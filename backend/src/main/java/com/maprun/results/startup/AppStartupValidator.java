package com.maprun.results.startup;

import com.maprun.results.config.EventsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validates that at least one named event is configured and that every entry
 * has all four required fields populated (id, name, day1EventId, day2EventId).
 * Logs descriptive errors and exits with code 1 if any problem is found.
 */
@Component
public class AppStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupValidator.class);

    private final EventsConfig eventsConfig;

    public AppStartupValidator(EventsConfig eventsConfig) {
        this.eventsConfig = eventsConfig;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<EventsConfig.EventEntry> events = eventsConfig.getEvents();
        boolean hasError = false;

        if (events.isEmpty()) {
            logger.error("No events are configured. Add at least one entry under 'app.events' in application.properties.");
            hasError = true;
        } else {
            for (int i = 0; i < events.size(); i++) {
                EventsConfig.EventEntry e = events.get(i);

                if (isBlank(e.getId())) {
                    logger.error("app.events[{}].id is missing or blank.", i);
                    hasError = true;
                }
                if (isBlank(e.getName())) {
                    logger.error("app.events[{}].name is missing or blank.", i);
                    hasError = true;
                }
                if (isBlank(e.getDay1EventId())) {
                    logger.error("app.events[{}].day1EventId is missing or blank (id={}).", i, e.getId());
                    hasError = true;
                }
                if (isBlank(e.getDay2EventId())) {
                    logger.error("app.events[{}].day2EventId is missing or blank (id={}).", i, e.getId());
                    hasError = true;
                }
            }
        }

        if (hasError) {
            System.exit(1);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
