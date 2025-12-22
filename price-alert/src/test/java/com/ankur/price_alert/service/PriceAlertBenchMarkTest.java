package com.ankur.price_alert.service;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PriceAlertBenchMarkTest {

    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @State(Scope.Benchmark)
    @Fork(value = 1)
    @Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
    public class AlgorithmBenchmark {

        @Param({"100", "500", "1000", "2000", "5000", "10000"})
        private int arraySize;

        private int[] array;
        private int[] sortArray;

        @Setup(Level.Trial)
        public void setUp() {
            Random random = new Random(42);
            array = new int[arraySize];
            for (int i = 0; i < arraySize; i++) {
                array[i] = random.nextInt(10000);
            }
            sortArray = array.clone();
        }

        // Example 1: O(n) - Linear Search
        @Benchmark
        public void linearSearch(Blackhole blackhole) {
            int target = array[arraySize / 2];
            int result = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == target) {
                    result = i;
                    break;
                }
            }
            blackhole.consume(result);
        }

        // Example 2: O(n²) - Bubble Sort
        @Benchmark
        public void bubbleSort(Blackhole blackhole) {
            int[] arr = array.clone();
            int n = arr.length;

            for (int i = 0; i < n - 1; i++) {
                for (int j = 0; j < n - i - 1; j++) {
                    if (arr[j] > arr[j + 1]) {
                        int temp = arr[j];
                        arr[j] = arr[j + 1];
                        arr[j + 1] = temp;
                    }
                }
            }
            blackhole.consume(arr);
        }

        // Example 3: O(n log n) - Merge Sort
        @Benchmark
        public void mergeSort(Blackhole blackhole) {
            int[] arr = array.clone();
            mergeSortHelper(arr, 0, arr.length - 1);
            blackhole.consume(arr);
        }

        private void mergeSortHelper(int[] arr, int left, int right) {
            if (left < right) {
                int mid = left + (right - left) / 2;
                mergeSortHelper(arr, left, mid);
                mergeSortHelper(arr, mid + 1, right);
                merge(arr, left, mid, right);
            }
        }

        private void merge(int[] arr, int left, int mid, int right) {
            int n1 = mid - left + 1;
            int n2 = right - mid;
        }

    }
}
