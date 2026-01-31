package com.ankur.spring.schedular.service;

import com.ankur.spring.schedular.model.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class PlanService {

    private final TaskScheduler taskScheduler;

    public PlanService() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    public Alert scheduleAlertAfterMinutes(int minutes) {
        Instant scheduledTime = Instant.now().plus(minutes, ChronoUnit.MINUTES);

        Alert alert = new Alert();
        alert.setStartTime(scheduledTime);
        alert.setDuration(minutes);

        taskScheduler.schedule(() -> {
            log.info("ALERT TRIGGERED: Scheduled alert executed at {}", Instant.now());
            log.info("This alert was scheduled for {} and ran {} minutes after creation",
                    scheduledTime, minutes);
        }, scheduledTime);

        log.info("Alert scheduled to run at {} (in {} minutes)", scheduledTime, minutes);

        return alert;
    }

    public Alert scheduleAlertAtDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
        Instant scheduledTime = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

        if (scheduledTime.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Cannot schedule alert in the past. Time: " + dateTimeStr);
        }

        Alert alert = new Alert();
        alert.setStartTime(scheduledTime);

        taskScheduler.schedule(() -> {
            log.info("ALERT TRIGGERED: Scheduled alert executed at {}", Instant.now());
            log.info("This alert was scheduled for {}", scheduledTime);
        }, scheduledTime);

        log.info("Alert scheduled to run at {}", scheduledTime);

        return alert;
    }

    Alert setAlertExactTime(){
        return null;
    }

}
