package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericRecord;

import java.util.Collections;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class FullBookRolloverPredicate implements OpAdjPredicate {

    @Override
    public boolean test(@NonNull GenericRecord record) {
        return true;
    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public Set<String> missingTradeIds() {
        return Collections.emptySet();
    }
}
