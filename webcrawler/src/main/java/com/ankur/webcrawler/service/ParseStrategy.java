package com.ankur.webcrawler.service;

import java.io.IOException;

public interface ParseStrategy {
     String parseWebPage(String url) throws IOException;
}
