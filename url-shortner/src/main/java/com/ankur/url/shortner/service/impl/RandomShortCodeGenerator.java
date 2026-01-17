package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.service.ShortCodeGenerator;
import org.springframework.stereotype.Service;

@Service
public class RandomShortCodeGenerator implements ShortCodeGenerator {
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private long counter = 1000000; // Start from a non-obvious number

    @Override
    public synchronized String generate() {
        return encode(counter++);
    }

    private String encode(long num) {
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(BASE62.charAt((int) (num % 62)));
            num /= 62;
        }
        return sb.reverse().toString();
    }
}
