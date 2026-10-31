package com.ankur.spark.service.aggregation;

import org.apache.avro.generic.GenericData;

import java.util.Iterator;

public class CollectionGenericDataReader implements GenericDataReader {

    private final String name;
    private final Iterator<GenericData.Record> iterator;

    public CollectionGenericDataReader(String name, Iterable<GenericData.Record> records) {
        this.name = name;
        this.iterator = records.iterator();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GenericData.Record read() {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @Override
    public void close() {
    }
}
