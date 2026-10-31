package com.ankur.spark.service.aggregation;

import java.io.IOException;

@FunctionalInterface
public interface GenericDataReaderSupplier {

    GenericDataReader get() throws IOException;
}
