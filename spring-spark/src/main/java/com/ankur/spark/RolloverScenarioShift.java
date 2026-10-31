package com.ankur.spark;

import java.util.*;

import com.ankur.spark.exception.OpAdjException;

import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RolloverScenarioShift extends EditingOpAdj {

    private final int scenarioShift;

    @Override
    protected Stream<GenericData.Array<Object>> edit(GenericData.Array<Object> source, Utf8 objectType,
                                                       Function<Object, Double> valueSupplier, DoubleFunction<Object> valueConsumer) {
        // Dictionary is always length 1 => check type before length
        if (objectType != null && objectType.toString().equals(DICTIONARY)) {
            log.info("Object type is '{}', rollover is skipped", objectType);
            return Stream.of(source);
        }

        if (scenarioShift > source.size() || source.isEmpty()) {
            throw new OpAdjException(
                    String.format("Can't apply scenario shift %s to the value array of %s elements", scenarioShift, source.size()));
        }

        GenericData.Array<Object> target = new GenericData.Array<>(source.size(), source.getSchema());
        target.addAll(source.subList(scenarioShift, source.size()));

        for (int i = 0; i < scenarioShift; i++) {
            target.add(valueConsumer.apply(0.0d));
        }

        return Stream.of(target);
    }
}
