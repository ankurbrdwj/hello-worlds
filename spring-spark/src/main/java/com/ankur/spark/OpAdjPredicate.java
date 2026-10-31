package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericRecord;

import java.util.Set;
import java.util.function.Predicate;

public interface OpAdjPredicate extends Predicate<GenericRecord> {

    boolean hasError();

    Set<String> missingTradeIds();
}
