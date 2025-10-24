package com.ankur.webcrawler.service.impl;

import com.ankur.webcrawler.service.ParseStrategy;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JsoupParser implements ParseStrategy {
    @Override
    public String parseWebPage(String url) throws IOException {
        return Jsoup.connect(url).timeout(5000).get().outerHtml();
    }
}
