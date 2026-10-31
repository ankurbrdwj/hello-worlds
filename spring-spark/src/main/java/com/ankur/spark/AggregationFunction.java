package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericData;

import java.util.List;

public interface AggregationFunction {

    GenericData.Record apply(List<String> columns, GenericData.Record baseRecord, GenericData.Record patchRecord);

}
