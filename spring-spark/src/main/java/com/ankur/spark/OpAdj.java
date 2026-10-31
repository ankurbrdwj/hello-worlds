package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericData;

import java.util.stream.Stream;

public interface OpAdj {

    Stream<GenericData.Record> adjust(GenericData.Record sourceRecord);
}
