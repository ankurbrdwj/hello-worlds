package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.service.ParseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("tika")
public class TikaParser implements ParseStrategy {
    @Override
    public String parseWebPage(String url) {
        return "";
    }
}
