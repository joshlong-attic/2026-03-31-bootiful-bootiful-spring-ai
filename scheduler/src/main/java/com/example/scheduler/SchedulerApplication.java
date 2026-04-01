package com.example.scheduler;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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
        var authentication = Objects.requireNonNull(SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .getAuthentication());
        var instant = Instant.now().plus(3, ChronoUnit.DAYS);
        var das = new DogAdoptionSchedule(instant, authentication.getName());
        IO.println("scheduling " + dogId + '/' + dogName + " for " + das);
        return das;
    }


}

record DogAdoptionSchedule(Instant when, String user) {
}