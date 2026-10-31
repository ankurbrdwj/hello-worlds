package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericData;

import java.io.IOException;
import java.util.List;

public interface ParquetReader {

    List<GenericData.Record> readParquet(String basePath, String fileName) throws IOException;

    List<GenericData.Record> readParquet(byte[] content) throws IOException;
}
