package com.maprun.results;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MapRunResultsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapRunResultsApplication.class, args);
    }
}
