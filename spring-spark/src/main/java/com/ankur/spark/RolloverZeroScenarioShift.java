package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericData;

import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RolloverZeroScenarioShift implements OpAdj {

    @Override
    public Stream<GenericData.Record> adjust(GenericData.Record sourceRecord) {
        log.debug("Rollover zero scenario shift");
        return Stream.of(sourceRecord);
    }
}
