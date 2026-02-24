package com.ankur.billing.period.service;


import com.ankur.billing.period.entity.BillingPeriod;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingPeriodService {

    // NOTE: Caching disabled for single objects due to LinkedHashMap deserialization issue
    // See CacheConfig.java for explanation. Only caching collections works reliably.
    // @Cacheable(value = "billingPeriods", key = "#date.toString()")
    public BillingPeriod getPeriodForDate(LocalDate date) {
        System.out.println("Computing period for date: " + date);
        int year = date.getYear();
        List<LocalDate> periodStarts = calculatePeriodStarts(year);

        for (int i = 0; i < periodStarts.size(); i++) {
            BillingPeriod period = createBillingPeriod(periodStarts, i, year);
            if (!date.isBefore(period.getStartDate()) && !date.isAfter(period.getEndDate())) {
                return period;
            }
        }
        return null;
    }

    @Cacheable(value = "yearPeriods", key = "#year")
    public List<BillingPeriod> getAllPeriodsForYear(int year) {
        System.out.println("CACHE MISS! Computing all periods for year: " + year);
        List<LocalDate> periodStarts = calculatePeriodStarts(year);
        List<BillingPeriod> periods = new ArrayList<>();

        for (int i = 0; i < periodStarts.size(); i++) {
            periods.add(createBillingPeriod(periodStarts, i, year));
        }
        return periods;
    }

    private BillingPeriod createBillingPeriod(List<LocalDate> periodStarts, int index, int year) {
        LocalDate periodStart = periodStarts.get(index);
        LocalDate periodEnd = (index < periodStarts.size() - 1)
            ? periodStarts.get(index + 1).minusDays(1)
            : LocalDate.of(year, 12, 31);

        return new BillingPeriod(index + 1, periodStart, periodEnd);
    }

    private List<LocalDate> calculatePeriodStarts(int year) {
        List<LocalDate> starts = new ArrayList<>();
        LocalDate current = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);

        starts.add(current);
        current = current.plusDays(1);

        while (!current.isAfter(endOfYear)) {
            if (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfMonth() == 1) {
                starts.add(current);
            }
            current = current.plusDays(1);
        }

        return starts;
    }
}