package com.ankur.billing.period.controller;

import com.ankur.billing.period.entity.BillingPeriod;
import com.ankur.billing.period.service.BillingPeriodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class BillingPeriodController {

    private final BillingPeriodService billingPeriodService;

    public BillingPeriodController(BillingPeriodService billingPeriodService) {
        this.billingPeriodService = billingPeriodService;
    }

    @GetMapping("/periods/by-date")
    public BillingPeriod getPeriodByDate(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return billingPeriodService.getPeriodForDate(localDate);
    }

    @GetMapping("/periods")
    public List<BillingPeriod> getAllPeriods(@RequestParam int year) {
        return billingPeriodService.getAllPeriodsForYear(year);
    }
}