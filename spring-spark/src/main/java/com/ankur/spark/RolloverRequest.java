package com.ankur.spark;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString
public class RolloverRequest extends OpAdjRequest {

    private int scenarioShift;
}
