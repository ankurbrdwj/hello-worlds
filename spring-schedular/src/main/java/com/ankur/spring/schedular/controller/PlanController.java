package com.ankur.spring.schedular.controller;

import com.ankur.spring.schedular.model.Alert;
import com.ankur.spring.schedular.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/schedule/{minutes}")
    public ResponseEntity<Alert> scheduleAlert(@PathVariable int minutes) {
        Alert alert = planService.scheduleAlertAfterMinutes(minutes);
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/schedule-at")
    public ResponseEntity<Alert> scheduleAlertAtTime(@RequestBody Map<String, String> request) {
        String dateTime = request.get("dateTime");
        Alert alert = planService.scheduleAlertAtDateTime(dateTime);
        return ResponseEntity.ok(alert);
    }

}
