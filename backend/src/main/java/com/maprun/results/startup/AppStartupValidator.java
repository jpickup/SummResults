package com.maprun.results.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AppStartupValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AppStartupValidator.class);

    private final Environment environment;

    public AppStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String day1EventId = environment.getProperty("DAY1_EVENT_ID");
        String day2EventId = environment.getProperty("DAY2_EVENT_ID");

        boolean hasError = false;

        if (day1EventId == null || day1EventId.isBlank()) {
            logger.error("Required environment variable DAY1_EVENT_ID is missing or blank");
            hasError = true;
        }

        if (day2EventId == null || day2EventId.isBlank()) {
            logger.error("Required environment variable DAY2_EVENT_ID is missing or blank");
            hasError = true;
        }

        if (hasError) {
            System.exit(1);
        }
    }
}
