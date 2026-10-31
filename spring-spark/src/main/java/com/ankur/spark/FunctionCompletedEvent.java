package com.ankur.spark;

import com.ankur.spark.service.common.CalcTask;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class FunctionCompletedEvent implements MonitorableEvent {

    private final CalcTask calcTask;
    private final EventType eventType;

    public FunctionCompletedEvent(CalcTask calcTask, EventType eventType) {
        this.calcTask = calcTask;
        this.eventType = eventType;
    }
}
