package com.ankur.ratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateControlller {
  @GetMapping("/api/test")
  public String testEndpoint() {
    return "Request successful!";
  }
}
