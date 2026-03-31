package com.example.scheduler;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

}

@Service
class DogAdoptionScheduler {


    @McpTool(description = """
                schedule an appointment to pickup or adopt a dog from a Pooch Palace location
            """)
    DogAdoptionSchedule schedule(
            @McpToolParam(description = "the id of the dog") int dogId,
            @McpToolParam(description = "the name of the dog") String dogName) {
        var das = new DogAdoptionSchedule(Instant.now()
                .plus(3, ChronoUnit.DAYS),
                SecurityContextHolder
                        .getContextHolderStrategy()
                        .getContext()
                        .getAuthentication()
                        .getName()
        );
        IO.println("schedulign " + dogId + '/' + dogName + " for " + das);
        return das;
    }


}

record DogAdoptionSchedule(Instant when, String user) {
}