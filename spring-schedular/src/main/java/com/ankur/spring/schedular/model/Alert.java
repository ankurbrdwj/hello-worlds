package com.ankur.spring.schedular.model;

import lombok.Data;

import java.time.Instant;

@Data
public class Alert {
    private Instant startTime;
    private int duration;
}
