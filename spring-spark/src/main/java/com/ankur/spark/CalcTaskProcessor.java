package com.ankur.spark;

import java.util.*;

import com.ankur.spark.exception.RunProcessException;
import com.ankur.spark.exception.DbaxException;

public interface CalcTaskProcessor<T> {

    void processCalcTask(T compactionContext) throws RunProcessException, DbaxException;
}
