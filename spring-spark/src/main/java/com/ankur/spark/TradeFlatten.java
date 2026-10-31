package com.ankur.spark;

import java.util.*;

import com.ankur.spark.exception.OpAdjException;

import org.apache.avro.generic.GenericData;
import org.apache.avro.util.Utf8;

import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeFlatten extends EditingOpAdj {

    @Override
    protected Stream<GenericData.Array<Object>> edit(GenericData.Array<Object> source, Utf8 objectType,
                                                       Function<Object, Double> valueSupplier, DoubleFunction<Object> valueConsumer) throws OpAdjException {
        if (objectType != null && objectType.toString().equals(DICTIONARY)) {
            log.info("Object type is '{}', object is excluded from flatten", objectType);
            return Stream.empty();
        }

        GenericData.Array<Object> target = new GenericData.Array<>(source.size(), source.getSchema());
        for (Object o : source) {
            Double value = valueSupplier.apply(o);
            target.add(valueConsumer.apply(-value));
        }

        return Stream.of(target);
    }
}
