package com.ankur.billing.period.entity;

import java.io.Serializable;
import java.time.LocalDate;

public class BillingPeriod implements Serializable {
    private final int periodNumber;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String periodId;

    public BillingPeriod() {
        this.periodNumber = 0;
        this.startDate = null;
        this.endDate = null;
        this.periodId = null;
    }

    public BillingPeriod(int periodNumber, LocalDate startDate, LocalDate endDate) {
        this.periodNumber = periodNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.periodId = startDate.getYear() + "-" + periodNumber;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getPeriodId() {
        return periodId;
    }
}
